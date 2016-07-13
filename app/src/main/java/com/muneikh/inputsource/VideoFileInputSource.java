package com.muneikh.inputsource;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;
import android.view.Surface;

import com.muneikh.gles.Size;
import com.muneikh.gles.FullFrameRect;
import com.muneikh.gles.Texture2dProgram;

import java.io.IOException;
import java.util.Arrays;

public class VideoFileInputSource implements TextureVideoInputSource, MediaPlayer.OnPreparedListener {

    private static final String TAG = "VideoFileInputSource";
    private static final int INVALID = -1;

    private Context context;
    private final String filename;
    private Size frameSize;
    private float[] oldTransform;
    private OnFrameOrientationChangedListener onFrameOrientationChangedListener;
    private OnFrameSizeChangedListener onFrameSizeChangedListener;
    private MediaPlayer player;
    private volatile boolean ready;
    private SurfaceTexture surfaceTexture;
    private int textureId;
    private float[] transform;
    private FullFrameRect fullScreen;

    class VideoOnCompletionListener implements OnCompletionListener {
        public void onCompletion(MediaPlayer mp) {
            VideoFileInputSource.this.player.start();
        }
    }

    public VideoFileInputSource(Context context, String assertFilename) {
        transform = new float[16];
        oldTransform = new float[16];
        onFrameOrientationChangedListener = OnFrameOrientationChangedListener.EMPTY;
        onFrameSizeChangedListener = OnFrameSizeChangedListener.EMPTY;
        ready = false;
        this.context = context;
        this.filename = assertFilename;
    }

    public void onResume() {
    }

    public void onGlContextCreated() {
        fullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        textureId = fullScreen.createTextureObject();
        surfaceTexture = new SurfaceTexture(this.textureId);
        startPreview();
    }

    public void nextFrame() {
        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(this.transform);
        fullScreen.drawFrame(textureId, this.transform);
        checkOrientationChanged();
    }

    public void checkOrientationChanged() {
        if (!Arrays.equals(this.transform, this.oldTransform)) {
            this.onFrameOrientationChangedListener.onFrameOrientationChanged();
            System.arraycopy(this.transform, 0, this.oldTransform, 0, this.transform.length);
        }
    }

    public int getTextureId() {
        return this.textureId;
    }

    public float[] getTextureTransform() {
        return this.transform;
    }

    public void startPreview() {
        try {
            player = new MediaPlayer();
            AssetFileDescriptor afd = this.context.getAssets().openFd(this.filename);
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.setLooping(false);
            player.setOnCompletionListener(new VideoOnCompletionListener());
            player.setOnPreparedListener(this);
            player.prepareAsync();
            frameSize = new Size(this.player.getVideoWidth(), this.player.getVideoHeight());
            player.setSurface(new Surface(this.surfaceTexture));
            Log.d(TAG, "Video starting playback at: " + this.frameSize.width + "x" + this.frameSize.height);
        } catch (IOException e) {
            throw new RuntimeException("Could not open input video!", e);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        player.start();
        ready = true;
    }

    @Override
    public void switchCamera() {
        Log.d(TAG, "Switch Camera");
    }

    @Override
    public int getCameraFacing() {
        return INVALID;
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public void release() {
        this.ready = false;
        if (this.player != null) {
            this.player.stop();
        }
        if (this.surfaceTexture != null) {
            this.surfaceTexture.release();
            this.surfaceTexture = null;
        }
    }

    public void setOnFrameOrientationChangedListener(OnFrameOrientationChangedListener onFrameOrientationChangedListener) {
        this.onFrameOrientationChangedListener = onFrameOrientationChangedListener;
    }

    public void setOnFrameSizeChangedListener(OnFrameSizeChangedListener onFrameSizeChangedListener) {
        this.onFrameSizeChangedListener = onFrameSizeChangedListener;
    }

    public Size getFrameSize() {
        return frameSize;
    }

    public boolean isReady() {
        return ready;
    }

    public void setCameraOpenErrorListener(TextureVideoInputSourceErrorListener cameraOpenErrorListener) {
    }
}
