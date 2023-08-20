package com.yunianvh.camera1;

import android.Manifest;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;

import java.util.List;

public class SurfaceTextureActivityJava extends Activity {
    private String[] permissionArray = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XXPermissions.with(this)
                .permission(permissionArray)
                .request(new OnPermissionCallback() {

                    @Override
                    public void onGranted(List<String> permissions, boolean allGranted) {
                        if (!allGranted) {
                            Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG).show();
                            return;
                        }
                        initCamera();
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

            // 这里使用surfaceTexture来承载相机的预览，而不需要设置一个可见的view
            mCamera.setPreviewTexture(new SurfaceTexture(0));
            // 启动相机预览
            mCamera.startPreview();
        } catch (Exception e) {
            // 处理相机初始化异常
            e.printStackTrace();
        }
    }

    // 停止相机预览并释放相机资源
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}
