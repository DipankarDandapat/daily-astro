package com.dailyastro.app.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyastro.app.R
import com.dailyastro.app.data.*
import com.dailyastro.app.model.Horoscope
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDate(iso: String?, language: String, pattern: String = "MMM d, yyyy"): String =
    runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag(language))) }.getOrDefault(iso ?: "")

fun formatTime(iso: String?, language: String): String? =
    iso?.let { runCatching { OffsetDateTime.parse(it).format(DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag(language))) }.getOrNull() }

@Composable
fun MainScreen(s: AstroState, vm: AstroViewModel, activity: android.app.Activity) {
    val c = LocalAstro.current
    Scaffold(containerColor = c.bg, contentWindowInsets = WindowInsets(0), bottomBar = { BottomNav(s.tab, vm::selectTab) }) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            when (s.tab) {
                Tab.Home -> HomeTab(s, vm)
                Tab.Horoscope -> HoroscopeTab(s, vm, activity)
                Tab.Profile -> ProfileTab(s, vm)
                Tab.More -> MoreTab(s, vm, activity)
            }
        }
    }
}

@Composable
private fun BottomNav(tab: Tab, onSelect: (Tab) -> Unit) {
    val c = LocalAstro.current
    val items = listOf(Triple(Tab.Home, R.string.nav_home, Icons.Outlined.Home), Triple(Tab.Horoscope, R.string.nav_horoscope, Icons.Outlined.WbSunny),
        Triple(Tab.Profile, R.string.nav_profile, Icons.Outlined.Person), Triple(Tab.More, R.string.nav_more, Icons.Outlined.Menu))
    Column(Modifier.background(c.card)) {
        HorizontalDivider(color = c.line)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 8.dp, bottom = 10.dp)) {
            items.forEach { (t, label, icon) ->
                val on = t == tab
                val color = if (on) (if (c.dark) Color(0xFF8FA8FF) else c.navy2) else c.mut
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onSelect(t) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                    T(stringResource(label), 11.sp, color, if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun Body(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), content = content)

@Composable
private fun Navy(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(LocalAstro.current.navy).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
private fun LoadingOrError(s: AstroState, key: String, onRetry: () -> Unit) {
    val c = LocalAstro.current
    if (key in s.loading) {
        Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), color = c.navy2, strokeWidth = 2.dp)
            T(stringResource(R.string.loading), 13.sp, c.mut, modifier = Modifier.padding(start = 10.dp))
        }
    } else if (s.errors[key] == MISSING) {
        AstroCard { T(stringResource(R.string.no_reading_stored), 13.sp, c.mut) }
    } else if (key in s.errors) {
        AstroCard {
            T(stringResource(R.string.error_network), 13.sp, c.ink)
            TextButton(onRetry) { T(stringResource(R.string.retry), 13.sp, c.navy2, FontWeight.SemiBold) }
        }
    }
}

// ---------- Home ----------

@Composable
fun HomeTab(s: AstroState, vm: AstroViewModel) {
    val c = LocalAstro.current
    val lang = s.settings.language
    val h = s.home
    NavyHeader(stringResource(R.string.hello, s.settings.name.ifBlank { stringResource(R.string.stargazer) }), formatDate(LocalDate.now().toString(), lang), alignStart = true)
    Body {
        Navy {
            RoundImage(Catalog.sign(s.sign).image, 64.dp)
            Column(Modifier.weight(1f)) {
                T(stringResource(R.string.your_sign), 11.sp, Color.White.copy(alpha = .8f))
                T(Catalog.name(s.sign, lang), 17.sp, Color.White, FontWeight.Bold)
                T(Catalog.range(s.sign, lang), 11.sp, Color.White.copy(alpha = .8f))
            }
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).clickable { vm.selectDay("today"); vm.selectTab(Tab.Horoscope) }.padding(horizontal = 13.dp, vertical = 7.dp)) {
                T(stringResource(R.string.read_today), 12.5.sp, Color(0xFF1B2340))
            }
        }
        if (s.key(s.sign, "today") in s.offline) OfflineBanner()
        LoadingOrError(s, s.key(s.sign, "today")) { vm.load(s.sign, "today", force = true) }
        if (h != null) {
            SectionTitle(stringResource(R.string.todays_energy))
            AstroCard(Modifier.padding(bottom = 12.dp)) {
                listOf("love" to R.string.love, "career" to R.string.career, "health" to R.string.health, "spiritual" to R.string.spiritual).forEachIndexed { i, (key, label) ->
                    val score = h.energy?.get(key) ?: 0
                    Row(Modifier.padding(top = if (i == 0) 0.dp else 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RoundImage(Catalog.categoryIcon(key), 28.dp, 8.dp)
                        T(stringResource(label), 12.5.sp, modifier = Modifier.width(86.dp).padding(start = 10.dp), maxLines = 1)
                        EnergyBar(score / 100f, Modifier.weight(1f))
                        T("$score%", 11.sp, c.mut, modifier = Modifier.width(38.dp), align = TextAlign.End)
                    }
                }
            }
            SectionTitle(stringResource(R.string.lucky_today))
            Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val lucky = h.lucky
                listOf(R.string.color to (lucky?.color ?: h.lucky_color), R.string.number to "${lucky?.number ?: h.lucky_number}",
                    R.string.time to (lucky?.time?.substringBefore(" –") ?: "—")).forEach { (label, value) ->
                    AstroCard(Modifier.weight(1f), padding = 10.dp) {
                        T(stringResource(label), 11.sp, c.mut, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
                        T(value, 14.sp, c.ink, FontWeight.Bold, Modifier.fillMaxWidth(), TextAlign.Center, maxLines = 1)
                    }
                }
            }
            Navy {
                Column(Modifier.weight(1f)) {
                    T(stringResource(R.string.daily_affirmation), 14.sp, Color.White, FontWeight.Bold, Modifier.padding(bottom = 3.dp))
                    T(h.affirmation ?: "", 12.sp, Color.White.copy(alpha = .9f))
                }
                Image(painterResource(R.drawable.img_planet), null, Modifier.size(62.dp).clip(CircleShape))
            }
            h.sky?.let { SkyCard(it, lang) }
        }
        SectionTitle(stringResource(R.string.explore_signs))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Catalog.signs.forEach { sign ->
                SelectableCell(sign.id == s.sign, Modifier.width(78.dp), onClick = { vm.previewSign(sign.id) }) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        FitImage(sign.image, 44.dp, Modifier.clip(RoundedCornerShape(8.dp)))
                        T(Catalog.name(sign.id, lang), 12.sp, maxLines = 1)
                    }
                }
            }
        }
        Attribution(h)
    }
}

@Composable
private fun SkyCard(sky: com.dailyastro.app.model.Sky, lang: String) {
    val c = LocalAstro.current
    SectionTitle(stringResource(R.string.sky_today) + (sky.location?.let { " · " + stringResource(R.string.for_location, it) } ?: ""))
    AstroCard(Modifier.padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(moonGlyph(sky.moon_phase), 30.sp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                T(stringResource(R.string.moon_in, Catalog.name(sky.moon_sign.lowercase(), lang)), 14.sp, c.ink, FontWeight.SemiBold)
                T(listOfNotNull(sky.moon_phase, sky.moon_illumination?.let { stringResource(R.string.moon_lit, it) }).joinToString(" · "), 11.5.sp, c.mut)
            }
            T(stringResource(R.string.sun_in, Catalog.name(sky.sun_sign.lowercase(), lang)), 11.5.sp, c.mut)
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = c.line)
        val facts = listOfNotNull(
            formatTime(sky.sunrise, lang)?.let { R.string.sunrise to it }, formatTime(sky.sunset, lang)?.let { R.string.sunset to it },
            formatTime(sky.rahu_kaal_start, lang)?.let { R.string.rahu_kaal to "$it – ${formatTime(sky.rahu_kaal_end, lang)}" },
            sky.tithi?.let { R.string.tithi to it }, sky.nakshatra?.let { R.string.nakshatra to it },
        )
        facts.chunked(2).forEach { row ->
            Row(Modifier.padding(vertical = 4.dp)) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f)) { T(stringResource(label), 11.sp, c.mut); T(value, 13.sp, c.ink, FontWeight.Medium) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        val retro = sky.retrogrades.orEmpty()
        T(if (retro.isEmpty()) stringResource(R.string.no_retrogrades) else stringResource(R.string.retrograde_now) + ": " + retro.joinToString { it.replaceFirstChar(Char::uppercase) } + " ℞",
            11.5.sp, c.mut, modifier = Modifier.padding(top = 6.dp))
    }
}

private fun moonGlyph(phase: String?) = when {
    phase == null -> "🌙"
    phase.contains("New", true) -> "🌑"; phase.contains("Waxing Crescent", true) -> "🌒"; phase.contains("First", true) -> "🌓"
    phase.contains("Waxing Gibbous", true) -> "🌔"; phase.contains("Full", true) -> "🌕"; phase.contains("Waning Gibbous", true) -> "🌖"
    phase.contains("Last", true) || phase.contains("Third", true) -> "🌗"; else -> "🌘"
}

@Composable
private fun OfflineBanner() {
    Box(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(LocalAstro.current.acc.copy(alpha = .18f)).padding(10.dp)) {
        T("⚡ " + stringResource(R.string.offline_banner), 12.sp)
    }
}

@Composable
private fun Attribution(h: Horoscope?) {
    val c = LocalAstro.current
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        T(stringResource(R.string.powered_by), 11.sp, c.mut, modifier = Modifier.clickable { uri.openUri(h?.attribution_url ?: "https://freehoroscopeapi.com") }.padding(4.dp))
        if (h?.translated == true) T(stringResource(R.string.machine_translated), 10.sp, c.mut)
    }
}

// ---------- Horoscope ----------

@Composable
fun HoroscopeTab(s: AstroState, vm: AstroViewModel, activity: android.app.Activity) {
    val c = LocalAstro.current
    val lang = s.settings.language
    val key = s.key(s.viewSign, s.day)
    val h = s.current
    val dateIso = h?.date ?: LocalDate.now().plusDays(when (s.day) { "yesterday" -> -1; "tomorrow" -> 1; else -> 0 }).toString()
    LaunchedEffect(key) { vm.load(s.viewSign, s.day) }
    NavyHeader(stringResource(R.string.horoscope_title, formatDate(dateIso, lang)))
    Body {
        Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("yesterday" to R.string.yesterday, "today" to R.string.today, "tomorrow" to R.string.tomorrow).forEach { (day, label) ->
                val on = s.day == day
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (on) c.navy2 else c.card)
                    .border(1.5.dp, if (on) c.navy2 else c.line, RoundedCornerShape(10.dp)).clickable { vm.selectDay(day) }.padding(8.dp), contentAlignment = Alignment.Center) {
                    T(stringResource(label), 12.5.sp, if (on) Color.White else c.ink)
                }
            }
        }
        if (s.previewSign != null) {
            Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                T(stringResource(R.string.showing_sign, Catalog.name(s.previewSign, lang)), 12.5.sp, c.mut, modifier = Modifier.weight(1f))
                Chip(stringResource(R.string.back_to_my_sign, Catalog.name(s.sign, lang)), false, vm::clearPreview)
            }
        }
        if (key in s.offline) OfflineBanner()
        LoadingOrError(s, key) { vm.load(s.viewSign, s.day, force = true) }
        if (h != null) {
            AstroCard(Modifier.padding(bottom = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundImage(Catalog.sign(h.sign.id).image, 70.dp, 12.dp)
                    Column(Modifier.weight(1f)) {
                        T(h.sign.name, 15.sp, c.ink, FontWeight.Bold)
                        T(Catalog.range(h.sign.id, lang), 11.sp, c.mut)
                        T(h.summary, 12.5.sp, c.ink, modifier = Modifier.padding(top = 6.dp), lineHeight = 19.sp)
                    }
                }
                if (h.kind == "forecast") T("ⓘ " + stringResource(R.string.forecast_note), 11.sp, c.mut, modifier = Modifier.padding(top = 10.dp))
                else Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton({ vm.toggleSave(h) }) {
                        Icon(if (h.saved) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkBorder, null, tint = c.navy2, modifier = Modifier.size(18.dp))
                        T(" " + stringResource(if (h.saved) R.string.saved_label else R.string.save), 12.5.sp, c.navy2, FontWeight.Medium)
                    }
                    TextButton({ share(activity, h, lang, activity.localized(lang)) }) {
                        Icon(Icons.Outlined.Share, null, tint = c.navy2, modifier = Modifier.size(18.dp))
                        T(" " + stringResource(R.string.share), 12.5.sp, c.navy2, FontWeight.Medium)
                    }
                }
            }
            Navy {
                Column(Modifier.weight(1f)) {
                    T(stringResource(if (h.kind == "forecast") R.string.special_advice_tomorrow else R.string.special_advice), 14.sp, Color.White, FontWeight.Bold, Modifier.padding(bottom = 3.dp))
                    T(h.special_advice, 12.sp, Color.White.copy(alpha = .9f), lineHeight = 17.sp)
                }
                Image(painterResource(R.drawable.img_planet), null, Modifier.size(62.dp).clip(CircleShape))
            }
            CategoryList(h)
            if (s.day == "today" && s.previewSign == null) {
                listOfNotNull(s.weekly?.let { R.string.this_week to it.summary }, s.monthly?.let { R.string.this_month to it.summary }).forEach { (label, text) ->
                    var open by rememberSaveable(label) { mutableStateOf(false) }
                    AstroCard(Modifier.padding(bottom = 12.dp).clickable { open = !open }) {
                        Row(verticalAlignment = Alignment.CenterVertically) { T(stringResource(label), 14.sp, c.ink, FontWeight.Medium, Modifier.weight(1f)); Chevron(open) }
                        AnimatedVisibility(open) { T(text, 12.5.sp, c.mut, modifier = Modifier.padding(top = 8.dp), lineHeight = 19.sp) }
                    }
                }
            }
            h.fetched_at?.let { T(stringResource(R.string.updated_at, it.take(16).replace('T', ' ') + " UTC"), 10.sp, c.mut, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center) }
        }
        Attribution(h)
    }
}

@Composable
private fun CategoryList(h: Horoscope) {
    val c = LocalAstro.current
    var open by rememberSaveable { mutableStateOf("love") }
    val titles = mapOf("love" to R.string.cat_love, "career" to R.string.cat_career, "health" to R.string.cat_health, "spiritual" to R.string.cat_spiritual, "lucky" to R.string.cat_lucky)
    AstroCard(Modifier.padding(bottom = 12.dp), padding = 10.dp) {
        h.categories.forEachIndexed { i, cat ->
            Column {
                Row(Modifier.fillMaxWidth().clickable { open = if (open == cat.key) "" else cat.key }.padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundImage(Catalog.categoryIcon(cat.key), 34.dp, 10.dp)
                    T(titles[cat.key]?.let { stringResource(it) } ?: cat.title, 14.sp, c.ink, FontWeight.Medium, Modifier.weight(1f).padding(start = 12.dp))
                    cat.score?.let { T("$it%", 11.5.sp, c.acc, FontWeight.SemiBold, Modifier.padding(end = 8.dp)) }
                    Chevron(open == cat.key)
                }
                AnimatedVisibility(open == cat.key) {
                    Column(Modifier.padding(start = 50.dp, end = 4.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val details = cat.details.orEmpty().ifEmpty { listOf(com.dailyastro.app.model.Detail("", cat.summary)) }
                        details.forEach { d ->
                            Text(androidx.compose.ui.text.buildAnnotatedString {
                                if (d.label.isNotBlank()) { pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.SemiBold, color = c.ink)); append(d.label + ": "); pop() }
                                append(d.text)
                            }, color = c.mut, fontSize = 12.5.sp, fontFamily = Poppins, lineHeight = 19.sp)
                        }
                    }
                }
                if (i < h.categories.lastIndex) HorizontalDivider(color = c.line)
            }
        }
    }
}

private fun share(activity: android.app.Activity, h: Horoscope, lang: String, ctx: android.content.Context) {
    val text = ctx.getString(R.string.share_reading, h.sign.name, formatDate(h.date, lang), h.summary, h.lucky?.color ?: h.lucky_color, h.lucky?.number ?: h.lucky_number)
    activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
}

// ---------- Profile ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTab(s: AstroState, vm: AstroViewModel) {
    val c = LocalAstro.current
    val lang = s.settings.language
    var name by remember(s.settings.name) { mutableStateOf(s.settings.name) }
    var pickDate by remember { mutableStateOf(false) }
    NavyHeader(stringResource(R.string.profile))
    Body {
        AstroCard(Modifier.padding(bottom = 12.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RoundImage(Catalog.sign(s.sign).image, 84.dp)
                T(s.settings.name.ifBlank { stringResource(R.string.stargazer) }, 17.sp, c.ink, FontWeight.SemiBold, Modifier.padding(top = 8.dp))
                T("${Catalog.name(s.sign, lang)} · ${Catalog.range(s.sign, lang)}", 12.sp, c.mut)
            }
        }
        SectionTitle(stringResource(R.string.personal_details))
        AstroCard(Modifier.padding(bottom = 12.dp)) {
            T(stringResource(R.string.name), 12.sp, modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(name, { name = it.take(40); vm.updateSettings { st -> st.copy(name = name.trim()) } }, Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Poppins, fontSize = 14.sp, color = c.ink),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = c.line, focusedBorderColor = c.navy2))
            T(stringResource(R.string.sun_sign), 12.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded, { expanded = it }) {
                OutlinedTextField(Catalog.name(s.sign, lang), {}, Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), readOnly = true,
                    shape = RoundedCornerShape(12.dp), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Poppins, fontSize = 14.sp, color = c.ink),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = c.line, focusedBorderColor = c.navy2))
                ExposedDropdownMenu(expanded, { expanded = false }) {
                    Catalog.signs.forEach { sign ->
                        DropdownMenuItem({ T("${sign.symbol}  ${Catalog.name(sign.id, lang)}") }, { expanded = false; vm.updateSettings { it.copy(sign = sign.id) } })
                    }
                }
            }
            T(stringResource(R.string.birth_date), 12.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, c.line, RoundedCornerShape(12.dp)).clickable { pickDate = true }.padding(14.dp)) {
                T(s.settings.dob?.let { formatDate(it, lang, "d MMMM yyyy") } ?: stringResource(R.string.birth_date_hint), 14.sp, if (s.settings.dob == null) c.mut else c.ink)
            }
            s.settings.dob?.let { LocalDate.parse(it) }?.let(Catalog::signFor)?.takeIf { it.id != s.sign }?.let { suggested ->
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    T(stringResource(R.string.sign_from_birth, Catalog.name(suggested.id, lang)), 12.sp, c.mut, modifier = Modifier.weight(1f))
                    Chip(stringResource(R.string.use_it), true, { vm.updateSettings { it.copy(sign = suggested.id) } })
                }
            }
        }
        SectionTitle(stringResource(R.string.preferences))
        AstroCard(Modifier.padding(bottom = 12.dp), padding = 10.dp) {
            Divided(listOf(
                { SettingsRow(null, stringResource(R.string.language), Catalog.lang(lang).native, Catalog.lang(lang).image, { vm.openSheet(Sheet.Language) }) },
                { SettingsRow("🔔", stringResource(R.string.notifications), notificationSummary(s.settings), onClick = { vm.openSheet(Sheet.Notifications) }) },
            ))
        }
        SectionTitle(stringResource(R.string.your_stats))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(s.stats?.streak to R.string.day_streak, s.stats?.read to R.string.read, s.stats?.saved to R.string.saved_stat).forEach { (value, label) ->
                AstroCard(Modifier.weight(1f).clickable(enabled = label == R.string.saved_stat) { vm.openSheet(Sheet.Saved) }) {
                    T("${value ?: "–"}", 16.sp, c.ink, FontWeight.Bold, Modifier.fillMaxWidth(), TextAlign.Center)
                    T(stringResource(label), 11.sp, c.mut, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
                }
            }
        }
    }
    if (pickDate) {
        val initial = s.settings.dob?.let { LocalDate.parse(it).toEpochDay() * 86_400_000 }
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog({ pickDate = false }, confirmButton = {
            TextButton({ pickDate = false; state.selectedDateMillis?.let { ms -> vm.updateSettings { it.copy(dob = LocalDate.ofEpochDay(ms / 86_400_000).toString()) } } }) { T("OK", color = c.navy2) }
        }, dismissButton = { TextButton({ pickDate = false }) { T(stringResource(R.string.cancel), color = c.navy2) } }) { DatePicker(state) }
    }
}

@Composable
fun notificationSummary(st: Settings): String {
    if (!st.notifEnabled) return stringResource(R.string.off)
    val freq = stringResource(when (st.notifFrequency) { "twice" -> R.string.freq_twice; "weekly" -> R.string.freq_weekly; else -> R.string.freq_daily })
    return stringResource(R.string.freq_at, freq, st.notifTime)
}

// ---------- More ----------

@Composable
fun MoreTab(s: AstroState, vm: AstroViewModel, activity: android.app.Activity) {
    val lang = s.settings.language
    val ctx = LocalContext.current
    NavyHeader(stringResource(R.string.more))
    Body {
        SectionTitle(stringResource(R.string.settings))
        AstroCard(Modifier.padding(bottom = 12.dp), padding = 10.dp) {
            Divided(listOf(
                { SettingsRow("🌐", stringResource(R.string.change_language), Catalog.lang(lang).native, onClick = { vm.openSheet(Sheet.Language) }) },
                { SettingsRow("🔔", stringResource(R.string.notification_frequency), notificationSummary(s.settings), onClick = { vm.openSheet(Sheet.Notifications) }) },
                { SettingsRow("🌓", stringResource(R.string.appearance), stringResource(when (s.settings.theme) { "light" -> R.string.light; "dark" -> R.string.dark; else -> R.string.match_system }), onClick = { vm.openSheet(Sheet.Appearance) }) },
                { Toggle("🔤", R.string.large_text, R.string.easier_to_read, s.settings.largeText) { v -> vm.updateSettings { it.copy(largeText = v) } } },
                { Toggle("📍", R.string.today_widget, R.string.today_widget_sub, s.settings.widget) { v -> vm.updateSettings { it.copy(widget = v) }; if (v) com.dailyastro.app.widget.TodayWidget.requestPin(activity) } },
                { Toggle("📴", R.string.offline_mode, R.string.offline_mode_sub, s.settings.offline) { v -> vm.updateSettings { it.copy(offline = v) } } },
            ))
        }
        SectionTitle(stringResource(R.string.explore))
        AstroCard(Modifier.padding(bottom = 12.dp), padding = 10.dp) {
            Divided(listOf(
                { SettingsRow("🕘", stringResource(R.string.horoscope_history), stringResource(R.string.past_7_days), onClick = { vm.openSheet(Sheet.History) }) },
                { SettingsRow("🔖", stringResource(R.string.saved_horoscopes), stringResource(R.string.n_saved, s.stats?.saved ?: 0), onClick = { vm.openSheet(Sheet.Saved) }) },
                { SettingsRow("💞", stringResource(R.string.compatibility), stringResource(R.string.match_two_signs), onClick = { vm.openSheet(Sheet.Compatibility) }) },
                { SettingsRow("🔗", stringResource(R.string.share_app), stringResource(R.string.invite_friends), onClick = {
                    activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.share_app_text)), null))
                }) },
            ))
        }
        SectionTitle(stringResource(R.string.support))
        AstroCard(Modifier.padding(bottom = 12.dp), padding = 10.dp) {
            Divided(listOf(
                { SettingsRow("⭐", stringResource(R.string.rate_app), onClick = { vm.openSheet(Sheet.Feedback) }) },
                { SettingsRow("💬", stringResource(R.string.send_feedback), onClick = { vm.openSheet(Sheet.Feedback) }) },
                { SettingsRow("🛡️", stringResource(R.string.privacy_data), stringResource(R.string.privacy_sub), onClick = { vm.openSheet(Sheet.Privacy) }) },
                { SettingsRow("🗑️", stringResource(R.string.delete_my_data), onClick = { vm.openSheet(Sheet.DeleteConfirm) }) },
                { SettingsRow("ℹ️", stringResource(R.string.about), stringResource(R.string.version, com.dailyastro.app.BuildConfig.VERSION_NAME), onClick = { vm.openSheet(Sheet.About) }) },
            ))
        }
        Attribution(s.home)
    }
}

@Composable
private fun Toggle(icon: String, title: Int, subtitle: Int, checked: Boolean, onChange: (Boolean) -> Unit) =
    SettingsRow(icon, stringResource(title), stringResource(subtitle), onClick = { onChange(!checked) }) { AstroSwitch(checked, onChange) }
