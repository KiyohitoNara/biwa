package io.github.kiyohitonara.biwa.presentation.tagmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kiyohitonara.biwa.domain.usecase.AddTagToMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.CreateTagUseCase
import io.github.kiyohitonara.biwa.domain.usecase.DeleteTagUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllTagsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetTagsForMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RemoveTagFromMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RenameTagUseCase
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
 * Manages tag CRUD operations and, when [mediaId] is set, the tag assignments
 * for a specific media item.
 *
 * Pass [mediaId] to enable media-specific tag toggling; pass null for
 * global tag management (create / rename / delete only).
 */
class TagManagementViewModel(
    private val mediaId: String?,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val createTagUseCase: CreateTagUseCase,
    private val renameTagUseCase: RenameTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase,
    private val getTagsForMediaUseCase: GetTagsForMediaUseCase,
    private val addTagToMediaUseCase: AddTagToMediaUseCase,
    private val removeTagFromMediaUseCase: RemoveTagFromMediaUseCase,
) : ViewModel() {

    /**
     * Current state combining all tags with the media-specific tag list.
     *
     * Stays [TagManagementUiState.Loading] until the first DB emission arrives.
     */
    val uiState: StateFlow<TagManagementUiState> = combine(
        getAllTagsUseCase.execute(),
        mediaId?.let { getTagsForMediaUseCase.execute(it) } ?: flowOf(emptyList()),
    ) { allTags, mediaTags ->
        TagManagementUiState.Ready(allTags = allTags, mediaTags = mediaTags)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TagManagementUiState.Loading,
    )

    private val _error = MutableSharedFlow<String>()

    /** Emits an error message when a tag operation fails (e.g. duplicate name). One-shot event. */
    val error: SharedFlow<String> = _error.asSharedFlow()

    /**
     * Creates a new tag with [name].
     *
     * Emits on [error] if the name is blank or already taken.
     */
    fun createTag(name: String) {
        viewModelScope.launch {
            try {
                createTagUseCase.execute(name)
            } catch (e: Exception) {
                _error.emit(e.message ?: "Failed to create tag")
            }
        }
    }

    /**
     * Renames the tag identified by [id] to [name].
     *
     * Emits on [error] if the name is blank or already taken.
     */
    fun renameTag(id: String, name: String) {
        viewModelScope.launch {
            try {
                renameTagUseCase.execute(id, name)
            } catch (e: Exception) {
                _error.emit(e.message ?: "Failed to rename tag")
            }
        }
    }

    /**
     * Deletes the tag identified by [id] along with all its media associations.
     */
    fun deleteTag(id: String) {
        viewModelScope.launch { deleteTagUseCase.execute(id) }
    }

    /**
     * Toggles the tag identified by [tagId] on the current media item.
     *
     * Attaches the tag if not already assigned; detaches it otherwise.
     * No-op when [mediaId] is null.
     */
    fun toggleTagForMedia(tagId: String) {
        val mid = mediaId ?: return
        val state = uiState.value as? TagManagementUiState.Ready ?: return
        viewModelScope.launch {
            if (state.mediaTags.any { it.id == tagId }) {
                removeTagFromMediaUseCase.execute(mid, tagId)
            } else {
                addTagToMediaUseCase.execute(mid, tagId)
            }
        }
    }
}
