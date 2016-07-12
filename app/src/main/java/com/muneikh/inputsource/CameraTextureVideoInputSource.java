package com.muneikh.inputsource;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.muneikh.Size;
import com.muneikh.gles.FullFrameRect;
import com.muneikh.gles.Texture2dProgram;
import com.muneikh.inputsource.exception.UnableToOpenCameraException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraTextureVideoInputSource implements TextureVideoInputSource {

    private static final String TAG = "CameraTextureVideoInput";
    
    private static final int DESIRED_PREVIEW_FPS = 15;
    private static final int FRAME_PIXELS_COUNT = Size.FRAME_HEIGHT * Size.FRAME_WIDTH;

    private volatile boolean isFrontCamera;
    private Activity activity;
    private CameraHandler cameraHandler;
    private TextureVideoInputSourceErrorListener cameraOpenErrorListener;
    private Camera.Parameters cameraParameters;
    private Size cameraPreviewSize;
    private float[] oldTransform;
    protected OnFrameOrientationChangedListener onFrameOrientationChangedListener;
    protected OnFrameSizeChangedListener onFrameSizeChangedListener;
    private volatile boolean ready;
    private volatile SurfaceTexture surfaceTexture;
    private int textureId;
    private float[] transform;
    private FullFrameRect fullScreen;

    private class CameraHandler extends Handler {
        private static final int SWITCH_CAMERA_MESSAGE = 1;
        private static final int OPEN_CAMERA_AND_START_PREVIEW_MESSAGE = 2;
        private static final int RELEASE_CAMERA_MESSAGE = 3;
        private static final int RELEASE_SURFACE_TEXTURE_MESSAGE = 4;
        public static final int STOP_CAMERA_THREAD_MESSAGE = 5;

        private volatile Camera camera;

        class SizeComparator implements Comparator<Camera.Size> {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                return CameraTextureVideoInputSource.compareInts(Math.abs((lhs.width * lhs.height) - FRAME_PIXELS_COUNT), Math.abs((rhs.width * rhs.height) - FRAME_PIXELS_COUNT));
            }
        }

        public CameraHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SWITCH_CAMERA_MESSAGE /*1*/:
                    if (ready) {
                        handleSwitchCamera();
                    }
                    break;
                case OPEN_CAMERA_AND_START_PREVIEW_MESSAGE /*2*/:
                    handleOpenCameraAndStartPreview();
                    break;
                case RELEASE_CAMERA_MESSAGE /*3*/:
                    removeMessages(OPEN_CAMERA_AND_START_PREVIEW_MESSAGE);
                    removeMessages(SWITCH_CAMERA_MESSAGE);
                    handleReleaseCamera();
                    break;
                case RELEASE_SURFACE_TEXTURE_MESSAGE /*4*/:
                    removeMessages(OPEN_CAMERA_AND_START_PREVIEW_MESSAGE);
                    removeMessages(SWITCH_CAMERA_MESSAGE);
                    handleReleaseSurfaceTexture();
                    break;
                case STOP_CAMERA_THREAD_MESSAGE /*5*/:
                    getLooper().quitSafely();
                    break;
                default:
            }
        }

        private void handleUnableToOpenCameraException(Exception e) {
            if (cameraOpenErrorListener != null) {
                cameraOpenErrorListener.onError(e, !doesUserHaveCameraPermission());
            }
        }

        public void switchCamera() {
            removeMessages(SWITCH_CAMERA_MESSAGE);
            sendEmptyMessage(SWITCH_CAMERA_MESSAGE);
        }

        public void openCameraAndStartPreview() {
            sendEmptyMessage(OPEN_CAMERA_AND_START_PREVIEW_MESSAGE);
        }

        public void releaseCamera() {
            sendEmptyMessage(RELEASE_CAMERA_MESSAGE);
        }

        public void releaseSurfaceTexture() {
            sendEmptyMessage(RELEASE_SURFACE_TEXTURE_MESSAGE);
        }

        public void stopCameraThread() {
            sendEmptyMessage(STOP_CAMERA_THREAD_MESSAGE);
        }

        private int getCameraFacing(boolean isFront) {
            return isFront ? 1 : 0;
        }

        @CameraThread
        private void handleOpenCamera() throws UnableToOpenCameraException {
            handleOpenCamera(getCameraFacing(isFrontCamera));
        }

        @CameraThread
        private void handleSwitchCamera() {
            boolean cameraFacing = true;
            try {
                handleReleaseCamera();
                handleOpenCamera(getCameraFacing(!isFrontCamera));
                CameraTextureVideoInputSource cameraTextureVideoInputSource = CameraTextureVideoInputSource.this;
                if (isFrontCamera) {
                    cameraFacing = false;
                }
                cameraTextureVideoInputSource.isFrontCamera = cameraFacing;
                handleStartPreview();
            } catch (UnableToOpenCameraException e) {
                handleUnableToOpenCameraException(e);
            }
        }

        @CameraThread
        private void handleOpenCamera(int cameraFacing) throws UnableToOpenCameraException {
            if (camera != null) {
                throw new UnableToOpenCameraException("Camera already initialized.");
            }
            try {
                camera = Camera.open(getCameraIndexByFacing(cameraFacing));
                Log.d(TAG, "Camera opened.");

                cameraParameters = camera.getParameters();
                Size size = choosePreviewSize(cameraParameters);

                Log.w(TAG, "Camera using size: " + size);
                setCameraPreviewSize(size);
                cameraParameters.setRecordingHint(true);
                int[] fpsRange = choosePreviewFpsRange(cameraParameters);
                cameraParameters.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
                try {
                    camera.setParameters(cameraParameters);
                    chooseOrientation(cameraFacing);
                    logCameraParams(fpsRange);
                } catch (Exception e) {
                    throw new UnableToOpenCameraException("Can't set params.", e, cameraParameters.flatten());
                }
            } catch (Throwable t) {
                throw new UnableToOpenCameraException("Unable to open camera", t);
            }
        }

        private int getCameraIndexByFacing(int cameraFacing) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i += 1) {
                Camera.getCameraInfo(i, info);
                if (info.facing == cameraFacing) {
                    return i;
                }
            }
            return 0;
        }

        private void logCameraParams(int[] fpsRange) {
            String previewFacts = cameraPreviewSize.width + "x" + cameraPreviewSize.height;
            if (fpsRange[0] == fpsRange[1]) {
                previewFacts = previewFacts + " @" + (((double) fpsRange[0]) / 1000.0d) + "fps";
            } else {
                previewFacts = previewFacts + " @[" + (((double) fpsRange[0]) / 1000.0d) + " - " + (((double) fpsRange[1]) / 1000.0d) + "] fps";
            }
            Log.i(TAG, "Camera config: " + previewFacts);
            Log.i(TAG, "Camera params: " + cameraParameters.flatten());
        }

        @CameraThread
        private void handleReleaseCamera() {
            if (camera != null) {
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        }

        public void handleOpenCameraAndStartPreview() {
            try {
                handleOpenCamera();
                handleStartPreview();
            } catch (Exception e) {
                handleUnableToOpenCameraException(e);
            }
        }

        @CameraThread
        private void handleStartPreview() {
            try {
                camera.setPreviewTexture(surfaceTexture);
                camera.startPreview();
                ready = true;
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        @CameraThread
        private void handleReleaseSurfaceTexture() {
            if (surfaceTexture != null) {
                surfaceTexture.release();
                surfaceTexture = null;
            }
            ready = false;
        }

        @CameraThread
        private void setCameraPreviewSize(Size size) {
            Log.d(TAG, "Camera size set to: " + size.width + "x" + size.height);
            cameraPreviewSize = size;
            cameraParameters.setPreviewSize(size.width, size.height);
            onFrameSizeChangedListener.onFrameSizeChanged(size);
        }

        private Size choosePreviewSize(Camera.Parameters params) {
            for (Camera.Size size : params.getSupportedPreviewSizes()) {
                Log.d(TAG, "Camera supported size: " + size.width + "x" + size.height);
            }
            List<Camera.Size> sizes = new ArrayList(params.getSupportedPreviewSizes());
            Collections.sort(sizes, new SizeComparator());
            Camera.Size bestMatchSize = sizes.get(0);
            return new Size(bestMatchSize.width, bestMatchSize.height);
        }

        private void chooseOrientation(int facing) {
            int resultRotation;
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(getCameraIndexByFacing(facing), info);
            int cameraRotationDegree = info.orientation;
            Log.d(TAG, "Camera facing: " + info.facing + " orientation: " + cameraRotationDegree);
            int screenRotationDegrees = getScreenRotation();
            if (info.facing == 1) {
                resultRotation = (360 - ((cameraRotationDegree + screenRotationDegrees) % 360)) % 360;
            } else {
                resultRotation = ((cameraRotationDegree - screenRotationDegrees) + 360) % 360;
            }
            Log.d(TAG, "Camera result rotation: " + resultRotation);
            camera.setDisplayOrientation(resultRotation);
        }

        private int[] choosePreviewFpsRange(Camera.Parameters params) {
            int[] maxRange = null;
            for (int[] range : params.getSupportedPreviewFpsRange()) {
                if (maxRange == null || range[0] + range[1] > maxRange[0] + maxRange[1]) {
                    maxRange = range;
                }
            }
            return maxRange;
        }
    }

    public CameraTextureVideoInputSource(Activity activity) {
        onFrameOrientationChangedListener = OnFrameOrientationChangedListener.EMPTY;
        onFrameSizeChangedListener = OnFrameSizeChangedListener.EMPTY;
        isFrontCamera = true;
        transform = new float[16];
        oldTransform = new float[16];
        ready = false;
        this.activity = activity;
    }

    @Override
    public void onGlContextCreated() {
        fullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        textureId = fullScreen.createTextureObject();
        surfaceTexture = new SurfaceTexture(textureId);
        startPreview();
    }

    @Override
    public void startPreview() {
        cameraHandler.handleOpenCameraAndStartPreview();
    }

    @Override
    public void switchCamera() {
        cameraHandler.switchCamera();
    }

    @Override
    public int getCameraFacing() {
        if (isFrontCamera) {
            return Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            return Camera.CameraInfo.CAMERA_FACING_BACK;
        }
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public int getFrameRate() {
        return chooseFixedPreviewFps(cameraParameters, DESIRED_PREVIEW_FPS * 1000);
    }

    @Override
    public void checkOrientationChanged() {
        if (!Arrays.equals(transform, oldTransform)) {
            onFrameOrientationChangedListener.onFrameOrientationChanged();
            System.arraycopy(transform, 0, oldTransform, 0, transform.length);
        }
    }

    @Override
    public Size getFrameSize() {
        if (cameraPreviewSize != null) {
            return cameraPreviewSize.flipAxes();
        }
        throw new IllegalStateException("Init camera first.");
    }

    @Override
    public int getTextureId() {
        return textureId;
    }

    @Override
    public float[] getTextureTransform() {
        return transform;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void nextFrame() {
        if (surfaceTexture != null) {
            surfaceTexture.updateTexImage();
            surfaceTexture.getTransformMatrix(transform);
            fullScreen.drawFrame(textureId, transform);
            checkOrientationChanged();
        }
    }

    @Override
    public void onResume() {
        HandlerThread cameraThread = new HandlerThread(":CameraThread");
        cameraThread.start();
        cameraHandler = new CameraHandler(cameraThread.getLooper());
    }

    @Override
    public void release() {
        cameraHandler.handleReleaseSurfaceTexture();
        cameraHandler.handleReleaseCamera();
        cameraHandler.stopCameraThread();
    }

    @Override
    public void setCameraOpenErrorListener(TextureVideoInputSourceErrorListener cameraOpenErrorListener) {
        this.cameraOpenErrorListener = cameraOpenErrorListener;
    }

    @Override
    public void setOnFrameOrientationChangedListener(OnFrameOrientationChangedListener onFrameOrientationChangedListener) {
        this.onFrameOrientationChangedListener = onFrameOrientationChangedListener;
    }

    @Override
    public void setOnFrameSizeChangedListener(OnFrameSizeChangedListener onFrameSizeChangedListener) {
        this.onFrameSizeChangedListener = onFrameSizeChangedListener;
    }

    private static int compareInts(int lhs, int rhs) {
        if (lhs < rhs) {
            return -1;
        }
        return lhs == rhs ? 0 : 1;
    }

    private boolean doesUserHaveCameraPermission() {
        return activity.checkCallingOrSelfPermission("android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED;
    }

    private int getScreenRotation() {
        switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
            case 0:
                return 0;
            case 1:
                return 90;
            case 2:
                return 180;
            case 3:
                return 270;
            default:
                return 0;
        }
    }

    public static int chooseFixedPreviewFps(Camera.Parameters parms, int desiredThousandFps) {
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            if ((entry[0] == entry[1]) && (entry[0] == desiredThousandFps)) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }

        int[] tmp = new int[2];
        parms.getPreviewFpsRange(tmp);
        int guess;
        if (tmp[0] == tmp[1]) {
            guess = tmp[0];
        } else {
            guess = tmp[1] / 2;     // shrug
        }

        Log.d(TAG, "Couldn't find match for " + desiredThousandFps + ", using " + guess);
        return guess;
    }
}
