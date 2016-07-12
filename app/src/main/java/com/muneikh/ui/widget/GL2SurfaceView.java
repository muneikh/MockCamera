package com.muneikh.ui.widget;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class GL2SurfaceView extends GLSurfaceView {
    public static final int OPENGL_VERSION = 2;

    public GL2SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(OPENGL_VERSION);
    }

    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }
}