# V2.9.0 GitHub Actions 测试说明

本工程以 GitHub Actions 作为标准 Android 测试/构建环境，**不使用 Gradle Wrapper**。

## Debug 测试

推送任意分支后自动触发：

```text
.github/workflows/android-apk.yml
```

标准环境：

```text
Ubuntu latest
Java 17 (Temurin)
Gradle 8.10.2（由 gradle/actions/setup-gradle@v4 安装）
Android SDK 35
Build Tools 35.0.0
```

核心构建命令：

```bash
gradle :app:assembleDebug --stacktrace
```

输出 APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

随后工作流会检查 APK 包信息、重命名并上传为 GitHub Artifact。

## Release 测试/发布

推送 `v*` Tag 或手动运行：

```text
.github/workflows/release-apk.yml
```

核心构建命令：

```bash
gradle :app:assembleRelease --stacktrace
```

Release 需要仓库 Secrets：

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

## 推荐测试分支

```powershell
git switch -c test/v2.9.0
git add -A
git commit -m "test: v2.9.0 github actions build"
git push -u origin test/v2.9.0
```

在 GitHub → Actions → **Build Android APK** 查看构建结果并下载 Artifact。

## 覆盖旧仓库时的说明

本项目的 GitHub Actions **不会调用 `./gradlew`**。测试与发布均由 `gradle/actions/setup-gradle@v4` 安装 Gradle 8.10.2，然后直接执行 `gradle` 命令。

如果将本压缩包直接覆盖到一个旧工作目录，旧目录中原先存在的 `gradlew` / `gradlew.bat` 可能不会因为“覆盖”操作而自动删除；这不影响当前 CI，因为 workflow 不检查也不调用这些文件。若希望仓库内容也完全清理，可在本地手动删除后再提交。
