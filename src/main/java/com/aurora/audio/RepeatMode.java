package com.aurora.audio;

public enum RepeatMode {
    OFF("Repeat: Off"),
    ALL("Repeat: All"),
    ONE("Repeat: Track");

    private final String label;

    RepeatMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public RepeatMode next() {
        RepeatMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
