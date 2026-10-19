package app.screenclock.domain.time

/** Date is a layout choice, independent of a clock's finish and scenery. */
enum class DatePosition {
    Above,
    Below;

    companion object {
        fun fromStored(raw: String?): DatePosition =
            entries.firstOrNull { it.name == raw } ?: Above
    }
}

/** Keep larger ink inside the clock's safe area on narrow portrait screens. */
enum class DigitSize(val factor: Float) {
    Compact(0.86f),
    Regular(1f),
    Large(1.04f);

    companion object {
        fun fromStored(raw: String?): DigitSize =
            entries.firstOrNull { it.name == raw } ?: Regular
    }
}
