package io.github.kiyohitonara.biwa.presentation.albummanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.kiyohitonara.biwa.domain.usecase.AddMediaToAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.CreateAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.DeleteAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAlbumsForMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllAlbumsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RemoveMediaFromAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RenameAlbumUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Manages album CRUD operations and, when [mediaId] is set, the album assignments
 * for a specific media item.
 *
 * Pass [mediaId] to enable media-specific album toggling; pass null for
 * global album management (create / rename / delete only).
 */
class AlbumManagementViewModel(
    private val mediaId: String?,
    private val getAllAlbumsUseCase: GetAllAlbumsUseCase,
    private val createAlbumUseCase: CreateAlbumUseCase,
    private val renameAlbumUseCase: RenameAlbumUseCase,
    private val deleteAlbumUseCase: DeleteAlbumUseCase,
    private val getAlbumsForMediaUseCase: GetAlbumsForMediaUseCase,
    private val addMediaToAlbumUseCase: AddMediaToAlbumUseCase,
    private val removeMediaFromAlbumUseCase: RemoveMediaFromAlbumUseCase,
    logger: Logger,
) : ViewModel() {
    private val log = logger.withTag("AlbumManagementViewModel")

    /**
     * Current state combining all albums with the media-specific album list.
     *
     * Stays [AlbumManagementUiState.Loading] until the first DB emission arrives.
     */
    val uiState: StateFlow<AlbumManagementUiState> =
        combine(
            getAllAlbumsUseCase.execute(),
            mediaId?.let { getAlbumsForMediaUseCase.execute(it) } ?: flowOf(emptyList()),
        ) { allAlbums, mediaAlbums ->
            AlbumManagementUiState.Ready(allAlbums = allAlbums, mediaAlbums = mediaAlbums)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AlbumManagementUiState.Loading,
        )

    private val _error = MutableSharedFlow<String>()

    /** Emits an error message when a album operation fails (e.g. duplicate name). One-shot event. */
    val error: SharedFlow<String> = _error.asSharedFlow()

    /**
     * Creates a new album with [name].
     *
     * Emits on [error] if the name is blank or already taken.
     */
    @Suppress("TooGenericExceptionCaught") // Translate any use-case failure into a UI error event.
    fun createAlbum(name: String) {
        log.i { "Create album name=$name" }
        viewModelScope.launch {
            try {
                createAlbumUseCase.execute(name)
            } catch (e: Exception) {
                log.w(e) { "Failed to create album name=$name" }
                _error.emit(e.message ?: "Failed to create album")
            }
        }
    }

    /**
     * Renames the album identified by [id] to [name].
     *
     * Emits on [error] if the name is blank or already taken.
     */
    @Suppress("TooGenericExceptionCaught") // Translate any use-case failure into a UI error event.
    fun renameAlbum(
        id: String,
        name: String,
    ) {
        log.i { "Rename album id=$id name=$name" }
        viewModelScope.launch {
            try {
                renameAlbumUseCase.execute(id, name)
            } catch (e: Exception) {
                log.w(e) { "Failed to rename album id=$id name=$name" }
                _error.emit(e.message ?: "Failed to rename album")
            }
        }
    }

    /**
     * Deletes the album identified by [id] along with all its media associations.
     */
    fun deleteAlbum(id: String) {
        log.i { "Delete album id=$id" }
        viewModelScope.launch { deleteAlbumUseCase.execute(id) }
    }

    /**
     * Toggles the album identified by [albumId] on the current media item.
     *
     * Attaches the album if not already assigned; detaches it otherwise.
     * No-op when [mediaId] is null.
     */
    fun toggleMediaInAlbum(albumId: String) {
        val mid = mediaId ?: return
        val state = uiState.value as? AlbumManagementUiState.Ready ?: return
        viewModelScope.launch {
            if (state.mediaAlbums.any { it.id == albumId }) {
                log.d { "Detach album albumId=$albumId from mediaId=$mid" }
                removeMediaFromAlbumUseCase.execute(mid, albumId)
            } else {
                log.d { "Attach album albumId=$albumId to mediaId=$mid" }
                addMediaToAlbumUseCase.execute(mid, albumId)
            }
        }
    }
}
