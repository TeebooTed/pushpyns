package com.pushpyns.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pushpyns.domain.model.Profile
import com.pushpyns.domain.model.UseType
import com.pushpyns.domain.repository.CategoryRepository
import com.pushpyns.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StartDestination { Profile, Home }

@HiltViewModel
class StartViewModel
@Inject
constructor(private val profiles: ProfileRepository, private val categories: CategoryRepository) :
    ViewModel() {
    private val _destination = MutableStateFlow<StartDestination?>(null)
    val destination = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            categories.ensureDefaults()
            _destination.value = if (profiles.get() == null) StartDestination.Profile else StartDestination.Home
        }
    }
}

data class ProfileUiState(
    val name: String = "",
    val businessName: String = "",
    val useType: UseType = UseType.PERSONAL,
    val nameError: Boolean = false,
    val saving: Boolean = false,
    val editing: Boolean = false,
)

@HiltViewModel
class ProfileViewModel
@Inject
constructor(private val profiles: ProfileRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()
    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved = _saved.asSharedFlow()

    fun load(editing: Boolean) {
        _state.update { it.copy(editing = editing) }
        if (!editing) return
        viewModelScope.launch {
            profiles.get()?.let { profile ->
                _state.update {
                    it.copy(
                        name = profile.name,
                        businessName = profile.businessName,
                        useType = profile.useType,
                        editing = true,
                    )
                }
            }
        }
    }

    fun onName(value: String) = _state.update { it.copy(name = value, nameError = false) }

    fun onBusiness(value: String) = _state.update { it.copy(businessName = value) }

    fun onUseType(value: UseType) = _state.update { it.copy(useType = value) }

    fun save() {
        val current = _state.value
        if (current.name.trim().isEmpty()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            profiles.save(
                Profile(
                    name = current.name,
                    businessName = current.businessName,
                    useType = current.useType,
                ),
            )
            _state.update { it.copy(saving = false) }
            _saved.tryEmit(Unit)
        }
    }
}
