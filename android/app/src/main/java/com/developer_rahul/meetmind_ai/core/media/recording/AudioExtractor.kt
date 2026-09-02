package com.developer_rahul.meetmind_ai.core.media.recording

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

class AudioExtractor {
    
    private val TAG = "AudioExtractor"

    fun extractAudio(inputPath: String, outputPath: String): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        
        try {
            extractor.setDataSource(inputPath)
            val trackCount = extractor.trackCount
            var audioTrackIndex = -1
            
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    extractor.selectTrack(i)
                    break
                }
            }
            
            if (audioTrackIndex == -1) {
                Log.e(TAG, "No audio track found in $inputPath")
                return false
            }
            
            val audioFormat = extractor.getTrackFormat(audioTrackIndex)
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val writeAudioTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()
            
            val maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                @Suppress("WrongConstant")
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(writeAudioTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }
            
            muxer.stop()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Audio extraction failed", e)
            return false
        } finally {
            extractor.release()
            muxer?.release()
        }
    }
}
