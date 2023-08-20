package com.yunianvh.camera1;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;
import com.yunianvh.camera1.databinding.ActivityTextureViewBinding;

import java.util.List;

public class JavaSurfaceTextureActivity extends Activity implements TextureView.SurfaceTextureListener {
    private static final String TAG = JavaSurfaceTextureActivity.class.getSimpleName();
    private String[] permissionArray = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
    private ActivityTextureViewBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTextureViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        XXPermissions.with(this)
                .permission(permissionArray)
                .request(new OnPermissionCallback() {

                    @Override
                    public void onGranted(List<String> permissions, boolean allGranted) {
                        if (!allGranted) {
                            Toast.makeText(getApplicationContext(), "请务必开启所有权限", Toast.LENGTH_LONG).show();
                            return;
                        }
                        binding.cameraTextureView.setSurfaceTextureListener(JavaSurfaceTextureActivity.this);
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean doNotAskAgain) {
                        if (doNotAskAgain) {
                            Toast.makeText(getApplicationContext(), "被永久拒绝授权，请手动授予权限", Toast.LENGTH_LONG).show();
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(getApplicationContext(), permissions);
                        } else {
                            Toast.makeText(getApplicationContext(), "获取权限失败", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    // 定义相机实例
    private Camera mCamera;
    private int width = 1920;
    private int height = 1080;

    // 初始化相机方法，注意要想让代码跑起来，要钱申请Camera权限
    private void initCamera() {
        try {
            // 获取相机实例
            mCamera = Camera.open();

            // 设置相机参数
            Camera.Parameters parameters = mCamera.getParameters();
            // 设置自动对焦模式
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            // 设置相机预览图像的尺寸
            parameters.setPreviewSize(width, height);
            // 设置其他相机参数...

            // 将参数应用到相机
            mCamera.setParameters(parameters);

            //寻转角度
            mCamera.setDisplayOrientation(getDisplayOrientation());

            // 这里使用surfaceTexture来承载相机的预览，而不需要设置一个可见的view
            mCamera.setPreviewTexture(binding.cameraTextureView.getSurfaceTexture());
            // 启动相机预览
            mCamera.startPreview();
            Log.e(TAG, "initCamera: 启动相机预览");
        } catch (Exception e) {
            // 处理相机初始化异常
            e.printStackTrace();
            Log.e(TAG, "initCamera Exception: " + e);
        }
    }

    // 停止相机预览并释放相机资源
    private void releaseCamera() {
        Log.e(TAG, "releaseCamera: 停止相机预览并释放相机资源");
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 自适应相机角度
     * @return
     */
    private int getDisplayOrientation() {
        WindowManager windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);
        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        return result;
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        initCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        releaseCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }
}
