package com.aurora.ui;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class AlbumArtUtil {

    private static final Color[][] GRADIENTS = {
        { new Color(138, 43, 226), new Color(30, 144, 255) },   // Purple -> Blue
        { new Color(255, 64, 129), new Color(255, 145, 0) },   // Pink -> Orange
        { new Color(0, 201, 167), new Color(132, 94, 194) },   // Teal -> Violet
        { new Color(24, 90, 157), new Color(67, 206, 162) },   // Deep Blue -> Mint
        { new Color(195, 20, 50), new Color(36, 11, 54) },     // Crimson -> Dark Plum
        { new Color(97, 67, 133), new Color(81, 99, 149) }     // Muted Lavender
    };

    public static BufferedImage createRoundedImage(BufferedImage src, int width, int height, int cornerRadius) {
        if (src == null) {
            return null;
        }

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        RoundRectangle2D.Float clipShape = new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius);
        g2.setClip(clipShape);

        // Calculate aspect fill
        double srcAspect = (double) src.getWidth() / src.getHeight();
        double dstAspect = (double) width / height;

        int drawW = width;
        int drawH = height;
        int drawX = 0;
        int drawY = 0;

        if (srcAspect > dstAspect) {
            drawW = (int) (height * srcAspect);
            drawX = (width - drawW) / 2;
        } else {
            drawH = (int) (width / srcAspect);
            drawY = (height - drawH) / 2;
        }

        g2.drawImage(src, drawX, drawY, drawW, drawH, null);

        // Subtle inner border
        g2.setClip(null);
        g2.setColor(new Color(255, 255, 255, 25));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(clipShape);

        g2.dispose();
        return output;
    }

    public static BufferedImage generatePlaceholderArt(String title, String artist, int size, int cornerRadius) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Pick gradient based on title hash
        int hash = Math.abs((title + artist).hashCode());
        Color[] gradColors = GRADIENTS[hash % GRADIENTS.length];

        RoundRectangle2D.Float clipShape = new RoundRectangle2D.Float(0, 0, size, size, cornerRadius, cornerRadius);
        g2.setClip(clipShape);

        // Gradient background
        GradientPaint paint = new GradientPaint(0, 0, gradColors[0], size, size, gradColors[1]);
        g2.setPaint(paint);
        g2.fillRect(0, 0, size, size);

        // Subtle vinyl ring effects
        g2.setColor(new Color(255, 255, 255, 18));
        g2.setStroke(new BasicStroke(1.2f));
        int center = size / 2;
        for (int r = (int)(size * 0.22); r <= (int)(size * 0.44); r += 14) {
            g2.draw(new Ellipse2D.Float(center - r, center - r, r * 2, r * 2));
        }

        // Center vinyl core
        int coreR = (int)(size * 0.16);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fill(new Ellipse2D.Float(center - coreR, center - coreR, coreR * 2, coreR * 2));
        g2.setColor(new Color(255, 255, 255, 40));
        g2.draw(new Ellipse2D.Float(center - coreR, center - coreR, coreR * 2, coreR * 2));

        // Center music note icon / initials
        String initial = (title != null && !title.isEmpty()) ? title.substring(0, 1).toUpperCase() : "♫";
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, (int) (size * 0.22)));
        FontMetrics fm = g2.getFontMetrics();
        int textX = center - (fm.stringWidth(initial) / 2);
        int textY = center + (fm.getAscent() / 3);
        g2.drawString(initial, textX, textY);

        // Border
        g2.setClip(null);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(clipShape);

        g2.dispose();
        return image;
    }
}
