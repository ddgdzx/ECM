package com.ecm.inventory.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ecm.inventory.ui.components.RowSeparator
import com.ecm.inventory.ui.screens.ComponentDetailScreen
import com.ecm.inventory.ui.screens.ComponentEditScreen
import com.ecm.inventory.ui.screens.InventoryScreen
import com.ecm.inventory.ui.screens.LocationDetailScreen
import com.ecm.inventory.ui.screens.LocationEditScreen
import com.ecm.inventory.ui.screens.LocationsScreen
import com.ecm.inventory.ui.screens.SlotPickerScreen
import com.ecm.inventory.ui.screens.StatsScreen
import com.ecm.inventory.ui.screens.SettingsScreen
import com.ecm.inventory.ui.theme.AppleText
import com.ecm.inventory.ui.theme.AppleTheme

object Routes {
    const val INVENTORY = "inventory"
    const val LOCATIONS = "locations"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val COMPONENT_DETAIL = "component/{id}"
    const val COMPONENT_EDIT = "component_edit"
    const val LOCATION_DETAIL = "location/{id}"
    const val LOCATION_EDIT = "location_edit"
    const val SLOT_PICKER = "slot_picker"

    fun componentDetail(id: Long) = "component/$id"
    fun locationDetail(id: Long) = "location/$id"
}

private data class Tab(
    val route: String,
    val labelKey: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val tabs = listOf(
    Tab(Routes.INVENTORY, "inventory", Icons.Outlined.Inventory2, Icons.Rounded.Inventory2),
    Tab(Routes.LOCATIONS, "storage", Icons.Outlined.GridView, Icons.Rounded.GridView),
    Tab(Routes.STATS, "overview", Icons.Outlined.PieChart, Icons.Rounded.PieChart),
    Tab(Routes.SETTINGS, "settings", Icons.Outlined.Settings, Icons.Rounded.Settings)
)

@Composable
fun EcmNavHost(vm: EcmViewModel, navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("arxan_ecm_preferences", 0) }
    var language by remember { mutableStateOf(AppLanguage.fromCode(preferences.getString("app_language", null))) }
    val colors = AppleTheme.colors
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showTabBar = currentRoute in tabs.map { it.route }
    val tabBarHeight = 49.dp

    CompositionLocalProvider(LocalAppLanguage provides language) {
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.INVENTORY,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { slideInHorizontally { it / 6 } + fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { slideOutHorizontally { it / 6 } + fadeOut() }
        ) {
            composable(Routes.INVENTORY) {
                InventoryScreen(
                    vm = vm,
                    onAdd = {
                        vm.startComponentEdit(null)
                        navController.navigate(Routes.COMPONENT_EDIT)
                    },
                    onOpen = { id -> navController.navigate(Routes.componentDetail(id)) },
                    contentPadding = PaddingValues(bottom = tabBarHeight)
                )
            }

            composable(Routes.LOCATIONS) {
                LocationsScreen(
                    vm = vm,
                    onAdd = {
                        vm.startLocationEdit(null)
                        navController.navigate(Routes.LOCATION_EDIT)
                    },
                    onOpen = { id -> navController.navigate(Routes.locationDetail(id)) },
                    contentPadding = PaddingValues(bottom = tabBarHeight)
                )
            }

            composable(Routes.STATS) {
                StatsScreen(
                    vm = vm,
                    onOpenComponent = { id -> navController.navigate(Routes.componentDetail(id)) },
                    contentPadding = PaddingValues(bottom = tabBarHeight)
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    selectedLanguage = language,
                    onLanguageChange = {
                        language = it
                        preferences.edit().putString("app_language", it.code).apply()
                    },
                    contentPadding = PaddingValues(bottom = tabBarHeight)
                )
            }

            composable(
                route = Routes.COMPONENT_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                ComponentDetailScreen(
                    vm = vm,
                    componentId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        vm.startComponentEdit(id)
                        navController.navigate(Routes.COMPONENT_EDIT)
                    },
                    onOpenLocation = { locId -> navController.navigate(Routes.locationDetail(locId)) }
                )
            }

            composable(
                route = Routes.COMPONENT_EDIT,
                enterTransition = { slideInVertically { it } },
                popExitTransition = { slideOutVertically { it } }
            ) {
                ComponentEditScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onPickLocation = { navController.navigate(Routes.SLOT_PICKER) },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.SLOT_PICKER,
                enterTransition = { slideInHorizontally { it } },
                popExitTransition = { slideOutHorizontally { it } }
            ) {
                SlotPickerScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onCreateLocation = {
                        vm.startLocationEdit(null)
                        navController.navigate(Routes.LOCATION_EDIT)
                    }
                )
            }

            composable(
                route = Routes.LOCATION_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                LocationDetailScreen(
                    vm = vm,
                    locationId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        vm.startLocationEdit(id)
                        navController.navigate(Routes.LOCATION_EDIT)
                    },
                    onOpenComponent = { cid -> navController.navigate(Routes.componentDetail(cid)) }
                )
            }

            composable(
                route = Routes.LOCATION_EDIT,
                enterTransition = { slideInVertically { it } },
                popExitTransition = { slideOutVertically { it } }
            ) {
                LocationEditScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
        }

        AnimatedVisibility(
            visible = showTabBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TabBar(
                currentRoute = currentRoute,
                onSelect = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(Routes.INVENTORY) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
    }
}

@Composable
private fun TabBar(currentRoute: String?, onSelect: (String) -> Unit) {
    val colors = AppleTheme.colors
    val language = LocalAppLanguage.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.barBackground)
    ) {
        RowSeparator(startInset = 0.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .height(49.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val label = appText(tab.labelKey, language)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(tab.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = label,
                        tint = if (selected) colors.accent else colors.gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        label,
                        style = AppleText.caption2.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (selected) colors.accent else colors.gray
                    )
                }
            }
        }
        Spacer(
            Modifier
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .fillMaxWidth()
        )
    }
}
