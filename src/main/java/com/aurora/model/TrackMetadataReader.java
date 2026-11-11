package com.aurora.model;

import com.aurora.ui.AlbumArtUtil;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrackMetadataReader {

    private static final Logger LOGGER = Logger.getLogger(TrackMetadataReader.class.getName());

    static {
        // Suppress noisy jaudiotagger internal logs
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    public static Track read(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        String title = null;
        String artist = null;
        String album = null;
        String year = null;
        String genre = null;
        String format = extractExtension(file).toUpperCase();
        int bitrateKbps = 0;
        int sampleRate = 44100;
        int channels = 2;
        long durationMs = 0;
        BufferedImage albumArt = null;

        // 1. Try reading with jaudiotagger
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader header = audioFile.getAudioHeader();
            if (header != null) {
                durationMs = header.getTrackLength() * 1000L;
                bitrateKbps = (int) header.getBitRateAsNumber();
                sampleRate = header.getSampleRateAsNumber();
                try {
                    String chStr = header.getChannels();
                    if (chStr != null && !chStr.isEmpty()) {
                        channels = Integer.parseInt(chStr.replaceAll("[^0-9]", ""));
                    }
                } catch (Exception ignored) {}
                String fmt = header.getFormat();
                if (fmt != null && !fmt.isEmpty()) {
                    format = fmt.toUpperCase();
                }
            }

            Tag tag = audioFile.getTag();
            if (tag != null) {
                title = tag.getFirst(FieldKey.TITLE);
                artist = tag.getFirst(FieldKey.ARTIST);
                album = tag.getFirst(FieldKey.ALBUM);
                year = tag.getFirst(FieldKey.YEAR);
                genre = tag.getFirst(FieldKey.GENRE);

                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null && artwork.getBinaryData() != null) {
                    try {
                        albumArt = ImageIO.read(new ByteArrayInputStream(artwork.getBinaryData()));
                    } catch (Exception e) {
                        LOGGER.log(Level.FINE, "Failed to parse artwork image", e);
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Jaudiotagger fallback for: " + file.getName(), t);
        }

        // 2. Fallback to Java Sound API if duration or audio format is missing
        if (durationMs <= 0 || sampleRate <= 0) {
            try {
                AudioFileFormat aff = AudioSystem.getAudioFileFormat(file);
                AudioFormat af = aff.getFormat();
                if (sampleRate <= 0) sampleRate = (int) af.getSampleRate();
                if (channels <= 0) channels = af.getChannels();

                long frameLength = aff.getFrameLength();
                if (frameLength > 0 && af.getSampleRate() > 0) {
                    durationMs = (long) ((frameLength / af.getSampleRate()) * 1000.0);
                } else {
                    // Try opening stream to calculate duration from file size and bytes/sec
                    try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
                        AudioFormat streamFormat = ais.getFormat();
                        long fl = ais.getFrameLength();
                        if (fl > 0 && streamFormat.getSampleRate() > 0) {
                            durationMs = (long) ((fl / streamFormat.getSampleRate()) * 1000.0);
                        } else if (streamFormat.getFrameSize() > 0 && streamFormat.getFrameRate() > 0) {
                            long bytesPerSec = (long) (streamFormat.getFrameSize() * streamFormat.getFrameRate());
                            if (bytesPerSec > 0) {
                                durationMs = (file.length() * 1000L) / bytesPerSec;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 3. Fallback for title and artist if missing
        if (title == null || title.trim().isEmpty()) {
            String cleanName = extractFileNameWithoutExt(file);
            // Check for common "Artist - Title" filename convention
            if (cleanName.contains(" - ")) {
                String[] parts = cleanName.split(" - ", 2);
                if (artist == null || artist.trim().isEmpty()) {
                    artist = parts[0].trim();
                }
                title = parts[1].trim();
            } else {
                title = cleanName;
            }
        }

        if (artist == null || artist.trim().isEmpty()) {
            artist = "Unknown Artist";
        }

        if (album == null || album.trim().isEmpty()) {
            album = "Single Track";
        }

        // 4. Default cover art placeholder if none
        if (albumArt == null) {
            albumArt = AlbumArtUtil.generatePlaceholderArt(title, artist, 300, 16);
        }

        return new Track(file, title, artist, album, year, genre, format, bitrateKbps, sampleRate, channels, durationMs, albumArt);
    }

    private static String extractFileNameWithoutExt(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(0, dot) : name;
    }

    private static String extractExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
    }
}
