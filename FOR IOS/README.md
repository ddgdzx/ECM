# 元件库 · iOS

`../FOR Android/` 那个安卓 App 的 iOS 版，功能、文案、配色、交互逐项对齐。
Swift + SwiftUI + SwiftData，最低 iOS 17。

支持一个元件选择多个格口，并可逐笔登记消耗数量、用途、时间和扣减后的库存余量。

设置页支持将统一 JSON 快照同步到 fnOS WebDAV。密码只保存在 iOS Keychain；应用保留本地数据库，联网时自动备份，并按最后修改时间避免旧数据覆盖离线修改。

```bash
open ECM.xcodeproj     # Xcode 15+
```

选一个模拟器直接 Run 就能跑。要装到自己手机上，在 target 的
Signing & Capabilities 里选自己的 Apple ID（免费账号也行），换个唯一的
Bundle Identifier，再 Run 到设备上。

## 目录

```
ECM/
├── ECMApp.swift              @main 入口
├── Data/
│   ├── Models.swift          元件类型/容器类型/槽位/两个实体
│   ├── Persistence.swift     SwiftData 记录类与仓库（对应安卓的 Room）
│   └── EcmViewModel.swift    筛选、排序、编辑草稿
├── UI/
│   ├── Theme.swift           语义色与文本样式
│   ├── EcmRootView.swift     底部三个标签页 + 路由
│   ├── Components/
│   │   ├── Cupertino.swift        胶囊标签、空状态、主按钮、统计块、几种列表行
│   │   └── ComponentSymbol.swift  15 种元件电路符号（Canvas 手绘）
│   ├── Iso/IsoStorageView.swift   立体示意图（投影、着色、命中测试）
│   └── Screens/                   元件列表/详情/编辑、位置列表/详情/编辑、格口选择、概览
└── Assets.xcassets           应用图标与主题色
```

## 和安卓端的差异

功能没有差异，差的只是"谁来画控件"：

- 安卓端为了做出 iOS 观感，手写了导航栏、分组列表、分段控件、步进器、搜索框、底部标签栏。
  iOS 端这些统统换成系统原生的 `NavigationStack` / `List(.insetGrouped)` /
  `Picker(.segmented)` / `Stepper` / `.searchable` / `TabView`，观感一致，还自动跟随
  动态字体、辅助功能和深浅色。
- 色板：安卓端把 Apple HIG 的取值手抄了一份深浅两套；iOS 端直接用系统语义色。
  元件类型的 15 个色值两端仍是同一组硬编码值，保证立体图配色完全一致。
- 数据库：Room → SwiftData。SwiftData 没有自增主键，仓库层手工模拟了 Room 的
  `autoGenerate`，这样元件/位置 id 的语义两端相同。
- 编辑页在安卓端是从底部滑入的整页，iOS 端是标准模态卡片（`.sheet`）——这本来就是
  安卓端在模仿的东西。

## 立体示意图

`UI/Iso/IsoStorageView.swift` 是 `IsoStorageView.kt` 的逐行移植：

- 绕竖轴 `yaw` + 俯仰 `tilt` 的可调轴测投影，`zoom` 控制缩放；
- 逐面背面剔除，兰伯特着色（光照方向 `normalize(-0.45, -0.55, 0.75)`）；
- 画家算法按中心点深度排序，底座固定第一个画（否则会盖住远端那排格口）；
- 射线法多边形命中测试，从最靠近相机的格口往回找；
- 高亮格口会呼吸变亮、抬高 12%，并在上方弹出编号气泡。

安卓端用 `animateFloatAsState` / `rememberInfiniteTransition` 驱动动画；
SwiftUI 这边没有对应物能直接喂给 `Canvas`，改成用 `TimelineView(.animation)`
按时间自己算分层展开的补间和高亮的呼吸相位，效果一致。

## 工程文件

`ECM.xcodeproj` 是手写生成的经典工程（显式列出每个源文件），任何版本的 Xcode 都能打开。
`project.yml` 是同一份配置的 [XcodeGen](https://github.com/yonaskolb/XcodeGen) spec，
改了文件结构后可以 `xcodegen generate` 重新生成工程，省得手动往工程里加文件。
