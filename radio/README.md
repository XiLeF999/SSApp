# Radio（收音机）

基于 AOSP FMRadio 二次开发的 FM 收音机应用，面向车载/工控 Android 设备，作为系统级应用运行（`sharedUserId="android.uid.system"`，使用平台密钥签名，依赖 `framework.jar` 中的隐藏 API）。

## 功能

- 频率调节：刻度盘拖拽（RadioScaleView）、±0.1MHz 步进，范围 87.5–108.0MHz
- 电台搜索：全自动扫描（FmService + RadioTuner），搜索进度 UI 动画
- 收藏管理：收藏/取消收藏电台，FM 列表与收藏列表双页签切换
- 播放控制：播放/暂停、上一个/下一个电台（循环切换）
- 后台播放：FmService 前台服务（mediaPlayback），退出 Activity 后收音机继续工作

## 技术栈

- Java + Kotlin 混合开发，ViewBinding + 部分 View（非 Compose，虽引入了 Compose 依赖）
- Android API：RadioManager / RadioTuner（硬件收音）、AudioTrack/AudioRecord（播放录音通路）、前台服务 + 通知
- 签名：platform.jks（ac8257/ky1045）、platform15.jks（ac8267），位于项目根目录

## 代码结构

```
radio/src/main/java/com/sansan/radio/
├── SSApplication.kt            Application，持有全局 Context
├── activity/
│   └── MainActivity.java       主界面：刻度盘、控制按钮、Fragment 切换、服务绑定与消息处理
├── fm/                         核心 FM 逻辑（源自 AOSP FMRadio）
│   ├── FmService.java          前台服务：搜台/切换/播放暂停/录音/音频焦点/耳机插拔等
│   ├── FmStation.java          电台数据库操作（ContentProvider 封装）
│   ├── FmProvider.java         ContentProvider（authority: com.sansan.radio.fm）
│   ├── FmUtils.java            频率/电台换算、公共常量（87.5–108.0MHz，步进 100kHz）
│   ├── FmRecorder.java         录音
│   └── FmListener.java         服务回调接口与消息 ID
├── fragment/
│   ├── FmListFragment.kt       电台列表（搜索到的电台）
│   └── FavListFragment.kt      收藏列表
├── adapter/                    FmAdapter / FavAdapter（RecyclerView）
├── bean/StationInfo.kt         电台数据类
├── dialog/QuitDialog.kt        退出确认弹窗
├── utils/                      线程池、View 显示/隐藏扩展
└── widget/
    └── RadioScaleView.java     自定义频率刻度盘控件
```

## 构建

```bash
./gradlew :radio:assemble<Flavor><Debug|Release>
# 例如：
./gradlew :radio:assembleKy1045Release
```

输出 APK 命名规则：`Radio<芯片型号><D|R><版本名>_<打包时间>.apk`，例如 `Radio1045R1.0_2509171530.apk`。

### Product Flavors（dimension: device_type）

| Flavor | 签名 | 说明 |
|--------|------|------|
| ac8257 | platform.jks | AC8257 方案设备 |
| ac8267 | platform15.jks | AC8267 方案设备（Android 15） |
| ky1045 | platform.jks | KY1045 设备，独立皮肤资源（`src/ky1045/res`），通过 `is_ky1045` 布尔值切换 UI 行为 |

三个 flavor 共用 `src/main` 下的代码与布局；`ac8257`/`ac8267` 无独立资源目录。

## 注意事项

- **平台签名**：应用声明了系统 uid 并使用隐藏 API（framework.jar 以 `compileOnly` + bootstrapClasspath 方式引入），必须使用对应平台密钥签名并预装为系统应用，普通安装无法运行。
- **Java 8**：`compileOptions` 固定为 1.8，改为高版本会导致 framework.jar 编译不过。
- **targetSdk 34**：刻意不升到 35 以避免 Android 15 强制 Edge-to-Edge 全屏影响现有布局。
- **abiFilters**：arm64-v8a / armeabi-v7a / x86 / x86_64。
- 依赖版本统一在根目录 `gradle/libs.versions.toml` 中管理。
