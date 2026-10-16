package app.screenclock.ui.clock

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screenclock.R
import app.screenclock.ui.AppIcon
import app.screenclock.ui.glassPressable
import app.screenclock.ui.glassSurface
import app.screenclock.ui.AppIconKind
import app.screenclock.ui.ChromeBar
import app.screenclock.ui.Cream
import app.screenclock.ui.Hairline
import app.screenclock.ui.Ink
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.clockContentDescription
import app.screenclock.domain.weather.formatWeatherLine
import app.screenclock.ui.theme.ThemedClock
import app.screenclock.ui.theme.scene.ThemeScene
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ClockShell(viewModel: ClockViewModel) {
    val chrome by viewModel.chrome.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val burnIn by viewModel.burnInSession.collectAsStateWithLifecycle()
    val occasionLine by viewModel.occasionLine.collectAsStateWithLifecycle()
    val pinnedOccasionId by viewModel.pinnedOccasionId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = checkNotNull(LocalActivity.current) {
        "ClockShell must run inside an Activity"
    }
    val widthClass = calculateWindowSizeClass(activity).widthSizeClass
    val slot = when (widthClass) {
        WindowWidthSizeClass.Compact -> ClockWidth.Compact
        else -> ClockWidth.Expanded
    }
    val hideAt = chrome.hideAtEpochMillis
    LaunchedEffect(hideAt, chrome.visible) {
        if (!chrome.visible || hideAt == null) return@LaunchedEffect
        val remaining = hideAt - System.currentTimeMillis()
        if (remaining > 0L) delay(remaining)
        viewModel.onChromeTick(System.currentTimeMillis())
    }

    val face = clock?.face
    val dateLine = clock?.dateLine
    val weatherLine = formatWeatherLine(clock?.weather, showRange = slot == ClockWidth.Expanded)
    val description = when {
        face == null -> ""
        else -> clockContentDescription(dateLine, face.spoken, weatherLine, occasionLine)
    }
    val openOccasionLabel = stringResource(R.string.occasion_open)
    val openId = pinnedOccasionId?.takeIf { !occasionLine.isNullOrBlank() }
    // One cross-process read per composition entry, not one per recomposition.
    val animate = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var pillPosition by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .clipToBounds()
            .onGloballyPositioned { rootSize = it.size },
    ) {
        BurnInShift(
            phase = burnIn.phase,
            paused = burnIn.paused,
            anchorMillis = burnIn.anchorMillis,
            expanded = slot == ClockWidth.Expanded,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(viewModel) {
                    detectTapGestures(
                        onPress = {
                            val dismiss = viewModel.onBurnInContact()
                            val released = tryAwaitRelease()
                            if (released && !dismiss) {
                                viewModel.onBlankTap(System.currentTimeMillis())
                            }
                        },
                    )
                }
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    onClick {
                        viewModel.onClockActivate(System.currentTimeMillis())
                        true
                    }
                    if (openId != null) {
                        customActions = listOf(
                            CustomAccessibilityAction(openOccasionLabel) {
                                viewModel.openCeremony(openId, fromList = false)
                                true
                            },
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (face != null) {
                ThemedClock(
                    theme = clock?.theme ?: ClockThemeId.Flip,
                    colorway = clock?.colorway ?: ColorwayId.InkBlack,
                    face = face,
                    dateLine = dateLine,
                    weatherLine = weatherLine,
                    occasionLine = occasionLine,
                    expanded = slot == ClockWidth.Expanded,
                    animate = animate,
                    onOccasionClick = openId?.let { id ->
                        { viewModel.openCeremony(id, fromList = false) }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clearAndSetSemantics { },
                )
            }
        }
        AnimatedVisibility(
            visible = chrome.visible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 20.dp)
                .fillMaxWidth(),
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 3 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 3 },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { pillPosition = it.positionInRoot() }
                    .glassSurface(corner = 26.dp, fill = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.10f)),
            ) {
                // A blurred copy of the theme scene, shifted so its pixels line up
                // with the screen behind the glass.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clipToBounds(),
                ) {
                    val density = LocalDensity.current
                    val sceneWidth = with(density) { rootSize.width.toDp() }
                    val sceneHeight = with(density) { rootSize.height.toDp() }
                    ThemeScene(
                        theme = clock?.theme ?: ClockThemeId.Flip,
                        modifier = Modifier
                            .requiredSize(sceneWidth, sceneHeight)
                            .offset {
                                IntOffset(-pillPosition.x.roundToInt(), -pillPosition.y.roundToInt())
                            }
                            .blur(18.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChromeAction(
                        icon = AppIconKind.Sliders,
                        label = stringResource(R.string.settings),
                        onClick = viewModel::openSettings,
                    )
                    ChromeAction(
                        icon = AppIconKind.Timer,
                        label = stringResource(R.string.focus),
                        onClick = viewModel::openFocus,
                    )
                    ChromeAction(
                        icon = AppIconKind.Heart,
                        label = stringResource(R.string.occasion),
                        onClick = { viewModel.openOccasions(fromSettings = false) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.ChromeAction(
    icon: AppIconKind,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .glassPressable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(kind = icon, tint = Cream, size = 22.dp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Cream, fontSize = 13.sp)
    }
}
