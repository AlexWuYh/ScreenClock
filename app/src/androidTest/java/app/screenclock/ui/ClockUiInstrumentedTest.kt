package app.screenclock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.screenclock.ui.occasion.DateWheelDialog
import app.screenclock.ui.theme.flip.FlipFlaps
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClockUiInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun rtlStillShowsHourCardToTheLeftOfMinuteCard() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.size(320.dp, 180.dp)) {
                    FlipFlaps(listOf("14", "30"), animate = false, modifier = Modifier.fillMaxSize())
                }
            }
        }
        val hour = compose.onNodeWithTag("flip-group-0", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.left
        val minute = compose.onNodeWithTag("flip-group-1", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.left
        assertTrue("RTL must not reverse numeric groups", hour < minute)
    }

    @Test
    fun dateWheelAnnouncesCurrentSelectionAndOffersAdjustment() {
        compose.setContent {
            DateWheelDialog(
                title = "选择日期",
                showYear = false,
                yearRange = 2026..2028,
                initialYear = 2026,
                initialMonth = 2,
                initialDay = 28,
                dayMaxOf = { _, month -> if (month == 2) 28 else 31 },
                onPick = { _, _, _ -> },
                onDismiss = { },
            )
        }
        val month = compose.onNode(hasStateDescription("月，当前2，共12项"), useUnmergedTree = true)
        month.assertExists()
        compose.onNode(hasStateDescription("日，当前28，共28项"), useUnmergedTree = true)
            .assertExists()
        val actions = month.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertTrue(actions.any { it.label == "增加月" })
        assertTrue(actions.any { it.label == "减少月" })
    }
}
