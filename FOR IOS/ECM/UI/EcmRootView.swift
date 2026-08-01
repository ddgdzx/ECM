import SwiftUI

/// 页面之间的跳转目标，对应安卓端 Routes 里的两个带参数路由。
enum Route: Hashable {
    case componentDetail(Int64)
    case locationDetail(Int64)
}

/// 底部三个标签页，对应安卓端 EcmNavHost 里的 TabBar。
struct EcmRootView: View {
    @StateObject private var vm = EcmViewModel()
    @AppStorage("appLanguage") private var languageRaw = AppLanguage.simplifiedChinese.rawValue

    private var language: AppLanguage {
        get { AppLanguage(rawValue: languageRaw) ?? .simplifiedChinese }
        nonmutating set { languageRaw = newValue.rawValue }
    }

    var body: some View {
        TabView {
            InventoryScreen()
                .tabItem { Label(AppCopy.text("inventory", language), systemImage: "shippingbox") }
            LocationsScreen()
                .tabItem { Label(AppCopy.text("storage", language), systemImage: "square.grid.3x3") }
            StatsScreen()
                .tabItem { Label(AppCopy.text("overview", language), systemImage: "chart.pie") }
            SettingsScreen(
                selectedLanguage: Binding(
                    get: { language },
                    set: { language = $0 }
                )
            )
                .tabItem { Label(AppCopy.text("settings", language), systemImage: "gearshape") }
        }
        .environmentObject(vm)
        .environment(\.appLanguage, language)
        .environment(\.locale, Locale(identifier: language.rawValue))
        .tint(AppleColors.accent)
    }
}

/// 详情页共用的落地：把两种目标页接到当前导航栈上，并像安卓端那样在二级页隐藏标签栏。
extension View {
    func ecmNavigationDestinations() -> some View {
        navigationDestination(for: Route.self) { route in
            switch route {
            case .componentDetail(let id):
                ComponentDetailScreen(componentId: id)
                    .toolbar(.hidden, for: .tabBar)
            case .locationDetail(let id):
                LocationDetailScreen(locationId: id)
                    .toolbar(.hidden, for: .tabBar)
            }
        }
    }
}
