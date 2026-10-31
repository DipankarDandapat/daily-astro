package com.dailyastro.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyastro.app.R
import com.dailyastro.app.data.AstroState
import com.dailyastro.app.data.AstroViewModel

@Composable
private fun StarryBackdrop(modifier: Modifier = Modifier) = Canvas(modifier) {
    val stars = listOf(.14f to .09f, .82f to .2f, .6f to .06f, .3f to .55f, .9f to .7f, .45f to .82f)
    stars.forEachIndexed { i, (x, y) -> drawCircle(Color.White.copy(alpha = if (i % 2 == 0) .9f else .5f), 1.6.dp.toPx(), Offset(size.width * x, size.height * y)) }
    drawCircle(Brush.radialGradient(listOf(Color(0xFF7FA4FF), Color(0xFF2A4FD0)), Offset(28.dp.toPx() + 4.dp.toPx(), 70.dp.toPx() + 4.dp.toPx()), 8.dp.toPx()),
        6.dp.toPx(), Offset(34.dp.toPx(), 76.dp.toPx()))
}

@Composable
private fun OnboardingFrame(title: @Composable ColumnScope.() -> Unit, onBack: (() -> Unit)?, sheet: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(OnboardingGradient)) {
        Box(Modifier.fillMaxWidth().statusBarsPadding()) {
            StarryBackdrop(Modifier.matchParentSize())
            T("☾", 26.sp, LocalAstro.current.gold, modifier = Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 26.dp))
            T("✦", 12.sp, LocalAstro.current.gold, modifier = Modifier.align(Alignment.TopEnd).padding(top = 70.dp, end = 70.dp))
            T("✧", 12.sp, LocalAstro.current.gold, modifier = Modifier.align(Alignment.TopStart).padding(top = 22.dp, start = 64.dp))
            if (onBack != null) IconButton(onBack, Modifier.padding(start = 6.dp, top = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
            }
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 22.dp), horizontalAlignment = Alignment.CenterHorizontally, content = title)
        }
        Column(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(LocalAstro.current.card)
            .navigationBarsPadding().imePadding().padding(horizontal = 14.dp, vertical = 16.dp), content = sheet)
    }
}

@Composable
fun WelcomeScreen(s: AstroState, vm: AstroViewModel) {
    val c = LocalAstro.current
    OnboardingFrame(title = {
        FitImage(R.drawable.img_logo, 44.dp, Modifier.clip(RoundedCornerShape(6.dp)))
        Spacer(Modifier.height(6.dp))
        PlayfairTitle(stringResource(R.string.app_name), 36)
        T(stringResource(R.string.tagline), 12.sp, Color.White.copy(alpha = .85f))
    }, onBack = null) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            FitImage(R.drawable.img_hero, 138.dp, Modifier)
            T(stringResource(R.string.welcome_title), 18.sp, c.ink, FontWeight.SemiBold, Modifier.padding(top = 8.dp, bottom = 3.dp), TextAlign.Center)
            T(stringResource(R.string.welcome_body), 11.5.sp, c.mut, align = TextAlign.Center, modifier = Modifier.padding(bottom = 14.dp))
            T(stringResource(R.string.name_prompt), 12.sp, c.ink, FontWeight.Medium, Modifier.fillMaxWidth().padding(bottom = 6.dp))
            Row(Modifier.fillMaxWidth().border(1.5.dp, c.line, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person, null, tint = c.mut, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f).padding(start = 8.dp, top = 14.dp, bottom = 14.dp)) {
                    if (s.draftName.isEmpty()) T(stringResource(R.string.name_hint), 14.sp, c.mut)
                    BasicTextField(s.draftName, vm::setName, singleLine = true, textStyle = TextStyle(color = c.ink, fontSize = 14.sp, fontFamily = Poppins),
                        cursorBrush = SolidColor(c.navy2), modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { vm.begin() }))
                }
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton(stringResource(R.string.get_started)) { vm.begin() }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VerifiedUser, null, tint = c.mut, modifier = Modifier.size(13.dp))
                T(stringResource(R.string.data_safe), 11.sp, c.mut, modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
}

@Composable
fun PlayfairTitle(text: String, size: Int) =
    androidx.compose.material3.Text(text, color = Color.White, fontSize = size.sp, fontFamily = Playfair, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)

@Composable
fun LanguageScreen(s: AstroState, vm: AstroViewModel) {
    val c = LocalAstro.current
    BackHandler { vm.toWelcome() }
    OnboardingFrame(title = {
        PlayfairTitle(stringResource(R.string.choose_language), 25)
        T(stringResource(R.string.choose_language_sub), 14.sp, Color.White.copy(alpha = .92f), align = TextAlign.Center, modifier = Modifier.widthIn(max = 240.dp).padding(top = 8.dp))
    }, onBack = vm::toWelcome) {
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Catalog.languages, key = { it.code }) { lang ->
                val selected = s.draftLanguage == lang.code
                SelectableCell(selected, Modifier.height(76.dp), onClick = { vm.pickDraftLanguage(lang.code) }) {
                    Row(Modifier.fillMaxSize().padding(start = 10.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(lang.image), null, Modifier.size(width = 56.dp, height = 50.dp))
                        T(lang.native, 17.sp, if (selected) c.selInk else c.ink, FontWeight.Medium, Modifier.weight(1f), TextAlign.Center, maxLines = 1)
                        if (selected) CheckBadge()
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.continue_button)) { vm.toSign() }
    }
}

@Composable
fun SignScreen(s: AstroState, vm: AstroViewModel) {
    val c = LocalAstro.current
    BackHandler { vm.toLanguage() }
    OnboardingFrame(title = {
        PlayfairTitle(stringResource(R.string.pick_sign), 25)
        T(stringResource(R.string.pick_sign_sub), 14.sp, Color.White.copy(alpha = .92f), align = TextAlign.Center, modifier = Modifier.widthIn(max = 240.dp).padding(top = 8.dp))
    }, onBack = vm::toLanguage) {
        LazyVerticalGrid(GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Catalog.signs, key = { it.id }) { sign ->
                Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable { vm.finishOnboarding(sign.id) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    FitImage(sign.image, 64.dp, Modifier.clip(RoundedCornerShape(10.dp)))
                    T(Catalog.name(sign.id, s.draftLanguage), 12.5.sp, c.ink, FontWeight.Medium, Modifier.padding(top = 4.dp))
                    T(Catalog.range(sign.id, s.draftLanguage), 9.5.sp, c.mut)
                }
            }
        }
    }
}
