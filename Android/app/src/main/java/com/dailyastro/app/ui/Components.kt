package com.dailyastro.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val OnboardingGradient = Brush.verticalGradient(listOf(Color(0xFF0A2486), Color(0xFF0E3AA8)))

@Composable
fun T(text: String, size: TextUnit = 14.sp, color: Color = LocalAstro.current.ink, weight: FontWeight = FontWeight.Normal,
      modifier: Modifier = Modifier, align: TextAlign? = null, lineHeight: TextUnit = TextUnit.Unspecified, maxLines: Int = Int.MAX_VALUE) =
    Text(text, modifier, color = color, fontSize = size, fontWeight = weight, fontFamily = Poppins, textAlign = align, lineHeight = lineHeight, maxLines = maxLines)

@Composable
fun AstroCard(modifier: Modifier = Modifier, color: Color = LocalAstro.current.card, padding: Dp = 14.dp, border: Boolean = true,
              content: @Composable ColumnScope.() -> Unit) {
    val c = LocalAstro.current
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color)
        .then(if (border) Modifier.border(1.dp, c.line, RoundedCornerShape(16.dp)) else Modifier).padding(padding), content = content)
}

@Composable
fun SectionTitle(text: String) = T(text, 13.sp, LocalAstro.current.mut, FontWeight.Medium, Modifier.padding(start = 4.dp, top = 6.dp, bottom = 8.dp))

@Composable
fun NavyHeader(title: String, subtitle: String? = null, alignStart: Boolean = false, trailing: @Composable RowScope.() -> Unit = {}) {
    val c = LocalAstro.current
    Row(Modifier.fillMaxWidth().background(c.navy).statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), horizontalAlignment = if (alignStart) Alignment.Start else Alignment.CenterHorizontally) {
            if (alignStart && subtitle != null) T(subtitle, 12.sp, Color.White.copy(alpha = .8f))
            T(title, if (alignStart) 19.sp else 16.sp, Color.White, FontWeight.SemiBold, maxLines = 1)
            if (!alignStart && subtitle != null) T(subtitle, 12.sp, Color.White.copy(alpha = .8f))
        }
        trailing()
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, modifier.fillMaxWidth().height(54.dp), enabled = enabled, shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LocalAstro.current.button, contentColor = Color.White,
            disabledContainerColor = LocalAstro.current.button.copy(alpha = .45f), disabledContentColor = Color.White)) {
        T(text, 15.sp, Color.White, FontWeight.SemiBold)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAstro.current
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(if (selected) c.navy2 else c.card)
        .border(1.5.dp, if (selected) c.navy2 else c.line, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 7.dp)) {
        T(text, 12.5.sp, if (selected) Color.White else c.ink)
    }
}

@Composable
fun RoundImage(@DrawableRes res: Int, size: Dp, radius: Dp? = null, modifier: Modifier = Modifier) =
    Image(painterResource(res), null, modifier.size(size).clip(if (radius == null) CircleShape else RoundedCornerShape(radius)), contentScale = ContentScale.Crop)

@Composable
fun EnergyBar(fraction: Float, modifier: Modifier = Modifier) {
    val c = LocalAstro.current
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "energy")
    Box(modifier.height(7.dp).clip(RoundedCornerShape(5.dp)).background(c.line)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animated).clip(RoundedCornerShape(5.dp)).background(c.acc))
    }
}

@Composable
fun SettingsRow(icon: String?, title: String, subtitle: String? = null, @DrawableRes image: Int? = null, onClick: () -> Unit,
                trailing: @Composable () -> Unit = { T("›", 18.sp, LocalAstro.current.mut) }) {
    val c = LocalAstro.current
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            image != null -> RoundImage(image, 34.dp, 10.dp)
            icon != null -> T(icon, 20.sp, modifier = Modifier.width(34.dp), align = TextAlign.Center)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            T(title, 14.sp, c.ink, FontWeight.Medium)
            if (subtitle != null) T(subtitle, 11.5.sp, c.mut)
        }
        trailing()
    }
}

@Composable
fun Divided(items: List<@Composable () -> Unit>) {
    val c = LocalAstro.current
    items.forEachIndexed { i, item -> item(); if (i < items.lastIndex) HorizontalDivider(color = c.line, thickness = 1.dp) }
}

@Composable
fun AstroSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalAstro.current
    Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = c.navy2, checkedThumbColor = Color.White,
        uncheckedTrackColor = Color(0xFFC5CBE0), uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent))
}

@Composable
fun Chevron(open: Boolean) {
    val angle by animateFloatAsState(if (open) 90f else 0f, label = "chevron")
    T("›", 18.sp, LocalAstro.current.mut, modifier = Modifier.rotate(angle))
}

@Composable
fun SelectableCell(selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val c = LocalAstro.current
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) c.selBg else c.card)
        .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) c.selBorder else c.line), RoundedCornerShape(14.dp))
        .clickable(onClick = onClick), content = content)
}

@Composable
fun CheckBadge(modifier: Modifier = Modifier) =
    Box(modifier.size(22.dp).clip(CircleShape).background(Color(0xFF0E2F86)), contentAlignment = Alignment.Center) { T("✓", 11.sp, Color.White) }

/** The design's PNGs are small; size by height and keep their aspect ratio instead of wrapping to intrinsic pixels. */
@Composable
fun FitImage(@DrawableRes res: Int, height: Dp, modifier: Modifier = Modifier) {
    val painter = painterResource(res)
    val ratio = painter.intrinsicSize.let { if (it.height > 0f) it.width / it.height else 1f }
    Image(painter, null, modifier.height(height).aspectRatio(ratio), contentScale = ContentScale.FillBounds)
}
