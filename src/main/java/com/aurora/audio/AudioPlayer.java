package com.aurora.audio;

import com.aurora.model.Track;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AudioPlayer {

    private static final Logger LOGGER = Logger.getLogger(AudioPlayer.class.getName());
    private static final int BUFFER_SIZE = 4096;

    private final List<AudioPlayerListener> listeners = new ArrayList<>();
    private final Object stateLock = new Object();

    private volatile PlaybackState state = PlaybackState.STOPPED;
    private volatile Track currentTrack = null;

    private Thread playbackThread = null;
    private SourceDataLine line = null;
    private AudioInputStream baseStream = null;
    private AudioInputStream decodedStream = null;

    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicLong seekRequestMs = new AtomicLong(-1);

    private volatile float volume = 0.85f; // 0.0 to 1.0
    private volatile boolean muted = false;

    private volatile long currentPositionMs = 0;
    private volatile long totalDurationMs = 0;

    public void addListener(AudioPlayerListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(AudioPlayerListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public PlaybackState getState() {
        return state;
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public long getCurrentPositionMs() {
        return currentPositionMs;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public float getVolume() {
        return volume;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMute(boolean mute) {
        this.muted = mute;
        updateHardwareVolume();
    }

    public void setVolume(float vol) {
        this.volume = Math.max(0.0f, Math.min(1.0f, vol));
        if (this.muted && this.volume > 0) {
            this.muted = false;
        }
        updateHardwareVolume();
    }

    public void play(Track track) {
        if (track == null) return;

        synchronized (stateLock) {
            if (currentTrack != null && currentTrack.equals(track) && state == PlaybackState.PAUSED) {
                resume();
                return;
            }

            stopInternal();

            this.currentTrack = track;
            this.totalDurationMs = track.getDurationMs();
            this.currentPositionMs = 0;
            this.seekRequestMs.set(-1);
            this.stopRequested.set(false);
            this.isPaused.set(false);

            startPlaybackThread(0);
        }
    }

    public void pause() {
        synchronized (stateLock) {
            if (state == PlaybackState.PLAYING) {
                isPaused.set(true);
                setState(PlaybackState.PAUSED);
            }
        }
    }

    public void resume() {
        synchronized (stateLock) {
            if (state == PlaybackState.PAUSED) {
                isPaused.set(false);
                stateLock.notifyAll();
                setState(PlaybackState.PLAYING);
            }
        }
    }

    public void stop() {
        synchronized (stateLock) {
            stopInternal();
            currentPositionMs = 0;
            setState(PlaybackState.STOPPED);
            fireProgress(0, totalDurationMs);
        }
    }

    public void seekTo(long targetMs) {
        if (currentTrack == null) return;
        targetMs = Math.max(0, Math.min(targetMs, totalDurationMs));
        seekRequestMs.set(targetMs);

        synchronized (stateLock) {
            stateLock.notifyAll();
        }
    }

    private void stopInternal() {
        stopRequested.set(true);
        isPaused.set(false);

        synchronized (stateLock) {
            stateLock.notifyAll();
        }

        if (playbackThread != null) {
            try {
                playbackThread.join(400);
            } catch (InterruptedException ignored) {}
            playbackThread = null;
        }

        closeResources();
    }

    private void startPlaybackThread(long startMs) {
        playbackThread = new Thread(() -> runPlaybackLoop(startMs), "AudioPlayer-Worker");
        playbackThread.setDaemon(true);
        playbackThread.start();
        setState(PlaybackState.PLAYING);
    }

    private void runPlaybackLoop(long startMs) {
        while (!stopRequested.get() && currentTrack != null) {
            File audioFile = currentTrack.getFile();
            long targetSeek = seekRequestMs.getAndSet(-1);
            long initialPosMs = (targetSeek >= 0) ? targetSeek : startMs;
            startMs = 0; // only for the very first pass

            try {
                baseStream = AudioSystem.getAudioInputStream(audioFile);
                AudioFormat baseFormat = baseStream.getFormat();

                float sampleRate = baseFormat.getSampleRate();
                if (sampleRate <= 0) sampleRate = 44100.0f;
                int channels = baseFormat.getChannels();
                if (channels <= 0) channels = 2;

                AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate,
                    16,
                    channels,
                    channels * 2,
                    sampleRate,
                    false // Little-endian
                );

                decodedStream = AudioSystem.getAudioInputStream(decodedFormat, baseStream);

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                if (!AudioSystem.isLineSupported(info)) {
                    throw new LineUnavailableException("Audio line not supported for format: " + decodedFormat);
                }

                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(decodedFormat, BUFFER_SIZE * 4);
                line.start();
                updateHardwareVolume();

                long bytesPerSec = (long) (sampleRate * channels * 2);
                long bytesToSkip = 0;
                if (initialPosMs > 0 && bytesPerSec > 0) {
                    bytesToSkip = (initialPosMs * bytesPerSec) / 1000L;
                    // Align to frame
                    bytesToSkip -= (bytesToSkip % (channels * 2));
                    long skipped = skipFully(decodedStream, bytesToSkip);
                    currentPositionMs = (skipped * 1000L) / bytesPerSec;
                } else {
                    currentPositionMs = 0;
                }

                byte[] buffer = new byte[BUFFER_SIZE];
                long totalBytesRead = bytesToSkip;
                boolean reachedEnd = false;

                while (!stopRequested.get()) {
                    // Check seek request
                    long newSeek = seekRequestMs.get();
                    if (newSeek >= 0) {
                        // Re-cycle outer loop to cleanly reopen and seek
                        break;
                    }

                    // Handle pause
                    while (isPaused.get() && !stopRequested.get()) {
                        synchronized (stateLock) {
                            try {
                                if (line != null && line.isRunning()) {
                                    line.stop();
                                }
                                stateLock.wait(100);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }

                    if (stopRequested.get()) break;

                    if (line != null && !line.isRunning()) {
                        line.start();
                    }

                    int bytesRead = decodedStream.read(buffer, 0, buffer.length);
                    if (bytesRead == -1) {
                        reachedEnd = true;
                        break;
                    }

                    totalBytesRead += bytesRead;
                    if (bytesPerSec > 0) {
                        currentPositionMs = (totalBytesRead * 1000L) / bytesPerSec;
                        fireProgress(currentPositionMs, totalDurationMs);
                    }

                    // Apply software volume/mute and deliver data to visualizer
                    byte[] processedBuffer = applyVolumeAndCopy(buffer, bytesRead, channels);

                    line.write(processedBuffer, 0, bytesRead);
                    fireAudioData(processedBuffer, bytesRead, channels, sampleRate);
                }

                if (line != null) {
                    line.drain();
                }

                closeResources();

                // If finished track naturally
                if (reachedEnd && !stopRequested.get() && seekRequestMs.get() < 0) {
                    setState(PlaybackState.STOPPED);
                    fireTrackFinished();
                    break;
                }

            } catch (Exception e) {
                if (!stopRequested.get()) {
                    LOGGER.log(Level.WARNING, "Audio playback error: " + e.getMessage(), e);
                    fireError("Error playing track: " + e.getMessage(), e);
                    setState(PlaybackState.STOPPED);
                }
                closeResources();
                break;
            }
        }
    }

    private long skipFully(AudioInputStream stream, long bytesToSkip) throws IOException {
        long totalSkipped = 0;
        byte[] skipBuffer = new byte[4096];
        while (totalSkipped < bytesToSkip) {
            long remaining = bytesToSkip - totalSkipped;
            int toRead = (int) Math.min(skipBuffer.length, remaining);
            int read = stream.read(skipBuffer, 0, toRead);
            if (read == -1) break;
            totalSkipped += read;
        }
        return totalSkipped;
    }

    private byte[] applyVolumeAndCopy(byte[] buffer, int length, int channels) {
        byte[] copy = new byte[length];
        System.arraycopy(buffer, 0, copy, 0, length);

        float effVol = muted ? 0.0f : volume;
        if (effVol >= 0.99f) {
            return copy; // No scaling needed
        }

        // 16-bit signed PCM scaling
        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short) ((copy[i] & 0xFF) | (copy[i + 1] << 8));
            short scaled = (short) (sample * effVol);
            copy[i] = (byte) (scaled & 0xFF);
            copy[i + 1] = (byte) ((scaled >> 8) & 0xFF);
        }
        return copy;
    }

    private void updateHardwareVolume() {
        if (line != null && line.isOpen()) {
            try {
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                    float effVol = muted ? 0.0001f : Math.max(0.0001f, volume);
                    // Convert linear volume (0.0001 - 1.0) to decibels
                    float dB = (float) (Math.log10(effVol) * 20.0);
                    dB = Math.max(gainControl.getMinimum(), Math.min(dB, gainControl.getMaximum()));
                    gainControl.setValue(dB);
                }
            } catch (Exception ignored) {}
        }
    }

    private void closeResources() {
        if (line != null) {
            try {
                line.stop();
                line.close();
            } catch (Exception ignored) {}
            line = null;
        }
        if (decodedStream != null) {
            try {
                decodedStream.close();
            } catch (Exception ignored) {}
            decodedStream = null;
        }
        if (baseStream != null) {
            try {
                baseStream.close();
            } catch (Exception ignored) {}
            baseStream = null;
        }
    }

    private void setState(PlaybackState newState) {
        this.state = newState;
        List<AudioPlayerListener> copy;
        synchronized (listeners) {
            copy = new ArrayList<>(listeners);
        }
        for (AudioPlayerListener l : copy) {
            try {
                l.onStateChanged(newState);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Listener error", e);
            }
        }
    }

    private void fireProgress(long currentMs, long totalMs) {
        List<AudioPlayerListener> copy;
        synchronized (listeners) {
            copy = new ArrayList<>(listeners);
        }
        for (AudioPlayerListener l : copy) {
            try {
                l.onProgress(currentMs, totalMs);
            } catch (Exception ignored) {}
        }
    }

    private void fireTrackFinished() {
        List<AudioPlayerListener> copy;
        synchronized (listeners) {
            copy = new ArrayList<>(listeners);
        }
        for (AudioPlayerListener l : copy) {
            try {
                l.onTrackFinished();
            } catch (Exception ignored) {}
        }
    }

    private void fireAudioData(byte[] data, int length, int channels, float sampleRate) {
        List<AudioPlayerListener> copy;
        synchronized (listeners) {
            copy = new ArrayList<>(listeners);
        }
        for (AudioPlayerListener l : copy) {
            try {
                l.onAudioData(data, length, channels, sampleRate);
            } catch (Exception ignored) {}
        }
    }

    private void fireError(String message, Throwable t) {
        List<AudioPlayerListener> copy;
        synchronized (listeners) {
            copy = new ArrayList<>(listeners);
        }
        for (AudioPlayerListener l : copy) {
            try {
                l.onError(message, t);
            } catch (Exception ignored) {}
        }
    }
}
