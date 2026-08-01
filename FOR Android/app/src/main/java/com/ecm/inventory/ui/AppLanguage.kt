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
    "local_data" to listOf("库存数据仅保存在本机", "庫存資料僅儲存在本機", "Inventory data stays on this device", "Los datos se guardan en este dispositivo", "Bestandsdaten bleiben auf diesem Gerät")
)

fun appText(key: String, language: AppLanguage): String =
    copy[key]?.getOrNull(language.ordinal) ?: copy[key]?.firstOrNull() ?: key
