import SwiftUI

/// 页面之间的跳转目标，对应安卓端 Routes 里的两个带参数路由。
enum Route: Hashable {
    case componentDetail(Int64)
    case locationDetail(Int64)
}

/// 底部三个标签页，对应安卓端 EcmNavHost 里的 TabBar。
struct EcmRootView: View {
    @StateObject private var vm = EcmViewModel()

    var body: some View {
        TabView {
            InventoryScreen()
                .tabItem { Label("元件库", systemImage: "shippingbox") }
            LocationsScreen()
                .tabItem { Label("存储", systemImage: "square.grid.3x3") }
            StatsScreen()
                .tabItem { Label("概览", systemImage: "chart.pie") }
        }
        .environmentObject(vm)
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
