package com.dailyastro.app.data

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyastro.app.R
import com.dailyastro.app.model.*
import com.dailyastro.app.network.ApiProvider
import com.dailyastro.app.notify.Reminders
import com.dailyastro.app.widget.TodayWidget
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen { Welcome, Language, Sign, Main }
enum class Tab { Home, Horoscope, Profile, More }
enum class Sheet { Language, Notifications, Appearance, History, Saved, Compatibility, Feedback, Privacy, About, DeleteConfirm }

data class AstroState(
    val ready: Boolean = false,
    val settings: Settings = Settings(),
    val screen: Screen = Screen.Welcome,
    val draftName: String = "",
    val draftLanguage: String = "en",
    val tab: Tab = Tab.Home,
    val day: String = "today",
    val previewSign: String? = null,
    val readings: Map<String, Horoscope> = emptyMap(),
    val loading: Set<String> = emptySet(),
    val errors: Map<String, String> = emptyMap(),
    val offline: Set<String> = emptySet(),
    val weekly: PeriodReading? = null,
    val monthly: PeriodReading? = null,
    val stats: Stats? = null,
    val history: List<HistoryItem>? = null,
    val saved: List<Bookmark>? = null,
    val compatibility: Compatibility? = null,
    val sheet: Sheet? = null,
    @StringRes val message: Int? = null,
) {
    val sign: String get() = settings.sign ?: "aries"
    val viewSign: String get() = previewSign ?: sign
    fun key(sign: String, day: String) = "$sign|${settings.language}|$day"
    val current: Horoscope? get() = readings[key(viewSign, day)]
    val home: Horoscope? get() = readings[key(sign, "today")]
}

/** Error marker for a day the backend never stored (e.g. yesterday on a fresh install), as opposed to a network failure. */
const val MISSING = "missing"

class AstroViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    private val cache = HoroscopeCache(app)
    private val api = ApiProvider.service
    private val _state = MutableStateFlow(AstroState())
    val state = _state.asStateFlow()
    private var syncJob: Job? = null

    init {
        viewModelScope.launch {
            ApiProvider.deviceId = store.deviceId()
            val s = store.current()
            _state.update { it.copy(ready = true, settings = s, draftName = s.name, draftLanguage = s.language, screen = if (s.onboarded) Screen.Main else Screen.Welcome) }
            if (s.onboarded) { refreshAll(); Reminders.schedule(getApplication(), s) }
        }
    }

    // Onboarding
    fun setName(value: String) = _state.update { it.copy(draftName = value.take(40)) }
    fun begin() = _state.update { it.copy(screen = Screen.Language) }
    fun pickDraftLanguage(code: String) = _state.update { it.copy(draftLanguage = code, settings = it.settings.copy(language = code)) }
    fun toLanguage() = _state.update { it.copy(screen = Screen.Language) }
    fun toWelcome() = _state.update { it.copy(screen = Screen.Welcome) }
    fun toSign() = _state.update { it.copy(screen = Screen.Sign) }
    fun finishOnboarding(sign: String) {
        val s = _state.value
        persist { it.copy(onboarded = true, name = s.draftName.trim(), language = s.draftLanguage, sign = sign) }
        _state.update { it.copy(screen = Screen.Main, tab = Tab.Home, day = "today") }
        viewModelScope.launch { refreshAll() }
    }

    // Navigation
    fun selectTab(tab: Tab) {
        _state.update { it.copy(tab = tab, previewSign = if (tab == Tab.Horoscope) it.previewSign else null) }
        if (tab == Tab.Profile) loadStats()
        if (tab == Tab.Horoscope) load(_state.value.viewSign, _state.value.day)
    }
    fun selectDay(day: String) { _state.update { it.copy(day = day) }; load(_state.value.viewSign, day) }
    fun previewSign(sign: String) {
        _state.update { it.copy(previewSign = sign.takeIf { s -> s != it.sign }, tab = Tab.Horoscope, day = "today") }
        load(_state.value.viewSign, "today")
    }
    fun clearPreview() { _state.update { it.copy(previewSign = null) }; load(_state.value.sign, _state.value.day) }
    fun openSheet(sheet: Sheet?) {
        _state.update { it.copy(sheet = sheet) }
        when (sheet) { Sheet.History -> loadHistory(); Sheet.Saved -> loadSaved(); else -> Unit }
    }
    fun consumeMessage() = _state.update { it.copy(message = null) }
    private fun toast(@StringRes res: Int) = _state.update { it.copy(message = res) }

    // Data
    fun refreshAll() {
        val s = _state.value
        load(s.sign, "today", force = true)
        if (s.tab == Tab.Horoscope && (s.viewSign != s.sign || s.day != "today")) load(s.viewSign, s.day, force = true)
        loadPeriods(); loadStats()
    }

    fun load(sign: String, day: String, force: Boolean = false) {
        val key = _state.value.key(sign, day)
        if (!force && (key in _state.value.readings || key in _state.value.loading)) return
        val language = _state.value.settings.language
        _state.update { it.copy(loading = it.loading + key, errors = it.errors - key) }
        viewModelScope.launch {
            runCatching { api.horoscope(sign, language, day) }
                .onSuccess { h ->
                    if (_state.value.settings.offline) cache.put(h)
                    _state.update { it.copy(readings = it.readings + (key to h), loading = it.loading - key, offline = it.offline - key) }
                    if (day == "today" && sign == _state.value.sign) TodayWidget.update(getApplication(), h)
                }
                .onFailure { e ->
                    val cached = cache.get(sign, language, day)
                    _state.update {
                        if (cached != null) it.copy(readings = it.readings + (key to cached), loading = it.loading - key, offline = it.offline + key)
                        else it.copy(loading = it.loading - key, errors = it.errors + (key to if ((e as? retrofit2.HttpException)?.code() == 404) MISSING else (e.message ?: "error")))
                    }
                }
        }
    }

    private fun loadPeriods() {
        val s = _state.value
        viewModelScope.launch { runCatching { api.period(s.sign, "weekly", s.settings.language) }.onSuccess { r -> _state.update { it.copy(weekly = r) } } }
        viewModelScope.launch { runCatching { api.period(s.sign, "monthly", s.settings.language) }.onSuccess { r -> _state.update { it.copy(monthly = r) } } }
    }

    fun loadStats() = viewModelScope.launch {
        runCatching { api.stats(store.deviceId()) }.onSuccess { r -> _state.update { it.copy(stats = r) } }
    }

    private fun loadHistory() = viewModelScope.launch {
        _state.update { it.copy(history = null) }
        val s = _state.value
        runCatching { api.history(s.sign, s.settings.language, 7) }.onSuccess { r -> _state.update { it.copy(history = r) } }
            .onFailure { _state.update { it.copy(history = emptyList()) } }
    }

    private fun loadSaved() = viewModelScope.launch {
        runCatching { api.saved(store.deviceId()) }.onSuccess { r -> _state.update { it.copy(saved = r) } }
            .onFailure { _state.update { it.copy(saved = it.saved ?: emptyList()) } }
    }

    fun toggleSave(h: Horoscope) = viewModelScope.launch {
        val device = store.deviceId()
        runCatching { if (h.saved) api.unsave(device, h.date, h.sign.id) else api.save(device, BookmarkRequest(h.date, h.sign.id, h.language)) }
            .onSuccess { stats ->
                val key = _state.value.key(h.sign.id, h.day)
                _state.update { it.copy(stats = stats, readings = it.readings + (key to h.copy(saved = !h.saved))) }
                toast(if (h.saved) R.string.toast_removed else R.string.toast_saved)
            }.onFailure { toast(R.string.error_network) }
    }

    fun checkCompatibility(a: String, b: String) = viewModelScope.launch {
        runCatching { api.compatibility(a, b, _state.value.settings.language) }.onSuccess { r -> _state.update { it.copy(compatibility = r) } }
            .onFailure { toast(R.string.error_network) }
    }

    fun sendFeedback(rating: Int?, message: String) = viewModelScope.launch {
        runCatching { api.feedback(FeedbackRequest(store.deviceId(), rating, message)) }
            .onSuccess { _state.update { it.copy(sheet = null, message = if (message.isBlank()) R.string.toast_thanks else R.string.toast_feedback_sent) } }
            .onFailure { toast(R.string.error_network) }
    }

    fun deleteData() = viewModelScope.launch {
        runCatching { api.deleteData(store.deviceId()) }
        Reminders.cancel(getApplication()); cache.clear(); store.clear(); TodayWidget.clear(getApplication())
        ApiProvider.deviceId = store.deviceId()
        _state.value = AstroState(ready = true, settings = store.current(), message = R.string.toast_data_deleted)
    }

    // Settings
    fun updateSettings(transform: (Settings) -> Settings) {
        val before = _state.value.settings
        persist(transform)
        val after = _state.value.settings
        if (before.language != after.language || before.sign != after.sign) {
            _state.update { it.copy(readings = emptyMap(), errors = emptyMap(), weekly = null, monthly = null, previewSign = null) }
            refreshAll()
        }
    }

    private fun persist(transform: (Settings) -> Settings) {
        val next = transform(_state.value.settings)
        _state.update { it.copy(settings = next) }
        viewModelScope.launch {
            store.update { transform(it) }
            Reminders.schedule(getApplication(), next)
            if (!next.widget) TodayWidget.clear(getApplication()) else _state.value.home?.let { TodayWidget.update(getApplication(), it) }
        }
        syncProfile(next)
    }

    /** Debounced push of the profile to the backend; local settings stay the source of truth if it fails. */
    private fun syncProfile(s: Settings) {
        if (!s.onboarded) return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            runCatching {
                api.upsertDevice(DeviceProfile(store.deviceId(), s.name, s.language, s.sign, s.dob, s.notifEnabled, s.notifFrequency, s.notifTime, s.theme, s.largeText))
            }
        }
    }
}
