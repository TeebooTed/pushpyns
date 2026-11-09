package com.pushpyns.ui.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pushpyns.domain.repository.ProAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PaywallViewModel
@Inject
constructor(private val proAccess: ProAccess) : ViewModel() {
    val isPro = proAccess.observeIsPro().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { proAccess.setPreviewEnabled(enabled) }
    }
}
