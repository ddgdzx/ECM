# 元件库 · ECM

电子元器件库存管理 App，一套设计、两个平台，各自独立成一个文件夹：

| 目录 | 平台 | 技术栈 | 出包 |
| --- | --- | --- | --- |
| [`FOR Android/`](FOR%20Android/) | Android 8.0+ | Kotlin + Jetpack Compose + Room | [`.github/workflows/android.yml`](.github/workflows/android.yml) → APK |
| [`FOR IOS/`](FOR%20IOS/) | iOS 17+ | Swift + SwiftUI + SwiftData | [`.github/workflows/ios.yml`](.github/workflows/ios.yml) → 未签名 ipa |

两端功能、文案、配色、交互逐项对齐。安卓端当初是照着 Apple HIG 手写了一套 iOS 风格控件
（分组列表、分段控件、步进器、大标题导航栏、底部标签栏）；iOS 端把这些换回系统原生控件，
观感一致但更贴合平台。数据都保存在本机数据库里，不联网、不上传。

## 功能

- **元件管理**：15 种元件类型（电阻/电容/电感/二极管/LED/三极管/IC/晶振/连接器/开关/传感器/模块/电源器件/结构件/其他），
  每种自带电路符号图标和常用封装建议；可填写型号、参数值、封装、数量、单位、库存预警值和备注。
- **搜索与筛选**：按型号/参数/封装/备注全文搜索，按类型筛选，按最近更新 / 型号 / 数量排序，一键查看库存偏低的元件。
- **多格口存放**：同一种元件可同时分配到同一容器的多个格口，旧版本的单格口数据会自动兼容。
- **消耗明细**：在元件详情页逐笔登记消耗数量和用途，自动扣减库存并保留时间、用途与剩余库存。
- **存储位置**：可创建元件柜、元件盒、抽屉、货架等容器，自定义 **层 × 行 × 列**，每个格口都是一个可分配的槽位；
  容器尺寸调小后，落在范围外的元件会自动变为“未分配”，不会凭空消失。
- **立体示意图**：轴测投影绘制的三维容器图。单指拖动旋转、双指缩放、点击格口查看内容，
  支持“分层展开”、单层聚焦以及俯视/正视快捷视角。元件所在格口会高亮并弹出编号气泡。
- **概览**：库存总量、需要补货清单、类型分布条形图、格口占用率、未分配元件。

立体图两端都是纯 Canvas 手绘，没有引入任何 3D 引擎或图片资源：绕竖轴 yaw + 俯仰 tilt 的可调轴测投影，
逐面背面剔除 + 兰伯特着色，画家算法按深度排序保证遮挡关系，点击用射线法做多边形命中测试。
元件电路符号同样是逐笔画出来的。

## 两端代码的对应关系

| 职责 | Android (`FOR Android/app/src/main/java/com/ecm/inventory/`) | iOS (`FOR IOS/ECM/`) |
| --- | --- | --- |
| 实体与枚举 | `data/Model.kt` | `Data/Models.swift` |
| 本地数据库 | `data/EcmDatabase.kt` + `data/Repository.kt`（Room） | `Data/Persistence.swift`（SwiftData） |
| 状态与草稿 | `ui/EcmViewModel.kt` | `Data/EcmViewModel.swift` |
| 色板与排版 | `ui/theme/Theme.kt` | `UI/Theme.swift` |
| 通用控件 | `ui/components/Cupertino.kt` | `UI/Components/Cupertino.swift` |
| 电路符号 | `ui/components/ComponentSymbol.kt` | `UI/Components/ComponentSymbol.swift` |
| 立体示意图 | `ui/iso/IsoStorageView.kt` | `UI/Iso/IsoStorageView.swift` |
| 导航骨架 | `ui/EcmNavHost.kt` | `UI/EcmRootView.swift` |
| 八个页面 | `ui/screens/*.kt` | `UI/Screens/*.swift` |

## 构建

安卓：

```bash
cd "FOR Android"
echo "sdk.dir=/path/to/android-sdk" > local.properties   # 需要 JDK 17、Android SDK（compileSdk 34）
./gradlew :app:assembleDebug
# 产物：FOR Android/app/build/outputs/apk/debug/app-debug.apk
```

iOS：

```bash
open "FOR IOS/ECM.xcodeproj"    # 需要 Xcode 15+
# 选一个模拟器或自己的手机，直接 Run
```

首次安装后库存为空，从“存储位置”新建容器、再到“元件库”添加元件即可。

## 自动出包（GitHub Actions）

| 触发方式 | 结果 |
| --- | --- |
| 推送任意分支 / 提 PR | 两个工作流各自编译，产物传到该次运行的 Artifacts（保留 30 天） |
| 推 `v*` 标签（如 `v1.1`） | 同上，并自动创建 GitHub Release，把 APK 和 ipa 都附上去 |
| Actions 页面手动 Run workflow | 同分支构建 |

版本号由 CI 注入：安卓的 `versionName` / iOS 的 `MARKETING_VERSION` 取标签名（无标签时为 `1.0-<短 sha>`），
`versionCode` / `CURRENT_PROJECT_VERSION` 取运行序号。安卓侧对应 `FOR Android/app/build.gradle.kts` 里读取的
`ECM_VERSION_NAME` / `ECM_VERSION_CODE` 环境变量；iOS 侧直接由 `xcodebuild` 命令行覆盖。

两个工作流打 tag 时会往同一个 Release 上传：谁先跑完谁负责创建，后到的自动改成追加文件。

### 安卓正式签名（可选）

不配置时 release 包会退回 debug 签名，能装能用，但不适合长期分发。配置步骤：

```bash
# 1. 本地生成 keystore（有效期 ~27 年）
keytool -genkeypair -v -keystore release.jks -alias ecm \
  -keyalg RSA -keysize 2048 -validity 10000

# 2. 转成 base64
base64 -w0 release.jks   # macOS 用 base64 -i release.jks
```

在仓库 Settings → Secrets and variables → Actions 添加 4 个 secret：

| 名称 | 值 |
| --- | --- |
| `ECM_KEYSTORE_BASE64` | 上一步的 base64 内容 |
| `ECM_KEYSTORE_PASSWORD` | keystore 密码 |
| `ECM_KEY_ALIAS` | 别名，如 `ecm` |
| `ECM_KEY_PASSWORD` | 私钥密码 |

配好后 CI 会自动改用正式签名（keystore 本身不进仓库，只以 secret 形式存在）。
本地想出正式包也一样，把这几个值设成环境变量即可（`ECM_KEYSTORE_FILE` 指向 jks 路径）。

### iOS 签名

CI 里没有开发者证书，所以出的是**未签名 ipa**，只能配合自签工具使用。
想装到自己手机上，最省事的办法是用 Xcode 打开 `FOR IOS/ECM.xcodeproj`，
在 Signing & Capabilities 里选自己的 Apple ID（免费账号也行），然后直接 Run。

## 环境说明

安卓工程在 JDK 21 + Gradle 8.9 + AGP 8.5.2 + Kotlin 2.0.20 下编译通过（`:app:assembleDebug`）。
iOS 工程是在 Linux 容器里移植的，编译验证走的是 Actions 上的 macOS runner：
模拟器 Debug 与真机 Release 两个 target 都编过，未签名 ipa 也正常产出。

两端都没有在真机/模拟器上做过界面走查，界面细节以实际运行为准。
立体图的投影、遮挡与分层展开两端用的是同一套算法。
