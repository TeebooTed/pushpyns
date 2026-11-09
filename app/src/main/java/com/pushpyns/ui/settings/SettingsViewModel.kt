package com.pushpyns.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pushpyns.domain.model.Profile
import com.pushpyns.domain.model.ThemeMode
import com.pushpyns.domain.repository.ProAccess
import com.pushpyns.domain.repository.ProfileRepository
import com.pushpyns.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profile: Profile? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val pro: Boolean = false,
)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(profiles: ProfileRepository, private val settings: SettingsRepository, proAccess: ProAccess) :
    ViewModel() {
    val uiState =
        combine(profiles.observe(), settings.themeMode, settings.dynamicColor, proAccess.observeIsPro()) {
                profile,
                theme,
                dynamic,
                pro,
            ->
            SettingsUiState(profile, theme, dynamic, pro)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    val themeMode =
        settings.themeMode.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ThemeMode.SYSTEM,
        )
    val dynamicColor = settings.dynamicColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setDynamic(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColor(enabled) }
    }
}
