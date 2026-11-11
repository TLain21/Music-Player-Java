package com.aurora.audio;

public interface AudioPlayerListener {
    void onStateChanged(PlaybackState state);
    void onProgress(long currentMs, long totalMs);
    void onTrackFinished();
    void onAudioData(byte[] pcmData, int length, int channels, float sampleRate);
    void onError(String message, Throwable t);
}
