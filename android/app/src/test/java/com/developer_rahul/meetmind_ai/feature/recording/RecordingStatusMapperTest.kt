package com.developer_rahul.meetmind_ai.feature.recording

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingStatusMapperTest {

    @Test
    fun `maps backend status to readable string`() {
        val statuses = listOf(
            "AUDIO_PREPARING" to "AUDIO PREPARING",
            "AUDIO_CHUNKING" to "AUDIO CHUNKING",
            "READY_FOR_TRANSCRIPTION" to "READY FOR TRANSCRIPTION",
            "FAILED" to "FAILED"
        )

        statuses.forEach { (backend, expected) ->
            val actual = backend.replace("_", " ")
            assertEquals(expected, actual)
        }
    }
}
