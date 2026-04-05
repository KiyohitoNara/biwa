package io.github.kiyohitonara.biwa.presentation.addmedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kiyohitonara.biwa.domain.extractor.MediaMetadataExtractor
import io.github.kiyohitonara.biwa.domain.model.AddMediaRequest
import io.github.kiyohitonara.biwa.domain.usecase.AddMediaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages UI state for the add-media flow.
 *
 * Extracts file metadata via [MediaMetadataExtractor], delegates file copy
 * and persistence to [AddMediaUseCase], and exposes results via [uiState].
 */
class AddMediaViewModel(
    private val addMediaUseCase: AddMediaUseCase,
    private val metadataExtractor: MediaMetadataExtractor,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AddMediaUiState>(AddMediaUiState.Idle)

    /** Current state of the add-media operation. */
    val uiState: StateFlow<AddMediaUiState> = _uiState.asStateFlow()

    /**
     * Starts adding the file identified by [uri].
     *
     * Extracts metadata from the URI, then transitions through
     * [AddMediaUiState.Loading] and resolves to [AddMediaUiState.Success]
     * or [AddMediaUiState.Error].
     */
    fun addMedia(uri: String) {
        viewModelScope.launch {
            _uiState.value = AddMediaUiState.Loading
            try {
                val metadata = metadataExtractor.extract(uri)
                val request = AddMediaRequest(
                    sourceUri = uri,
                    fileName = metadata.fileName,
                    mediaType = metadata.mediaType,
                    displayName = metadata.fileName,
                    durationMs = metadata.durationMs,
                    widthPx = metadata.widthPx,
                    heightPx = metadata.heightPx,
                    fileSizeBytes = metadata.fileSizeBytes,
                    takenAt = metadata.takenAt,
                )
                val item = addMediaUseCase.execute(request)
                _uiState.value = AddMediaUiState.Success(item)
            } catch (e: Exception) {
                _uiState.value = AddMediaUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Resets [uiState] to [AddMediaUiState.Idle]. */
    fun resetState() {
        _uiState.value = AddMediaUiState.Idle
    }
}
