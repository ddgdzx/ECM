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
            "storage_positions": [.simplifiedChinese: "存储位置", .traditionalChinese: "儲存位置", .english: "Storage locations", .spanish: "Ubicaciones", .german: "Lagerplätze"],
            "items_summary": [.simplifiedChinese: "%d 个条目 · 合计 %d 件", .traditionalChinese: "%d 個項目 · 合計 %d 件", .english: "%d items · %d total units", .spanish: "%d artículos · %d unidades en total", .german: "%d Einträge · insgesamt %d Stück"],
            "all": [.simplifiedChinese: "全部", .traditionalChinese: "全部", .english: "All", .spanish: "Todos", .german: "Alle"],
            "low_stock": [.simplifiedChinese: "库存偏低", .traditionalChinese: "庫存偏低", .english: "Low stock", .spanish: "Existencias bajas", .german: "Niedriger Bestand"],
            "low_stock_count": [.simplifiedChinese: "库存偏低 %d", .traditionalChinese: "庫存偏低 %d", .english: "Low stock %d", .spanish: "Existencias bajas %d", .german: "Niedriger Bestand %d"],
            "sort_recent": [.simplifiedChinese: "最近更新", .traditionalChinese: "最近更新", .english: "Recently updated", .spanish: "Actualización reciente", .german: "Zuletzt aktualisiert"],
            "sort_name": [.simplifiedChinese: "型号", .traditionalChinese: "型號", .english: "Model", .spanish: "Modelo", .german: "Modell"],
            "sort_quantity": [.simplifiedChinese: "库存数量", .traditionalChinese: "庫存數量", .english: "Stock quantity", .spanish: "Cantidad", .german: "Bestandsmenge"],
            "search_inventory": [.simplifiedChinese: "搜索型号、参数、封装", .traditionalChinese: "搜尋型號、參數、封裝", .english: "Search model, value, or package", .spanish: "Buscar modelo, valor o encapsulado", .german: "Modell, Wert oder Gehäuse suchen"],
            "add_component": [.simplifiedChinese: "添加元件", .traditionalChinese: "新增元件", .english: "Add component", .spanish: "Añadir componente", .german: "Bauteil hinzufügen"],
            "component_photo": [.simplifiedChinese: "元件照片", .traditionalChinese: "元件照片", .english: "Component photo", .spanish: "Foto del componente", .german: "Bauteilfoto"],
            "take_photo": [.simplifiedChinese: "拍摄照片", .traditionalChinese: "拍攝照片", .english: "Take photo", .spanish: "Tomar foto", .german: "Foto aufnehmen"],
            "choose_photo": [.simplifiedChinese: "从相册选择", .traditionalChinese: "從相簿選擇", .english: "Choose from library", .spanish: "Elegir de la galería", .german: "Aus Mediathek wählen"],
            "remove_photo": [.simplifiedChinese: "移除照片", .traditionalChinese: "移除照片", .english: "Remove photo", .spanish: "Eliminar foto", .german: "Foto entfernen"],
            "photo_processing": [.simplifiedChinese: "正在自动抠图…", .traditionalChinese: "正在自動去背…", .english: "Removing background…", .spanish: "Eliminando el fondo…", .german: "Hintergrund wird entfernt…"],
            "photo_hint": [.simplifiedChinese: "建议在光线充足、背景简洁的环境拍摄。图片会自动抠图并随元件资料保存。", .traditionalChinese: "建議在光線充足、背景簡潔的環境拍攝。圖片會自動去背並隨元件資料儲存。", .english: "Use good lighting and a simple background. The background is removed automatically and the image is saved with the component.", .spanish: "Usa buena iluminación y un fondo sencillo. El fondo se elimina automáticamente y la imagen se guarda con el componente.", .german: "Bei gutem Licht vor einfachem Hintergrund fotografieren. Der Hintergrund wird automatisch entfernt und das Bild mit dem Bauteil gespeichert."],
            "photo_failed": [.simplifiedChinese: "自动抠图暂不可用，已保留处理后的原图。", .traditionalChinese: "自動去背暫時無法使用，已保留處理後的原圖。", .english: "Background removal is unavailable; the processed original was kept.", .spanish: "No se pudo eliminar el fondo; se conservó la imagen procesada.", .german: "Die Hintergrundentfernung ist nicht verfügbar; das bearbeitete Original wurde beibehalten."],
            "empty_components": [.simplifiedChinese: "还没有元件", .traditionalChinese: "尚無元件", .english: "No components yet", .spanish: "Aún no hay componentes", .german: "Noch keine Bauteile"],
            "no_results": [.simplifiedChinese: "没有匹配的结果", .traditionalChinese: "沒有符合的結果", .english: "No matching results", .spanish: "No hay resultados", .german: "Keine passenden Ergebnisse"],
            "empty_components_hint": [.simplifiedChinese: "点击右上角 + 添加第一颗元件，登记型号、数量和存放位置。", .traditionalChinese: "點擊右上角 + 新增第一個元件，登記型號、數量和存放位置。", .english: "Tap + to add your first component with its model, quantity, and location.", .spanish: "Pulsa + para añadir el primer componente con modelo, cantidad y ubicación.", .german: "Tippe auf +, um das erste Bauteil mit Modell, Menge und Lagerort anzulegen."],
            "no_results_hint": [.simplifiedChinese: "换个关键词，或清除类型筛选试试。", .traditionalChinese: "更換關鍵字，或清除類型篩選。", .english: "Try another keyword or clear the type filter.", .spanish: "Prueba otra palabra o elimina el filtro de tipo.", .german: "Versuche einen anderen Suchbegriff oder entferne den Typfilter."],
            "unassigned_location": [.simplifiedChinese: "未分配位置", .traditionalChinese: "未分配位置", .english: "No location", .spanish: "Sin ubicación", .german: "Kein Lagerort"],
            "locations_summary": [.simplifiedChinese: "%d 个容器 · %d 个格口", .traditionalChinese: "%d 個容器 · %d 個格口", .english: "%d containers · %d slots", .spanish: "%d contenedores · %d espacios", .german: "%d Behälter · %d Fächer"],
            "new_location": [.simplifiedChinese: "新建存储位置", .traditionalChinese: "新增儲存位置", .english: "New storage location", .spanish: "Nueva ubicación", .german: "Neuer Lagerort"],
            "empty_locations": [.simplifiedChinese: "还没有存储位置", .traditionalChinese: "尚無儲存位置", .english: "No storage locations yet", .spanish: "Aún no hay ubicaciones", .german: "Noch keine Lagerorte"],
            "empty_locations_hint": [.simplifiedChinese: "先建一个元件柜或贴片盒，设定层/行/列，之后就能把元件放进具体格口。", .traditionalChinese: "先建立元件櫃或貼片盒並設定層／行／列，之後即可把元件放入指定格口。", .english: "Create a cabinet or component box, set its layers, rows, and columns, then assign components to slots.", .spanish: "Crea un armario o caja, define capas, filas y columnas y asigna componentes a los espacios.", .german: "Lege einen Schrank oder eine Box mit Ebenen, Reihen und Spalten an und weise Bauteile den Fächern zu."],
            "location_dimensions": [.simplifiedChinese: "%@ · %d层 × %d行 × %d列", .traditionalChinese: "%@ · %d層 × %d行 × %d列", .english: "%@ · %d layers × %d rows × %d columns", .spanish: "%@ · %d capas × %d filas × %d columnas", .german: "%@ · %d Ebenen × %d Reihen × %d Spalten"],
            "types_count": [.simplifiedChinese: "%d 种", .traditionalChinese: "%d 種", .english: "%d types", .spanish: "%d tipos", .german: "%d Typen"],
            "slots_used": [.simplifiedChinese: "已用 %d/%d", .traditionalChinese: "已用 %d/%d", .english: "%d/%d used", .spanish: "%d/%d usados", .german: "%d/%d belegt"],
            "component_types": [.simplifiedChinese: "元件种类", .traditionalChinese: "元件種類", .english: "Component types", .spanish: "Tipos de componentes", .german: "Bauteilarten"],
            "total_stock": [.simplifiedChinese: "库存总数", .traditionalChinese: "庫存總數", .english: "Total stock", .spanish: "Existencias totales", .german: "Gesamtbestand"],
            "slot_usage": [.simplifiedChinese: "格口占用", .traditionalChinese: "格口佔用", .english: "Slot usage", .spanish: "Uso de espacios", .german: "Fachbelegung"],
            "restock_needed": [.simplifiedChinese: "需要补货", .traditionalChinese: "需要補貨", .english: "Restock needed", .spanish: "Reponer existencias", .german: "Nachbestellen"],
            "restock_hint": [.simplifiedChinese: "数量低于预警值的元件会出现在这里。", .traditionalChinese: "數量低於預警值的元件會顯示在此。", .english: "Components below their warning level appear here.", .spanish: "Aquí aparecen los componentes por debajo del nivel de alerta.", .german: "Bauteile unterhalb des Warnbestands erscheinen hier."],
            "type_distribution": [.simplifiedChinese: "类型分布", .traditionalChinese: "類型分佈", .english: "Type distribution", .spanish: "Distribución por tipo", .german: "Typverteilung"],
            "unassigned_count": [.simplifiedChinese: "未分配位置（%d）", .traditionalChinese: "未分配位置（%d）", .english: "No location (%d)", .spanish: "Sin ubicación (%d)", .german: "Kein Lagerort (%d)"],
            "data_storage_note": [.simplifiedChinese: "数据优先保存在本机；配置 NAS 后会自动同步。", .traditionalChinese: "資料優先儲存在本機；設定 NAS 後會自動同步。", .english: "Data is stored locally first and syncs automatically after NAS setup.", .spanish: "Los datos se guardan primero en el dispositivo y se sincronizan al configurar el NAS.", .german: "Daten werden zuerst lokal gespeichert und nach der NAS-Einrichtung automatisch synchronisiert."],
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
            "nas_server": [.simplifiedChinese: "服务器或 WebDAV 地址", .traditionalChinese: "伺服器或 WebDAV 位址", .english: "Server or WebDAV address", .spanish: "Servidor o dirección WebDAV", .german: "Server- oder WebDAV-Adresse"],
            "nas_port": [.simplifiedChinese: "WebDAV 端口", .traditionalChinese: "WebDAV 連接埠", .english: "WebDAV port", .spanish: "Puerto WebDAV", .german: "WebDAV-Port"],
            "nas_account": [.simplifiedChinese: "账户", .traditionalChinese: "帳戶", .english: "Account", .spanish: "Cuenta", .german: "Konto"],
            "nas_password": [.simplifiedChinese: "输入 NAS 密码", .traditionalChinese: "輸入 NAS 密碼", .english: "Enter NAS password", .spanish: "Introduce la contraseña del NAS", .german: "NAS-Passwort eingeben"],
            "nas_connect": [.simplifiedChinese: "连接并同步", .traditionalChinese: "連線並同步", .english: "Connect and sync", .spanish: "Conectar y sincronizar", .german: "Verbinden und synchronisieren"],
            "nas_status": [.simplifiedChinese: "状态", .traditionalChinese: "狀態", .english: "Status", .spanish: "Estado", .german: "Status"],
            "nas_not_connected": [.simplifiedChinese: "未连接", .traditionalChinese: "未連線", .english: "Not connected", .spanish: "Sin conexión", .german: "Nicht verbunden"],
            "nas_syncing": [.simplifiedChinese: "正在同步…", .traditionalChinese: "正在同步…", .english: "Syncing…", .spanish: "Sincronizando…", .german: "Synchronisieren…"],
            "nas_synced": [.simplifiedChinese: "已同步", .traditionalChinese: "已同步", .english: "Synced", .spanish: "Sincronizado", .german: "Synchronisiert"],
            "nas_failed": [.simplifiedChinese: "连接失败，请确认地址、密码及 WebDAV 端口（fnOS 通常为 5006）", .traditionalChinese: "連線失敗，請確認位址、密碼及 WebDAV 連接埠（fnOS 通常為 5006）", .english: "Connection failed. Check the address, password, and WebDAV port (usually 5006 on fnOS)", .spanish: "Error de conexión. Comprueba la dirección, la contraseña y el puerto WebDAV (normalmente 5006 en fnOS)", .german: "Verbindung fehlgeschlagen. Adresse, Passwort und WebDAV-Port prüfen (bei fnOS meist 5006)"],
            "nas_download": [.simplifiedChinese: "从 NAS 获取最新数据", .traditionalChinese: "從 NAS 取得最新資料", .english: "Get latest data from NAS", .spanish: "Obtener datos del NAS", .german: "Neueste Daten vom NAS laden"],
            "nas_upload": [.simplifiedChinese: "立即备份到 NAS", .traditionalChinese: "立即備份至 NAS", .english: "Back up to NAS now", .spanish: "Crear copia en el NAS", .german: "Jetzt auf NAS sichern"],
            "nas_disconnect": [.simplifiedChinese: "断开 NAS", .traditionalChinese: "中斷 NAS 連線", .english: "Disconnect NAS", .spanish: "Desconectar NAS", .german: "NAS trennen"],
            "nas_footer": [.simplifiedChinese: "请输入 HTTPS WebDAV 服务地址和端口，而不是 NAS 管理页面端口。地址可包含 https:// 或 WebDAV 子路径。密码仅保存在本机安全存储中。", .traditionalChinese: "請輸入 HTTPS WebDAV 服務位址與連接埠，而非 NAS 管理頁面連接埠。位址可包含 https:// 或 WebDAV 子路徑。密碼僅儲存在本機安全儲存空間。", .english: "Enter the HTTPS WebDAV service address and port, not the NAS admin-page port. The address may include https:// or a WebDAV subpath. The password stays in secure device storage.", .spanish: "Introduce la dirección y el puerto HTTPS de WebDAV, no los de la página de administración. La dirección puede incluir https:// o una subruta WebDAV. La contraseña se guarda de forma segura.", .german: "HTTPS-WebDAV-Adresse und -Port eingeben, nicht den Port der NAS-Verwaltung. Die Adresse darf https:// oder einen WebDAV-Unterpfad enthalten. Das Passwort bleibt sicher auf dem Gerät."],
            "type_resistor": [.simplifiedChinese: "电阻", .traditionalChinese: "電阻", .english: "Resistor", .spanish: "Resistencia", .german: "Widerstand"],
            "type_capacitor": [.simplifiedChinese: "电容", .traditionalChinese: "電容", .english: "Capacitor", .spanish: "Condensador", .german: "Kondensator"],
            "type_inductor": [.simplifiedChinese: "电感", .traditionalChinese: "電感", .english: "Inductor", .spanish: "Inductor", .german: "Induktivität"],
            "type_diode": [.simplifiedChinese: "二极管", .traditionalChinese: "二極體", .english: "Diode", .spanish: "Diodo", .german: "Diode"],
            "type_led": [.simplifiedChinese: "发光二极管", .traditionalChinese: "發光二極體", .english: "LED", .spanish: "LED", .german: "LED"],
            "type_transistor": [.simplifiedChinese: "三极管", .traditionalChinese: "電晶體", .english: "Transistor", .spanish: "Transistor", .german: "Transistor"],
            "type_ic": [.simplifiedChinese: "集成电路", .traditionalChinese: "積體電路", .english: "Integrated circuit", .spanish: "Circuito integrado", .german: "Integrierter Schaltkreis"],
            "type_crystal": [.simplifiedChinese: "晶振", .traditionalChinese: "晶振", .english: "Crystal", .spanish: "Cristal", .german: "Quarz"],
            "type_connector": [.simplifiedChinese: "连接器", .traditionalChinese: "連接器", .english: "Connector", .spanish: "Conector", .german: "Steckverbinder"],
            "type_switch": [.simplifiedChinese: "开关按键", .traditionalChinese: "開關按鍵", .english: "Switch", .spanish: "Interruptor", .german: "Schalter"],
            "type_sensor": [.simplifiedChinese: "传感器", .traditionalChinese: "感測器", .english: "Sensor", .spanish: "Sensor", .german: "Sensor"],
            "type_module": [.simplifiedChinese: "模块", .traditionalChinese: "模組", .english: "Module", .spanish: "Módulo", .german: "Modul"],
            "type_power": [.simplifiedChinese: "电源器件", .traditionalChinese: "電源元件", .english: "Power component", .spanish: "Componente de potencia", .german: "Leistungsbauteil"],
            "type_mechanical": [.simplifiedChinese: "结构件", .traditionalChinese: "結構件", .english: "Mechanical", .spanish: "Mecánico", .german: "Mechanik"],
            "type_other": [.simplifiedChinese: "其他", .traditionalChinese: "其他", .english: "Other", .spanish: "Otro", .german: "Sonstige"],
            "kind_cabinet": [.simplifiedChinese: "元件柜", .traditionalChinese: "元件櫃", .english: "Component cabinet", .spanish: "Armario", .german: "Bauteilschrank"],
            "kind_box": [.simplifiedChinese: "元件盒", .traditionalChinese: "元件盒", .english: "Component box", .spanish: "Caja", .german: "Bauteilbox"],
            "kind_drawer": [.simplifiedChinese: "抽屉", .traditionalChinese: "抽屜", .english: "Drawer", .spanish: "Cajón", .german: "Schublade"],
            "kind_shelf": [.simplifiedChinese: "货架", .traditionalChinese: "貨架", .english: "Shelf", .spanish: "Estante", .german: "Regal"],
            "kind_bag": [.simplifiedChinese: "防静电袋盒", .traditionalChinese: "防靜電袋盒", .english: "ESD bag box", .spanish: "Caja para bolsas ESD", .german: "ESD-Beutelbox"]
        ]
        return values[key]?[language] ?? values[key]?[.simplifiedChinese] ?? key
    }

    static func format(_ key: String, _ language: AppLanguage, _ arguments: CVarArg...) -> String {
        String(format: text(key, language), locale: Locale(identifier: language.rawValue), arguments: arguments)
    }

    static func componentType(_ type: ComponentType, _ language: AppLanguage) -> String {
        text("type_\(type.rawValue.lowercased())", language)
    }

    static func locationKind(_ kind: LocationKind, _ language: AppLanguage) -> String {
        text("kind_\(kind.rawValue.lowercased())", language)
    }

    static func sortMode(_ mode: SortMode, _ language: AppLanguage) -> String {
        switch mode {
        case .recent: return text("sort_recent", language)
        case .name: return text("sort_name", language)
        case .quantity: return text("sort_quantity", language)
        }
    }
}
