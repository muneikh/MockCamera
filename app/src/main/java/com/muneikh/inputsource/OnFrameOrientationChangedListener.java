package com.muneikh.inputsource;

public interface OnFrameOrientationChangedListener {
    OnFrameOrientationChangedListener EMPTY = new OnFrameOrientationChangedListener() {
        @Override
        public void onFrameOrientationChanged() {

        }
    };

    void onFrameOrientationChanged();
}
