package com.pushpyns.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pushpyns.domain.repository.CategoryRepository
import com.pushpyns.domain.repository.CategoryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CategoryViewModel
@Inject
constructor(private val repository: CategoryRepository) : ViewModel() {
    val categories =
        repository.observeAll().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    fun add(name: String) = mutate { repository.add(name) }

    fun rename(id: Long, name: String) = mutate { repository.rename(id, name) }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    private fun mutate(block: suspend () -> CategoryResult) {
        viewModelScope.launch {
            when (block()) {
                CategoryResult.Blank -> _messages.emit("Enter a category name.")
                CategoryResult.Duplicate -> _messages.emit("You already have a category with that name.")
                is CategoryResult.Saved -> Unit
            }
        }
    }
}
