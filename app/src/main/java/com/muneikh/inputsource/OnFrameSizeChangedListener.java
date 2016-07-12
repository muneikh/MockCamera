package com.muneikh.inputsource;

import com.muneikh.Size;

public interface OnFrameSizeChangedListener {
    OnFrameSizeChangedListener EMPTY = new OnFrameSizeChangedListener() {
        @Override
        public void onFrameSizeChanged(Size size) {

        }
    };

    void onFrameSizeChanged(Size size);
}
