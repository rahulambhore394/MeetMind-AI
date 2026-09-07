package com.developer_rahul.meetmind_ai.feature.meetings.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StandalonePresenceRepository : PresenceRepository {

    private val _onlineParticipantIds = MutableStateFlow<Set<Long>>(setOf(101L, 102L, 103L))
    override val onlineParticipantIds: StateFlow<Set<Long>> = _onlineParticipantIds.asStateFlow()

    override fun subscribeToPresence(meetingId: Long) {
        _onlineParticipantIds.value = setOf(101L, 102L, 103L)
    }

    override fun unsubscribeFromPresence(meetingId: Long) {
        _onlineParticipantIds.value = emptySet()
    }
}
