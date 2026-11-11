package com.aurora.ui;

import com.aurora.model.Track;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public class NowPlayingPanel extends JPanel {

    private final JLabel artLabel;
    private final JLabel titleLabel;
    private final JLabel artistLabel;
    private final JLabel albumLabel;
    private final JPanel badgePanel;
    private final JLabel formatBadge;
    private final JLabel qualityBadge;
    private final VisualizerPanel visualizerPanel;

    private static final int ART_SIZE = 240;

    public NowPlayingPanel(VisualizerPanel visualizer) {
        this.visualizerPanel = visualizer;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(new Color(24, 26, 32));
        setPreferredSize(new Dimension(300, 500));
        setMinimumSize(new Dimension(280, 400));

        // 1. Album Artwork Container
        artLabel = new JLabel();
        artLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        artLabel.setPreferredSize(new Dimension(ART_SIZE, ART_SIZE));
        artLabel.setMaximumSize(new Dimension(ART_SIZE, ART_SIZE));
        artLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Initial placeholder artwork
        BufferedImage defaultArt = AlbumArtUtil.generatePlaceholderArt("Aurora", "Music", ART_SIZE, 16);
        artLabel.setIcon(new ImageIcon(defaultArt));

        // 2. Track Title
        titleLabel = new JLabel("No Track Selected");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 3. Artist Label
        artistLabel = new JLabel("Choose a song to start listening");
        artistLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        artistLabel.setForeground(new Color(170, 175, 190));
        artistLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        artistLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 4. Album Label
        albumLabel = new JLabel("");
        albumLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        albumLabel.setForeground(new Color(130, 135, 150));
        albumLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        albumLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 5. Badges Panel
        badgePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        badgePanel.setOpaque(false);
        badgePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        formatBadge = createBadge("FLAC / MP3 / WAV", new Color(121, 40, 202));
        qualityBadge = createBadge("Hi-Fi Audio", new Color(0, 180, 160));

        badgePanel.add(formatBadge);
        badgePanel.add(qualityBadge);

        // Visualizer container
        visualizerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components with spacing
        add(artLabel);
        add(Box.createRigidArea(new Dimension(0, 16)));
        add(titleLabel);
        add(Box.createRigidArea(new Dimension(0, 4)));
        add(artistLabel);
        add(Box.createRigidArea(new Dimension(0, 2)));
        add(albumLabel);
        add(Box.createRigidArea(new Dimension(0, 12)));
        add(badgePanel);
        add(Box.createRigidArea(new Dimension(0, 16)));
        add(visualizerPanel);
        add(Box.createVerticalGlue());
    }

    private JLabel createBadge(String text, Color bg) {
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(Color.WHITE);
        label.setBorder(new EmptyBorder(4, 10, 4, 10));
        label.setOpaque(false);
        return label;
    }

    public void updateTrack(Track track) {
        if (track == null) {
            titleLabel.setText("No Track Selected");
            artistLabel.setText("Choose a song to start listening");
            albumLabel.setText("");
            formatBadge.setText("READY");
            qualityBadge.setText("Hi-Fi Audio");
            BufferedImage defaultArt = AlbumArtUtil.generatePlaceholderArt("Aurora", "Music", ART_SIZE, 16);
            artLabel.setIcon(new ImageIcon(defaultArt));
            return;
        }

        // Title
        String t = track.getTitle();
        if (t.length() > 28) {
            t = t.substring(0, 26) + "...";
        }
        titleLabel.setText(t);
        titleLabel.setToolTipText(track.getTitle());

        // Artist & Album
        artistLabel.setText(track.getArtist());
        artistLabel.setToolTipText(track.getArtist());
        albumLabel.setText(track.getAlbum());

        // Badges
        formatBadge.setText(track.getFormat());
        String qualityText = track.getFormattedBitrate();
        if ("VBR/Lossless".equals(qualityText) || "FLAC".equalsIgnoreCase(track.getFormat()) || "WAV".equalsIgnoreCase(track.getFormat())) {
            qualityText = "Lossless • " + track.getFormattedSampleRate();
        }
        qualityBadge.setText(qualityText);

        // Artwork
        BufferedImage art = track.getAlbumArt();
        if (art != null) {
            BufferedImage rounded = AlbumArtUtil.createRoundedImage(art, ART_SIZE, ART_SIZE, 16);
            artLabel.setIcon(new ImageIcon(rounded));
        } else {
            BufferedImage gen = AlbumArtUtil.generatePlaceholderArt(track.getTitle(), track.getArtist(), ART_SIZE, 16);
            artLabel.setIcon(new ImageIcon(gen));
        }

        revalidate();
        repaint();
    }
}
