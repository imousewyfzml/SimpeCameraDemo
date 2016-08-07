package org.wonderland.worldwari;

import android.content.SharedPreferences;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.app.AppCompatActivity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class CameraPreviewActivity extends AppCompatActivity {

    final static String TAG = "CameraPreviewActivity";
    private Camera mCamera = null;
    private FrameLayout mPreviewContiner = null;
    private CameraPreview mPreview = null;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    // button
    private Button mSwitchCamera = null;

    // module
    final static String mSharedPreferencs = "CameraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        Log.i(TAG, "on Create");

        SharedPreferences pref = getApplicationContext().getSharedPreferences(mSharedPreferencs, MODE_PRIVATE);
        mCameraId = pref.getInt("CameraId", Camera.CameraInfo.CAMERA_FACING_BACK);

        setUpView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        openCamera();
        startPreview();
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        destroyPreviewSurface();
        Log.i(TAG, "onPause");
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
        Log.i(TAG, "onDestroy");

    }

    private void setUpView() {
        mSwitchCamera = (Button)findViewById(R.id.btnSwitch);
        mSwitchCamera.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        switchCamera();
                    }
                }
        );
    }

    private void openCamera() {
        // default open Camera 0
        mCamera = getCameraInstance(mCameraId);
        if (mCamera == null) {
            return;
        }
        setDefaultParameter();
    }

    private void startPreview() {
        mPreview = new CameraPreview(this, mCamera);
        mPreviewContiner  = (FrameLayout)findViewById(R.id.camera_preview);
        mPreviewContiner.addView(mPreview);
    }

    private void closeCamera() {
        if (mCamera == null) {
            return;
        }
        mCamera.stopPreview();
        mCamera.release();
    }

    private void destroyPreviewSurface() {
        // TODO: it is not good to rebuild surfaceview;
        if(mPreview != null) {
            mPreview.surfaceDestroyed(mPreview.getHolder());
            mPreview.getHolder().removeCallback(mPreview);
            mPreview.destroyDrawingCache();

            mPreviewContiner.removeView(mPreview);
            mPreview.destroyedCamera();
            mPreview = null;
        }
    }

    //event handler
    private void switchCamera() {

        // close camera which was opened before;
        closeCamera();
        mCameraId = mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK ?
                Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
        Log.i(TAG, "switch to camera " + mCameraId);

        openCamera();
        destroyPreviewSurface();
        startPreview();

        SharedPreferences pref = getApplicationContext().getSharedPreferences(mSharedPreferencs, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("CameraId", mCameraId);
        editor.commit();
    }

    private Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            c = Camera.open(cameraId);
        } catch (Exception e) {
            Log.e(TAG, "error open camera: " + e.getMessage());
        }

        return c;
    }

    private void setDefaultParameter() {
        if (mCamera == null) {
            Log.e(TAG, "set defualt parameter.");
            return;
        }

        Camera.Parameters params = mCamera.getParameters();

        // size
        params.setPreviewSize(1920, 1080);
        // auto foucs
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        // Orientation
        setCameraDisplayOrientation();

        mCamera.setParameters(params);
    }

    public void setCameraDisplayOrientation() {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(mCameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

}
