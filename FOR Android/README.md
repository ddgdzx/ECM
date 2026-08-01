# 元件库 · Android

电子元器件库存管理 App 的安卓端。Kotlin + Jetpack Compose 编写，UI 按 Apple HIG 的观感做了一套
iOS 风格的控件（分组列表、分段控件、步进器、大标题导航栏、底部标签栏），数据全部保存在本机 Room 数据库。

支持一个元件选择多个格口，并可逐笔登记消耗数量、用途、时间和扣减后的库存余量。

设置页可自行填写 HTTPS WebDAV 地址（可带子路径）、WebDAV 端口、管理员用户名和密码，将统一 JSON 快照同步到 WebDAV 根目录的 `ArxanECM/ecm-data.json`。请填写 WebDAV 服务端口而不是 NAS 管理页面端口（fnOS 通常为 5006）。密码只保存在 Android 加密存储；应用保留本地数据库，联网时自动备份，并按最后修改时间避免旧数据覆盖离线修改。

对应的 iOS 版在 [`../FOR IOS/`](../FOR%20IOS/)，功能与文案逐项对齐。完整说明见[仓库根目录的 README](../README.md)。

## 构建

```bash
# 需要 JDK 17、Android SDK（compileSdk 34）
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 代码结构

```
app/src/main/java/com/ecm/inventory/
├── data/          Room 实体、DAO、数据库、仓库
├── ui/
│   ├── theme/     iOS 语义色板（深浅两套）与排版
│   ├── components/Cupertino 风格控件、元件电路符号
│   ├── iso/       立体示意图（投影、着色、命中测试）
│   ├── screens/   元件列表/详情/编辑、位置列表/详情/编辑、格口选择、概览
│   ├── EcmViewModel.kt
│   └── EcmNavHost.kt
└── MainActivity.kt
```

工程在 JDK 21 + Gradle 8.9 + AGP 8.5.2 + Kotlin 2.0.20 下编译通过（`:app:assembleDebug`）。
