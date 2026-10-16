# ScreenClock

全屏安卓时钟。v1（M0 到 M6）已完成。M7 床头已完成。当前没有进行中的里程碑。

## 当前阶段

Review 修复已完成（记录见 `.ai/REVIEW.md`）。这轮没有新功能，只修问题：天气在前台按 30 分钟过期自动刷新（失败后不短于 30 分钟重试）；长按结束专注有按住进度条和到点触觉；删除纪念日有确认框；宽屏主题预览不再把页片压塌；纪念日日期改用滚轮选择器（保留 +/− 微调）；`ClockViewModel` 拆成 ClockFace/Weather/Occasion/Focus/BurnIn 五个控制器并注入时钟；纪念日预览计算移出组合；定位竞态与 JSON/文件容错加固；测试 161 个，含控制器接线测试。

M7 已完成：夜间在本地钟点窗口内只压低本窗口亮度，默认关，默认 22 点到 7 点。暖色默认关，只在变暗生效时叠一层。充电时常亮默认关，插着电才算。常亮标志只在应用根上设置一次。

M6 已完成：可以保存最多 30 条纪念日，种类是阳历一次、阳历每年、农历每年（含闰月）。添加时能看到下一次公历日期。置顶只保留一条，出现在钟面下方。点钟面条或列表进入全屏仪式：不少于 24 小时只翻天数，最后 24 小时翻时、分、秒，刚进入时显示 23:59:59。到点后只留一句完成文案。仪式页不因此常亮。回到前台或系统改了时间，天数和时钟马上重算。进程重启不恢复仪式页。日期打开且当天是法定休息日时，日期行带库给出的节日短名。

专注仍在：控制层进入，25 分钟、50 分钟或 1–180 分钟。不足 1 小时是分、秒两块，满 1 小时是时、分、秒三块。长按约 600ms 结束。到 0 后停住，点一下回钟面。专注只在倒计时和完成态要求常亮。选择时长那一页不因此常亮。

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

这三项在本仓库已经跑通。不要把它们写进 CI，除非用户要求。若 `lintDebug` 在 `lintAnalyze` 阶段 JVM 崩溃（本机 JDK 17 的 C1 编译器 bug），用 `./gradlew :app:lintDebug -Dorg.gradle.jvmargs="-Xmx4g -XX:-TieredCompilation"` 重跑。

## 工程

- 单模块 `:app`。`compileSdk` 写 `release(37)`，对应目录 `platforms/android-37.0`。`minSdk` 26，`targetSdk` 37。
- AGP 9 自带 Kotlin。不要再套 `org.jetbrains.kotlin.android`。Compose 编译器用 `org.jetbrains.kotlin.plugin.compose`。
- 版本在 `gradle/libs.versions.toml`。
- 自适应图标留在 `mipmap-anydpi-v26`。lint 会说 v26 多余；挪到 `mipmap-anydpi` 后 aapt 会报找不到 `mipmap/ic_launcher`。

## 约定

- 只实现 [`MILESTONES.md`](./.ai/MILESTONES.md) 里状态为 `in_progress` 的里程碑。当前没有进行中的里程碑。不要开始「以后」里的素白、声音、秒钟、小组件。
- 天气只在开关打开且已有城市时请求。主机只有 `geocoding-api.open-meteo.com` 和 `api.open-meteo.com`，不跟随跳转到别的主机。粗略定位只在用户打开「使用当前位置」时申请。Open-Meteo 没有反向地理编码，当前位置的显示名是「当前位置」。
- 界面文案用简体中文，字符串放 `res/values/strings.xml`。例外：纯规则产出的文案（预览句、天气短语、日期与读屏格式）留在 `domain` 纯 Kotlin 里，用单元测试锁字面值，不进资源文件。
- 主屏数字不每秒重绘。防烧屏开启且不在屏保中时，允许大约 1 秒一步的 1–2px 漂移；只有屏保运行中才按帧移动。秒级翻页只允许出现在专注页，以及仪式页的最后 24 小时。
- 翻页是两位一块页片，翻折按设计文档的两段式自己绘制。不要放入外部翻页钟的字体、图片、音效或源码。
- 不做账号、网络背景、白噪音、语音报时、庆祝视频。
- 主屏某个信息失败时藏起该槽，不把错误原文放上钟面。
- 小时制、主题、天气、纪念日、防烧屏都用设计文档里的现有模型。
- 钟面只有深色一种。v1 不做亮色主题。
- 纯规则（闲置状态机、下次纪念日、天气代码文案、小时制格式、专注剩余、夜间窗口、常亮合成）放 JVM 可测的 Kotlin，不进 Composable。

## 禁止

- 闹钟、桌面小组件、世界时钟、多城市、白噪音、专注统计、前台服务、`USE_EXACT_ALARM`、`SCHEDULE_EXACT_ALARM`。
- 天气 API 密钥，以及 Open-Meteo 以外的天气服务。
- 手写农历算法。农历换算只用 `cn.6tail:lunar`。
- 防烧屏屏保循环播放。一次闲置周期只跑一轮。
- 入库密钥、keystore 密码。`local.properties` 不入库。
- 提交 `.ai/`。该目录只留在本机，已经写进 `.gitignore`。

## 文档同步

改行为、权限、命令或里程碑状态时，同一任务里更新本机 `.ai/` 里的对应文件，以及本文件的「当前阶段」和「命令」。不要把 `.ai/` 加进提交。
