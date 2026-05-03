/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 */

package org.eu.nl.syu.hearth.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eu.nl.syu.hearth.data.ModelRepository
import org.eu.nl.syu.hearth.data.UserPersona
import org.eu.nl.syu.hearth.data.local.CharacterDao
import org.eu.nl.syu.hearth.data.local.ChatThreadDao
import org.eu.nl.syu.hearth.data.local.UserPersonaDao
import org.eu.nl.syu.hearth.data.local.toDomain
import org.eu.nl.syu.hearth.data.local.toEntity
import org.eu.nl.syu.hearth.runtime.LoreSyncManager
import java.util.UUID
import javax.inject.Inject

data class UserPersonaUiState(
    val selectedPersona: UserPersona? = null,
    val allPersonas: List<UserPersona> = emptyList(),
    val currentScope: EditScope = EditScope.GLOBAL,
    val threadId: String? = null,
    val characterId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class UserPersonaViewModel @Inject constructor(
    private val userPersonaDao: UserPersonaDao,
    private val characterDao: CharacterDao,
    private val chatThreadDao: ChatThreadDao,
    private val modelRepository: ModelRepository,
    private val loreSyncManager: LoreSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserPersonaUiState())
    val uiState: StateFlow<UserPersonaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPersonaDao.getAllPersonasFlow().collect { personas ->
                _uiState.update { it.copy(allPersonas = personas.map { p -> p.toDomain() }) }
            }
        }
    }

    fun init(scope: EditScope, threadId: String?, characterId: String?) {
        _uiState.update { it.copy(currentScope = scope, threadId = threadId, characterId = characterId) }
        loadCurrentPersona()
    }

    private fun loadCurrentPersona() {
        viewModelScope.launch {
            val state = _uiState.value
            val personaId = when (state.currentScope) {
                EditScope.GLOBAL -> modelRepository.globalDefaultPersonaId.first()
                EditScope.CHARACTER -> state.characterId?.let { characterDao.getCharacterById(it)?.defaultUserPersonaId }
                EditScope.THREAD -> state.threadId?.let { chatThreadDao.getThreadById(it)?.userPersonaId }
            }
            
            val persona = personaId?.let { userPersonaDao.getPersonaById(it)?.toDomain() }
            _uiState.update { it.copy(selectedPersona = persona) }
        }
    }

    fun selectPersona(persona: UserPersona) {
        viewModelScope.launch {
            val state = _uiState.value
            when (state.currentScope) {
                EditScope.GLOBAL -> modelRepository.setGlobalDefaultPersonaId(persona.id)
                EditScope.CHARACTER -> state.characterId?.let { characterDao.updateDefaultPersona(it, persona.id) }
                EditScope.THREAD -> state.threadId?.let { tid ->
                    val thread = chatThreadDao.getThreadById(tid)
                    if (thread != null) {
                        chatThreadDao.insertThread(thread.copy(userPersonaId = persona.id, threadUserPersonaBio = null))
                        // Trigger RAG sync if bio changed
                        triggerLoreSync(tid, persona.name, persona.bio)
                    }
                }
            }
            _uiState.update { it.copy(selectedPersona = persona) }
        }
    }

    fun saveRawBio(bio: String) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.currentScope == EditScope.THREAD && state.threadId != null) {
                val thread = chatThreadDao.getThreadById(state.threadId)
                if (thread != null) {
                    chatThreadDao.insertThread(thread.copy(threadUserPersonaBio = bio, userPersonaId = null))
                    triggerLoreSync(state.threadId, "User", bio)
                }
            }
        }
    }

    private suspend fun triggerLoreSync(threadId: String, name: String, bio: String) {
        val thread = chatThreadDao.getThreadById(threadId) ?: return
        val character = characterDao.getCharacterById(thread.characterId)?.toDomain() ?: return
        loreSyncManager.syncLore(character, thread.threadLore ?: "", threadId, name, bio)
    }

    fun createPersona(name: String, bio: String) {
        viewModelScope.launch {
            val persona = UserPersona(name = name, bio = bio, lastUsedAt = System.currentTimeMillis())
            userPersonaDao.insertPersona(persona.toEntity())
            selectPersona(persona)
        }
    }
}
