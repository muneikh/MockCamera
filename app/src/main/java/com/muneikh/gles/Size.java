package com.muneikh.gles;

public final class Size {
    public static final int FRAME_WIDTH = 1280;
    public static final int FRAME_HEIGHT = 720;

    // Record at 1280x720, regardless of the window dimensions.  The encoder may
    // explode if given "strange" dimensions, e.g. a width that is not a multiple
    // of 16.  We can box it as needed to preserve dimensions.
    public static final Size OPTIMAL_FOR_RECORDING = new Size(FRAME_WIDTH, FRAME_HEIGHT);

    public static final float OPTIMAL_FACTOR_FOR_DETECTION = 0.25f;

    // h264 requires width and height to be multiple of 16
    private static final int MASK = 0xFFF0;

    public final int height;
    public final int width;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Size flipAxes() {
        return new Size(height, width);
    }

    public Size optimizeForProcessing() {
        // h264 requires width and height to be multiple of 16
        int newWidth = MASK & (int) (width * OPTIMAL_FACTOR_FOR_DETECTION);
        int newHeight = MASK & (int) (height * OPTIMAL_FACTOR_FOR_DETECTION);
        return new Size(newWidth, newHeight);
    }

    public String toString() {
        return "Size { " + width + "x" + height + " }";
    }
}
