# 社会工作闪卡

面向社会工作考研复习的 Android 离线闪卡应用。当前题库延续上一版扩充内容，并重新设计了学习逻辑、首页、复习页、卡片库、统计页和应用图标。

## V2 UI / 学习逻辑更新

- App 显示名称统一为 **社会工作闪卡**。
- 原“每日复习量 + 每日新学量”合并为一个 **每日学习量**。
- 首页提供 **开始学习** 与 **继续学习** 两个并列入口。
- 学习进度会保存，退出或切换页面后可从上次中断位置继续。
- **复习只会从已经学习过的卡片中抽取**；未学习卡只会作为首次学习内容进入学习队列。
- 已学习卡允许重复复习；最近 10 次中多次“不清楚”的卡片会提高优先级，并可在同一学习队列中重复出现 2—3 次。
- 卡片继续保留“例子 / 完整答案 / 最近10次”三个按需入口，避免把长内容挤在主卡面。
- 卡片库按“社会工作原理 / 社会工作实务 / 督导与管理 / 个案工作 / 小组工作 / 社区工作”展示学习进度。
- 统计页提供最近 7 天学习趋势、掌握分布与薄弱卡片 TOP 10。
- 新增薄荷绿教育/关怀主题应用图标。

## 每日学习量

在“我的”页面只需设置一个数字，例如 30：

- 当天尚有学习额度时，“开始学习”生成当天剩余数量的智能学习队列；
- 当天目标已完成后，再次开始学习会进入加练；
- “继续学习”不会重新抽卡，而是读取上次未完成的队列和位置。

## 智能复习原则

每张卡保留最近 10 次“熟悉 / 不清楚”记录。排序综合考虑：

1. 是否已经学习；
2. 是否达到间隔复习时间；
3. 最近 10 次中“不清楚”的比例；
4. 最近连续“不清楚”的次数；
5. 距离到期时间。

复习内容严格来自已学习卡片。未学习卡不会伪装成“复习卡”。

## GitHub Actions 自动构建 APK

项目已经使用 GitHub 当前 Runner 可用的 Android SDK 绝对路径方案，不依赖 `android-actions/setup-android@v3`。

Push 到 `main` 后：

1. 打开 GitHub 仓库 → **Actions**；
2. 进入 **Build Android APK**；
3. 构建成功后在页面底部 **Artifacts** 下载 `socialwork-flashcards-debug-apk`；
4. 解压即可得到 `app-debug.apk`。

发布版本可使用：

```bash
git tag v2.0.0
git push origin v2.0.0
```

随后 GitHub Releases 会自动生成对应 APK。

## 关键文件

```text
app/src/main/assets/cards.json        闪卡题库
app/src/main/assets/details.json      例子与完整答案
app/src/main/java/.../MainActivity.java  UI 与页面交互
app/src/main/java/.../ReviewEngine.java  智能选卡/复习算法
app/src/main/java/.../StudyStore.java    学习记录与会话进度
app/src/main/res/mipmap-*/ic_launcher.png 应用图标
```

## 数据隐私

学习进度、最近 10 次记录、每日学习量和中断会话全部保存在 Android 本机 `SharedPreferences`，不需要账号，也不会上传学习数据。
