package app.screenclock.ui.ceremony

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.screenclock.R
import app.screenclock.ui.AppIcon
import app.screenclock.ui.AppIconKind
import kotlinx.coroutines.delay

private const val CONTROLS_HIDE_MILLIS: Long = 10_000L

/**
 * Whether the corner buttons of a full-screen page are showing. They fade out after a quiet
 * spell and come back on any touch. For a screen reader user they stay: such a user cannot
 * reach a button in time.
 */
internal class AutoHideControls(val keepVisible: Boolean) {
    var visible by mutableStateOf(true)
    var tick by mutableIntStateOf(0)

    fun touch() {
        visible = true
        tick++
    }
}

@Composable
internal fun rememberAutoHideControls(): AutoHideControls {
    val context = LocalContext.current
    val keep = remember(context) {
        context.getSystemService(AccessibilityManager::class.java)?.isTouchExplorationEnabled == true
    }
    val controls = remember(keep) { AutoHideControls(keep) }
    LaunchedEffect(controls.tick, controls.visible) {
        if (controls.visible && !controls.keepVisible) {
            delay(CONTROLS_HIDE_MILLIS)
            controls.visible = false
        }
    }
    return controls
}

/** Watches presses without consuming them, so the page underneath still gets its taps. */
internal fun Modifier.revealOnTouch(controls: AutoHideControls): Modifier = pointerInput(controls) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press) controls.touch()
        }
    }
}

/** Back at the top left and the light/dark switch at the top right. */
@Composable
internal fun BoxScope.PageControls(
    controls: AutoHideControls,
    palette: CeremonyPalette,
    onBack: () -> Unit,
    onToggleTone: () -> Unit,
) {
    AnimatedVisibility(
        visible = controls.visible,
        enter = fadeIn(),
        exit = fadeOut(tween(400)),
        modifier = Modifier.align(Alignment.TopStart),
    ) {
        CornerButton(palette, AppIconKind.Back, stringResource(R.string.back), onBack, Modifier.padding(12.dp))
    }
    AnimatedVisibility(
        visible = controls.visible,
        enter = fadeIn(),
        exit = fadeOut(tween(400)),
        modifier = Modifier.align(Alignment.TopEnd),
    ) {
        // It shows the tone it will switch to.
        CornerButton(
            palette = palette,
            icon = if (palette.dark) AppIconKind.Sun else AppIconKind.Moon,
            label = stringResource(if (palette.dark) R.string.ceremony_to_light else R.string.ceremony_to_dark),
            onClick = onToggleTone,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun CornerButton(
    palette: CeremonyPalette,
    icon: AppIconKind,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // pointerInput never restarts, so it has to read the newest callback, not the first one.
    val latest = rememberUpdatedState(onClick)
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .background(palette.chip)
            .border(1.dp, palette.chipBorder, shape)
            .semantics { contentDescription = label }
            .pointerInput(Unit) { detectTapGestures(onTap = { latest.value() }) },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(kind = icon, tint = palette.text2, size = 22.dp)
    }
}
