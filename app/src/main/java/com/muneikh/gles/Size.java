package com.muneikh.gles;

public final class Size {
    public static final int FRAME_WIDTH = 1280;
    public static final int FRAME_HEIGHT = 720;

    public final int height;
    public final int width;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Size flipAxes() {
        return new Size(height, width);
    }

    public String toString() {
        return "Size { " + width + "x" + height + " }";
    }
}
