package com.muneikh.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.muneikh.inputsource.CameraTextureVideoInputSource;
import com.muneikh.inputsource.TextureVideoInputSource;
import com.muneikh.inputsource.TextureVideoInputSourceErrorListener;
import com.muneikh.inputsource.VideoFileInputSource;
import com.muneikh.inputsource.VideoRenderer;
import com.muneikh.mockcamera.BuildConfig;
import com.muneikh.mockcamera.R;
import com.muneikh.ui.widget.GL2SurfaceView;

public class CameraActivity extends Activity implements TextureVideoInputSourceErrorListener {

    private static final String TAG = "CameraActivity";
    private static final int GRANT_PERMISSIONS_REQUEST_CODE = 101;

    private TextureVideoInputSource textureVideoInputSource;
    private GL2SurfaceView glSurfaceView;
    private VideoRenderer videoRenderer;
    private Switch cameraToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        glSurfaceView = (GL2SurfaceView) findViewById(R.id.gl_surface_view);
        cameraToggle = (Switch) findViewById(R.id.toggle);

        if (BuildConfig.AUTO_TEST) {
            this.textureVideoInputSource = new VideoFileInputSource(this, "mock_input_video.mp4");
        } else {
            this.textureVideoInputSource = new CameraTextureVideoInputSource(this);
        }

        this.videoRenderer = new VideoRenderer(CameraActivity.this, this.textureVideoInputSource);
        this.videoRenderer.setGLSurfaceView(this.glSurfaceView);
        this.textureVideoInputSource.setCameraOpenErrorListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.textureVideoInputSource.onResume();
        this.glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        this.glSurfaceView.onPause();
        this.textureVideoInputSource.release();
        super.onPause();
    }

    @Override
    public void onError(Exception exception, boolean value) {
        Log.d(TAG, "No camera permission");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {

                CameraActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showRationaleDialog();
                    }
                });


                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        GRANT_PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case GRANT_PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    showRationaleDialog();

                }
                return;
            }
        }
    }

    private void showRationaleDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.camera_permission)
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CameraActivity.this.finish();
                    }
                }).show();
    }
}
