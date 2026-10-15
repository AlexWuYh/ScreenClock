# 结构

单模块 `:app`。业务规则放纯 Kotlin，界面放 Compose。不要为了分层再拆 Gradle 模块。

M6 有翻页和灯管两套主题，共用同一套槽。槽里可以有一行天气，以及置顶纪念日。防烧屏状态机已接入钟面。专注和仪式都从控制层进入，倒计时用同一套页片。

## 现在有的

```
gradle/libs.versions.toml
app/src/main/java/app/screenclock/
  MainActivity.kt
  ScreenClockApp.kt
  ui/Palette.kt
  ui/NightTint.kt   夜间窗口亮度，以及暖色矩阵
  ui/clock/         控制层、分钟钟面、壳、天气刷新、屏保位移、纪念日条
  ui/focus/         选时长、秒级翻页倒计时
  ui/occasion/      纪念日列表和添加表单
  ui/ceremony/      全屏天数、最后 24 小时、完成句
  ui/settings/      显示、天气、防烧屏、夜间、纪念日入口、关于
  ui/theme/         两套主题共用的槽位
  ui/theme/flip/    页片尺寸、翻折、专注和仪式共用的页片排
  ui/theme/tube/    灯管，换分时亮一下
  domain/time/      小时制、日期行、分钟边界、翻折、灯管亮度、底部槽
  domain/weather/   现象文案、天气行、缓存是否过期、城市查询
  domain/burnin/    闲置状态机、时长夹紧、漂移和屏保位移
  domain/focus/     结束时刻、剩余页片、读屏文案
  domain/occasion/  下次发生、预览句、钟面条、仪式模式、天数分组
  domain/bedside/   夜间窗口、暖色是否生效、常亮合成
  data/settings/    DataStore：hour_mode、show_date、theme、colorway
  data/weather/     同一个 DataStore 的天气键，Open-Meteo，一次粗略定位
  data/burnin/      同一个 DataStore 的防烧屏键
  data/focus/       同一个 DataStore 的专注结束时刻
  data/bedside/     同一个 DataStore 的夜间与充电键
  data/occasion/    occasions.json，以及 cn.6tail:lunar 适配
app/src/test/       控制层、小时制、日期、分钟、翻折、灯管、页片尺寸、槽位、天气、防烧屏、专注、纪念日、仪式、节假日名、床头
```

包名与应用 id 都是 `app.screenclock`。

## 边界

| 位置 | 放什么 | 不放什么 |
|------|--------|----------|
| `domain` | 小时制、闲置状态机、下次纪念日、天气代码文案、专注剩余时间、夜间窗口、常亮合成 | Android API、网络、DataStore |
| `data` | DataStore、JSON、HTTP | 页片绘制、文案拼接进界面 |
| `ui` | 布局、翻页绘制、设置表单 | 农历推算、屏保状态转移 |

`domain` 不依赖 Android。测试因此走 `:app:testDebugUnitTest`，不需要设备。

主题从 `ui/clock` 拿已经准备好的显示模型：时间数字、日期、可选天气、可选纪念日条。主题不读取 DataStore，不发网络请求。

## 持久化

- 设置：一个 DataStore，文件名 `clock_settings`。现在写入 `hour_mode`（`HourMode` 的名字）、`show_date`（默认开）、`theme`（`Flip` 或 `Tube`）、`colorway`（缺省 `InkBlack`，设置里先不提供选择）。天气键是 `weather_enabled`、`weather_use_location`、`weather_city_label`、`weather_city_lat`、`weather_city_lon`，以及缓存 `weather_temp_c`、`weather_code`、`weather_high_c`、`weather_low_c`、`weather_fetched_at`。防烧屏键是 `burnin_enabled`（默认关）、`burnin_idle_millis`（默认 5 分钟）、`burnin_run_millis`（默认 2 分钟）。运行中的 `Armed` / `Running` / `Spent` 不写入磁盘，进程重启后若开关开着就从 `Armed` 重新计。专注结束时刻是 `focus_end_epoch_millis`。没有这次专注时不写这个键。床头键是 `night_dim_enabled`（默认关）、`night_dim_start_hour`（默认 22）、`night_dim_end_hour`（默认 7）、`night_warm`（默认关）、`charge_keep_awake`（默认关）。不另开第二个 DataStore 文件。插没插电不落盘。
- 纪念日：`filesDir/occasions.json`，不进 DataStore。写入用临时文件再替换。农历字段是 `month`、`day`、`leap`。不用负月。上限 30 条。不要加 Room。

## 清单

- 一个启动 Activity。
- 不设置 `screenOrientation`。
- `resizeableActivity` 为真。
- 清单里已声明 `INTERNET` 和 `ACCESS_COARSE_LOCATION`。申请粗略定位的代码只在用户打开「使用当前位置」时走。细节见 [安全文档](./03_SECURITY.md)。
