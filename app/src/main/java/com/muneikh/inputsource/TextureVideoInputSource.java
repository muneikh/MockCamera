package com.muneikh.inputsource;

import android.graphics.SurfaceTexture;

import com.muneikh.gles.Size;

public interface TextureVideoInputSource {

    void checkOrientationChanged();

    Size getFrameSize();

    int getTextureId();

    float[] getTextureTransform();

    boolean isReady();

    void nextFrame();

    void onGlContextCreated();

    void onResume();

    void release();

    void setCameraOpenErrorListener(TextureVideoInputSourceErrorListener textureVideoInputSourceErrorListener);

    void setOnFrameOrientationChangedListener(OnFrameOrientationChangedListener onFrameOrientationChangedListener);

    void setOnFrameSizeChangedListener(OnFrameSizeChangedListener onFrameSizeChangedListener);

    void startPreview();

    void switchCamera();

    int getCameraFacing();

    SurfaceTexture getSurfaceTexture();
}
