package com.ecm.inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecm.inventory.ui.AppLanguage
import com.ecm.inventory.ui.LocalAppLanguage
import com.ecm.inventory.ui.appText
import com.ecm.inventory.ui.components.CupertinoNavBar
import com.ecm.inventory.ui.components.InsetSection
import com.ecm.inventory.ui.components.RowSeparator
import com.ecm.inventory.ui.components.SettingsRow
import com.ecm.inventory.ui.theme.AppleText
import com.ecm.inventory.ui.theme.AppleTheme

@Composable
fun SettingsScreen(
    selectedLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    contentPadding: PaddingValues
) {
    val colors = AppleTheme.colors
    val language = LocalAppLanguage.current

    Column(Modifier.fillMaxSize().background(colors.groupedBackground)) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        CupertinoNavBar(title = appText("settings", language), showTitle = true, showSeparator = true)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                InsetSection {
                    Row(
                        Modifier.fillMaxWidth().background(colors.accent.copy(alpha = 0.08f)).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(colors.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Settings, null, tint = colors.barBackground, modifier = Modifier.size(26.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(appText("preferences", language), style = AppleText.title3, color = colors.label)
                            Text(appText("preferences_hint", language), style = AppleText.footnote, color = colors.secondaryLabel)
                        }
                    }
                }
            }

            item {
                InsetSection(header = appText("language", language)) {
                    SettingsRow(
                        title = appText("language", language),
                        leading = { Icon(Icons.Outlined.Language, null, tint = colors.accent) }
                    )
                    AppLanguage.entries.forEach { option ->
                        RowSeparator(startInset = 16.dp)
                        Row(
                            Modifier.fillMaxWidth().clickable { onLanguageChange(option) }.padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(option.displayName, style = AppleText.body, color = colors.label, modifier = Modifier.weight(1f))
                            if (option == selectedLanguage) {
                                Icon(Icons.Outlined.Check, null, tint = colors.accent, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }

            item {
                InsetSection {
                    SettingsRow(
                        title = appText("appearance", language),
                        value = appText("follow_system", language),
                        leading = { Icon(Icons.Outlined.DarkMode, null, tint = colors.accent) }
                    )
                }
            }

            item {
                InsetSection(header = appText("about", language), footer = appText("local_data", language)) {
                    SettingsRow(title = "Arxan ECM", value = "1.0")
                }
            }
        }
    }
}
