# ScreenClock

放在床头或桌上的全屏安卓时钟。默认是翻页钟，手机和平板、竖屏和横屏共用一块钟面。

当前能打出调试包。装上后主屏是翻页钟或灯管钟：竖屏时、分上下排列，横屏同一行。点空白处出现「设置」和「专注」，大约 4 秒后收起。设置里可以改小时制、主题、日期、天气和防烧屏。主题用两块钟面预览选择。天气默认关。打开后可以搜索一个城市，或使用一次大致位置；钟面在时分下方显示一行现象和温度。数据来自 [Open-Meteo](https://open-meteo.com/)。防烧屏默认关。打开后，无操作一段时间，钟面缓慢移动一轮然后停住，再点一下才重新计时。专注可选 25 分钟、50 分钟或 1 到 180 分钟。倒计时是全屏翻页，长按结束；到 0 后停住，点一下回到钟面。

## 构建

需要 JDK 17，以及 Android SDK 平台 `android-37.0` 和 Build-Tools `36.0.0`。SDK 路径写在不入库的 `local.properties` 里。

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

调试包在 `app/build/outputs/apk/debug/app-debug.apk`。

## 计划里的能力

- 翻页时钟：两位一块页片（界面参考 FliTik，翻折参考 FlipClock），以及换分时亮一下的灯管主题
- 跟随系统、12 小时、24 小时
- 可关闭的一行天气
- 防烧屏：无操作一段时间后，屏保走一轮
- 专注倒计时
- 仪式倒计时：阳历的一次性日期和每年重复，以及农历生日（含闰月）。一天以上显示天数，最后一天才翻时分秒

夜间变暗、充电时常亮、声音和更多主题在后续里程碑里，不在当前这一轮。

## 文档

| 文档 | 内容 |
|------|------|
| [AGENTS.md](./AGENTS.md) | 给编码助手的入口 |
| [.ai/00_PROJECT.md](./.ai/00_PROJECT.md) | 目标和范围 |
| [.ai/MILESTONES.md](./.ai/MILESTONES.md) | 里程碑与验收 |
| [.ai/01_DESIGN.md](./.ai/01_DESIGN.md) | 交互、翻页、农历和屏保 |
| [.ai/02_STRUCTURE.md](./.ai/02_STRUCTURE.md) | 开工后的目录 |
| [.ai/03_SECURITY.md](./.ai/03_SECURITY.md) | 权限、天气接口和本地数据 |

下一里程碑是 M6 仪式倒计时。
