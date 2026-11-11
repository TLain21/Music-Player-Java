package com.aurora.ui;

import com.aurora.audio.AudioPlayer;
import com.aurora.audio.AudioPlayerListener;
import com.aurora.audio.PlaybackState;
import com.aurora.model.Playlist;
import com.aurora.model.Track;
import com.aurora.model.TrackMetadataReader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame implements AudioPlayerListener {

    private final AudioPlayer player;
    private final Playlist playlist;

    private NowPlayingPanel nowPlayingPanel;
    private PlaylistPanel playlistPanel;
    private PlayerControlBar controlBar;
    private VisualizerPanel visualizerPanel;

    private static final String[] SUPPORTED_EXTS = { "mp3", "wav", "flac", "ogg", "aiff", "aif", "au" };

    public MainWindow() {
        this.player = new AudioPlayer();
        this.playlist = new Playlist();

        initWindow();
        initUI();
        wireEvents();

        player.addListener(this);

        // Load sample tracks if present
        loadInitialSample();
    }

    private void initWindow() {
        setTitle("Aurora Music Player — Hi-Res Multi-Format Player (WAV, MP3, FLAC, OGG)");
        setSize(1080, 720);
        setMinimumSize(new Dimension(850, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(new Color(18, 20, 24));

        // 1. Top Header Bar
        JPanel topHeader = createHeaderBar();
        rootPanel.add(topHeader, BorderLayout.NORTH);

        // 2. Center Split (Left: Now Playing & Visualizer | Right: Playlist)
        visualizerPanel = new VisualizerPanel();
        nowPlayingPanel = new NowPlayingPanel(visualizerPanel);
        playlistPanel = new PlaylistPanel(playlist, this::playTrack, this::importFilesAsync);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, nowPlayingPanel, playlistPanel);
        splitPane.setDividerLocation(310);
        splitPane.setDividerSize(4);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setContinuousLayout(true);
        rootPanel.add(splitPane, BorderLayout.CENTER);

        // 3. Bottom Control Bar
        controlBar = new PlayerControlBar();
        rootPanel.add(controlBar, BorderLayout.SOUTH);

        setContentPane(rootPanel);
        setupKeyNavigation();
    }

    private JPanel createHeaderBar() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(22, 24, 29));
        header.setBorder(new EmptyBorder(10, 16, 10, 16));

        // Brand Logo
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandPanel.setOpaque(false);

        JLabel logoIcon = new JLabel("✨");
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        JLabel brandName = new JLabel("AURORA");
        brandName.setFont(new Font("Segoe UI", Font.BOLD, 17));
        brandName.setForeground(new Color(0, 245, 212));

        JLabel subName = new JLabel("AUDIO PLAYER");
        subName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        subName.setForeground(new Color(140, 145, 160));

        brandPanel.add(logoIcon);
        brandPanel.add(brandName);
        brandPanel.add(subName);

        // Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);

        JButton addFilesBtn = createHeaderButton("➕ Add Files", "Open audio files (.wav, .mp3, .flac, .ogg)");
        JButton addFolderBtn = createHeaderButton("📁 Add Folder", "Scan a folder recursively for music");
        JButton clearBtn = createHeaderButton("🧹 Clear", "Clear current playlist");
        JButton aboutBtn = createHeaderButton("ℹ️ Formats", "View supported audio formats");

        addFilesBtn.addActionListener(e -> chooseFiles());
        addFolderBtn.addActionListener(e -> chooseFolder());
        clearBtn.addActionListener(e -> {
            player.stop();
            playlist.clear();
            playlistPanel.updatePlaylistData();
            nowPlayingPanel.updateTrack(null);
            controlBar.updateTrack(null);
        });
        aboutBtn.addActionListener(e -> showFormatsDialog());

        actionPanel.add(addFilesBtn);
        actionPanel.add(addFolderBtn);
        actionPanel.add(clearBtn);
        actionPanel.add(aboutBtn);

        header.add(brandPanel, BorderLayout.WEST);
        header.add(actionPanel, BorderLayout.EAST);

        return header;
    }

    private JButton createHeaderButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(new Color(220, 225, 235));
        btn.setBackground(new Color(32, 35, 43));
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(tooltip);
        return btn;
    }

    private void wireEvents() {
        controlBar.setOnPlayPause(() -> {
            if (player.getState() == PlaybackState.PLAYING) {
                player.pause();
            } else if (player.getState() == PlaybackState.PAUSED) {
                player.resume();
            } else {
                Track current = playlist.getCurrentTrack();
                if (current == null && playlist.size() > 0) {
                    current = playlist.setIndex(0);
                }
                if (current != null) {
                    playTrack(current);
                }
            }
        });

        controlBar.setOnNext(() -> {
            Track next = playlist.next();
            if (next != null) {
                playTrack(next);
            }
        });

        controlBar.setOnPrevious(() -> {
            Track prev = playlist.previous();
            if (prev != null) {
                playTrack(prev);
            }
        });

        controlBar.setOnToggleShuffle(() -> {
            boolean nextShuffle = !playlist.isShuffle();
            playlist.setShuffle(nextShuffle);
            controlBar.updateShuffleState(nextShuffle);
        });

        controlBar.setOnToggleRepeat(() -> {
            playlist.setRepeatMode(playlist.getRepeatMode().next());
            controlBar.updateRepeatMode(playlist.getRepeatMode());
        });

        controlBar.setOnSeek(player::seekTo);
        controlBar.setOnVolumeChange(player::setVolume);
        controlBar.setOnMuteToggle(player::setMute);
    }

    private void setupKeyNavigation() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                // If focus is in a textfield (search box), don't hijack spacebar/arrows
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof JTextField) {
                    return false;
                }

                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (player.getState() == PlaybackState.PLAYING) player.pause();
                    else if (player.getState() == PlaybackState.PAUSED) player.resume();
                    else {
                        Track t = playlist.getCurrentTrack();
                        if (t != null) playTrack(t);
                    }
                    return true;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && e.isControlDown()) {
                    Track next = playlist.next();
                    if (next != null) playTrack(next);
                    return true;
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT && e.isControlDown()) {
                    Track prev = playlist.previous();
                    if (prev != null) playTrack(prev);
                    return true;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    player.seekTo(player.getCurrentPositionMs() + 5000);
                    return true;
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    player.seekTo(player.getCurrentPositionMs() - 5000);
                    return true;
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    player.setVolume(player.getVolume() + 0.05f);
                    return true;
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    player.setVolume(player.getVolume() - 0.05f);
                    return true;
                }
            }
            return false;
        });
    }

    private void chooseFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Select Audio Files");
        chooser.setFileFilter(new FileNameExtensionFilter("All Supported Audio (*.mp3, *.wav, *.flac, *.ogg, *.aiff, *.au)", SUPPORTED_EXTS));

        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File[] selected = chooser.getSelectedFiles();
            if (selected != null && selected.length > 0) {
                importFilesAsync(List.of(selected));
            }
        }
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Music Folder to Scan");

        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            if (folder != null && folder.isDirectory()) {
                importFilesAsync(List.of(folder));
            }
        }
    }

    private void importFilesAsync(List<File> fileList) {
        SwingWorker<List<Track>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Track> doInBackground() {
                List<File> audioFiles = new ArrayList<>();
                for (File f : fileList) {
                    collectAudioFiles(f, audioFiles);
                }

                List<Track> loaded = new ArrayList<>();
                for (File af : audioFiles) {
                    Track t = TrackMetadataReader.read(af);
                    if (t != null) {
                        loaded.add(t);
                    }
                }
                return loaded;
            }

            @Override
            protected void done() {
                try {
                    List<Track> loaded = get();
                    if (!loaded.isEmpty()) {
                        boolean wasEmpty = playlist.isEmpty();
                        playlist.addTracks(loaded);
                        playlistPanel.updatePlaylistData();

                        if (wasEmpty) {
                            Track first = playlist.setIndex(0);
                            nowPlayingPanel.updateTrack(first);
                            controlBar.updateTrack(first);
                            playlistPanel.refreshTableSelection();
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainWindow.this, "Error importing audio: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void collectAudioFiles(File file, List<File> result) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File c : children) {
                    collectAudioFiles(c, result);
                }
            }
        } else if (file.isFile()) {
            String name = file.getName().toLowerCase();
            for (String ext : SUPPORTED_EXTS) {
                if (name.endsWith("." + ext)) {
                    result.add(file);
                    break;
                }
            }
        }
    }

    private void playTrack(Track track) {
        if (track == null) return;
        nowPlayingPanel.updateTrack(track);
        controlBar.updateTrack(track);
        playlistPanel.refreshTableSelection();
        player.play(track);
    }

    private void loadInitialSample() {
        File sampleDir = new File("samples");
        if (sampleDir.exists() && sampleDir.isDirectory()) {
            File[] files = sampleDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));
            if (files != null && files.length > 0) {
                importFilesAsync(List.of(files));
            }
        }
    }

    private void showFormatsDialog() {
        String msg = "<html><body style='width: 320px; font-family: Segoe UI, sans-serif;'>" +
            "<h3 style='color: #00F5D4; margin-bottom: 8px;'>Supported Audio Formats:</h3>" +
            "<ul>" +
            "<li><b>WAV</b> (.wav) — Uncompressed PCM Lossless (16/24/32-bit)</li>" +
            "<li><b>MP3</b> (.mp3) — MPEG-1/2 Audio Layer III (CBR & VBR up to 320kbps)</li>" +
            "<li><b>FLAC</b> (.flac) — Free Lossless Audio Codec (Studio Master Quality)</li>" +
            "<li><b>OGG</b> (.ogg) — Ogg Vorbis Audio Stream</li>" +
            "<li><b>AIFF / AU</b> (.aif, .aiff, .au) — Apple / Sun Audio</li>" +
            "</ul>" +
            "<h4 style='color: #8A2BE2; margin-top: 10px;'>Features:</h4>" +
            "• Real-time 36-Band Spectrum Audio Visualizer<br>" +
            "• Sub-millisecond Seek & Volume controls<br>" +
            "• Embedded ID3v2 & FLAC Vorbis Album Art display<br>" +
            "• Drag & Drop files and entire folders<br>" +
            "• Instant live playlist search filter<br>" +
            "• Keyboard Shortcuts: Space (Play/Pause), Left/Right (Seek 5s), Up/Down (Volume)" +
            "</body></html>";

        JOptionPane.showMessageDialog(this, new JLabel(msg), "Audio Format Support & Features", JOptionPane.INFORMATION_MESSAGE);
    }

    // AudioPlayerListener callbacks
    @Override
    public void onStateChanged(PlaybackState state) {
        SwingUtilities.invokeLater(() -> {
            controlBar.updatePlaybackState(state);
            visualizerPanel.setPlaying(state == PlaybackState.PLAYING);
        });
    }

    @Override
    public void onProgress(long currentMs, long totalMs) {
        SwingUtilities.invokeLater(() -> controlBar.updateProgress(currentMs, totalMs));
    }

    @Override
    public void onTrackFinished() {
        SwingUtilities.invokeLater(() -> {
            Track next = playlist.next();
            if (next != null) {
                playTrack(next);
            } else {
                player.stop();
            }
        });
    }

    @Override
    public void onAudioData(byte[] pcmData, int length, int channels, float sampleRate) {
        visualizerPanel.processAudioData(pcmData, length, channels, sampleRate);
    }

    @Override
    public void onError(String message, Throwable t) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, "Playback Error", JOptionPane.ERROR_MESSAGE));
    }
}
