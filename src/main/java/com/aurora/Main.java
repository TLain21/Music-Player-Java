package com.aurora;

import com.aurora.ui.MainWindow;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        // High-DPI support on modern Windows displays
        System.setProperty("sun.java2d.uiScale.enabled", "true");

        // Set FlatLaf styling properties before initializing
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("ProgressBar.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.width", 9);
        UIManager.put("Slider.trackWidth", 4);
        UIManager.put("Slider.thumbSize", new Dimension(14, 14));
        UIManager.put("TabbedPane.showTabSeparators", true);

        // Apply dark theme
        try {
            FlatDarkLaf.setup();
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatDarkLaf theme, falling back to system L&F.");
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow window = new MainWindow();
                window.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Fatal error launching application:\n" + e.getMessage(), "Launch Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
