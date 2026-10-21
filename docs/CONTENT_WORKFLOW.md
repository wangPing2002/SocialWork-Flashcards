# 项目维护约定（v3，持续适用）

## 唯一职责与目录边界

| 目录/文件 | 职责 | 是否进入 APK |
|---|---|---|
| `app/src/main/assets/cards.json` | 实际卡片、正式答案 | 是 |
| `app/src/main/assets/quick_memory.json` | **已迁移删除；仅保存在独立 ZIP/Git 历史中** | 否 |
| `app/src/main/assets/quick_memory_v3.json` | 唯一运行时快速记忆资源；2497 张完整覆盖，内容审核状态独立记录 | 是 |
| `app/src/main/assets/service_framework.json` | 教材框架资源 | 是 |
| `content/master/cards_master_2497.md` | 2497 张正式答案母版 | 否 |
| `content/ledger/review_progress.csv` | 逐卡审校状态、正式答案 SHA-256 | 否 |
| `content/references/`、`content/metadata/` | 资料索引与题库元数据 | 否 |
| `archive/manifests/`、`archive/reviews/` | 已验收历史及审校依据（只读，勿重复写入） | 否 |
| `archive/qa/`、`archive/reports/` | 旧检查结果，非运行时资料，择时审查清退 | 否 |
| `scripts/` | **跨批次长期维护的工具**，禁止按 Round 复制版本 | 否 |

### 严格的工具准入规则

- 正式答案修订：始终使用 **`scripts/apply_answer_patch.py`**，其本身执行旧答案指纹核验、母版/卡片/台账同步。不得新增 `sync_roundXX_*.py`。
- 快速记忆修订：统一使用 `scripts/apply_memory_patch.py` v3 单文件协议，逐卡核验题干、正式答案 SHA、原 v3 记录 SHA。旧双文件补丁不可直接重放。
- 全库检查：**`scripts/verify_repository.py`**，不修改文件；GitHub Actions 在 APK 构建前调用。
- 备份：**`scripts/backup_project.py`** 或补丁工具自己的独立 ZIP 备份。不使用 Gradle 备份。
- 每一批只生成 **审校数据 JSON、审计清单/报告**，不复制一份 Python 脚本，也不把交付中间稿塞到 Git 仓库。
- `archive/legacy_scripts/` 已淘汰并清理；Git 历史仍可回溯旧脚本。历史审核哈希、已确认的旧审校证据不能跟随脚本误删。

## 如何使用快速记忆统一工具（B02 及后续）

在 PowerShell 中：

```powershell
$repo = 'D:\AndroidProjects\SocialWork-Flashcards'
$patch = 'E:\Download\B02_25_REVIEW_CANDIDATES.json'
$backup = 'E:\Download\SocialWork-Flashcards-backups\v3'

python "$repo\scripts\apply_memory_patch.py" --repo $repo --patch $patch
# 检查 PREFLIGHT OK 与预计更新卡片数后再继续
python "$repo\scripts\apply_memory_patch.py" --repo $repo --patch $patch --backup-dir $backup --apply
python "$repo\scripts\verify_repository.py"
```

**注意：** 本示例中的 B02 第一组仍是候选稿，不表示已通过教材逐题核实或正式内容验收。正式应用应等待 B02 完成最终审阅后再进行。

### 快速记忆 v3 单文件补丁协议

仅允许更新 `app/src/main/assets/quick_memory_v3.json`，旧文件只在 Git 历史和独立备份中保存。以下为样式说明，不含真实卡片：

```json
{
  "schemaVersion": "SWF-v3-memory-single-v1",
  "batch": "demo",
  "changes": {
    "C0001": {
      "question": "与现有题干完全相同",
      "questionType": "简答题",
      "expectedAnswerSha256": "真实完整答案 UTF-8 SHA-256",
      "expectedStudySha256": "修改前本卡 v3 记录的规范 JSON SHA-256",
      "study": {
        "framework": ["得分点骨架"],
        "cues": ["提示词"],
        "example": "与题目贴合的真实情境",
        "mnemonic": "能逆向还原得分点的口诀",
        "recallQuestions": ["自测1", "自测2", "自测3"]
      }
    }
  }
}
```

规范 JSON 哈希采用 `json.dumps(ensure_ascii=False, sort_keys=True, separators=(',', ':'))` 的 UTF-8 字节；历史附加字段如 `reviewStatus`、`reviewNote`、`sourceNote` 必须保留。新版工具在写入之前自动独立 ZIP 备份。批量草稿并不等于内容终审批准。

## 提交前不变量

1. `python scripts/verify_repository.py` 成功（2497 卡片、2497 母版、2497 台账、0 答案偏差、0 失效链接）。
2. `git diff --check` 成功；检查全部暂存文件，拒绝密钥与本地缓存。
3. 每次补丁都有可恢复的 ZIP 原始文件备份；APK 编译/真机测试单独验收。
4. 历史已验收正式答案不因补丁版本号更大就自动覆盖。
5. `README.md`、`docs/` 只维护长期规范，不新建按批次复制的工作流介绍文档；临时工作材料存放在外部交付目录。
