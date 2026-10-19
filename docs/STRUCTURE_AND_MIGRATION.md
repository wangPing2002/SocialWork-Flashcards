# 项目目录规范与迁移说明（2026-10-18）

## 重构原则

这一次仅重构仓库目录、资源名称、审校工具与文档；**不改变 2497 张卡片的题目、答案、标签、考频、真题来源、学习状态数据库结构及 Android 包名**。

- `app/src/main/assets/`：**真正随 APK 打包的运行资源**。`cards.json` 是 App 卡片数据文件，`quick_memory.json` 与 `service_framework.json` 采用稳定文件名。`details.json`、`exam_meta.json`、`exam_answers.json` 保持旧版兼容占位。
- `content/master/cards_master_2497.md`：**标准答案 Markdown 母版**，从 ZIP 里乱码且无法正常解压的长文件名无损恢复；文件正文和字节原样保存。
- `content/metadata/`：题库结构、来源等静态元信息。
- `content/references/`：2497 题的参考匹配与索引，供下一阶段教材核对使用，不进入 APK。
- `content/ledger/review_progress.csv`：逐卡答案 SHA-256 和历史证据状态。第一次生成覆盖 2497/2497 张卡。`HISTORICAL_TARGET_HASH_MATCH` 只表示可核对的 Round13—23 清单的目标答案哈希仍与当前答案一致，**不表示重新验证了教材准确性**；其他状态不能认定为未精修。
- `archive/manifests/`：Round13—23 精修清单，只读历史证据。
- `archive/qa/`、`archive/reports/`、`archive/reviews/`：历史 QA、专题、精修与验收记录。
- `archive/legacy_scripts/`：原始逐 Round 写回脚本、旧导入与旧分类脚本留档，**已经退出运行入口**；其历史相对路径未被维护，禁止直接执行。
- `scripts/`：仍维护的工具。任何自动更改答案应通过 `apply_answer_patch.py`，不应直接运行老脚本。
- `local_backups/`：本机应用安全补丁产生的完整文件备份，已加入 `.gitignore`，不提交 Git。

## 首次验证

在项目根目录执行：

```powershell
python scripts/verify_repository.py
python scripts/audit_answer_types_v2_9_0.py
```

第一条检查 2497 个 ID、母版/卡片答案一致性、哈希台账、快速记忆覆盖和框架关联等硬性不变量；第二条给出题型、字数结构等人工复核提醒，其 `review_flags` **不是**自动判定的错误或需要重写的卡片数。

## 后续每批答案精修

复制 `patches/example_review_patch.json` 为新的批次 JSON，填写被核实的真实 `card_id`、**修改前** `fullAnswer` SHA-256、修改后的完整答案、实际审校范围、核心教材或可追溯辅助材料依据，再执行：

```powershell
python scripts/apply_answer_patch.py --patch patches/next_review.json --dry-run
python scripts/apply_answer_patch.py --patch patches/next_review.json --apply
python scripts/verify_repository.py
```

- `--dry-run` 只检查，不写任何文件；`--apply` 才写入。
- 所有卡片的旧哈希必须与当前 App 和母版**同时相符**。中途发现旧补丁或人工修改时，立即停止。
- 程序更新 `cards.json`、Markdown 母版、逐卡 CSV 台账，生成 `content/ledger/applied_reviews.jsonl`；并在 `local_backups/` 备份三个原始文件。
- 本地验收成功后再提交 Git。不要把通过本地验证的卡片重新套入旧补丁。
- `fullAnswer` 的红色得分点继续统一 `#C62828`；其他字段（题干、真题来源、快速记忆、学习数据）保持不变。必要时单独制定经过审校的字段修改流程。

## 独立 ZIP 源码备份（不使用 Gradle）

在项目根目录运行以下命令即可生成独立 ZIP 快照，内含 `SNAPSHOT_MANIFEST.json`，会对每个文件计算并校验 SHA-256：

```powershell
python scripts/backup_project.py
# 或指定备份位置，例如 E 盘：
python scripts/backup_project.py --output 'E:\Backup\SocialWork-Flashcards-source.zip'
# 对已有快照做无修改复核：
python scripts/backup_project.py --verify 'E:\Backup\SocialWork-Flashcards-source.zip'
```

- 不调用 Gradle，也不复制 `.gradle/`、`build/`、`app/build/` 和 `local_backups/`。
- 默认跳过 `.git/`，这是**源码快照**而非完整 Git 仓库备份。若需要连同完整提交历史与本地分支一起保存，运行 `python scripts/backup_project.py --include-git`。此时压缩包可能显著增大。
- `*.jks`、`*.keystore` 和 `local.properties` 始终排除。**签名密钥必须另外进行安全备份**，不能仅依赖此 ZIP。
- 已有备份文件不会被覆盖；目标必须位于项目目录之外。后续精修的单批自动原文件备份仍使用 `local_backups/`，两者功能不同。
- Android 应用内的“导出备份”只保存学习进度、收藏等用户数据，与源码 ZIP 备份也不同。

## 安全迁移至现有 Git 工作区

**推荐在原 Git 仓库的副本操作**，不直接覆盖仍有未提交改动的工作目录。精简交付 ZIP 不含 `.git`，因为原上传包里的 `.git` 已有约 155 MB 的历史对象；这并不是删除线上仓库历史。

```powershell
# 假设本机已有 D:\AndroidProjects\SocialWork-Flashcards 原仓库
# 一、先备份整个现有仓库，特别是 .git 目录和任何未提交修改
Copy-Item 'D:\AndroidProjects\SocialWork-Flashcards' 'D:\AndroidProjects\SocialWork-Flashcards-backup' -Recurse

# 二、将本次 ZIP 解压为 D:\AndroidProjects\SocialWork-Flashcards-clean
# 三、将 clean 里的文件放入原仓库的 Git 工作树；不要覆盖 .git
# 四、手动删除原仓库中旧的 content/ 根文件、乱码母版、过时资源和旧版 scripts 文件
#    或者在独立分支上以 clean 目录作为新工作树，再逐项复核 Git 差异

cd D:\AndroidProjects\SocialWork-Flashcards
git status --short
python scripts/verify_repository.py
gradle :app:assembleDebug
# 仅当全部检查通过：git add -A; git diff --cached --stat; git commit ...; git push
```

**重要：** 由于 Git 将文件移动表现为“删除旧路径 + 新增新路径”，提交前需同时确认旧路径已删除、新路径已纳入 Git，不要只复制文件而保留旧文件，造成目录重复。**不要在本次结构整理中重新生成旧母版或运行归档的 Round 写回工具。**

## 已知验证边界

本包已执行 Python 级静态及题库一致性检查；此环境未配置完整 Android Gradle/SDK 构建环境，**不能声称已经完成 APK 编译和设备运行验收**。Android Java 改动仅涉及资源路径常量，仍需通过 GitHub Actions 或本地 Gradle 构建验收。
