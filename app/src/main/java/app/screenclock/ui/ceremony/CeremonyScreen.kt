package app.screenclock.ui.ceremony

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screenclock.R
import app.screenclock.domain.occasion.CeremonyFace
import app.screenclock.domain.occasion.CeremonyMode
import app.screenclock.domain.occasion.CeremonyParts
import app.screenclock.domain.occasion.CeremonyStyle
import app.screenclock.domain.occasion.ceremonyIsDark
import app.screenclock.domain.occasion.millisUntilCeremonyTick
import app.screenclock.domain.occasion.twoDigits
import app.screenclock.domain.time.DateParts
import app.screenclock.domain.time.formatDateLine
import app.screenclock.ui.AppIcon
import app.screenclock.ui.AppIconKind
import app.screenclock.ui.clock.ClockViewModel
import app.screenclock.ui.occasion.OccasionGlyph
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** The size step of the page, picked from its width like the design's S / M / L. */
private enum class Step { S, M, L }

private val Tabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun CeremonyScreen(viewModel: ClockViewModel) {
    val look by viewModel.ceremonyLook.collectAsStateWithLifecycle()
    val dark = ceremonyIsDark(look.tone, isSystemInDarkTheme())
    val palette = remember(look.style, dark) { ceremonyPalette(look.style, dark) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val face = viewModel.ceremonyNow(now)
    val occurrence = viewModel.ceremonyDate()
    val generation by viewModel.wallClockGeneration.collectAsStateWithLifecycle()
    // delay follows uptime, so sleep and a changed clock have to start a new wait.
    LaunchedEffect(occurrence, generation) {
        if (occurrence == null) return@LaunchedEffect
        while (isActive) {
            val current = LocalDateTime.now()
            now = current
            val wait = millisUntilCeremonyTick(current, occurrence) ?: break
            delay(wait)
        }
        now = LocalDateTime.now()
    }
    BackHandler { viewModel.closeCeremony() }
    if (face == null || occurrence == null) {
        LaunchedEffect(Unit) { viewModel.closeCeremony() }
        Box(modifier = Modifier.fillMaxSize().background(palette.bg))
        return
    }
    val leave = rememberUpdatedState(viewModel::closeCeremony)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .drawBehind { drawGlows(palette, look.style) }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .clipToBounds(),
    ) {
        val pageWidth = maxWidth
        val landscape = maxWidth > maxHeight
        val short = maxHeight < 500.dp
        val step = when {
            maxWidth >= 900.dp -> Step.L
            maxWidth >= 520.dp -> Step.M
            else -> Step.S
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (face.mode == CeremonyMode.Done) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(onPress = { if (tryAwaitRelease()) leave.value.invoke() })
                        }
                    } else {
                        Modifier
                    },
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = face.spoken
                    if (face.mode == CeremonyMode.Done) {
                        onClick {
                            leave.value.invoke()
                            true
                        }
                    }
                },
        ) {
            when (look.style) {
                CeremonyStyle.Card -> CardLayout(face, now, palette, step, landscape)
                CeremonyStyle.Immersive -> ImmersiveLayout(face, now, palette, step, landscape, short, pageWidth)
            }
        }
        BackButton(
            palette = palette,
            onClick = viewModel::closeCeremony,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlows(palette: CeremonyPalette, style: CeremonyStyle) {
    val w = size.width
    val h = size.height
    val big = maxOf(w, h)
    fun glow(color: androidx.compose.ui.graphics.Color, at: Offset, radius: Float) {
        if (color.alpha == 0f) return
        drawRect(Brush.radialGradient(listOf(color, androidx.compose.ui.graphics.Color.Transparent), center = at, radius = radius))
    }
    when (style) {
        CeremonyStyle.Card -> {
            glow(palette.glowA, Offset(w * 0.2f, 0f), big * 0.7f)
            glow(palette.glowB, Offset(w * 0.92f, h), big * 0.65f)
            glow(palette.glowC, Offset(w * 0.5f, h * 0.5f), big * 0.8f)
        }
        CeremonyStyle.Immersive -> {
            glow(palette.glowA, Offset(w * 0.15f, h * 0.1f), big * 0.75f)
            glow(palette.glowB, Offset(w * 0.85f, h * 0.9f), big * 0.75f)
        }
    }
}

@Composable
private fun BackButton(palette: CeremonyPalette, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.back)
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.chip)
            .border(1.dp, palette.chipBorder, RoundedCornerShape(16.dp))
            .semantics { contentDescription = label }
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(kind = AppIconKind.Back, tint = palette.text2, size = 22.dp)
    }
}

private fun clockText(now: LocalDateTime): String = "${twoDigits(now.hour)}:${twoDigits(now.minute)}"

private fun dateText(now: LocalDateTime): String =
    formatDateLine(now.toLocalDate(), parts = DateParts(lunar = false, holiday = false))

/** Days first, with as many digits as the count needs, then hours, minutes and seconds in pairs. */
private fun blockTexts(parts: CeremonyParts): List<String> =
    listOf(twoDigits(parts.days), twoDigits(parts.hours), twoDigits(parts.minutes), twoDigits(parts.seconds))

/** The four blocks with their colons. [maxSize] is the size the design asks for; it shrinks to fit. */
@Composable
private fun CountdownRow(
    parts: CeremonyParts,
    palette: CeremonyPalette,
    maxSize: TextUnit,
    labelSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val texts = blockTexts(parts)
    val labels = listOf(R.string.unit_day, R.string.unit_hour, R.string.unit_minute, R.string.unit_second)
    BoxWithConstraints(modifier = modifier) {
        val chars = texts.sumOf { it.length }
        // About 0.62 em per digit, plus room for the three colons and their gaps.
        val fit = maxWidth.value / (chars * 0.62f + 3 * 0.55f + 0.6f)
        val numSize = minOf(maxSize.value, fit).sp
        val colonSize = (numSize.value * 0.8f).sp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            texts.forEachIndexed { index, text ->
                if (index > 0) {
                    Text(
                        text = ":",
                        color = palette.text3,
                        fontSize = colonSize,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(top = (numSize.value * 0.04f).dp),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = text,
                        color = palette.text1,
                        fontSize = numSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        style = Tabular,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(text = stringResource(labels[index]), color = palette.text3, fontSize = labelSize, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun TitleLine(face: CeremonyFace, palette: CeremonyPalette, titleSize: TextUnit, glyph: Dp, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        OccasionGlyph(icon = face.icon, tint = palette.accent, size = glyph)
        Spacer(Modifier.width(10.dp))
        Text(
            text = face.title,
            color = palette.text1,
            fontSize = titleSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DoneMessage(face: CeremonyFace, palette: CeremonyPalette, size: TextUnit, modifier: Modifier = Modifier) {
    Text(
        text = face.message,
        color = palette.text1,
        fontSize = size,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun TargetChip(face: CeremonyFace, palette: CeremonyPalette, size: TextUnit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(text = stringResource(R.string.ceremony_target), color = palette.text3, fontSize = size)
        Spacer(Modifier.width(8.dp))
        Text(
            text = face.solarLabel,
            color = palette.accent,
            fontSize = size,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(palette.accentBg, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

// ───────────────────────── Card design ─────────────────────────

@Composable
private fun CardLayout(face: CeremonyFace, now: LocalDateTime, palette: CeremonyPalette, step: Step, landscape: Boolean) {
    val clockSize = when (step) { Step.S -> if (landscape) 48.sp else 51.sp; Step.M -> if (landscape) 58.sp else 61.sp; Step.L -> if (landscape) 74.sp else 70.sp }
    val countSize = when (step) { Step.S -> if (landscape) 32.sp else 34.sp; Step.M -> if (landscape) 38.sp else 40.sp; Step.L -> if (landscape) 51.sp else 46.sp }
    val labelSize = when (step) { Step.S -> 11.sp; Step.M -> 11.5.sp; Step.L -> 12.5.sp }
    val titleSize = when (step) { Step.S -> 20.sp; Step.M -> 21.6.sp; Step.L -> 24.sp }
    val maxWidth = when (step) { Step.S -> 360.dp; Step.M -> 520.dp; Step.L -> if (landscape) 900.dp else 720.dp }
    val pad = when (step) { Step.S -> 22.dp; Step.M -> 26.dp; Step.L -> 32.dp }
    val shape = RoundedCornerShape(38.dp)
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        val surface = Modifier
            .widthIn(max = maxWidth)
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.surfaceBorder, shape)
            .padding(horizontal = pad, vertical = pad + 4.dp)
        if (landscape) {
            Row(surface, verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.padding(end = 26.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = clockText(now), color = palette.text1, fontSize = clockSize, fontWeight = FontWeight.SemiBold, style = Tabular, maxLines = 1, softWrap = false)
                    Text(text = dateText(now), color = palette.text2, fontSize = 14.sp, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
                }
                Box(Modifier.width(1.dp).height(160.dp).background(palette.divider))
                Spacer(Modifier.width(26.dp))
                AnniversaryCard(face, palette, countSize, labelSize, titleSize, Modifier.weight(1f))
            }
        } else {
            Column(surface, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = clockText(now), color = palette.text1, fontSize = clockSize, fontWeight = FontWeight.SemiBold, style = Tabular, maxLines = 1, softWrap = false)
                Text(text = dateText(now), color = palette.text2, fontSize = 14.sp, maxLines = 1, modifier = Modifier.padding(top = 8.dp, bottom = 26.dp))
                AnniversaryCard(face, palette, countSize, labelSize, titleSize, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AnniversaryCard(
    face: CeremonyFace,
    palette: CeremonyPalette,
    countSize: TextUnit,
    labelSize: TextUnit,
    titleSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(30.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(palette.card)
            .border(1.dp, palette.cardBorder, shape)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TitleLine(face, palette, titleSize, 26.dp, Modifier.padding(horizontal = 4.dp))
        val wellShape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(wellShape)
                .background(palette.well)
                .border(1.dp, palette.wellBorder, wellShape)
                .padding(horizontal = 10.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (face.mode == CeremonyMode.Counting) {
                CountdownRow(face.parts, palette, countSize, labelSize)
            } else {
                DoneMessage(face, palette, (countSize.value * 0.9f).sp)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
        TargetChip(face, palette, 14.sp, Modifier.fillMaxWidth())
    }
}

// ───────────────────────── Immersive design ─────────────────────────

@Composable
private fun ImmersiveLayout(
    face: CeremonyFace,
    now: LocalDateTime,
    palette: CeremonyPalette,
    step: Step,
    landscape: Boolean,
    short: Boolean,
    width: Dp,
) {
    val clockSize = when {
        landscape && short -> minOf(width.value * 0.055f, 48f).coerceAtLeast(32f)
        landscape -> minOf(width.value * 0.08f, 96f).coerceAtLeast(56f)
        else -> (width.value * 0.15f).coerceIn(64f, 112f)
    }.sp
    val numSize = when {
        landscape && short -> 36.sp
        landscape -> 61.sp
        else -> 72.sp
    }
    val unitSize = if (short) 10.sp else if (step == Step.S) 12.sp else 14.sp
    val titleSize = if (short) 13.sp else if (step == Step.S) 17.sp else 20.sp
    val glyph = if (short) 16.dp else 22.dp
    val dateSize = if (short) 12.sp else if (step == Step.S) 14.sp else 17.sp
    val chipShape = RoundedCornerShape(50)
    val titleChip = Modifier
        .clip(chipShape)
        .background(palette.chip)
        .border(1.dp, palette.chipBorder, chipShape)
        .padding(horizontal = if (short) 10.dp else 16.dp, vertical = if (short) 4.dp else 8.dp)

    @Composable
    fun TimeBlock(align: Alignment.Horizontal) {
        Column(horizontalAlignment = align) {
            Text(
                text = clockText(now),
                color = palette.text1,
                fontSize = clockSize,
                fontWeight = FontWeight.Light,
                style = Tabular,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = dateText(now),
                color = palette.text2,
                fontSize = dateSize,
                letterSpacing = 2.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = if (short) 4.dp else 10.dp),
            )
        }
    }

    @Composable
    fun Counter(align: Alignment.Horizontal) {
        Column(horizontalAlignment = align, modifier = Modifier.fillMaxWidth()) {
            TitleLine(face, palette, titleSize, glyph, titleChip)
            Spacer(Modifier.height(if (short) 8.dp else 24.dp))
            if (face.mode == CeremonyMode.Counting) {
                CountdownRow(face.parts, palette, numSize, unitSize, Modifier.fillMaxWidth().widthIn(max = 560.dp))
            } else {
                DoneMessage(face, palette, if (short) 32.sp else 48.sp, Modifier.widthIn(max = 560.dp))
            }
            Spacer(Modifier.height(if (short) 8.dp else 24.dp))
            TargetChip(face, palette, if (short) 12.sp else 15.sp)
        }
    }

    if (landscape) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (short) 24.dp else 48.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(0.4f).fillMaxHeight().padding(end = 24.dp),
                contentAlignment = Alignment.CenterStart,
            ) { TimeBlock(Alignment.Start) }
            Box(Modifier.width(1.dp).fillMaxHeight(0.6f).background(palette.divider))
            Box(
                modifier = Modifier.weight(0.6f).fillMaxHeight().padding(start = 32.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Counter(Alignment.Start) }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimeBlock(Alignment.CenterHorizontally)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Counter(Alignment.CenterHorizontally)
            }
        }
    }
}
