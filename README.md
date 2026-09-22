# 东南大学社会工作考研闪卡 Android

一个面向东南大学社会工作考研复习的离线 Android 闪卡应用。当前内置 **306 张规范题干卡片 / 51 个知识点**，题库来自本项目已经整理的教材与历年真题学习资料。

## 功能

- 真题/教材习题式规范题干
- 固定尺寸复习卡片，点击后显示答案
- `不清楚 / 熟悉` 两档快速反馈
- 每张卡保留最近 10 次反馈及时间
- 薄弱卡提高出现权重，并可在当日重复 2—3 次
- 艾宾浩斯式间隔复习
- 每日复习量、新学量分别自定义（默认 30 / 15）
- 例子、完整答案、最近 10 次记录按需打开
- 卡片库按“教材章节 / 真题理论专题”组织
- 搜索题目与知识点
- 掌握 / 熟悉 / 模糊 / 未学习统计
- 薄弱卡 Top 10
- 完全离线；学习记录只保存在手机本地

## GitHub 自动生成 APK（推荐）

把整个仓库上传到 GitHub 后：

1. 打开仓库的 **Actions** 页面。
2. 第一次如有提示，启用 GitHub Actions。
3. 每次 push 到 `main` 或 `master`，`Build Android APK` 工作流会自动运行。
4. 构建完成后进入该次 Workflow，在 **Artifacts** 下载 `seu-socialwork-flashcards-debug-apk`。
5. 解压后得到 `app-debug.apk`，传到 Android 手机即可安装。

也可以在 GitHub Actions 里手动点击 **Run workflow**。

## 发布正式版本

项目还包含 `.github/workflows/release-apk.yml`。创建版本标签即可自动生成 GitHub Release：

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub 会自动构建 APK 并附加到 Release 页面。当前 Release 使用 debug 签名，适合个人学习和测试；如果未来公开分发，可再配置正式 keystore。

## 上传到 GitHub

在项目根目录运行：

```bash
git init
git add .
git commit -m "feat: initial Android flashcard app"
git branch -M main
git remote add origin https://github.com/YOUR_NAME/YOUR_REPO.git
git push -u origin main
```

如果 Git 还没有配置身份：

```bash
git config --global user.name "你的 GitHub 用户名"
git config --global user.email "你的 GitHub 邮箱"
```

## Android Studio

也可以直接用 Android Studio 打开项目根目录。项目使用：

- Java 17
- Android Gradle Plugin 8.7.3
- compileSdk / targetSdk 35
- minSdk 24
- 纯 Android Framework Java，无第三方 UI/数据库依赖

> 仓库没有提交官方 Gradle Wrapper 二进制文件；GitHub Actions 会自动安装 Gradle 8.10.2。Android Studio 可使用本机/IDE 管理的 Gradle，或在本机执行 `gradle wrapper --gradle-version 8.10.2` 后生成标准 wrapper。

## 题库文件

- `app/src/main/assets/cards.json`：碎片化记忆卡
- `app/src/main/assets/details.json`：例子、名词解释、简答、论述/案例完整答案

以后只需要更新这两个文件，就可以在不重写 UI 的情况下升级题库。

## 数据与隐私

应用不申请网络权限。熟悉度、最近 10 次历史、每日进度等全部使用 `SharedPreferences` 保存在手机本地。
