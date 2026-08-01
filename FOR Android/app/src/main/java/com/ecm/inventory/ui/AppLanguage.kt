package com.ecm.inventory.ui

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String) {
    SIMPLIFIED_CHINESE("zh-Hans", "简体中文"),
    TRADITIONAL_CHINESE("zh-Hant", "繁體中文"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    GERMAN("de", "Deutsch");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: SIMPLIFIED_CHINESE
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.SIMPLIFIED_CHINESE }

private val copy = mapOf(
    "inventory" to listOf("元件库", "元件庫", "Components", "Componentes", "Bauteile"),
    "storage" to listOf("存储", "儲存", "Storage", "Almacén", "Lager"),
    "overview" to listOf("概览", "概覽", "Overview", "Resumen", "Übersicht"),
    "settings" to listOf("设置", "設定", "Settings", "Ajustes", "Einstellungen"),
    "quick_consume" to listOf("快速登记消耗", "快速登記消耗", "Quick consumption", "Registrar consumo", "Verbrauch buchen"),
    "quick_consume_hint" to listOf("选择元件并记录用途", "選擇元件並記錄用途", "Select a component and record its use", "Elige un componente y registra el uso", "Bauteil wählen und Zweck erfassen"),
    "start" to listOf("开始", "開始", "Start", "Empezar", "Start"),
    "consume_title" to listOf("登记消耗", "登記消耗", "Record consumption", "Registrar consumo", "Verbrauch erfassen"),
    "choose_component" to listOf("选择元件", "選擇元件", "Choose component", "Elegir componente", "Bauteil wählen"),
    "search_component" to listOf("搜索元件", "搜尋元件", "Search components", "Buscar componentes", "Bauteile suchen"),
    "consume_quantity" to listOf("消耗数量", "消耗數量", "Quantity used", "Cantidad usada", "Verbrauchsmenge"),
    "remaining" to listOf("消耗后剩余", "消耗後剩餘", "Remaining after use", "Restante después", "Restbestand"),
    "consume_detail" to listOf("消耗明细（必填）", "消耗明細（必填）", "Usage details (required)", "Detalle de uso (obligatorio)", "Verwendungsdetails (erforderlich)"),
    "consume_placeholder" to listOf("请输入消耗用途、项目或备注…", "請輸入消耗用途、專案或備註…", "Enter purpose, project, or notes…", "Indica el uso, proyecto o notas…", "Zweck, Projekt oder Notiz eingeben…"),
    "cancel" to listOf("取消", "取消", "Cancel", "Cancelar", "Abbrechen"),
    "record" to listOf("登记", "登記", "Record", "Registrar", "Buchen"),
    "preferences" to listOf("偏好设置", "偏好設定", "Preferences", "Preferencias", "Präferenzen"),
    "preferences_hint" to listOf("自定义应用偏好与显示设置", "自訂應用程式偏好與顯示設定", "Customize app and display preferences", "Personaliza la aplicación y la pantalla", "App- und Anzeigeeinstellungen anpassen"),
    "language" to listOf("语言", "語言", "Language", "Idioma", "Sprache"),
    "appearance" to listOf("外观", "外觀", "Appearance", "Apariencia", "Darstellung"),
    "follow_system" to listOf("跟随系统", "跟隨系統", "Follow system", "Seguir sistema", "Systemeinstellung"),
    "about" to listOf("关于", "關於", "About", "Acerca de", "Über"),
    "local_data" to listOf("数据保留本地副本，连接后同步到 NAS", "資料保留本機副本，連線後同步至 NAS", "Data stays available locally and syncs to NAS", "Los datos se guardan localmente y se sincronizan con el NAS", "Daten bleiben lokal verfügbar und werden mit dem NAS synchronisiert"),
    "nas_sync" to listOf("NAS 数据同步", "NAS 資料同步", "NAS data sync", "Sincronización NAS", "NAS-Datensynchronisierung"),
    "nas_server" to listOf("服务器", "伺服器", "Server", "Servidor", "Server"),
    "nas_port" to listOf("WebDAV 端口", "WebDAV 連接埠", "WebDAV port", "Puerto WebDAV", "WebDAV-Port"),
    "nas_account" to listOf("账户", "帳戶", "Account", "Cuenta", "Konto"),
    "nas_password" to listOf("输入 NAS 密码", "輸入 NAS 密碼", "Enter NAS password", "Introduce la contraseña del NAS", "NAS-Passwort eingeben"),
    "nas_connect" to listOf("连接并同步", "連線並同步", "Connect and sync", "Conectar y sincronizar", "Verbinden und synchronisieren"),
    "nas_status" to listOf("状态", "狀態", "Status", "Estado", "Status"),
    "nas_not_connected" to listOf("未连接", "未連線", "Not connected", "Sin conexión", "Nicht verbunden"),
    "nas_syncing" to listOf("正在同步…", "正在同步…", "Syncing…", "Sincronizando…", "Synchronisieren…"),
    "nas_synced" to listOf("已同步", "已同步", "Synced", "Sincronizado", "Synchronisiert"),
    "nas_failed" to listOf("同步失败，请检查密码和网络", "同步失敗，請檢查密碼與網路", "Sync failed. Check the password and network", "Error de sincronización. Comprueba la contraseña y la red", "Synchronisierung fehlgeschlagen. Passwort und Netzwerk prüfen"),
    "nas_download" to listOf("从 NAS 获取最新数据", "從 NAS 取得最新資料", "Get latest data from NAS", "Obtener datos del NAS", "Neueste Daten vom NAS laden"),
    "nas_upload" to listOf("立即备份到 NAS", "立即備份至 NAS", "Back up to NAS now", "Crear copia en el NAS", "Jetzt auf NAS sichern"),
    "nas_disconnect" to listOf("断开 NAS", "中斷 NAS 連線", "Disconnect NAS", "Desconectar NAS", "NAS trennen"),
    "nas_footer" to listOf("密码仅保存在本机安全存储中。连接后修改会自动备份，断网时仍可使用本地数据。", "密碼僅儲存在本機安全儲存空間。連線後修改會自動備份，離線時仍可使用本機資料。", "The password stays in secure device storage. Changes sync automatically; local data remains available offline.", "La contraseña se guarda de forma segura en el dispositivo. Los cambios se sincronizan automáticamente y los datos siguen disponibles sin conexión.", "Das Passwort bleibt im sicheren Gerätespeicher. Änderungen werden automatisch synchronisiert; lokale Daten bleiben offline verfügbar.")
)

fun appText(key: String, language: AppLanguage): String =
    copy[key]?.getOrNull(language.ordinal) ?: copy[key]?.firstOrNull() ?: key
