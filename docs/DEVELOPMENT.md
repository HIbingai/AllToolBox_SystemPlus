# 开发指南（DEVELOPMENT）

本文档面向想要在本项目中开发、调试或审查代码的工程师。包含仓库结构说明、常见改动流程和调试技巧。

## 仓库简要结构

- `app/` — Android 模块主工程：Java/Kotlin 源码、资源与 manifest。
  - `app/src/main/java/com/atb/systemplus/` — 核心 Activity、prefs、UI 组件。
  - `app/src/main/res/layout/` — 布局文件（activity、片段、item）。
  - `app/src/main/res/drawable/` — shape、selector、矢量图标等。
  - `app/src/main/res/values/` — `colors.xml`, `dimens.xml`, `styles.xml`, `strings.xml`。
  - `app/src/main/res/xml/prefs.xml` — 设置入口定义（PreferenceScreen）。

## 常见开发操作

1. 克隆并准备环境

```bash
git clone https://github.com/HIbingai/AllToolBox_SystemPlus.git
cd AllToolBox_SystemPlus
cd app
# 使用仓库自带 gradlew（Windows 使用 gradlew.bat）
../gradlew assembleDebug
```

2. 在设备上安装并打开设置页

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.atb.systemplus/.SettingsActivity
```

3. 如果无法直接启动某个 Activity（系统未导出），可以通过点击设置入口进入，或临时在 AndroidManifest 中将 `exported="true"` 仅用于本地调试。

## 添加或修改设置入口

步骤示例（添加新设置页面）：

1. 在 `res/xml/prefs.xml` 添加 `<com.atb.systemplus.prefs.GoToActivityPreference android:key="NewKey" android:title="新功能" .../>`。
2. 新建 `NewActivity`，并在 `SettingsActivity.SettingsFragment.bindEntryNavigation()` 中调用 `bindGoToActivity("NewKey", NewActivity.class);`。
3. 编译、运行并在设置界面点击新入口进行验证。

## 主题、颜色与样式

- 所有可见文本应使用 `res/values/colors.xml` 与 `styles.xml` 管理，避免在布局中写死颜色。修改 `panel_bg_light`、`panel_stroke_light` 等变量以统一面板风格。
- 按钮与面板样式应放在 `res/drawable`（使用 `<shape>` 和 `<selector>` 实现 pressed/normal/disabled 状态）。

## 图标与资源规范

- 优先使用矢量 drawable（`res/drawable/*.xml`），确保 `viewport` 与 `width/height` 设置合理以兼容不同密度。
- 轻微的描边与相邻色（soft stroke）能提升视觉统一性，避免高饱和度大面积填充。

## 调试技巧

- 使用 `adb logcat` 观察运行时日志：`adb logcat -s SystemPlus` 或过滤相关 TAG。
- 截屏：`adb shell screencap -p /sdcard/sysp.png && adb pull /sdcard/sysp.png`。
- UI 层级 dump（便于定位 Preference 坐标）：`adb shell uiautomator dump /sdcard/uidump.xml && adb pull /sdcard/uidump.xml`。

## 测试与静态检查

- 单元测试：`./gradlew test`
- Android 测试（设备）：`./gradlew :app:connectedAndroidTest`
- Lint：`./gradlew lint`，修复关键错误和报警。

## 发布流程（简要）

1. 更新 `app/build.gradle` 中的 `versionCode` 与 `versionName`。
2. 运行 `./gradlew assembleRelease`（需要签名配置）。
3. 生成的 APK 在 `app/build/outputs/apk/release/`。

## 注意事项

- 切勿将大体积二进制（如完整 JDK、Gradle 发行包）提交到仓库。
- 所有可见文本应国际化（放入 `strings.xml`），便于后续本地化处理。

更多开发细节请参考仓库根目录的 `README.md` 与 `CONTRIBUTING.md`。
