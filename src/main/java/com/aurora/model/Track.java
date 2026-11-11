package com.aurora.model;

import java.awt.image.BufferedImage;
import java.io.File;

public class Track {
    private final File file;
    private final String title;
    private final String artist;
    private final String album;
    private final String year;
    private final String genre;
    private final String format;
    private final int bitrateKbps;
    private final int sampleRate;
    private final int channels;
    private final long durationMs;
    private final BufferedImage albumArt;

    public Track(File file, String title, String artist, String album, String year,
                 String genre, String format, int bitrateKbps, int sampleRate,
                 int channels, long durationMs, BufferedImage albumArt) {
        this.file = file;
        this.title = (title != null && !title.trim().isEmpty()) ? title.trim() : extractFileNameWithoutExt(file);
        this.artist = (artist != null && !artist.trim().isEmpty()) ? artist.trim() : "Unknown Artist";
        this.album = (album != null && !album.trim().isEmpty()) ? album.trim() : "Unknown Album";
        this.year = (year != null && !year.trim().isEmpty()) ? year.trim() : "";
        this.genre = (genre != null && !genre.trim().isEmpty()) ? genre.trim() : "Unknown Genre";
        this.format = (format != null && !format.trim().isEmpty()) ? format.toUpperCase() : extractExtension(file).toUpperCase();
        this.bitrateKbps = bitrateKbps;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.durationMs = Math.max(0, durationMs);
        this.albumArt = albumArt;
    }

    private static String extractFileNameWithoutExt(File file) {
        if (file == null) return "Unknown Track";
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(0, dot) : name;
    }

    private static String extractExtension(File file) {
        if (file == null) return "";
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
    }

    public File getFile() {
        return file;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getYear() {
        return year;
    }

    public String getGenre() {
        return genre;
    }

    public String getFormat() {
        return format;
    }

    public int getBitrateKbps() {
        return bitrateKbps;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public BufferedImage getAlbumArt() {
        return albumArt;
    }

    public String getFormattedDuration() {
        long totalSec = durationMs / 1000;
        long minutes = totalSec / 60;
        long seconds = totalSec % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public String getFormattedBitrate() {
        if (bitrateKbps > 0) {
            return bitrateKbps + " kbps";
        }
        return "VBR/Lossless";
    }

    public String getFormattedSampleRate() {
        if (sampleRate > 0) {
            return String.format("%.1f kHz", sampleRate / 1000.0);
        }
        return "";
    }

    public String getFormattedQuality() {
        StringBuilder sb = new StringBuilder();
        sb.append(format);
        if (sampleRate > 0) {
            sb.append(" • ").append(getFormattedSampleRate());
        }
        if (bitrateKbps > 0) {
            sb.append(" • ").append(bitrateKbps).append(" kbps");
        } else if ("FLAC".equalsIgnoreCase(format) || "WAV".equalsIgnoreCase(format)) {
            sb.append(" • Lossless");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return artist + " - " + title;
    }
}
