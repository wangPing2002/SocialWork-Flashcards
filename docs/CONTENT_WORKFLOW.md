# 项目维护约定（v3，持续适用）

## 唯一职责与目录边界

| 目录/文件 | 职责 | 是否进入 APK |
|---|---|---|
| `app/src/main/assets/cards.json` | 实际卡片、正式答案 | 是 |
| `app/src/main/assets/quick_memory.json` | 向下兼容的简易快速记忆 | 是 |
| `app/src/main/assets/quick_memory_v3.json` | 五模块结构化记忆，只有通过审查的 ID 才写入 | 是 |
| `app/src/main/assets/service_framework.json` | 教材框架资源 | 是 |
| `content/master/cards_master_2497.md` | 2497 张正式答案母版 | 否 |
| `content/ledger/review_progress.csv` | 逐卡审校状态、正式答案 SHA-256 | 否 |
| `content/references/`、`content/metadata/` | 资料索引与题库元数据 | 否 |
| `archive/manifests/`、`archive/reviews/` | 已验收历史及审校依据（只读，勿重复写入） | 否 |
| `archive/qa/`、`archive/reports/` | 旧检查结果，非运行时资料，择时审查清退 | 否 |
| `scripts/` | **跨批次长期维护的工具**，禁止按 Round 复制版本 | 否 |

### 严格的工具准入规则

- 正式答案修订：始终使用 **`scripts/apply_answer_patch.py`**，其本身执行旧答案指纹核验、母版/卡片/台账同步。不得新增 `sync_roundXX_*.py`。
- 快速记忆修订：始终使用 **`scripts/apply_memory_patch.py`**，支持 B02 及后续统一数据 JSON，要求题干、题型、正式答案 SHA、旧记忆 SHA 全部一致，不覆盖未知的结构化记忆条目。
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

### 快速记忆数据契约（不需要逐批写脚本）

每个增量包采用以下结构：

```json
{
  "schemaVersion": "B02-memory-v1",
  "batch": "B02-final",
  "changes": {
    "C0168": {
      "question": "必须与本地题干一致",
      "questionType": "简答题",
      "expectedAnswerSha256": "当前 fullAnswer 的精确 SHA-256（64 位十六进制）",
      "expectedMemorySha256": "当前旧快速记忆记录的规范 JSON SHA-256",
      "expectedStudyAbsent": true,
      "memory": {"example": "例子", "mnemonic": "口诀或逻辑提示"},
      "study": {
        "framework": ["按真实得分点组织的答题骨架"],
        "cues": ["关键词"],
        "example": "解释该题的贴题例子",
        "mnemonic": "口诀或逻辑提示",
        "recallQuestions": ["具体的闭卷自测问题一", "具体的闭卷自测问题二"]
      }
    }
  }
}
```

`expectedAnswerSha256` 为 `SHA256(fullAnswer UTF-8 原始字节)`；`expectedMemorySha256` 为规范 JSON（`ensure_ascii=False, sort_keys=True, separators=(',', ':')`）的 SHA-256。**绝不能绕过这些校验强制覆盖现有已审校内容。**

## 提交前不变量

1. `python scripts/verify_repository.py` 成功（2497 卡片、2497 母版、2497 台账、0 答案偏差、0 失效链接）。
2. `git diff --check` 成功；检查全部暂存文件，拒绝密钥与本地缓存。
3. 每次补丁都有可恢复的 ZIP 原始文件备份；APK 编译/真机测试单独验收。
4. 历史已验收正式答案不因补丁版本号更大就自动覆盖。
5. `README.md`、`docs/` 只维护长期规范，不新建按批次复制的工作流介绍文档；临时工作材料存放在外部交付目录。
