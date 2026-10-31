package com.dailyastro.app.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyastro.app.BuildConfig
import com.dailyastro.app.R
import com.dailyastro.app.data.AstroState
import com.dailyastro.app.data.AstroViewModel
import com.dailyastro.app.data.Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetHost(s: AstroState, vm: AstroViewModel, requestNotifications: () -> Unit) {
    val sheet = s.sheet ?: return
    val c = LocalAstro.current
    // The sheet is a separate window whose ComposeView re-provides the system context; carry the app language and text size across.
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    ModalBottomSheet({ vm.openSheet(null) }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.bg,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)) {
        CompositionLocalProvider(LocalContext provides context, androidx.compose.ui.platform.LocalConfiguration provides configuration,
            androidx.compose.ui.platform.LocalDensity provides density) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            when (sheet) {
                Sheet.Language -> LanguageSheet(s, vm)
                Sheet.Notifications -> NotificationSheet(s, vm, requestNotifications)
                Sheet.Appearance -> AppearanceSheet(s, vm)
                Sheet.History -> HistorySheet(s)
                Sheet.Saved -> SavedSheet(s)
                Sheet.Compatibility -> CompatibilitySheet(s, vm)
                Sheet.Feedback -> FeedbackSheet(vm)
                Sheet.Privacy -> InfoSheet(stringResource(R.string.privacy_data), stringResource(R.string.privacy_body), vm)
                Sheet.About -> InfoSheet(stringResource(R.string.about), stringResource(R.string.about_body, BuildConfig.VERSION_NAME), vm)
                Sheet.DeleteConfirm -> DeleteSheet(vm)
            }
        }
        }
    }
}

@Composable private fun SheetTitle(text: String) = T(text, 16.sp, LocalAstro.current.ink, FontWeight.Bold, Modifier.padding(bottom = 12.dp))

@Composable
private fun LanguageSheet(s: AstroState, vm: AstroViewModel) {
    val c = LocalAstro.current
    SheetTitle(stringResource(R.string.change_language))
    Catalog.languages.chunked(2).forEach { row ->
        Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { lang ->
                val selected = s.settings.language == lang.code
                SelectableCell(selected, Modifier.weight(1f), onClick = { vm.updateSettings { it.copy(language = lang.code) }; vm.openSheet(null) }) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(lang.image), null, Modifier.height(34.dp).width(44.dp).clip(RoundedCornerShape(6.dp)))
                        T(lang.native, 15.sp, if (selected) c.selInk else c.ink, FontWeight.Medium, Modifier.weight(1f).padding(start = 10.dp), maxLines = 1)
                        if (selected) CheckBadge()
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSheet(s: AstroState, vm: AstroViewModel, requestNotifications: () -> Unit) {
    val c = LocalAstro.current
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(s.settings.notifEnabled) }
    var frequency by remember { mutableStateOf(s.settings.notifFrequency) }
    var time by remember { mutableStateOf(s.settings.notifTime) }
    SheetTitle(stringResource(R.string.notifications))
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        T(stringResource(R.string.daily_alert), 14.sp, c.ink, FontWeight.Medium, Modifier.weight(1f))
        AstroSwitch(enabled) { enabled = it }
    }
    SectionTitle(stringResource(R.string.how_often))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("daily" to R.string.once_a_day, "twice" to R.string.morning_evening, "weekly" to R.string.weekly_summary).forEach { (key, label) ->
            Chip(stringResource(label), frequency == key, { frequency = key })
        }
    }
    SectionTitle(stringResource(R.string.time))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.5.dp, c.line, RoundedCornerShape(12.dp)).clickable {
        val (h, m) = time.split(":").map(String::toInt)
        TimePickerDialog(context, { _, hour, minute -> time = "%02d:%02d".format(hour, minute) }, h, m, false).show()
    }.padding(12.dp)) { T(time, 14.sp) }
    SectionTitle(stringResource(R.string.quiet_hours))
    Chip(stringResource(R.string.quiet_hours_value), true, {})
    Spacer(Modifier.height(16.dp))
    PrimaryButton(stringResource(R.string.save_changes)) {
        vm.updateSettings { it.copy(notifEnabled = enabled, notifFrequency = frequency, notifTime = time) }
        if (enabled) requestNotifications()
        vm.openSheet(null)
    }
}

@Composable
private fun AppearanceSheet(s: AstroState, vm: AstroViewModel) {
    SheetTitle(stringResource(R.string.appearance))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("auto" to R.string.match_system, "light" to R.string.light, "dark" to R.string.dark).forEach { (key, label) ->
            Chip(stringResource(label), s.settings.theme == key, { vm.updateSettings { it.copy(theme = key) }; vm.openSheet(null) })
        }
    }
}

@Composable
private fun HistorySheet(s: AstroState) {
    val c = LocalAstro.current
    SheetTitle(stringResource(R.string.horoscope_history))
    val items = s.history
    when {
        items == null -> CircularProgressIndicator(Modifier.padding(16.dp), color = c.navy2)
        items.isEmpty() -> T(stringResource(R.string.history_empty), 13.sp, c.mut)
        else -> items.forEach { item -> ReadingCard(formatDate(item.date, s.settings.language, "EEE, MMM d"), Catalog.name(item.sign, s.settings.language), item.summary) }
    }
}

@Composable
private fun SavedSheet(s: AstroState) {
    val c = LocalAstro.current
    SheetTitle(stringResource(R.string.saved_horoscopes))
    val items = s.saved
    when {
        items == null -> CircularProgressIndicator(Modifier.padding(16.dp), color = c.navy2)
        items.isEmpty() -> T(stringResource(R.string.saved_empty), 13.sp, c.mut)
        else -> items.forEach { item -> ReadingCard(formatDate(item.date, s.settings.language), Catalog.name(item.sign, s.settings.language), item.summary ?: "") }
    }
}

@Composable
private fun ReadingCard(date: String, sign: String, text: String) {
    var open by remember { mutableStateOf(false) }
    val c = LocalAstro.current
    AstroCard(Modifier.padding(bottom = 10.dp).clickable { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(date, 13.sp, c.ink, FontWeight.SemiBold, Modifier.weight(1f)); T(sign, 12.sp, c.mut)
        }
        T(text, 12.5.sp, c.mut, modifier = Modifier.padding(top = 6.dp), lineHeight = 19.sp, maxLines = if (open) Int.MAX_VALUE else 3)
    }
}

@Composable
private fun CompatibilitySheet(s: AstroState, vm: AstroViewModel) {
    val c = LocalAstro.current
    val lang = s.settings.language
    var a by remember { mutableStateOf(s.sign) }
    var b by remember { mutableStateOf(Catalog.signs[(Catalog.signs.indexOf(Catalog.sign(s.sign)) + 4) % 12].id) }
    LaunchedEffect(a, b) { vm.checkCompatibility(a, b) }
    SheetTitle(stringResource(R.string.compatibility))
    T(stringResource(R.string.compat_pick), 12.sp, c.mut, modifier = Modifier.padding(bottom = 8.dp))
    listOf(a to { v: String -> a = v }, b to { v: String -> b = v }).forEach { (selected, set) ->
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Catalog.signs.forEach { sign ->
                SelectableCell(selected == sign.id, Modifier.width(66.dp), onClick = { set(sign.id) }) {
                    Column(Modifier.fillMaxWidth().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        FitImage(sign.image, 34.dp, Modifier.clip(RoundedCornerShape(6.dp)))
                        T(Catalog.name(sign.id, lang), 10.5.sp, maxLines = 1)
                    }
                }
            }
        }
    }
    s.compatibility?.takeIf { it.a.id == a && it.b.id == b }?.let { match ->
        AstroCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T("${Catalog.sign(a).symbol}  ❤  ${Catalog.sign(b).symbol}", 20.sp, modifier = Modifier.weight(1f))
                T(stringResource(R.string.compat_match, match.score), 18.sp, c.acc, FontWeight.Bold)
            }
            EnergyBar(match.score / 100f, Modifier.fillMaxWidth().padding(vertical = 10.dp))
            T(match.summary, 12.5.sp, c.mut, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun FeedbackSheet(vm: AstroViewModel) {
    val c = LocalAstro.current
    var rating by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    SheetTitle(stringResource(R.string.send_feedback))
    Row(Modifier.padding(bottom = 12.dp)) {
        (1..5).forEach { i -> T(if ((rating ?: 0) >= i) "★" else "☆", 30.sp, c.acc, modifier = Modifier.clickable { rating = i }.padding(end = 6.dp)) }
    }
    OutlinedTextField(message, { message = it.take(2000) }, Modifier.fillMaxWidth().heightIn(min = 110.dp), placeholder = { T(stringResource(R.string.feedback_hint), 13.sp, c.mut) },
        shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = c.line, focusedBorderColor = c.navy2))
    Spacer(Modifier.height(14.dp))
    PrimaryButton(stringResource(R.string.send), enabled = rating != null || message.isNotBlank()) { vm.sendFeedback(rating, message) }
}

@Composable
private fun InfoSheet(title: String, body: String, vm: AstroViewModel) {
    SheetTitle(title)
    T(body, 13.sp, LocalAstro.current.mut, lineHeight = 20.sp)
    Spacer(Modifier.height(16.dp))
    PrimaryButton(stringResource(R.string.close)) { vm.openSheet(null) }
}

@Composable
private fun DeleteSheet(vm: AstroViewModel) {
    val c = LocalAstro.current
    SheetTitle(stringResource(R.string.delete_my_data))
    T(stringResource(R.string.delete_confirm), 13.sp, c.mut, lineHeight = 20.sp)
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton({ vm.openSheet(null) }, Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp)) { T(stringResource(R.string.cancel), 14.sp, c.ink) }
        Button({ vm.openSheet(null); vm.deleteData() }, Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFC62828))) {
            T(stringResource(R.string.delete), 14.sp, androidx.compose.ui.graphics.Color.White, FontWeight.SemiBold, align = TextAlign.Center)
        }
    }
}
