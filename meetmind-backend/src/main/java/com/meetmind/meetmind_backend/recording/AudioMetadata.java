package com.meetmind.meetmind_backend.recording;

import jakarta.persistence.*;

@Entity
@Table(name = "audio_metadata")
public class AudioMetadata {

    @Id
    private Long recordingId;

    private String codec;
    private Integer sampleRate;
    private Integer channels;
    private Long bitRate;
    private Long durationMs;
    private String format; // wav, aac, etc.

    public AudioMetadata() {}

    public Long getRecordingId() { return recordingId; }
    public void setRecordingId(Long recordingId) { this.recordingId = recordingId; }
    public String getCodec() { return codec; }
    public void setCodec(String codec) { this.codec = codec; }
    public Integer getSampleRate() { return sampleRate; }
    public void setSampleRate(Integer sampleRate) { this.sampleRate = sampleRate; }
    public Integer getChannels() { return channels; }
    public void setChannels(Integer channels) { this.channels = channels; }
    public Long getBitRate() { return bitRate; }
    public void setBitRate(Long bitRate) { this.bitRate = bitRate; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}
