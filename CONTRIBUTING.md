# 贡献指南（CONTRIBUTING）

欢迎为本项目贡献代码与文档！请按以下流程与规范提交变更，以便我们高效、可维护地合入你的提交。

## 贡献流程（简要）
1. Fork 仓库并在本地创建分支：`git checkout -b feat/描述` 或 `fix/描述`。
2. 在本地编译与运行相关模块，添加或更新单元测试（如有）。
3. 保持提交原子、描述清晰，遵循提交信息规范（见下文）。
4. 推送分支并在仓库创建 Pull Request，选择合适的目标分支（通常为 `master`）。
5. 在 PR 描述中说明变更目的、影响范围与验证步骤。

## 分支与提交规范
- 分支命名：`feat/xxx`、`fix/xxx`、`chore/xxx`、`docs/xxx`。
- 提交消息（建议使用 Conventional Commits 样式）：

```text
feat(settings): add ActiveSelf icon and improve layout

详细描述（如果需要）
```

## PR 验收要点（检查单）
- 项目可编译并通过本地测试：`./gradlew assembleDebug`、`./gradlew test`。
- 无新增大体积二进制（zip/jdk/SDK 包等）；必要的大文件请使用外部存储并在 README 说明。
- 无硬编码字符串（资源请放入 `strings.xml`）。
- 新增资源（图片、字符串）应遵循命名规范并放入对应目录。
- 更新文档（README、DEVELOPMENT、CHANGELOG）以说明影响与迁移步骤。

## 代码风格与检查
- Java/Kotlin 使用项目默认风格（建议使用 Android Studio 的格式化工具）。
- 提交前运行 `./gradlew lint`（如果适用）并修复明显警告。

## 资源与本地化
- UI 文本必须放在 `res/values/strings.xml` 中，并避免在布局中硬编码文本。
- 图标优先使用矢量 drawable（`res/drawable/*.xml`），确保在不同密度下显示正常。

## 问题与讨论
- 在实现非 trivial 功能前，建议先打开 Issue 讨论方案或在 PR 中详细描述设计与兼容性考量。

感谢你的贡献！
