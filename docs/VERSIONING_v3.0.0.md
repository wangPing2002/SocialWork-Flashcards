# 版本命名及发布门禁（v3.0.0）

- 上一个正式版本：`v2.9.0` (`versionCode=25`)。
- 下一目标正式版本：`v3.0.0`。
- 本代码包：`3.0.0-rc1` (`versionCode=27`)，属于未发布测试候选版。
- 功能分支：`feature/v3.0.0-rc1-case-enriched`，从 `b117c0a` 继承，保留现有题库及快速记忆工作。
- GitHub Actions 通过并完成真实 Android 设备运行、原题证据及专业审校后，才能升级为正式版；目前 **没有发布 APK、没有发布 Tag**。
- `BuildConfig.VERSION_NAME` 用于动态显示 App 版本；正式发布时更新 Gradle `versionName` 为 `3.0.0`，确认 `versionCode` 单调递增和签名一致。
- 为保持 RC 历史练习草稿和评级兼容，SharedPreferences 名称 `case_v31_drafts`、`case_v31_ratings` 继续保留，不得机械全局替换。
- 本轮仅更改版本元数据、资源命名、脚本/文档引用与设置页版本显示，不重新生成任何正式答案或原始案例材料。
