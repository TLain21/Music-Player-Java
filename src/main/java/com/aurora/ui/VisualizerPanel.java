package com.aurora.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class VisualizerPanel extends JPanel {

    private static final int NUM_BARS = 36;
    private static final float DECAY_RATE = 0.08f;
    private static final float PEAK_GRAVITY = 0.04f;

    private final float[] currentValues = new float[NUM_BARS];
    private final float[] targetValues = new float[NUM_BARS];
    private final float[] peakValues = new float[NUM_BARS];
    private final float[] peakSpeed = new float[NUM_BARS];

    private boolean isPlaying = false;
    private double idlePhase = 0;

    public VisualizerPanel() {
        setPreferredSize(new Dimension(300, 75));
        setMinimumSize(new Dimension(200, 50));
        setOpaque(false);

        // Animation timer ~50 FPS
        Timer timer = new Timer(20, e -> updatePhysicsAndRepaint());
        timer.start();
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        if (!playing) {
            for (int i = 0; i < NUM_BARS; i++) {
                targetValues[i] = 0;
            }
        }
    }

    public void processAudioData(byte[] pcmData, int length, int channels, float sampleRate) {
        if (!isPlaying || pcmData == null || length < 4) {
            return;
        }

        int bytesPerSample = 2; // 16-bit
        int frameSize = channels * bytesPerSample;
        int numFrames = length / frameSize;
        if (numFrames < 8) return;

        // Extract mono samples
        float[] monoSamples = new float[numFrames];
        for (int i = 0; i < numFrames; i++) {
            int offset = i * frameSize;
            short s = (short) ((pcmData[offset] & 0xFF) | (pcmData[offset + 1] << 8));
            monoSamples[i] = s / 32768.0f;
        }

        // Divide into logarithmically-spaced frequency bands
        int samplesPerBand = Math.max(2, numFrames / NUM_BARS);
        for (int b = 0; b < NUM_BARS; b++) {
            int start = b * samplesPerBand;
            int end = Math.min(start + samplesPerBand, numFrames);

            double sumSquares = 0;
            for (int i = start; i < end; i++) {
                sumSquares += monoSamples[i] * monoSamples[i];
            }

            double rms = Math.sqrt(sumSquares / Math.max(1, end - start));

            // Boost higher frequency bands for balanced visual presentation
            double boost = 1.0 + (b / (double) NUM_BARS) * 2.5;
            float val = (float) Math.min(1.0, Math.max(0.0, rms * 4.2 * boost));

            targetValues[b] = val;
        }
    }

    private void updatePhysicsAndRepaint() {
        idlePhase += 0.05;

        for (int i = 0; i < NUM_BARS; i++) {
            if (isPlaying) {
                // Smooth rise and decay
                if (targetValues[i] > currentValues[i]) {
                    currentValues[i] = targetValues[i];
                } else {
                    currentValues[i] = Math.max(0f, currentValues[i] - DECAY_RATE);
                }

                // Peaks with gravity fall
                if (currentValues[i] >= peakValues[i]) {
                    peakValues[i] = currentValues[i];
                    peakSpeed[i] = 0;
                } else {
                    peakSpeed[i] += PEAK_GRAVITY;
                    peakValues[i] = Math.max(0f, peakValues[i] - peakSpeed[i]);
                }
            } else {
                // Gentle idle wave animation
                double wave = (Math.sin(idlePhase + (i * 0.25)) + 1.0) * 0.08 + 0.02;
                currentValues[i] = (float) wave;
                peakValues[i] = (float) (wave + 0.03);
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background container rounded rect
        g2.setColor(new Color(22, 24, 29, 180));
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
        g2.setColor(new Color(255, 255, 255, 15));
        g2.draw(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));

        float padding = 8f;
        float barAreaW = w - (padding * 2);
        float barAreaH = h - (padding * 2);

        float spacing = 2.5f;
        float totalSpacing = spacing * (NUM_BARS - 1);
        float barW = Math.max(2.0f, (barAreaW - totalSpacing) / NUM_BARS);

        // Gradient for bars (Neon Violet -> Cyan)
        Color colBottom = new Color(121, 40, 202);
        Color colTop = new Color(0, 245, 212);
        Color peakColor = new Color(255, 255, 255, 220);

        for (int i = 0; i < NUM_BARS; i++) {
            float x = padding + i * (barW + spacing);
            float barHeight = Math.max(2.0f, currentValues[i] * barAreaH);
            float y = padding + barAreaH - barHeight;

            // Bar gradient
            GradientPaint gp = new GradientPaint(x, y + barHeight, colBottom, x, y, colTop);
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(x, y, barW, barHeight, barW, barW));

            // Peak cap dot
            float peakY = padding + barAreaH - (peakValues[i] * barAreaH);
            if (peakY < padding) peakY = padding;
            g2.setColor(peakColor);
            g2.fill(new RoundRectangle2D.Float(x, peakY, barW, 2f, 1f, 1f));
        }

        g2.dispose();
    }
}
