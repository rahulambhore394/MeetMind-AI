package com.developer_rahul.meetmind_ai.core.network.websocket.stomp

import org.junit.Assert.assertEquals
import org.junit.Test

class StompFrameTest {

    @Test
    fun `parse valid frame with body`() {
        val raw = "MESSAGE\ndestination:/topic/test\ncontent-type:text/plain\n\nHello World\u0000"
        val frame = StompFrame.parse(raw)
        
        assertEquals("MESSAGE", frame.command)
        assertEquals("/topic/test", frame.headers["destination"])
        assertEquals("text/plain", frame.headers["content-type"])
        assertEquals("Hello World", frame.body)
    }

    @Test
    fun `parse frame without body`() {
        val raw = "CONNECTED\nversion:1.2\n\n\u0000"
        val frame = StompFrame.parse(raw)
        
        assertEquals("CONNECTED", frame.command)
        assertEquals("1.2", frame.headers["version"])
        assertEquals(null, frame.body)
    }

    @Test
    fun `toString converts frame to STOMP format`() {
        val frame = StompFrame("SEND", mapOf("destination" to "/app/test"), "Payload")
        val expected = "SEND\ndestination:/app/test\n\nPayload\u0000"
        assertEquals(expected, frame.toString())
    }
}
