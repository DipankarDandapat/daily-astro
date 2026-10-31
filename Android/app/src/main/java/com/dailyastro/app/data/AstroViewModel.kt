package com.dailyastro.app.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyastro.app.model.*
import com.dailyastro.app.network.ApiProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen { Welcome, Language, Sign, Horoscope }
data class AstroState(val screen: Screen = Screen.Welcome, val name: String = "", val language: Language? = null, val signs: List<ZodiacSign> = emptyList(), val selectedSign: ZodiacSign? = null, val horoscope: Horoscope? = null, val loading: Boolean = false, val error: String? = null)

class AstroViewModel : ViewModel() {
    private val _state = MutableStateFlow(AstroState())
    val state = _state.asStateFlow()
    private val api = ApiProvider.service

    fun setName(value: String) { _state.value = _state.value.copy(name = value) }
    fun begin() { _state.value = _state.value.copy(screen = Screen.Language, loading = true); viewModelScope.launch { runCatching { api.languages() }.onSuccess { _state.value = _state.value.copy(loading = false) }.onFailure { _state.value = _state.value.copy(loading = false, error = it.message) } } }
    fun chooseLanguage(language: Language) { _state.value = _state.value.copy(language = language, screen = Screen.Sign, loading = true); viewModelScope.launch { runCatching { api.zodiacSigns(language.code) }.onSuccess { _state.value = _state.value.copy(signs = it, loading = false) }.onFailure { _state.value = _state.value.copy(loading = false, error = it.message) } } }
    fun chooseSign(sign: ZodiacSign) { _state.value = _state.value.copy(selectedSign = sign, screen = Screen.Horoscope, loading = true); viewModelScope.launch { runCatching { api.today(sign.id, _state.value.language?.code ?: "en") }.onSuccess { _state.value = _state.value.copy(horoscope = it, loading = false) }.onFailure { _state.value = _state.value.copy(loading = false, error = it.message) } } }
}
