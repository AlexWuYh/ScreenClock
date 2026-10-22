package app.screenclock.ui.clock

/**
 * Whether the control layer is on screen.
 *
 * A blank tap shows it and arms an idle hide time. Interacting with chrome resets it; another blank tap hides it.
 * Once [hideAtEpochMillis] is reached, it hides and stays hidden until the next tap.
 */
data class ChromeState(
    val visible: Boolean = false,
    val hideAtEpochMillis: Long? = null,
) {
    fun onBlankTap(nowEpochMillis: Long, hideAfterMillis: Long = HIDE_AFTER_MILLIS): ChromeState {
        require(hideAfterMillis > 0L) { "hideAfterMillis must be positive" }
        return if (visible) {
            ChromeState()
        } else {
            ChromeState(
                visible = true,
                hideAtEpochMillis = nowEpochMillis + hideAfterMillis,
            )
        }
    }

    /** Any action on visible chrome restarts its idle countdown. */
    fun onInteraction(nowEpochMillis: Long, hideAfterMillis: Long = HIDE_AFTER_MILLIS): ChromeState {
        require(hideAfterMillis > 0L) { "hideAfterMillis must be positive" }
        return if (visible) copy(hideAtEpochMillis = nowEpochMillis + hideAfterMillis) else this
    }

    fun after(nowEpochMillis: Long): ChromeState {
        val hideAt = hideAtEpochMillis
        if (visible && hideAt != null && nowEpochMillis >= hideAt) {
            return ChromeState()
        }
        return this
    }

    companion object {
        const val HIDE_AFTER_MILLIS: Long = 8_000L
    }
}

enum class ClockWidth {
    Compact,
    Expanded,
}

/** Same cut as Material 3 compact width. Medium and expanded share one slot. */
const val COMPACT_MAX_WIDTH_DP: Int = 600

fun clockWidth(windowWidthDp: Int): ClockWidth {
    return if (windowWidthDp < COMPACT_MAX_WIDTH_DP) ClockWidth.Compact else ClockWidth.Expanded
}
