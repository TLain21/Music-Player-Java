package com.aurora.ui;

import com.aurora.audio.PlaybackState;
import com.aurora.audio.RepeatMode;
import com.aurora.model.Track;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class PlayerControlBar extends JPanel {

    // Left info
    private final JLabel miniArtLabel;
    private final JLabel titleLabel;
    private final JLabel artistLabel;

    // Center controls
    private final JButton shuffleBtn;
    private final JButton prevBtn;
    private final JButton playPauseBtn;
    private final JButton nextBtn;
    private final JButton repeatBtn;

    private final JLabel currentTimeLabel;
    private final JSlider seekSlider;
    private final JLabel totalTimeLabel;

    // Right volume
    private final JButton muteBtn;
    private final JSlider volumeSlider;
    private final JLabel volumePctLabel;

    // State
    private boolean isUserSeeking = false;
    private long currentTrackDurationMs = 0;

    // Callbacks
    private Runnable onPlayPause;
    private Runnable onNext;
    private Runnable onPrevious;
    private Runnable onToggleShuffle;
    private Runnable onToggleRepeat;
    private java.util.function.Consumer<Long> onSeek;
    private java.util.function.Consumer<Float> onVolumeChange;
    private java.util.function.Consumer<Boolean> onMuteToggle;

    public PlayerControlBar() {
        setLayout(new BorderLayout());
        setBackground(new Color(15, 17, 21));
        setBorder(new EmptyBorder(10, 20, 10, 20));
        setPreferredSize(new Dimension(800, 80));

        // 1. LEFT PANEL (Mini Track Summary)
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(240, 60));

        miniArtLabel = new JLabel();
        miniArtLabel.setPreferredSize(new Dimension(48, 48));
        miniArtLabel.setIcon(new ImageIcon(AlbumArtUtil.generatePlaceholderArt("A", "M", 48, 8)));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        titleLabel = new JLabel("No Track");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(Color.WHITE);

        artistLabel = new JLabel("Music Player Ready");
        artistLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        artistLabel.setForeground(new Color(150, 155, 170));

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(titleLabel);
        textPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        textPanel.add(artistLabel);
        textPanel.add(Box.createVerticalGlue());

        leftPanel.add(miniArtLabel);
        leftPanel.add(textPanel);

        // 2. CENTER PANEL (Buttons + Seekbar)
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Top button row
        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        buttonsRow.setOpaque(false);

        shuffleBtn = createIconButton("🔀", "Shuffle", 13);
        prevBtn = createIconButton("⏮", "Previous", 15);
        playPauseBtn = createPlayButton();
        nextBtn = createIconButton("⏭", "Next", 15);
        repeatBtn = createIconButton("🔁", "Repeat Mode", 13);

        buttonsRow.add(shuffleBtn);
        buttonsRow.add(prevBtn);
        buttonsRow.add(playPauseBtn);
        buttonsRow.add(nextBtn);
        buttonsRow.add(repeatBtn);

        // Bottom seek row
        JPanel seekRow = new JPanel(new BorderLayout(8, 0));
        seekRow.setOpaque(false);
        seekRow.setBorder(new EmptyBorder(4, 0, 0, 0));

        currentTimeLabel = new JLabel("00:00");
        currentTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        currentTimeLabel.setForeground(new Color(160, 165, 180));

        totalTimeLabel = new JLabel("00:00");
        totalTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        totalTimeLabel.setForeground(new Color(160, 165, 180));

        seekSlider = new JSlider(0, 1000, 0);
        seekSlider.setOpaque(false);
        seekSlider.setCursor(new Cursor(Cursor.HAND_CURSOR));

        seekSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isUserSeeking = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isUserSeeking = false;
                if (currentTrackDurationMs > 0 && onSeek != null) {
                    double pct = seekSlider.getValue() / 1000.0;
                    long targetMs = (long) (pct * currentTrackDurationMs);
                    onSeek.accept(targetMs);
                }
            }
        });

        seekSlider.addChangeListener(e -> {
            if (isUserSeeking && currentTrackDurationMs > 0) {
                double pct = seekSlider.getValue() / 1000.0;
                long targetMs = (long) (pct * currentTrackDurationMs);
                currentTimeLabel.setText(formatTime(targetMs));
            }
        });

        seekRow.add(currentTimeLabel, BorderLayout.WEST);
        seekRow.add(seekSlider, BorderLayout.CENTER);
        seekRow.add(totalTimeLabel, BorderLayout.EAST);

        centerPanel.add(buttonsRow);
        centerPanel.add(seekRow);

        // 3. RIGHT PANEL (Volume Controls)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 14));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(240, 60));

        muteBtn = createIconButton("🔊", "Mute / Unmute", 14);
        volumeSlider = new JSlider(0, 100, 85);
        volumeSlider.setPreferredSize(new Dimension(100, 20));
        volumeSlider.setOpaque(false);
        volumeSlider.setCursor(new Cursor(Cursor.HAND_CURSOR));

        volumePctLabel = new JLabel("85%");
        volumePctLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        volumePctLabel.setForeground(new Color(160, 165, 180));
        volumePctLabel.setPreferredSize(new Dimension(32, 20));

        muteBtn.addActionListener(e -> {
            if (onMuteToggle != null) {
                boolean isMute = "🔇".equals(muteBtn.getText());
                onMuteToggle.accept(!isMute);
            }
        });

        volumeSlider.addChangeListener(e -> {
            float vol = volumeSlider.getValue() / 100.0f;
            volumePctLabel.setText(volumeSlider.getValue() + "%");
            if (vol == 0) {
                muteBtn.setText("🔇");
            } else {
                muteBtn.setText("🔊");
            }
            if (onVolumeChange != null) {
                onVolumeChange.accept(vol);
            }
        });

        rightPanel.add(muteBtn);
        rightPanel.add(volumeSlider);
        rightPanel.add(volumePctLabel);

        // Assemble Bar
        add(leftPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Wire center button actions
        playPauseBtn.addActionListener(e -> { if (onPlayPause != null) onPlayPause.run(); });
        nextBtn.addActionListener(e -> { if (onNext != null) onNext.run(); });
        prevBtn.addActionListener(e -> { if (onPrevious != null) onPrevious.run(); });
        shuffleBtn.addActionListener(e -> { if (onToggleShuffle != null) onToggleShuffle.run(); });
        repeatBtn.addActionListener(e -> { if (onToggleRepeat != null) onToggleRepeat.run(); });
    }

    private JButton createIconButton(String text, String tooltip, int fontSize) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, fontSize));
        btn.setForeground(new Color(200, 205, 220));
        btn.setBackground(new Color(26, 28, 34));
        btn.setBorder(new EmptyBorder(6, 10, 6, 10));
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createPlayButton() {
        JButton btn = new JButton("▶") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Vibrant Gradient Circle
                GradientPaint gp = new GradientPaint(0, 0, new Color(121, 40, 202), getWidth(), getHeight(), new Color(0, 245, 212));
                g2.setPaint(gp);
                g2.fillOval(0, 0, getWidth(), getHeight());

                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setPreferredSize(new Dimension(42, 42));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Play / Pause");
        return btn;
    }

    public void updateTrack(Track track) {
        if (track == null) {
            titleLabel.setText("No Track");
            artistLabel.setText("Music Player Ready");
            currentTimeLabel.setText("00:00");
            totalTimeLabel.setText("00:00");
            seekSlider.setValue(0);
            currentTrackDurationMs = 0;
            miniArtLabel.setIcon(new ImageIcon(AlbumArtUtil.generatePlaceholderArt("A", "M", 48, 8)));
            return;
        }

        String t = track.getTitle();
        if (t.length() > 22) {
            t = t.substring(0, 20) + "...";
        }
        titleLabel.setText(t);
        titleLabel.setToolTipText(track.getTitle());

        String a = track.getArtist();
        if (a.length() > 24) {
            a = a.substring(0, 22) + "...";
        }
        artistLabel.setText(a);

        currentTrackDurationMs = track.getDurationMs();
        totalTimeLabel.setText(formatTime(currentTrackDurationMs));
        currentTimeLabel.setText("00:00");
        seekSlider.setValue(0);

        BufferedImage art = track.getAlbumArt();
        if (art != null) {
            miniArtLabel.setIcon(new ImageIcon(AlbumArtUtil.createRoundedImage(art, 48, 48, 8)));
        } else {
            miniArtLabel.setIcon(new ImageIcon(AlbumArtUtil.generatePlaceholderArt(track.getTitle(), track.getArtist(), 48, 8)));
        }
    }

    public void updatePlaybackState(PlaybackState state) {
        if (state == PlaybackState.PLAYING) {
            playPauseBtn.setText("⏸");
        } else {
            playPauseBtn.setText("▶");
        }
    }

    public void updateProgress(long currentMs, long totalMs) {
        if (isUserSeeking) return;

        currentTimeLabel.setText(formatTime(currentMs));
        totalTimeLabel.setText(formatTime(totalMs));

        if (totalMs > 0) {
            int sliderVal = (int) ((currentMs * 1000.0) / totalMs);
            seekSlider.setValue(Math.min(1000, Math.max(0, sliderVal)));
        }
    }

    public void updateShuffleState(boolean shuffle) {
        if (shuffle) {
            shuffleBtn.setForeground(new Color(0, 245, 212));
            shuffleBtn.setBackground(new Color(121, 40, 202, 120));
        } else {
            shuffleBtn.setForeground(new Color(200, 205, 220));
            shuffleBtn.setBackground(new Color(26, 28, 34));
        }
    }

    public void updateRepeatMode(RepeatMode mode) {
        switch (mode) {
            case ONE:
                repeatBtn.setText("🔂");
                repeatBtn.setForeground(new Color(0, 245, 212));
                repeatBtn.setToolTipText("Repeat: Current Track");
                break;
            case ALL:
                repeatBtn.setText("🔁");
                repeatBtn.setForeground(new Color(121, 40, 202));
                repeatBtn.setToolTipText("Repeat: All Tracks");
                break;
            case OFF:
            default:
                repeatBtn.setText("🔁");
                repeatBtn.setForeground(new Color(200, 205, 220));
                repeatBtn.setToolTipText("Repeat: Off");
                break;
        }
    }

    public void updateMuteState(boolean muted) {
        muteBtn.setText(muted ? "🔇" : "🔊");
    }

    private String formatTime(long ms) {
        long totalSec = Math.max(0, ms / 1000);
        long mins = totalSec / 60;
        long secs = totalSec % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    // Callback setters
    public void setOnPlayPause(Runnable onPlayPause) { this.onPlayPause = onPlayPause; }
    public void setOnNext(Runnable onNext) { this.onNext = onNext; }
    public void setOnPrevious(Runnable onPrevious) { this.onPrevious = onPrevious; }
    public void setOnToggleShuffle(Runnable onToggleShuffle) { this.onToggleShuffle = onToggleShuffle; }
    public void setOnToggleRepeat(Runnable onToggleRepeat) { this.onToggleRepeat = onToggleRepeat; }
    public void setOnSeek(java.util.function.Consumer<Long> onSeek) { this.onSeek = onSeek; }
    public void setOnVolumeChange(java.util.function.Consumer<Float> onVolumeChange) { this.onVolumeChange = onVolumeChange; }
    public void setOnMuteToggle(java.util.function.Consumer<Boolean> onMuteToggle) { this.onMuteToggle = onMuteToggle; }
}
