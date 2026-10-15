# ScreenClock

全屏安卓时钟。M5 专注时钟已完成。下一里程碑是 **M6 仪式倒计时**（`planned`）。

## 当前阶段

M5 已完成：控制层可以进入专注。先选 25 分钟、50 分钟或 1–180 分钟，再开始。倒计时按结束时刻计算。不足 1 小时是分、秒两块页片，满 1 小时是时、分、秒三块。这一页没有日期、天气和纪念日。长按约 600ms 提前结束。到 0 后停住，点一下回钟面。倒计时和完成态在前台时屏幕常亮，离开该页就取消。进程重启后，还没被清掉的专注会回到这一页。还没有纪念日。

用户未要求进入下一里程碑时，不要把 M6 标成 `in_progress` 并开始编码。

详情：[`.ai/MILESTONES.md`](./.ai/MILESTONES.md)

## 必读

| 文档 | 何时读 |
|------|--------|
| [`.ai/00_PROJECT.md`](./.ai/00_PROJECT.md) | 动手前 |
| [`.ai/MILESTONES.md`](./.ai/MILESTONES.md) | 动手前。只实现允许的里程碑 |
| [`.ai/01_DESIGN.md`](./.ai/01_DESIGN.md) | 改界面、动画、天气、纪念日、防烧屏、专注之前 |
| [`.ai/02_STRUCTURE.md`](./.ai/02_STRUCTURE.md) | 新增包或模块之前 |
| [`.ai/03_SECURITY.md`](./.ai/03_SECURITY.md) | 涉及网络、定位、权限之前 |

## 命令

JDK 17。Android SDK 需要 `platforms;android-37.0` 和 `build-tools;36.0.0`。路径写在不入库的 `local.properties`（`sdk.dir=`）。没有名为 `platforms;android-37` 的包。

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

这三项在本仓库已经跑通。不要把它们写进 CI，除非用户要求。

## 工程

- 单模块 `:app`。`compileSdk` 写 `release(37)`，对应目录 `platforms/android-37.0`。`minSdk` 26，`targetSdk` 37。
- AGP 9 自带 Kotlin。不要再套 `org.jetbrains.kotlin.android`。Compose 编译器用 `org.jetbrains.kotlin.plugin.compose`。
- 版本在 `gradle/libs.versions.toml`。
- 自适应图标留在 `mipmap-anydpi-v26`。lint 会说 v26 多余；挪到 `mipmap-anydpi` 后 aapt 会报找不到 `mipmap/ic_launcher`。

## 约定

- 只实现 [`MILESTONES.md`](./.ai/MILESTONES.md) 里状态为 `in_progress` 的里程碑。M5 已是 `done`。现在没有 `in_progress`。
- 天气只在开关打开且已有城市时请求。主机只有 `geocoding-api.open-meteo.com` 和 `api.open-meteo.com`，不跟随跳转到别的主机。粗略定位只在用户打开「使用当前位置」时申请。Open-Meteo 没有反向地理编码，当前位置的显示名是「当前位置」。
- 界面文案用简体中文，字符串放 `res/values/strings.xml`。
- 主屏数字不每秒重绘。防烧屏开启且不在屏保中时，允许大约 1 秒一步的 1–2px 漂移；只有屏保运行中才按帧移动。秒级翻页只允许出现在专注页，以及仪式页的最后 24 小时。
- 翻页是两位一块页片，翻折按设计文档的两段式自己绘制。不要引入 FlipClock 或 FliTik 的字体、图片、音效和源码。
- 不把 FliTik 的账号、网络背景、白噪音、语音报时、庆祝视频做进 v1。
- 主屏某个信息失败时藏起该槽，不把错误原文放上钟面。
- 小时制、主题、天气、纪念日、防烧屏都用设计文档里的现有模型。
- 钟面只有深色一种。v1 不做亮色主题。
- 纯规则（闲置状态机、下次纪念日、天气代码文案、小时制格式、专注剩余）放 JVM 可测的 Kotlin，不进 Composable。

## 禁止

- 闹钟、桌面小组件、世界时钟、多城市、白噪音、专注统计、前台服务、`USE_EXACT_ALARM`、`SCHEDULE_EXACT_ALARM`。
- 天气 API 密钥，以及 Open-Meteo 以外的天气服务。
- 手写农历算法。农历换算只用 `cn.6tail:lunar`。
- 防烧屏屏保循环播放。一次闲置周期只跑一轮。
- 入库密钥、keystore 密码。`local.properties` 不入库。

## 文档同步

改行为、权限、命令或里程碑状态时，同一任务里更新对应 `.ai/` 文件，以及本文件的「当前阶段」和「命令」。
