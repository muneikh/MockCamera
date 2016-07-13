package com.muneikh.inputsource;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

import com.muneikh.gles.Size;
import com.muneikh.ui.widget.GL2SurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VideoRenderer implements Renderer, OnFrameOrientationChangedListener {

    private static final String TAG = "VideoRenderer";
    private TextureVideoInputSource videoInputSource;

    public VideoRenderer(Context context, TextureVideoInputSource videoInputSource) {
        this.videoInputSource = videoInputSource;
    }

    @Override
    public void onFrameOrientationChanged() {
        Log.d(TAG, "onFrameOrientationChanged");
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        this.videoInputSource.onGlContextCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 ignored, int width, int height) {
        Size screenSize = new Size(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (this.videoInputSource.isReady()) {
            this.videoInputSource.nextFrame();
        }
    }

    public void setGLSurfaceView(GL2SurfaceView surfaceView) {
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        this.videoInputSource.setOnFrameOrientationChangedListener(this);
    }
}
