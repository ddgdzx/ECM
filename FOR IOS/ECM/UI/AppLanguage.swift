import SwiftUI

enum AppLanguage: String, CaseIterable, Identifiable {
    case simplifiedChinese = "zh-Hans"
    case traditionalChinese = "zh-Hant"
    case english = "en"
    case spanish = "es"
    case german = "de"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .simplifiedChinese: return "简体中文"
        case .traditionalChinese: return "繁體中文"
        case .english: return "English"
        case .spanish: return "Español"
        case .german: return "Deutsch"
        }
    }
}

private struct AppLanguageKey: EnvironmentKey {
    static let defaultValue = AppLanguage.simplifiedChinese
}

extension EnvironmentValues {
    var appLanguage: AppLanguage {
        get { self[AppLanguageKey.self] }
        set { self[AppLanguageKey.self] = newValue }
    }
}

enum AppCopy {
    static func text(_ key: String, _ language: AppLanguage) -> String {
        let values: [String: [AppLanguage: String]] = [
            "inventory": [.simplifiedChinese: "元件库", .traditionalChinese: "元件庫", .english: "Components", .spanish: "Componentes", .german: "Bauteile"],
            "storage": [.simplifiedChinese: "存储", .traditionalChinese: "儲存", .english: "Storage", .spanish: "Almacén", .german: "Lager"],
            "overview": [.simplifiedChinese: "概览", .traditionalChinese: "概覽", .english: "Overview", .spanish: "Resumen", .german: "Übersicht"],
            "settings": [.simplifiedChinese: "设置", .traditionalChinese: "設定", .english: "Settings", .spanish: "Ajustes", .german: "Einstellungen"],
            "quick_consume": [.simplifiedChinese: "快速登记消耗", .traditionalChinese: "快速登記消耗", .english: "Quick consumption", .spanish: "Registrar consumo", .german: "Verbrauch buchen"],
            "quick_consume_hint": [.simplifiedChinese: "选择元件并记录用途", .traditionalChinese: "選擇元件並記錄用途", .english: "Select a component and record its use", .spanish: "Elige un componente y registra el uso", .german: "Bauteil wählen und Zweck erfassen"],
            "start": [.simplifiedChinese: "开始", .traditionalChinese: "開始", .english: "Start", .spanish: "Empezar", .german: "Start"],
            "consume_title": [.simplifiedChinese: "登记消耗", .traditionalChinese: "登記消耗", .english: "Record consumption", .spanish: "Registrar consumo", .german: "Verbrauch erfassen"],
            "choose_component": [.simplifiedChinese: "选择元件", .traditionalChinese: "選擇元件", .english: "Choose component", .spanish: "Elegir componente", .german: "Bauteil wählen"],
            "search_component": [.simplifiedChinese: "搜索元件", .traditionalChinese: "搜尋元件", .english: "Search components", .spanish: "Buscar componentes", .german: "Bauteile suchen"],
            "consume_quantity": [.simplifiedChinese: "消耗数量", .traditionalChinese: "消耗數量", .english: "Quantity used", .spanish: "Cantidad usada", .german: "Verbrauchsmenge"],
            "remaining": [.simplifiedChinese: "消耗后剩余", .traditionalChinese: "消耗後剩餘", .english: "Remaining after use", .spanish: "Restante después", .german: "Restbestand"],
            "consume_detail": [.simplifiedChinese: "消耗明细（必填）", .traditionalChinese: "消耗明細（必填）", .english: "Usage details (required)", .spanish: "Detalle de uso (obligatorio)", .german: "Verwendungsdetails (erforderlich)"],
            "consume_placeholder": [.simplifiedChinese: "请输入消耗用途、项目或备注…", .traditionalChinese: "請輸入消耗用途、專案或備註…", .english: "Enter purpose, project, or notes…", .spanish: "Indica el uso, proyecto o notas…", .german: "Zweck, Projekt oder Notiz eingeben…"],
            "cancel": [.simplifiedChinese: "取消", .traditionalChinese: "取消", .english: "Cancel", .spanish: "Cancelar", .german: "Abbrechen"],
            "record": [.simplifiedChinese: "登记", .traditionalChinese: "登記", .english: "Record", .spanish: "Registrar", .german: "Buchen"],
            "preferences": [.simplifiedChinese: "偏好设置", .traditionalChinese: "偏好設定", .english: "Preferences", .spanish: "Preferencias", .german: "Präferenzen"],
            "preferences_hint": [.simplifiedChinese: "自定义应用偏好与显示设置", .traditionalChinese: "自訂應用程式偏好與顯示設定", .english: "Customize app and display preferences", .spanish: "Personaliza la aplicación y la pantalla", .german: "App- und Anzeigeeinstellungen anpassen"],
            "language": [.simplifiedChinese: "语言", .traditionalChinese: "語言", .english: "Language", .spanish: "Idioma", .german: "Sprache"],
            "appearance": [.simplifiedChinese: "外观", .traditionalChinese: "外觀", .english: "Appearance", .spanish: "Apariencia", .german: "Darstellung"],
            "follow_system": [.simplifiedChinese: "跟随系统", .traditionalChinese: "跟隨系統", .english: "Follow system", .spanish: "Seguir sistema", .german: "Systemeinstellung"],
            "about": [.simplifiedChinese: "关于", .traditionalChinese: "關於", .english: "About", .spanish: "Acerca de", .german: "Über"],
            "local_data": [.simplifiedChinese: "数据保留本地副本，连接后同步到 NAS", .traditionalChinese: "資料保留本機副本，連線後同步至 NAS", .english: "Data stays available locally and syncs to NAS", .spanish: "Los datos se guardan localmente y se sincronizan con el NAS", .german: "Daten bleiben lokal verfügbar und werden mit dem NAS synchronisiert"],
            "nas_sync": [.simplifiedChinese: "NAS 数据同步", .traditionalChinese: "NAS 資料同步", .english: "NAS data sync", .spanish: "Sincronización NAS", .german: "NAS-Datensynchronisierung"],
            "nas_server": [.simplifiedChinese: "服务器", .traditionalChinese: "伺服器", .english: "Server", .spanish: "Servidor", .german: "Server"],
            "nas_account": [.simplifiedChinese: "账户", .traditionalChinese: "帳戶", .english: "Account", .spanish: "Cuenta", .german: "Konto"],
            "nas_password": [.simplifiedChinese: "输入 NAS 密码", .traditionalChinese: "輸入 NAS 密碼", .english: "Enter NAS password", .spanish: "Introduce la contraseña del NAS", .german: "NAS-Passwort eingeben"],
            "nas_connect": [.simplifiedChinese: "连接并同步", .traditionalChinese: "連線並同步", .english: "Connect and sync", .spanish: "Conectar y sincronizar", .german: "Verbinden und synchronisieren"],
            "nas_status": [.simplifiedChinese: "状态", .traditionalChinese: "狀態", .english: "Status", .spanish: "Estado", .german: "Status"],
            "nas_not_connected": [.simplifiedChinese: "未连接", .traditionalChinese: "未連線", .english: "Not connected", .spanish: "Sin conexión", .german: "Nicht verbunden"],
            "nas_syncing": [.simplifiedChinese: "正在同步…", .traditionalChinese: "正在同步…", .english: "Syncing…", .spanish: "Sincronizando…", .german: "Synchronisieren…"],
            "nas_synced": [.simplifiedChinese: "已同步", .traditionalChinese: "已同步", .english: "Synced", .spanish: "Sincronizado", .german: "Synchronisiert"],
            "nas_failed": [.simplifiedChinese: "同步失败，请检查密码和网络", .traditionalChinese: "同步失敗，請檢查密碼與網路", .english: "Sync failed. Check the password and network", .spanish: "Error de sincronización. Comprueba la contraseña y la red", .german: "Synchronisierung fehlgeschlagen. Passwort und Netzwerk prüfen"],
            "nas_download": [.simplifiedChinese: "从 NAS 获取最新数据", .traditionalChinese: "從 NAS 取得最新資料", .english: "Get latest data from NAS", .spanish: "Obtener datos del NAS", .german: "Neueste Daten vom NAS laden"],
            "nas_upload": [.simplifiedChinese: "立即备份到 NAS", .traditionalChinese: "立即備份至 NAS", .english: "Back up to NAS now", .spanish: "Crear copia en el NAS", .german: "Jetzt auf NAS sichern"],
            "nas_disconnect": [.simplifiedChinese: "断开 NAS", .traditionalChinese: "中斷 NAS 連線", .english: "Disconnect NAS", .spanish: "Desconectar NAS", .german: "NAS trennen"],
            "nas_footer": [.simplifiedChinese: "密码仅保存在本机安全存储中。连接后修改会自动备份，断网时仍可使用本地数据。", .traditionalChinese: "密碼僅儲存在本機安全儲存空間。連線後修改會自動備份，離線時仍可使用本機資料。", .english: "The password stays in secure device storage. Changes sync automatically; local data remains available offline.", .spanish: "La contraseña se guarda de forma segura en el dispositivo. Los cambios se sincronizan automáticamente y los datos siguen disponibles sin conexión.", .german: "Das Passwort bleibt im sicheren Gerätespeicher. Änderungen werden automatisch synchronisiert; lokale Daten bleiben offline verfügbar."]
        ]
        return values[key]?[language] ?? values[key]?[.simplifiedChinese] ?? key
    }
}
