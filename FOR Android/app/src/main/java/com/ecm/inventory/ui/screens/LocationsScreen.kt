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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecm.inventory.data.LocationEntity
import com.ecm.inventory.ui.EcmViewModel
import com.ecm.inventory.ui.components.CupertinoNavBar
import com.ecm.inventory.ui.components.EmptyState
import com.ecm.inventory.ui.components.FilledActionButton
import com.ecm.inventory.ui.components.LargeTitle
import com.ecm.inventory.ui.LocalAppLanguage
import com.ecm.inventory.ui.appFormat
import com.ecm.inventory.ui.appText
import com.ecm.inventory.ui.locationKindText
import com.ecm.inventory.ui.components.NavIconButton
import com.ecm.inventory.ui.iso.BinStyle
import com.ecm.inventory.ui.iso.IsoStorageView
import com.ecm.inventory.ui.theme.AppleText
import com.ecm.inventory.ui.theme.AppleTheme

@Composable
fun LocationsScreen(
    vm: EcmViewModel,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    contentPadding: PaddingValues
) {
    val colors = AppleTheme.colors
    val language = LocalAppLanguage.current
    val locations by vm.locations.collectAsState()
    val components by vm.allComponentsState.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        CupertinoNavBar(
            title = appText("storage_positions", language),
            showTitle = false,
            showSeparator = false,
            trailing = { NavIconButton(Icons.Rounded.Add, appText("new_location", language), onAdd) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    LargeTitle(appText("storage_positions", language))
                    Text(
                        appFormat("locations_summary", language, locations.size, locations.sumOf { it.slotCount }),
                        style = AppleText.footnote,
                        color = colors.secondaryLabel,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (locations.isEmpty()) {
                item {
                    EmptyState(
                        title = appText("empty_locations", language),
                        subtitle = appText("empty_locations_hint", language),
                        action = {
                            Box(Modifier.padding(horizontal = 24.dp)) {
                                FilledActionButton(appText("new_location", language), onAdd)
                            }
                        }
                    )
                }
            }

            items(locations) { loc ->
                val inside = components.filter { it.locationId == loc.id }
                LocationCard(
                    location = loc,
                    bins = inside.flatMap { c ->
                        c.slots.map { it to BinStyle(fill = c.typeEnum.tint, count = c.quantity) }
                    }.toMap(),
                    usedSlots = inside.flatMap { it.slots }.distinct().size,
                    itemCount = inside.size,
                    language = language,
                    onClick = { onOpen(loc.id) }
                )
            }
        }
    }
}

@Composable
private fun LocationCard(
    location: LocationEntity,
    bins: Map<com.ecm.inventory.data.Slot, BinStyle>,
    usedSlots: Int,
    itemCount: Int,
    language: com.ecm.inventory.ui.AppLanguage,
    onClick: () -> Unit
) {
    val colors = AppleTheme.colors
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            IsoStorageView(
                layers = location.layers,
                rows = location.rows,
                cols = location.cols,
                bins = bins,
                interactive = false,
                modifier = Modifier.fillMaxSize()
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(location.name, style = AppleText.headline, color = colors.label)
                Spacer(Modifier.height(2.dp))
                Text(
                    appFormat("location_dimensions", language, locationKindText(location.kindEnum, language), location.layers, location.rows, location.cols),
                    style = AppleText.footnote,
                    color = colors.secondaryLabel
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    appFormat("types_count", language, itemCount),
                    style = AppleText.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.label
                )
                Text(
                    appFormat("slots_used", language, usedSlots, location.slotCount),
                    style = AppleText.caption,
                    color = colors.tertiaryLabel
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.tertiaryLabel)
            )
        }
    }
}
