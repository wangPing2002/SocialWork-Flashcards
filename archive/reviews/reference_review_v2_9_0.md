# V2.9.0《王思斌〈社会工作概论〉习题详解》参考优先级调整与首次逐卡精修

## 范围与边界

- 基线：`SocialWork-Flashcards-v2.9.0-fixed-search.zip`，不修改 Java、Gradle、GitHub Actions、题目及学习/快速记忆状态。
- PDF：272 页；通过文本解析提取 **324 条可独立定位的习题与考研真题/典型题解析**，每条保留原 PDF 页码、章节、题型和题干。
- 全卡 **2497/2497** 做了一轮全文检索与规则分级；其中自动近义分类仅是“候选”，不代表逐条都经人工逐字确证。
- 源书并不覆盖当前卡库全部细分的实务和案例内容。“无对应答案”表示当前提取题干未产生高置信直接匹配，不表示教材不存在相关知识。
- 本轮人工精修且实际更改 `fullAnswer` 的卡片 **16** 张，其余保持基线答案；避免为了更换引用而覆盖成熟答案。

## 保守匹配统计（按最适配条目，机器初筛+4条人工校订）

| 分类 | 卡数 | 理解 |
|---|---:|---|
| 完全对应 | 37 | 清洗后的题干一致；仍须考察答案是否已有覆盖 |
| 同义考查 | 11 | 提问方式不同，但人工校订或算法判断考查范围接近 |
| 部分覆盖 | 63 | 仅部分知识点一致，不允许整篇直接代替 |
| 无对应答案 | 2386 | 未找到足够置信度的对应解析，继续以教材和 PPT 校核 |
| 合计 | 2497 | 每张卡只有一个“最适配”主匹配，非完整的一对多关联图 |

## 交付文件

- `content/reference_matching_2497_v2_9_0.csv`：2497 张卡的主匹配、状态、页码、相似度及操作；Excel 可直接打开。
- `content/reference_matching_2497_v2_9_0.json`：结构化匹配表。
- `content/reference_book_question_index_v2_9_0.json`：272页详解的324道可定位主习题索引（仅题干和定位，不复制整本答案）。
- `content/reference_edits_v2_9_0.json`：16张实际修改卡片的理由、具体PDF页及原答案校验哈希。
- `scripts/sync_master_answers.py`：将精修后的 App 答案安全同步至 `content/master/*.md`；**需用户运行一次**，因为原ZIP的母版文件名存在跨平台乱码，不宜直接生成依赖该名字的 Git diff。
- `README.md`：合并上一份“答案参考优先级”规则。

## 保留项与风控

1. 只更改人工选定卡片的 `fullAnswer`、`sourceField`、`answerStatus`，**其他所有卡片及字段不变**。修订文字包含正常自然段和 `#C62828` 得分点标记。
2. 详解有真实高校和年份标注不等于东南大学真题；所有 `truthLabel/exactExamSources` 完全不变。
3. `quick_memory_v2_8_8.json`、`service_framework_v2_8_5.json`、`exam_meta.json`、App UI 和原仓库构建方式均不变。
4. 本轮只核查了 PDF 和已有答案能直接支持的改动，没有重新通读四本核心教材；涉及教材间潜在分歧需要下一轮按章节校核。
5. 改动形成于 V2.9.0 tag 发布后；若发布新的正式 APK，建议使用新的修订版本/tag，而不是移动历史 `v2.9.0` 标签。

## 应用

在**与 fixed-search 压缩包同一源码状态**的 Git 仓库根目录：

```powershell
git apply --check v2.9.0_reference_and_answers.patch
git apply v2.9.0_reference_and_answers.patch
python scripts/sync_master_answers.py
git diff --check
git status
```

在执行前保存尚未提交的更改；若 `git apply --check` 失败，请先查看工作区和 Git 版本是否与此基线一致，勿直接 `--reject` 强行合并。

## 尚待完成的后续工作

本轮不是“2497 张逐条人工校核均已完成”：其中 63 张部分覆盖卡及 2386 张没有高置信匹配的卡尚需分章节核对原教材与其他来源。该结果是**可追溯的第一轮筛选+明确的16处答案精修**，而不是把低置信候选批量改写为未经核验的答案。


## 第二轮进度

见 `content/reference_focus111_review_v2_9_0.md`，本轮新增8张精修，总计24张。原先37/11/63只是首次机器分组；第二轮将13个误匹配降级、若干同题问法归为同义考查。补丁应用位置为用户本机 E:\Download，补丁不复制到项目目录。
