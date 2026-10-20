package app.screenclock.ui.occasion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.screenclock.R
import app.screenclock.ui.Amber
import app.screenclock.ui.Cream
import app.screenclock.ui.GlassFillHigh
import app.screenclock.ui.glassSurface
import app.screenclock.ui.Info
import app.screenclock.ui.Muted
import app.screenclock.ui.Panel
import app.screenclock.ui.PanelHigh
import kotlinx.coroutines.launch

private val WheelItemHeight = 44.dp
private const val WheelVisibleItems = 3

/**
 * One rolling number column. The middle row is the selection and is reported
 * once the wheel settles, so the caller can clamp the other columns.
 */
@Composable
private fun WheelColumn(
    label: String,
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val start = selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))
    val state = rememberLazyListState(initialFirstVisibleItemIndex = start)
    val scope = rememberCoroutineScope()
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    // Keep a column in sync if the other column clamps its selected value.
    LaunchedEffect(start) {
        if (!state.isScrollInProgress && state.firstVisibleItemIndex != start) {
            state.scrollToItem(start)
        }
    }
    val settled by remember(items) {
        derivedStateOf {
            val base = state.firstVisibleItemIndex
            val half = state.layoutInfo.viewportEndOffset / (WheelVisibleItems * 2)
            if (state.firstVisibleItemScrollOffset > half) base + 1 else base
        }
    }
    LaunchedEffect(state, items.size) {
        snapshotFlow { state.isScrollInProgress to settled }
            .collect { (scrolling, index) ->
                if (!scrolling && items.isNotEmpty()) {
                    onSelected(index.coerceIn(items.indices))
                }
            }
    }
    val visibleIndex = if (state.isScrollInProgress) settled.coerceIn(items.indices) else start
    val stateText = stringResource(R.string.occasion_wheel_state, label, items[visibleIndex], items.size)
    val increaseLabel = stringResource(R.string.occasion_wheel_increase, label)
    val decreaseLabel = stringResource(R.string.occasion_wheel_decrease, label)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .height(WheelItemHeight * WheelVisibleItems)
                .widthIn(min = 72.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(WheelItemHeight)
                    .glassSurface(corner = 12.dp, fill = GlassFillHigh),
            )
            LazyColumn(
                state = state,
                flingBehavior = fling,
                contentPadding = PaddingValues(vertical = WheelItemHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .height(WheelItemHeight * WheelVisibleItems)
                    .fillMaxWidth()
                    // Add state/actions without clearing LazyColumn's built-in scroll semantics.
                    .semantics {
                        contentDescription = label
                        stateDescription = stateText
                        customActions = buildList {
                            if (visibleIndex < items.lastIndex) {
                                add(CustomAccessibilityAction(increaseLabel) {
                                    val next = visibleIndex + 1
                                    scope.launch {
                                        state.scrollToItem(next)
                                        onSelected(next)
                                    }
                                    true
                                })
                            }
                            if (visibleIndex > 0) {
                                add(CustomAccessibilityAction(decreaseLabel) {
                                    val previous = visibleIndex - 1
                                    scope.launch {
                                        state.scrollToItem(previous)
                                        onSelected(previous)
                                    }
                                    true
                                })
                            }
                        }
                    },
            ) {
                itemsIndexed(items) { index, item ->
                    Box(
                        modifier = Modifier
                            .height(WheelItemHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item,
                            color = if (index == settled) Cream else Muted,
                            fontWeight = if (index == settled) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 20.sp,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Info, fontSize = 14.sp, modifier = Modifier.clearAndSetSemantics { })
    }
}

/**
 * The date wheel for the occasion form. [showYear] is off for yearly dates.
 * Changes apply on 「完成」; the form preview follows immediately after.
 */
@Composable
internal fun DateWheelDialog(
    title: String,
    showYear: Boolean,
    yearRange: IntRange,
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    dayMaxOf: (year: Int, month: Int) -> Int,
    onPick: (year: Int, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var pickedYear by remember { mutableIntStateOf(initialYear) }
    var pickedMonth by remember { mutableIntStateOf(initialMonth) }
    var pickedDay by remember { mutableIntStateOf(initialDay) }
    val dayMax = dayMaxOf(pickedYear, pickedMonth)
    LaunchedEffect(dayMax) {
        if (pickedDay > dayMax) pickedDay = dayMax
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .glassSurface(corner = 22.dp, fill = GlassFillHigh)
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .widthIn(min = 280.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, color = Cream, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (showYear) {
                    WheelColumn(
                        label = stringResource(R.string.occasion_wheel_year),
                        items = yearRange.map { it.toString() },
                        selectedIndex = pickedYear - yearRange.first,
                        onSelected = { pickedYear = yearRange.first + it },
                    )
                }
                WheelColumn(
                    label = stringResource(R.string.occasion_wheel_month),
                    items = (1..12).map { it.toString() },
                    selectedIndex = pickedMonth - 1,
                    onSelected = { pickedMonth = it + 1 },
                )
                key(dayMax) {
                    WheelColumn(
                        label = stringResource(R.string.occasion_wheel_day),
                        items = (1..dayMax).map { it.toString() },
                        selectedIndex = pickedDay - 1,
                        onSelected = { pickedDay = it + 1 },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.occasion_cancel), color = Info, fontSize = 16.sp)
                }
                TextButton(
                    onClick = {
                        onPick(pickedYear, pickedMonth, pickedDay.coerceAtMost(dayMax))
                    },
                ) {
                    Text(
                        text = stringResource(R.string.occasion_done),
                        color = Amber,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
