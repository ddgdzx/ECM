package com.ecm.inventory.ui

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

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
    "storage_positions" to listOf("存储位置", "儲存位置", "Storage locations", "Ubicaciones", "Lagerplätze"),
    "items_summary" to listOf("%d 个条目 · 合计 %d 件", "%d 個項目 · 合計 %d 件", "%d items · %d total units", "%d artículos · %d unidades en total", "%d Einträge · insgesamt %d Stück"),
    "all" to listOf("全部", "全部", "All", "Todos", "Alle"),
    "low_stock" to listOf("库存偏低", "庫存偏低", "Low stock", "Existencias bajas", "Niedriger Bestand"),
    "low_stock_count" to listOf("库存偏低 %d", "庫存偏低 %d", "Low stock %d", "Existencias bajas %d", "Niedriger Bestand %d"),
    "sort_recent" to listOf("最近更新", "最近更新", "Recently updated", "Actualización reciente", "Zuletzt aktualisiert"),
    "sort_name" to listOf("型号", "型號", "Model", "Modelo", "Modell"),
    "sort_quantity" to listOf("库存数量", "庫存數量", "Stock quantity", "Cantidad", "Bestandsmenge"),
    "search_inventory" to listOf("搜索型号、参数、封装", "搜尋型號、參數、封裝", "Search model, value, or package", "Buscar modelo, valor o encapsulado", "Modell, Wert oder Gehäuse suchen"),
    "add_component" to listOf("添加元件", "新增元件", "Add component", "Añadir componente", "Bauteil hinzufügen"),
    "component_photo" to listOf("元件照片", "元件照片", "Component photo", "Foto del componente", "Bauteilfoto"),
    "take_photo" to listOf("拍摄照片", "拍攝照片", "Take photo", "Tomar foto", "Foto aufnehmen"),
    "choose_photo" to listOf("从相册选择", "從相簿選擇", "Choose from library", "Elegir de la galería", "Aus Mediathek wählen"),
    "remove_photo" to listOf("移除照片", "移除照片", "Remove photo", "Eliminar foto", "Foto entfernen"),
    "photo_processing" to listOf("正在自动抠图…", "正在自動去背…", "Removing background…", "Eliminando el fondo…", "Hintergrund wird entfernt…"),
    "photo_hint" to listOf("建议在光线充足、背景简洁的环境拍摄。图片会自动抠图并随元件资料保存。", "建議在光線充足、背景簡潔的環境拍攝。圖片會自動去背並隨元件資料儲存。", "Use good lighting and a simple background. The background is removed automatically and the image is saved with the component.", "Usa buena iluminación y un fondo sencillo. El fondo se elimina automáticamente y la imagen se guarda con el componente.", "Bei gutem Licht vor einfachem Hintergrund fotografieren. Der Hintergrund wird automatisch entfernt und das Bild mit dem Bauteil gespeichert."),
    "photo_failed" to listOf("自动抠图暂不可用，已保留处理后的原图。", "自動去背暫時無法使用，已保留處理後的原圖。", "Background removal is unavailable; the processed original was kept.", "No se pudo eliminar el fondo; se conservó la imagen procesada.", "Die Hintergrundentfernung ist nicht verfügbar; das bearbeitete Original wurde beibehalten."),
    "empty_components" to listOf("还没有元件", "尚無元件", "No components yet", "Aún no hay componentes", "Noch keine Bauteile"),
    "no_results" to listOf("没有匹配的结果", "沒有符合的結果", "No matching results", "No hay resultados", "Keine passenden Ergebnisse"),
    "empty_components_hint" to listOf("点击右上角 + 添加第一颗元件，登记型号、数量和存放位置。", "點擊右上角 + 新增第一個元件，登記型號、數量和存放位置。", "Tap + to add your first component with its model, quantity, and location.", "Pulsa + para añadir el primer componente con modelo, cantidad y ubicación.", "Tippe auf +, um das erste Bauteil mit Modell, Menge und Lagerort anzulegen."),
    "no_results_hint" to listOf("换个关键词，或清除类型筛选试试。", "更換關鍵字，或清除類型篩選。", "Try another keyword or clear the type filter.", "Prueba otra palabra o elimina el filtro de tipo.", "Versuche einen anderen Suchbegriff oder entferne den Typfilter."),
    "unassigned_location" to listOf("未分配位置", "未分配位置", "No location", "Sin ubicación", "Kein Lagerort"),
    "locations_summary" to listOf("共 %d 个容器 · %d 个格口", "共 %d 個容器 · %d 個格口", "%d containers · %d slots", "%d contenedores · %d espacios", "%d Behälter · %d Fächer"),
    "new_location" to listOf("新建存储位置", "新增儲存位置", "New storage location", "Nueva ubicación", "Neuer Lagerort"),
    "empty_locations" to listOf("还没有存储位置", "尚無儲存位置", "No storage locations yet", "Aún no hay ubicaciones", "Noch keine Lagerorte"),
    "empty_locations_hint" to listOf("先建一个元件柜或贴片盒，设定层/行/列，之后就能把元件放进具体格口。", "先建立元件櫃或貼片盒並設定層／行／列，之後即可把元件放入指定格口。", "Create a cabinet or component box, set its layers, rows, and columns, then assign components to slots.", "Crea un armario o caja, define capas, filas y columnas y asigna componentes a los espacios.", "Lege einen Schrank oder eine Box mit Ebenen, Reihen und Spalten an und weise Bauteile den Fächern zu."),
    "location_dimensions" to listOf("%s · %d层 × %d行 × %d列", "%s · %d層 × %d行 × %d列", "%s · %d layers × %d rows × %d columns", "%s · %d capas × %d filas × %d columnas", "%s · %d Ebenen × %d Reihen × %d Spalten"),
    "types_count" to listOf("%d 种", "%d 種", "%d types", "%d tipos", "%d Typen"),
    "slots_used" to listOf("已用 %d/%d", "已用 %d/%d", "%d/%d used", "%d/%d usados", "%d/%d belegt"),
    "component_types" to listOf("元件种类", "元件種類", "Component types", "Tipos de componentes", "Bauteilarten"),
    "total_stock" to listOf("库存总数", "庫存總數", "Total stock", "Existencias totales", "Gesamtbestand"),
    "slot_usage" to listOf("格口占用", "格口佔用", "Slot usage", "Uso de espacios", "Fachbelegung"),
    "restock_needed" to listOf("需要补货", "需要補貨", "Restock needed", "Reponer existencias", "Nachbestellen"),
    "restock_hint" to listOf("数量低于预警值的元件会出现在这里。", "數量低於預警值的元件會顯示在此。", "Components below their warning level appear here.", "Aquí aparecen los componentes por debajo del nivel de alerta.", "Bauteile unterhalb des Warnbestands erscheinen hier."),
    "type_distribution" to listOf("类型分布", "類型分佈", "Type distribution", "Distribución por tipo", "Typverteilung"),
    "unassigned_count" to listOf("未分配位置（%d）", "未分配位置（%d）", "No location (%d)", "Sin ubicación (%d)", "Kein Lagerort (%d)"),
    "data_storage_note" to listOf("数据优先保存在本机；配置 NAS 后会自动同步。", "資料優先儲存在本機；設定 NAS 後會自動同步。", "Data is stored locally first and syncs automatically after NAS setup.", "Los datos se guardan primero en el dispositivo y se sincronizan al configurar el NAS.", "Daten werden zuerst lokal gespeichert und nach der NAS-Einrichtung automatisch synchronisiert."),
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
    "nas_server" to listOf("服务器或 WebDAV 地址", "伺服器或 WebDAV 位址", "Server or WebDAV address", "Servidor o dirección WebDAV", "Server- oder WebDAV-Adresse"),
    "nas_port" to listOf("WebDAV 端口", "WebDAV 連接埠", "WebDAV port", "Puerto WebDAV", "WebDAV-Port"),
    "nas_account" to listOf("账户", "帳戶", "Account", "Cuenta", "Konto"),
    "nas_password" to listOf("输入 NAS 密码", "輸入 NAS 密碼", "Enter NAS password", "Introduce la contraseña del NAS", "NAS-Passwort eingeben"),
    "nas_connect" to listOf("连接并同步", "連線並同步", "Connect and sync", "Conectar y sincronizar", "Verbinden und synchronisieren"),
    "nas_status" to listOf("状态", "狀態", "Status", "Estado", "Status"),
    "nas_not_connected" to listOf("未连接", "未連線", "Not connected", "Sin conexión", "Nicht verbunden"),
    "nas_syncing" to listOf("正在同步…", "正在同步…", "Syncing…", "Sincronizando…", "Synchronisieren…"),
    "nas_synced" to listOf("已同步", "已同步", "Synced", "Sincronizado", "Synchronisiert"),
    "nas_failed" to listOf("连接失败，请确认地址、密码及 WebDAV 端口（fnOS 通常为 5006）", "連線失敗，請確認位址、密碼及 WebDAV 連接埠（fnOS 通常為 5006）", "Connection failed. Check the address, password, and WebDAV port (usually 5006 on fnOS)", "Error de conexión. Comprueba la dirección, la contraseña y el puerto WebDAV (normalmente 5006 en fnOS)", "Verbindung fehlgeschlagen. Adresse, Passwort und WebDAV-Port prüfen (bei fnOS meist 5006)"),
    "nas_download" to listOf("从 NAS 获取最新数据", "從 NAS 取得最新資料", "Get latest data from NAS", "Obtener datos del NAS", "Neueste Daten vom NAS laden"),
    "nas_upload" to listOf("立即备份到 NAS", "立即備份至 NAS", "Back up to NAS now", "Crear copia en el NAS", "Jetzt auf NAS sichern"),
    "nas_disconnect" to listOf("断开 NAS", "中斷 NAS 連線", "Disconnect NAS", "Desconectar NAS", "NAS trennen"),
    "nas_footer" to listOf("请输入 HTTPS WebDAV 服务地址和端口，而不是 NAS 管理页面端口。地址可包含 https:// 或 WebDAV 子路径。密码仅保存在本机安全存储中。", "請輸入 HTTPS WebDAV 服務位址與連接埠，而非 NAS 管理頁面連接埠。位址可包含 https:// 或 WebDAV 子路徑。密碼僅儲存在本機安全儲存空間。", "Enter the HTTPS WebDAV service address and port, not the NAS admin-page port. The address may include https:// or a WebDAV subpath. The password stays in secure device storage.", "Introduce la dirección y el puerto HTTPS de WebDAV, no los de la página de administración. La dirección puede incluir https:// o una subruta WebDAV. La contraseña se guarda de forma segura.", "HTTPS-WebDAV-Adresse und -Port eingeben, nicht den Port der NAS-Verwaltung. Die Adresse darf https:// oder einen WebDAV-Unterpfad enthalten. Das Passwort bleibt sicher auf dem Gerät."),
    "type_resistor" to listOf("电阻", "電阻", "Resistor", "Resistencia", "Widerstand"),
    "type_capacitor" to listOf("电容", "電容", "Capacitor", "Condensador", "Kondensator"),
    "type_inductor" to listOf("电感", "電感", "Inductor", "Inductor", "Induktivität"),
    "type_diode" to listOf("二极管", "二極體", "Diode", "Diodo", "Diode"),
    "type_led" to listOf("发光二极管", "發光二極體", "LED", "LED", "LED"),
    "type_transistor" to listOf("三极管", "電晶體", "Transistor", "Transistor", "Transistor"),
    "type_ic" to listOf("集成电路", "積體電路", "Integrated circuit", "Circuito integrado", "Integrierter Schaltkreis"),
    "type_crystal" to listOf("晶振", "晶振", "Crystal", "Cristal", "Quarz"),
    "type_connector" to listOf("连接器", "連接器", "Connector", "Conector", "Steckverbinder"),
    "type_switch" to listOf("开关按键", "開關按鍵", "Switch", "Interruptor", "Schalter"),
    "type_sensor" to listOf("传感器", "感測器", "Sensor", "Sensor", "Sensor"),
    "type_module" to listOf("模块", "模組", "Module", "Módulo", "Modul"),
    "type_power" to listOf("电源器件", "電源元件", "Power component", "Componente de potencia", "Leistungsbauteil"),
    "type_mechanical" to listOf("结构件", "結構件", "Mechanical", "Mecánico", "Mechanik"),
    "type_other" to listOf("其他", "其他", "Other", "Otro", "Sonstige"),
    "kind_cabinet" to listOf("元件柜", "元件櫃", "Component cabinet", "Armario", "Bauteilschrank"),
    "kind_box" to listOf("元件盒", "元件盒", "Component box", "Caja", "Bauteilbox"),
    "kind_drawer" to listOf("抽屉", "抽屜", "Drawer", "Cajón", "Schublade"),
    "kind_shelf" to listOf("货架", "貨架", "Shelf", "Estante", "Regal"),
    "kind_bag" to listOf("防静电袋盒", "防靜電袋盒", "ESD bag box", "Caja para bolsas ESD", "ESD-Beutelbox")
)

fun appText(key: String, language: AppLanguage): String =
    copy[key]?.getOrNull(language.ordinal) ?: copy[key]?.firstOrNull() ?: key

fun appFormat(key: String, language: AppLanguage, vararg values: Any): String =
    String.format(Locale.forLanguageTag(language.code), appText(key, language), *values)

fun componentTypeText(type: com.ecm.inventory.data.ComponentType, language: AppLanguage): String =
    appText("type_${type.name.lowercase()}", language)

fun locationKindText(kind: com.ecm.inventory.data.LocationKind, language: AppLanguage): String =
    appText("kind_${kind.name.lowercase()}", language)

fun sortModeText(mode: SortMode, language: AppLanguage): String = when (mode) {
    SortMode.RECENT -> appText("sort_recent", language)
    SortMode.NAME -> appText("sort_name", language)
    SortMode.QUANTITY -> appText("sort_quantity", language)
}
