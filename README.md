# Share Reborn

第三方微博客户端的**原生 Android 重写**（Kotlin + Jetpack Compose），仅手机端。

> 个人自用项目。当前阶段目标：**不登录（游客模式）也能正常浏览微博**。

## 现状

- [x] 工程骨架（Compose + Material3，minSdk 24 / targetSdk 36）
- [x] GitHub Actions 自动构建 + 固定签名 + 可覆盖升级
- [x] App 内置「游客模式自检」探针（访客票据 + 公开 AJAX 接口）
- [ ] M2 首页流（Compose UI）
- [ ] M3 详情 + 评论（只读）
- [ ] M4 用户主页 + 登录门禁
- [ ] P2 登录 / 发布 / 互动 / 消息

## 构建

```bash
./gradlew :app:assembleRelease -PversionCode=1
```

签名通过环境变量注入（`KEYSTORE_PATH` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`），CI 中从 Secrets 读取。

## 安装

从 Actions 的 Artifacts 或 Release 下载 APK 安装。签名固定，新版可直接覆盖升级。

## 声明

仅供个人学习与自用。接口来自公开网页端，请遵守微博服务条款；因使用产生的风险自负。
