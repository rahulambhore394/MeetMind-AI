package com.developer_rahul.meetmind_ai.feature.meetings.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.MeetingApiService
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.MeetingResponseDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.ParticipantResponseDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RealMeetingRepositoryTest {

    private val apiService = mockk<MeetingApiService>()
    private val repository = RealMeetingRepository(apiService)

    @Test
    fun `getAllMeetings maps DTOs to Domain models`() = runBlocking {
        // Arrange
        val dtos = listOf(
            MeetingResponseDto(1L, "Title", "Desc", 1L, "Host", "host@test.com", "2023-10-15T10:00:00", null, null, "2023-10-15T09:00:00", "SCHEDULED")
        )
        coEvery { apiService.getAllMeetings() } returns dtos

        // Act
        val result = repository.getAllMeetings()

        // Assert
        assertTrue(result is NetworkResult.Success)
        val meetings = (result as NetworkResult.Success).data
        assertEquals(1, meetings.size)
        assertEquals("Title", meetings[0].title)
    }

    @Test
    fun `createMeeting calls API and returns Success`() = runBlocking {
        // Arrange
        val dto = MeetingResponseDto(1L, "New", "Desc", 1L, "Host", "host@test.com", "2023-10-15T10:00:00", null, null, "2023-10-15T09:00:00", "SCHEDULED")
        coEvery { apiService.createMeeting(any()) } returns dto

        // Act
        val result = repository.createMeeting("New", "Desc", "2023-10-15T10:00:00")

        // Assert
        assertTrue(result is NetworkResult.Success)
        assertEquals("New", (result as NetworkResult.Success).data.title)
    }

    @Test
    fun `getParticipants returns list of participants`() = runBlocking {
        // Arrange
        val dtos: List<ParticipantResponseDto> = listOf(
            ParticipantResponseDto(1L, 1L, "Rahul", "rahul@test.com", "HOST", "JOINED")
        )
        coEvery { apiService.getParticipants(1L) } returns dtos

        // Act
        val result = repository.getParticipants(1L)

        // Assert
        assertTrue(result is NetworkResult.Success)
        val participants = (result as NetworkResult.Success).data
        assertEquals(1, participants.size)
        assertEquals("Rahul", participants[0].name)
    }
}
