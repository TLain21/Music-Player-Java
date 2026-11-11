package com.aurora.model;

import com.aurora.audio.RepeatMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Playlist {

    private final List<Track> tracks = new ArrayList<>();
    private final List<Integer> shuffleIndices = new ArrayList<>();
    private int currentIndex = -1;
    private int shuffleCursor = -1;
    private boolean shuffle = false;
    private RepeatMode repeatMode = RepeatMode.OFF;

    public synchronized void addTrack(Track track) {
        if (track != null) {
            tracks.add(track);
            regenerateShuffleList();
        }
    }

    public synchronized void addTracks(List<Track> newTracks) {
        if (newTracks != null && !newTracks.isEmpty()) {
            tracks.addAll(newTracks);
            regenerateShuffleList();
        }
    }

    public synchronized void removeTrack(int index) {
        if (index >= 0 && index < tracks.size()) {
            tracks.remove(index);
            if (currentIndex == index) {
                if (tracks.isEmpty()) {
                    currentIndex = -1;
                } else if (currentIndex >= tracks.size()) {
                    currentIndex = 0;
                }
            } else if (currentIndex > index) {
                currentIndex--;
            }
            regenerateShuffleList();
        }
    }

    public synchronized void clear() {
        tracks.clear();
        shuffleIndices.clear();
        currentIndex = -1;
        shuffleCursor = -1;
    }

    public synchronized List<Track> getTracks() {
        return new ArrayList<>(tracks);
    }

    public synchronized int size() {
        return tracks.size();
    }

    public synchronized boolean isEmpty() {
        return tracks.isEmpty();
    }

    public synchronized int getCurrentIndex() {
        return currentIndex;
    }

    public synchronized Track getCurrentTrack() {
        if (currentIndex >= 0 && currentIndex < tracks.size()) {
            return tracks.get(currentIndex);
        }
        return null;
    }

    public synchronized Track getTrack(int index) {
        if (index >= 0 && index < tracks.size()) {
            return tracks.get(index);
        }
        return null;
    }

    public synchronized Track setIndex(int index) {
        if (index >= 0 && index < tracks.size()) {
            currentIndex = index;
            // Align shuffle cursor if shuffle enabled
            if (shuffle) {
                for (int i = 0; i < shuffleIndices.size(); i++) {
                    if (shuffleIndices.get(i) == currentIndex) {
                        shuffleCursor = i;
                        break;
                    }
                }
            }
            return tracks.get(currentIndex);
        }
        return null;
    }

    public synchronized Track next() {
        if (tracks.isEmpty()) {
            return null;
        }

        if (repeatMode == RepeatMode.ONE && currentIndex >= 0) {
            return tracks.get(currentIndex);
        }

        if (shuffle) {
            if (shuffleIndices.isEmpty()) {
                regenerateShuffleList();
            }
            shuffleCursor++;
            if (shuffleCursor >= shuffleIndices.size()) {
                if (repeatMode == RepeatMode.ALL) {
                    regenerateShuffleList();
                    shuffleCursor = 0;
                } else {
                    shuffleCursor = shuffleIndices.size() - 1;
                    return null; // reached end of queue
                }
            }
            currentIndex = shuffleIndices.get(shuffleCursor);
            return tracks.get(currentIndex);
        } else {
            int nextIdx = currentIndex + 1;
            if (nextIdx >= tracks.size()) {
                if (repeatMode == RepeatMode.ALL) {
                    nextIdx = 0;
                } else {
                    return null; // reached end of playlist
                }
            }
            currentIndex = nextIdx;
            return tracks.get(currentIndex);
        }
    }

    public synchronized Track previous() {
        if (tracks.isEmpty()) {
            return null;
        }

        if (repeatMode == RepeatMode.ONE && currentIndex >= 0) {
            return tracks.get(currentIndex);
        }

        if (shuffle) {
            if (shuffleCursor > 0) {
                shuffleCursor--;
                currentIndex = shuffleIndices.get(shuffleCursor);
                return tracks.get(currentIndex);
            } else if (repeatMode == RepeatMode.ALL && !shuffleIndices.isEmpty()) {
                shuffleCursor = shuffleIndices.size() - 1;
                currentIndex = shuffleIndices.get(shuffleCursor);
                return tracks.get(currentIndex);
            } else {
                return getCurrentTrack();
            }
        } else {
            int prevIdx = currentIndex - 1;
            if (prevIdx < 0) {
                if (repeatMode == RepeatMode.ALL) {
                    prevIdx = tracks.size() - 1;
                } else {
                    prevIdx = 0;
                }
            }
            currentIndex = prevIdx;
            return tracks.get(currentIndex);
        }
    }

    public synchronized boolean isShuffle() {
        return shuffle;
    }

    public synchronized void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
        if (shuffle) {
            regenerateShuffleList();
            // sync cursor to current track
            if (currentIndex >= 0) {
                for (int i = 0; i < shuffleIndices.size(); i++) {
                    if (shuffleIndices.get(i) == currentIndex) {
                        shuffleCursor = i;
                        break;
                    }
                }
            }
        }
    }

    public synchronized RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public synchronized void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }

    private void regenerateShuffleList() {
        shuffleIndices.clear();
        for (int i = 0; i < tracks.size(); i++) {
            shuffleIndices.add(i);
        }
        Collections.shuffle(shuffleIndices, new Random());
    }
}
