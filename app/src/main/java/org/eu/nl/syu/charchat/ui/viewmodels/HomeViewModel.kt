package org.eu.nl.syu.charchat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.eu.nl.syu.charchat.data.Character
import org.eu.nl.syu.charchat.data.local.CharacterDao
import org.eu.nl.syu.charchat.data.local.toDomain
import javax.inject.Inject

data class HomeUiState(
    val characters: List<Character> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val characterDao: CharacterDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCharacters()
    }

    fun loadCharacters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val characters = characterDao.getAllCharacters().map { it.toDomain() }
            _uiState.update { it.copy(characters = characters, isLoading = false) }
        }
    }
}
