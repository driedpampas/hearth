/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 */

package org.eu.nl.syu.hearth.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.eu.nl.syu.hearth.data.UserPersona
import org.eu.nl.syu.hearth.data.local.UserPersonaDao
import org.eu.nl.syu.hearth.data.local.toEntity
import javax.inject.Inject

data class CreatePersonaState(
    val name: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreatePersonaViewModel @Inject constructor(
    private val userPersonaDao: UserPersonaDao
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePersonaState())
    val uiState: StateFlow<CreatePersonaState> = _uiState.asStateFlow()

    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updateBio(bio: String) = _uiState.update { it.copy(bio = bio) }
    fun updateAvatarUrl(url: String?) = _uiState.update { it.copy(avatarUrl = url) }

    fun savePersona() {
        val state = _uiState.value
        if (state.name.isBlank() || state.bio.isBlank()) {
            _uiState.update { it.copy(error = "Name and Bio cannot be empty") }
            return
        }

        viewModelScope.launch {
            val persona = UserPersona(
                name = state.name,
                bio = state.bio,
                avatarUrl = state.avatarUrl,
                lastUsedAt = System.currentTimeMillis()
            )
            userPersonaDao.insertPersona(persona.toEntity())
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
