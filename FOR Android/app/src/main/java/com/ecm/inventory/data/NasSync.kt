package com.ecm.inventory.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class EcmSnapshot(
    val schemaVersion: Int = 2,
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
            val raw = serverAddress.trim()
            val candidate = if (raw.contains("://")) raw else "https://$raw"
            val parsed = runCatching { URI(candidate) }.getOrNull() ?: return null
            val host = parsed.host?.takeIf { it.isNotBlank() } ?: return null
            val scheme = parsed.scheme?.lowercase()?.takeIf { it == "https" } ?: return null
            val rootPath = parsed.path.orEmpty().trim('/').let { if (it.isBlank()) "" else "/$it" }
            return NasConfiguration(
                host = host,
                username = username,
                password = password,
                fileUrl = URI(scheme, null, host, port, "$rootPath/ArxanECM/ecm-data.json", null, null).toString(),
                directoryUrl = URI(scheme, null, host, port, "$rootPath/ArxanECM/", null, null).toString()
            )
        }
}

data class NasConfiguration(
    val host: String,
    val username: String,
    val password: String,
    val fileUrl: String,
    val directoryUrl: String
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

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .sslSocketFactory(sslSocketFactory, trustManager)
        .hostnameVerifier { _, _ -> true }
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun request(method: String, url: String, configuration: NasConfiguration, body: ByteArray? = null): Request {
        val requestBody = body?.toRequestBody("application/json; charset=utf-8".toMediaType())
        return Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(configuration.username, configuration.password))
            .header("Accept", "application/json")
            .method(method, requestBody)
            .build()
    }

    private fun ensureDirectory(configuration: NasConfiguration) {
        client.newCall(request("MKCOL", configuration.directoryUrl, configuration)).execute().use { response ->
            if (response.code != 201 && response.code != 405) error("HTTP ${response.code}")
        }
    }

    fun upload(snapshot: EcmSnapshot, configuration: NasConfiguration) {
        ensureDirectory(configuration)
        val body = snapshot.toJson().toString().toByteArray()
        client.newCall(request("PUT", configuration.fileUrl, configuration, body)).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
        }
    }

    fun download(configuration: NasConfiguration): EcmSnapshot? {
        return client.newCall(request("GET", configuration.fileUrl, configuration)).execute().use { response ->
            when {
                response.code == 404 -> null
                response.code == 401 || response.code == 403 -> error("unauthorized")
                response.isSuccessful -> snapshotFromJson(JSONObject(response.body?.string().orEmpty()))
                else -> error("HTTP ${response.code}")
            }
        }
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
    put("slotsData", slotsData)
    put("photoData", photoData?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: JSONObject.NULL)
    put("note", note); put("updatedAt", updatedAt)
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
        row = item.getInt("row"), col = item.getInt("col"), slotsData = item.optString("slotsData", ""),
        photoData = item.optString("photoData").takeIf { it.isNotBlank() }
            ?.let { encoded -> runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull() },
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
