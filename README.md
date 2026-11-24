# Share Reborn

第三方微博客户端的**原生 Android 重写**（Kotlin + Jetpack Compose），仅手机端。

> 个人自用项目。当前阶段目标：**不登录（游客模式）也能正常浏览微博**。

## 现状

- [x] 工程骨架（Compose + Material3，minSdk 24 / targetSdk 36）
- [x] GitHub Actions 自动构建 + 固定签名 + 可覆盖升级
- [x] App 内置「游客模式自检」探针（访客票据 + 公开 AJAX 接口）
- [x] M2 首页：Compose 卡片流（正文/九宫格图/视频封面/转发块）、下拉刷新、上拉分页
- [x] M2 微博详情页：正文 + 评论列表（只读）、评论分页
- [x] M2 骨架：抽屉菜单 + 顶部 Tab（微博/消息/热门）+ FAB 发布
- [x] M2 游客门禁：点赞/评论/转发/发布 → 「需要登录」提示（不报错）
- [ ] M3 详情增强（楼中楼/图片查看器/视频播放）
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
