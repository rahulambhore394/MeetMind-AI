package com.developer_rahul.meetmind_ai.core.network.websocket.stomp

data class StompFrame(
    val command: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
) {
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(command).append("\n")
        headers.forEach { (key, value) ->
            sb.append(key).append(":").append(value).append("\n")
        }
        sb.append("\n")
        if (body != null) {
            sb.append(body)
        }
        sb.append("\u0000")
        return sb.toString()
    }

    companion object {
        fun parse(data: String): StompFrame {
            val lines = data.split("\n")
            val command = lines[0].trim()
            val headers = mutableMapOf<String, String>()
            var bodyIndex = 1
            for (i in 1 until lines.size) {
                if (lines[i].isBlank()) {
                    bodyIndex = i + 1
                    break
                }
                val headerParts = lines[i].split(":", limit = 2)
                if (headerParts.size == 2) {
                    headers[headerParts[0].trim()] = headerParts[1].trim()
                }
            }
            val body = if (bodyIndex < lines.size) {
                lines.subList(bodyIndex, lines.size).joinToString("\n").replace("\u0000", "").trim()
            } else null
            
            return StompFrame(command, headers, body)
        }
    }
}
