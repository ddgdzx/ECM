package com.ecm.inventory.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URI
import java.util.Base64
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class EcmSnapshot(
    val schemaVersion: Int = 1,
    val modifiedAt: Long,
    val components: List<ComponentEntity>,
    val locations: List<LocationEntity>,
    val consumptions: List<ConsumptionEntity>
)

sealed interface NasSyncState {
    data object NotConfigured : NasSyncState
    data object Syncing : NasSyncState
    data object Synced : NasSyncState
    data class Failed(val message: String) : NasSyncState
}

class NasCredentials(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "arxan_ecm_nas",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var password: String?
        get() = preferences.getString("password", null)
        set(value) {
            preferences.edit().apply {
                if (value.isNullOrBlank()) remove("password") else putString("password", value)
            }.apply()
        }

    var serverAddress: String
        get() = preferences.getString("server_address", "").orEmpty()
        set(value) { preferences.edit().putString("server_address", value.trim()).apply() }

    var port: Int
        get() = preferences.getInt("port", 5006)
        set(value) { preferences.edit().putInt("port", value).apply() }

    var username: String
        get() = preferences.getString("username", "").orEmpty()
        set(value) { preferences.edit().putString("username", value.trim()).apply() }

    var localModifiedAt: Long
        get() = preferences.getLong("local_modified_at", 0)
        set(value) { preferences.edit().putLong("local_modified_at", value).apply() }

    val configuration: NasConfiguration?
        get() {
            val password = password ?: return null
            if (username.isBlank() || port !in 1..65535) return null
            val candidate = serverAddress.trim().let { if (it.contains("://")) it else "https://$it" }
            val host = runCatching { URI(candidate).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
            return NasConfiguration(
                host = host,
                username = username,
                password = password,
                fileUrl = URI("https", null, host, port, "/file/ArxanECM/ecm-data.json", null, null).toURL(),
                directoryUrl = URI("https", null, host, port, "/file/ArxanECM/", null, null).toURL()
            )
        }
}

data class NasConfiguration(
    val host: String,
    val username: String,
    val password: String,
    val fileUrl: URL,
    val directoryUrl: URL
)

class NasSyncClient {
    private val trustManager = object : X509TrustManager {
        override fun getAcceptedIssuers() = emptyArray<java.security.cert.X509Certificate>()
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
    }
    private val sslSocketFactory = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf<TrustManager>(trustManager), java.security.SecureRandom())
    }.socketFactory

    private fun connection(method: String, url: URL, configuration: NasConfiguration): HttpsURLConnection {
        return (url.openConnection() as HttpsURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            sslSocketFactory = this@NasSyncClient.sslSocketFactory
            hostnameVerifier = HostnameVerifier { host, _ -> host == configuration.host }
            setRequestProperty("Content-Type", "application/json")
            val token = Base64.getEncoder().encodeToString("${configuration.username}:${configuration.password}".toByteArray())
            setRequestProperty("Authorization", "Basic $token")
        }
    }

    private fun ensureDirectory(configuration: NasConfiguration) {
        val connection = connection("MKCOL", configuration.directoryUrl, configuration)
        val code = connection.responseCode
        connection.disconnect()
        if (code != 201 && code != 405) error("HTTP $code")
    }

    fun upload(snapshot: EcmSnapshot, configuration: NasConfiguration) {
        ensureDirectory(configuration)
        val connection = connection("PUT", configuration.fileUrl, configuration).apply { doOutput = true }
        connection.outputStream.use { it.write(snapshot.toJson().toString().toByteArray()) }
        if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
        connection.disconnect()
    }

    fun download(configuration: NasConfiguration): EcmSnapshot? {
        val connection = connection("GET", configuration.fileUrl, configuration)
        return when (val code = connection.responseCode) {
            404 -> null
            in 200..299 -> connection.inputStream.bufferedReader().use { snapshotFromJson(JSONObject(it.readText())) }
            HttpURLConnection.HTTP_UNAUTHORIZED -> error("unauthorized")
            else -> error("HTTP $code")
        }.also { connection.disconnect() }
    }
}

private fun EcmSnapshot.toJson() = JSONObject().apply {
    put("schemaVersion", schemaVersion)
    put("modifiedAt", modifiedAt)
    put("components", JSONArray().apply { components.forEach { put(it.toJson()) } })
    put("locations", JSONArray().apply { locations.forEach { put(it.toJson()) } })
    put("consumptions", JSONArray().apply { consumptions.forEach { put(it.toJson()) } })
}

private fun ComponentEntity.toJson() = JSONObject().apply {
    put("id", id); put("type", type); put("model", model); put("value", value); put("packageSpec", packageSpec)
    put("quantity", quantity); put("minQuantity", minQuantity); put("unit", unit)
    put("locationId", locationId ?: JSONObject.NULL); put("layer", layer); put("row", row); put("col", col)
    put("slotsData", slotsData); put("note", note); put("updatedAt", updatedAt)
}

private fun LocationEntity.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("kind", kind); put("layers", layers); put("rows", rows); put("cols", cols)
    put("note", note); put("createdAt", createdAt)
}

private fun ConsumptionEntity.toJson() = JSONObject().apply {
    put("id", id); put("componentId", componentId); put("quantity", quantity); put("detail", detail)
    put("consumedAt", consumedAt); put("stockAfter", stockAfter)
}

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }

private fun snapshotFromJson(json: JSONObject) = EcmSnapshot(
    schemaVersion = json.optInt("schemaVersion", 1), modifiedAt = json.getLong("modifiedAt"),
    components = json.getJSONArray("components").objects().map { item -> ComponentEntity(
        id = item.getLong("id"), type = item.getString("type"), model = item.getString("model"),
        value = item.getString("value"), packageSpec = item.getString("packageSpec"), quantity = item.getInt("quantity"),
        minQuantity = item.getInt("minQuantity"), unit = item.getString("unit"),
        locationId = if (item.isNull("locationId")) null else item.getLong("locationId"), layer = item.getInt("layer"),
        row = item.getInt("row"), col = item.getInt("col"), slotsData = item.getString("slotsData"),
        note = item.getString("note"), updatedAt = item.getLong("updatedAt")
    ) },
    locations = json.getJSONArray("locations").objects().map { item -> LocationEntity(
        id = item.getLong("id"), name = item.getString("name"), kind = item.getString("kind"),
        layers = item.getInt("layers"), rows = item.getInt("rows"), cols = item.getInt("cols"),
        note = item.getString("note"), createdAt = item.getLong("createdAt")
    ) },
    consumptions = json.getJSONArray("consumptions").objects().map { item -> ConsumptionEntity(
        id = item.getLong("id"), componentId = item.getLong("componentId"), quantity = item.getInt("quantity"),
        detail = item.getString("detail"), consumedAt = item.getLong("consumedAt"), stockAfter = item.getInt("stockAfter")
    ) }
)
