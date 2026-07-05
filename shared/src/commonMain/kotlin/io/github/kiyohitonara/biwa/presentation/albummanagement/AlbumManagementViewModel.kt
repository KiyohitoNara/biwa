package io.github.kiyohitonara.biwa.presentation.albummanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.kiyohitonara.biwa.domain.model.Album
import io.github.kiyohitonara.biwa.domain.usecase.AddMediaToAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.CreateAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.DeleteAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAlbumsForMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllAlbumsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.MoveAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RemoveMediaFromAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RenameAlbumUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Pass [mediaId] to enable media-specific album toggling; pass null for global album
 * management (create / rename / move / delete). In management mode the view drills down
 * through the album tree one level at a time via [enterAlbum] and [navigateUp].
 */
@Suppress("TooManyFunctions") // Cohesive album management surface (CRUD + tree navigation).
class AlbumManagementViewModel(
    private val mediaId: String?,
    private val getAllAlbumsUseCase: GetAllAlbumsUseCase,
    private val createAlbumUseCase: CreateAlbumUseCase,
    private val renameAlbumUseCase: RenameAlbumUseCase,
    private val moveAlbumUseCase: MoveAlbumUseCase,
    private val deleteAlbumUseCase: DeleteAlbumUseCase,
    private val getAlbumsForMediaUseCase: GetAlbumsForMediaUseCase,
    private val addMediaToAlbumUseCase: AddMediaToAlbumUseCase,
    private val removeMediaFromAlbumUseCase: RemoveMediaFromAlbumUseCase,
    logger: Logger,
) : ViewModel() {
    private val log = logger.withTag("AlbumManagementViewModel")

    /** The album currently opened during drill-down, or null at the root level. */
    private val currentParentId = MutableStateFlow<String?>(null)

    /**
     * Current state combining all albums, the media-specific album list, and the
     * drill-down position.
     *
     * Stays [AlbumManagementUiState.Loading] until the first DB emission arrives.
     */
    val uiState: StateFlow<AlbumManagementUiState> =
        combine(
            getAllAlbumsUseCase.execute(),
            mediaId?.let { getAlbumsForMediaUseCase.execute(it) } ?: flowOf(emptyList()),
            currentParentId,
        ) { allAlbums, mediaAlbums, parentId ->
            // If the opened album disappeared (e.g. deleted via cascade), fall back to the root.
            val resolvedParentId = parentId?.takeIf { id -> allAlbums.any { it.id == id } }
            if (resolvedParentId != parentId) currentParentId.value = resolvedParentId
            AlbumManagementUiState.Ready(
                allAlbums = allAlbums,
                mediaAlbums = mediaAlbums,
                currentParentId = resolvedParentId,
                currentAlbums = allAlbums.filter { it.parentId == resolvedParentId },
                breadcrumb = buildBreadcrumb(allAlbums, resolvedParentId),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AlbumManagementUiState.Loading,
        )

    private val _error = MutableSharedFlow<String>()

    /** Emits an error message when an album operation fails (e.g. duplicate name). One-shot event. */
    val error: SharedFlow<String> = _error.asSharedFlow()

    /** Opens the album identified by [id], showing its child albums. */
    fun enterAlbum(id: String) {
        log.d { "enterAlbum id=$id" }
        currentParentId.value = id
    }

    /**
     * Navigates one level up in the album tree.
     *
     * @return true if the view moved up a level, or false if it was already at the root.
     */
    fun navigateUp(): Boolean {
        val current = currentParentId.value ?: return false
        val parent = (uiState.value as? AlbumManagementUiState.Ready)?.allAlbums?.find { it.id == current }?.parentId
        currentParentId.value = parent
        return true
    }

    /**
     * Creates a new album named [name] as a child of the currently opened album
     * (or at the root when none is open).
     *
     * Emits on [error] if the name is blank or already taken.
     */
    @Suppress("TooGenericExceptionCaught") // Translate any use-case failure into a UI error event.
    fun createAlbum(name: String) {
        val parentId = currentParentId.value
        log.i { "Create album name=$name parentId=$parentId" }
        viewModelScope.launch {
            try {
                createAlbumUseCase.execute(name, parentId)
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
     * Moves the album identified by [id] under [targetParentId], or to the root when null.
     *
     * Emits on [error] if the move would create a cycle.
     */
    @Suppress("TooGenericExceptionCaught") // Translate any use-case failure into a UI error event.
    fun moveAlbum(
        id: String,
        targetParentId: String?,
    ) {
        log.i { "Move album id=$id targetParentId=$targetParentId" }
        viewModelScope.launch {
            try {
                moveAlbumUseCase.execute(id, targetParentId)
            } catch (e: Exception) {
                log.w(e) { "Failed to move album id=$id" }
                _error.emit(e.message ?: "Failed to move album")
            }
        }
    }

    /**
     * Returns the albums that [albumId] can be moved under: every album except itself
     * and its descendants (which would form a cycle).
     */
    fun validMoveTargets(albumId: String): List<Album> {
        val all = (uiState.value as? AlbumManagementUiState.Ready)?.allAlbums ?: return emptyList()
        val blocked = descendants(albumId, all) + albumId
        return all.filterNot { it.id in blocked }
    }

    /** Deletes the album identified by [id] along with its descendant albums and all their media associations. */
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

    private fun buildBreadcrumb(
        all: List<Album>,
        currentId: String?,
    ): List<Album> {
        val byId = all.associateBy { it.id }
        val chain = ArrayDeque<Album>()
        var cursor = currentId
        while (cursor != null) {
            val album = byId[cursor] ?: break
            chain.addFirst(album)
            cursor = album.parentId
        }
        return chain.toList()
    }

    private fun descendants(
        rootId: String,
        all: List<Album>,
    ): Set<String> {
        val result = mutableSetOf<String>()
        var frontier = listOf(rootId)
        while (frontier.isNotEmpty()) {
            val children = all.filter { it.parentId in frontier && it.id !in result }.map { it.id }
            result += children
            frontier = children
        }
        return result
    }
}
