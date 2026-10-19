# 历史补丁与审校材料（只读）

`archive` 是**证据档案**，不是 App 运行时资源目录。

- `manifests/`：Round13—23 的目标答案指纹与其他审校元信息。
- `legacy_scripts/`：历史写回脚本，只用于代码溯源；搬迁后**未按新路径适配**，不得当作可执行生产工具。
- `qa/`、`reports/`、`reviews/`：既往自动质量检测、教材匹配、阶段精修和验收资料。

不要将旧 Round 补丁重新应用到当前 2497 张卡片；若要继续实质修改，使用 `scripts/apply_answer_patch.py` 和 `content/ledger/review_progress.csv`。
