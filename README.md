# AllToolBox — SystemPlus

项目简介
--------
SystemPlus 是一个用于定制 Android 系统/模块适配与增强的工程（包含 UI、偏好设置、模块激活、更新检查等）。本仓库提供可编译的 Android Gradle 工程，便于本地开发、调试与打包 APK。

主要功能
--------
- 可配置的设置界面（公告、检查更新、模块状态、激活工具等）
- 模块自激活逻辑（针对受限应用列表的机型）
- 更新检测与版本回退兼容处理
- 资源驱动的主题与样式（颜色、圆角、按钮样式）

快速开始
--------
先决条件：
- JDK 11/17（系统已包含或使用 IDE 自带）
- Android SDK 和 platform-tools（adb）
- Gradle wrapper（推荐使用仓库自带的 `gradlew`）

在 Windows PowerShell 下构建：

```powershell
cd D:\sysp\app\src\NEWWIRTE-clean
.\gradlew assembleDebug
```

安装到设备并启动设置页：

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.atb.systemplus/.SettingsActivity
```

运行单元测试：

```powershell
.\gradlew test
.\gradlew :app:connectedAndroidTest   # 如使用真实设备或模拟器
```

开发指南与贡献
--------
- 查看详细开发指南：`docs/DEVELOPMENT.md`
- 贡献说明与 PR 流程：`CONTRIBUTING.md`

代码组织
--------
- `app/src/main/java/com/atb/systemplus/` — Activity、工具类、UI 代码
- `app/src/main/res/` — 布局、颜色、drawables、字符串等资源
- `app/src/main/res/xml/prefs.xml` — 设置项定义（PreferenceScreen）

常用修改点
--------
- 添加设置项：编辑 `res/xml/prefs.xml`，在 `SettingsActivity` 中通过 `bindGoToActivity` 关联目标 Activity。
- 修改主题/颜色：编辑 `res/values/colors.xml` 与 `res/values/styles.xml`，并更新 drawable（矢量图或 shape）。
- 按钮/面板样式：放在 `res/drawable/*`，推荐使用 shape + selector 保持可复用性。

发布与许可
--------
请查看仓库根目录的 `LICENSE` 文件了解许可信息。

维护者
--------
仓库维护者：HIbingai

更多信息
--------
详细开发流程、代码规范和样式指南请参阅 `CONTRIBUTING.md` 和 `docs/DEVELOPMENT.md`。欢迎使用 Issues 或 Pull Requests 参与改进。
