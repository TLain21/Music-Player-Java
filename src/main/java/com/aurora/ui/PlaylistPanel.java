package com.aurora.ui;

import com.aurora.model.Playlist;
import com.aurora.model.Track;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class PlaylistPanel extends JPanel {

    private final Playlist playlist;
    private final Consumer<Track> onTrackSelected;
    private final Consumer<List<File>> onFilesDropped;

    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;
    private JLabel statusLabel;

    private static final String[] COLUMNS = { "#", "Title", "Artist", "Album", "Duration", "Format" };

    public PlaylistPanel(Playlist playlist, Consumer<Track> onTrackSelected, Consumer<List<File>> onFilesDropped) {
        this.playlist = playlist;
        this.onTrackSelected = onTrackSelected;
        this.onFilesDropped = onFilesDropped;

        setLayout(new BorderLayout());
        setBackground(new Color(18, 20, 24));
        setBorder(new EmptyBorder(12, 12, 8, 12));

        initTopBar();
        initTable();
        initBottomStatusBar();
        setupDragAndDrop();
    }

    private void initTopBar() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setOpaque(false);
        topPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel title = new JLabel("Playlist & Queue");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);

        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(240, 30));
        searchField.putClientProperty("JTextField.placeholderText", "🔍 Filter tracks...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filter(); }
        });

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
    }

    private void filter() {
        String text = searchField.getText();
        if (text == null || text.trim().isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 1, 2, 3));
        }
    }

    private void initTable() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);

        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setBackground(new Color(22, 24, 29));
        table.setForeground(new Color(220, 225, 235));
        table.setSelectionBackground(new Color(121, 40, 202, 90));
        table.setSelectionForeground(Color.WHITE);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(26, 28, 34));
        header.setForeground(new Color(160, 165, 180));
        header.setPreferredSize(new Dimension(header.getWidth(), 30));
        header.setReorderingAllowed(false);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(45);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(160);
        table.getColumnModel().getColumn(4).setPreferredWidth(75);
        table.getColumnModel().getColumn(4).setMaxWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(65);
        table.getColumnModel().getColumn(5).setMaxWidth(80);

        // Custom Renderer
        PlaylistTableCellRenderer renderer = new PlaylistTableCellRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Double-click to play
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    playSelectedRow();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(22, 24, 29));

        add(scrollPane, BorderLayout.CENTER);
    }

    private void initBottomStatusBar() {
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        bottomBar.setBorder(new EmptyBorder(8, 4, 4, 4));

        statusLabel = new JLabel("0 tracks • 00:00");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(130, 135, 150));

        bottomBar.add(statusLabel, BorderLayout.WEST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private void playSelectedRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow != -1) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            Track track = playlist.setIndex(modelRow);
            if (track != null && onTrackSelected != null) {
                onTrackSelected.accept(track);
            }
            refreshTableSelection();
        }
    }

    private void showContextMenu(MouseEvent e) {
        int r = table.rowAtPoint(e.getPoint());
        if (r >= 0 && r < table.getRowCount()) {
            table.setRowSelectionInterval(r, r);
        } else {
            return;
        }

        int viewRow = table.getSelectedRow();
        if (viewRow == -1) return;
        int modelRow = table.convertRowIndexToModel(viewRow);
        Track track = playlist.getTrack(modelRow);
        if (track == null) return;

        JPopupMenu menu = new JPopupMenu();

        JMenuItem playItem = new JMenuItem("▶  Play Now");
        playItem.addActionListener(ev -> playSelectedRow());
        menu.add(playItem);

        menu.addSeparator();

        JMenuItem showInFolder = new JMenuItem("📂  Open File Location");
        showInFolder.addActionListener(ev -> {
            try {
                if (track.getFile() != null && track.getFile().exists()) {
                    Desktop.getDesktop().open(track.getFile().getParentFile());
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not open folder: " + ex.getMessage());
            }
        });
        menu.add(showInFolder);

        JMenuItem removeItem = new JMenuItem("🗑  Remove from Playlist");
        removeItem.addActionListener(ev -> {
            playlist.removeTrack(modelRow);
            updatePlaylistData();
        });
        menu.add(removeItem);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void setupDragAndDrop() {
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (droppedFiles != null && !droppedFiles.isEmpty() && onFilesDropped != null) {
                        onFilesDropped.accept(droppedFiles);
                    }
                    dtde.dropComplete(true);
                } catch (Exception ex) {
                    dtde.dropComplete(false);
                }
            }
        });
    }

    public void updatePlaylistData() {
        tableModel.setRowCount(0);
        List<Track> tracks = playlist.getTracks();
        long totalMs = 0;

        for (int i = 0; i < tracks.size(); i++) {
            Track t = tracks.get(i);
            totalMs += t.getDurationMs();
            tableModel.addRow(new Object[]{
                String.valueOf(i + 1),
                t.getTitle(),
                t.getArtist(),
                t.getAlbum(),
                t.getFormattedDuration(),
                t.getFormat()
            });
        }

        // Update status label
        long totalSec = totalMs / 1000;
        long hours = totalSec / 3600;
        long mins = (totalSec % 3600) / 60;
        long secs = totalSec % 60;
        String durationStr = (hours > 0)
            ? String.format("%d hr %d min", hours, mins)
            : String.format("%d min %02d sec", mins, secs);

        statusLabel.setText(String.format("%d %s • %s", tracks.size(), (tracks.size() == 1 ? "track" : "tracks"), durationStr));
        refreshTableSelection();
    }

    public void refreshTableSelection() {
        int currentIdx = playlist.getCurrentIndex();
        if (currentIdx >= 0 && currentIdx < tableModel.getRowCount()) {
            int viewIdx = table.convertRowIndexToView(currentIdx);
            if (viewIdx != -1) {
                table.setRowSelectionInterval(viewIdx, viewIdx);
                table.scrollRectToVisible(table.getCellRect(viewIdx, 0, true));
            }
        }
        table.repaint();
    }

    private class PlaylistTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            int modelRow = table.convertRowIndexToModel(row);
            boolean isCurrentTrack = (modelRow == playlist.getCurrentIndex());

            if (isSelected) {
                c.setBackground(new Color(121, 40, 202, 120));
                c.setForeground(Color.WHITE);
            } else if (isCurrentTrack) {
                c.setBackground(new Color(0, 245, 212, 35));
                c.setForeground(new Color(0, 245, 212));
                if (column == 0) {
                    setText("▶ " + value);
                }
            } else {
                c.setBackground(row % 2 == 0 ? new Color(22, 24, 29) : new Color(26, 28, 34));
                c.setForeground(new Color(210, 215, 225));
            }

            if (column == 0 || column == 4 || column == 5) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }

            setBorder(new EmptyBorder(0, 8, 0, 8));
            return c;
        }
    }
}
