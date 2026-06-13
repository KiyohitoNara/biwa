package io.github.kiyohitonara.biwa.presentation.settings

import io.github.kiyohitonara.biwa.domain.model.AppTheme
import io.github.kiyohitonara.biwa.domain.model.UserPreferences
import io.github.kiyohitonara.biwa.domain.repository.UserPreferencesRepository
import io.github.kiyohitonara.biwa.domain.usecase.GetUserPreferencesUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SetThemeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakeUserPreferencesRepository()
    private lateinit var viewModel: SettingsViewModel
    private lateinit var collectionJob: Job

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = buildViewModel()
        collectionJob = CoroutineScope(testDispatcher).launch { viewModel.uiState.collect() }
    }

    @AfterTest
    fun teardown() {
        collectionJob.cancel()
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = SettingsViewModel(
        getUserPreferencesUseCase = GetUserPreferencesUseCase(fakeRepository),
        setThemeUseCase = SetThemeUseCase(fakeRepository),
    )

    @Test
    fun `uiState reflects default preferences initially`() = runTest {
        assertEquals(AppTheme.SYSTEM, viewModel.uiState.value.theme)
    }

    @Test
    fun `setTheme updates uiState`() = runTest {
        viewModel.setTheme(AppTheme.DARK)

        assertEquals(AppTheme.DARK, viewModel.uiState.value.theme)
    }
}

private class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val _prefs = MutableStateFlow(UserPreferences())

    override fun getPreferences(): Flow<UserPreferences> = _prefs.asStateFlow()

    override suspend fun setTheme(theme: AppTheme) {
        _prefs.value = _prefs.value.copy(theme = theme)
    }
}
