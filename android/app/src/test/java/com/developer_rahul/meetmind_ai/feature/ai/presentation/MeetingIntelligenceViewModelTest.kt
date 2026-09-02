package com.developer_rahul.meetmind_ai.feature.ai.presentation

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.SummaryDetailResponseDto
import com.developer_rahul.meetmind_ai.feature.intelligence.data.repository.IntelligenceRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Collections.emptyList

@OptIn(ExperimentalCoroutinesApi::class)
class MeetingIntelligenceViewModelTest {

    private val repository: IntelligenceRepository = mockk(relaxed = true)
    private lateinit var viewModel: MeetingIntelligenceViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val eventFlow = MutableSharedFlow<Map<String, String>>()
        coEvery { repository.intelligenceEvents } returns eventFlow
        
        viewModel = MeetingIntelligenceViewModel(repository, 1L)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadIntelligence updates uiState with success`() = runTest {
        val summary = SummaryDetailResponseDto(
            id = 1, meetingId = 1, transcriptId = 1,
            summary = "Test summary", 
            keyPoints = emptyList<String>(),
            decisions = emptyList<String>(), 
            topics = emptyList<String>(),
            questions = emptyList<String>(), 
            analysisMetrics = emptyMap<String, kotlinx.serialization.json.JsonElement>(),
            status = "COMPLETED", 
            createdAt = ""
        )
        
        coEvery { repository.getSummary(1L) } returns NetworkResult.Success(summary)
        
        viewModel.loadIntelligence()
        advanceUntilIdle()
        
        assertEquals(summary, viewModel.uiState.value.summary)
        assertEquals("COMPLETED", viewModel.uiState.value.status)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }
}
