# 元件库 · ECM

安卓端的电子元器件库存管理 App。Kotlin + Jetpack Compose 编写，UI 按 Apple HIG 的观感做了一套
iOS 风格的控件（分组列表、分段控件、步进器、大标题导航栏、底部标签栏），数据全部保存在本机 Room 数据库。

## 功能

- **元件管理**：15 种元件类型（电阻/电容/电感/二极管/LED/三极管/IC/晶振/连接器/开关/传感器/模块/电源器件/结构件/其他），
  每种自带电路符号图标和常用封装建议；可填写型号、参数值、封装、数量、单位、库存预警值和备注。
- **搜索与筛选**：按型号/参数/封装/备注全文搜索，按类型筛选，按最近更新 / 型号 / 数量排序，一键查看库存偏低的元件。
- **存储位置**：可创建元件柜、元件盒、抽屉、货架等容器，自定义 **层 × 行 × 列**，每个格口都是一个可分配的槽位；
  容器尺寸调小后，落在范围外的元件会自动变为"未分配"，不会凭空消失。
- **立体示意图**：轴测投影绘制的三维容器图。单指拖动旋转、双指缩放、点击格口查看内容，
  支持"分层展开"、单层聚焦以及俯视/正视快捷视角。元件所在格口会高亮并弹出编号气泡。
- **概览**：库存总量、需要补货清单、类型分布条形图、格口占用率、未分配元件。

立体图是纯 Canvas 实现（`ui/iso/IsoStorageView.kt`）：绕竖轴 yaw + 俯仰 tilt 的可调轴测投影，
逐面背面剔除 + 兰伯特着色，画家算法按深度排序保证遮挡关系，点击用射线法做多边形命中测试。
没有引入任何 3D 引擎或图片资源，元件符号也是 Canvas 画出来的。

## 构建

```bash
# 需要 JDK 17、Android SDK（compileSdk 34）
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

首次启动会写入一份示例数据（1 个 3 层元件柜、1 个贴片盒、12 条元件记录），方便直接看到效果。

## 自动出包（GitHub Actions）

工作流 `.github/workflows/android.yml`：

| 触发方式 | 结果 |
| --- | --- |
| 推送任意分支 / 提 PR | 编译 debug + release，APK 传到该次运行的 Artifacts（保留 30 天） |
| 推 `v*` 标签（如 `v1.1`） | 同上，并自动创建 GitHub Release 把两个 APK 附上去 |
| Actions 页面手动 Run workflow | 同分支构建 |

版本号由 CI 注入：`versionName` 取标签名（无标签时为 `1.0-<短 sha>`），`versionCode` 取运行序号，
对应 `app/build.gradle.kts` 里读取的 `ECM_VERSION_NAME` / `ECM_VERSION_CODE` 环境变量。

### 配置正式签名（可选）

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

## 代码结构

```
app/src/main/java/com/ecm/inventory/
├── data/          Room 实体、DAO、数据库、仓库（含示例数据）
├── ui/
│   ├── theme/     iOS 语义色板（深浅两套）与排版
│   ├── components/Cupertino 风格控件、元件电路符号
│   ├── iso/       立体示意图（投影、着色、命中测试）
│   ├── screens/   元件列表/详情/编辑、位置列表/详情/编辑、格口选择、概览
│   ├── EcmViewModel.kt
│   └── EcmNavHost.kt
└── MainActivity.kt
```

## 环境说明

工程在 JDK 21 + Gradle 8.9 + AGP 8.5.2 + Kotlin 2.0.20 下编译通过（`:app:assembleDebug`）。
开发容器内没有 KVM，无法启动模拟器，因此界面未做真机运行验证；立体图的投影、遮挡与分层展开
通过离线复现同一套算法渲染核对过。
