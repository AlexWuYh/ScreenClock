package app.screenclock.domain.time

enum class HourMode {
    FollowSystem,
    H12,
    H24,
    ;

    companion object {
        fun fromStored(value: String?): HourMode {
            return entries.firstOrNull { it.name == value } ?: FollowSystem
        }
    }
}

/** Digits for the hour and minute groups, plus the TalkBack line. No seconds. */
data class FaceTime(
    val hourDigits: String,
    val minuteDigits: String,
    val meridiem: String?,
    val spoken: String,
)

fun formatFaceTime(
    hour: Int,
    minute: Int,
    mode: HourMode,
    systemIs24Hour: Boolean,
): FaceTime {
    require(hour in 0..23) { "hour out of range: $hour" }
    require(minute in 0..59) { "minute out of range: $minute" }
    val use24 = when (mode) {
        HourMode.H24 -> true
        HourMode.H12 -> false
        HourMode.FollowSystem -> systemIs24Hour
    }
    val minuteDigits = minute.toString().padStart(2, '0')
    return if (use24) {
        FaceTime(
            hourDigits = hour.toString().padStart(2, '0'),
            minuteDigits = minuteDigits,
            meridiem = null,
            spoken = "${hour}点${minuteDigits}分",
        )
    } else {
        val hour12 = when (val wrapped = hour % 12) {
            0 -> 12
            else -> wrapped
        }
        val meridiem = if (hour < 12) "上午" else "下午"
        FaceTime(
            hourDigits = hour12.toString().padStart(2, '0'),
            minuteDigits = minuteDigits,
            meridiem = meridiem,
            spoken = "$meridiem${hour12}点${minuteDigits}分",
        )
    }
}
