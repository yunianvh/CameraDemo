package com.yunianvh.camera2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.hjq.permissions.XXPermissions;
import com.yunianvh.camera2.databinding.Camera2ActivityBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Camera2Activity extends Activity {
    private static final String TAG = Camera2Activity.class.getSimpleName();
    private Camera2ActivityBinding binding;
    private String[] permissionArray = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = Camera2ActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.camera2TakePicture.setOnClickListener(v -> takePicture());
        binding.camera2Recording.setOnClickListener(v -> {
            if (!isRecording) startRecordingVideo();
            else stopRecordingVideo();

            binding.camera2Recording.setText(isRecording ? "关闭录制" : "开启录制");
        });

        XXPermissions.with(this).permission(permissionArray).request((permissions, allGranted) -> {
            if (!allGranted) {
                Toast.makeText(Camera2Activity.this, "请务必开启所有权限", Toast.LENGTH_LONG).show();
                return;
            }
            //获取权限成功
            initCamera2();
        });

    }

    private void initCamera2() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(Camera2Activity.this, "请先申请权限", Toast.LENGTH_SHORT).show();
                return;
            }
            cameraManager.openCamera(cameraIdList[1], new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    // 相机打开成功，可以进行后续操作
                    createCaptureSession(cameraDevice);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    // 相机断开连接
                    mCameraDevice.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int error) {
                    // 打开相机发生错误
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private CameraDevice mCameraDevice;
    private CameraCaptureSession captureSession;
    private int imageWidth = 1920;
    private int imageHeight = 1080;
    private ImageReader mImageReader;

    private void createCaptureSession(CameraDevice cameraDevice) {
        mCameraDevice = cameraDevice;
        List<Surface> surfaces = new ArrayList<>();

        // 预览Surface
        SurfaceHolder surfaceHolder = binding.camera2SurfaceView.getHolder();
        Surface previewSurface = surfaceHolder.getSurface();
        surfaces.add(previewSurface);

        // 创建ImageReader对象(拍照)
        mImageReader = ImageReader.newInstance(imageWidth, imageHeight, ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(mImageReaderListener, null);
        surfaces.add(mImageReader.getSurface());

        //添加录制Surface
        initRecording();
        surfaces.add(mMediaRecorder.getSurface());

        try {
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        // 预览会话已创建成功，可以开始预览
                        captureSession = session;

                        // 创建预览请求
                        CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        previewRequestBuilder.addTarget(previewSurface); // 设置预览目标Surface

                        // 开启连续预览
                        captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    // 预览会话创建失败
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void takePicture() {
        try {
            // 创建拍照请求
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // 设置自动对焦
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO);

            // 设置闪光灯
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            // 获取设备方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation);

            captureSession.capture(captureBuilder.build(), null, null);

            // 播放拍照音效或显示闪光灯动画等

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private ImageReader.OnImageAvailableListener mImageReaderListener = reader -> {
        // 获取到拍照的图像数据
        Image image = reader.acquireLatestImage();

        // 获取图片的字节数组
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        // 保存图片到相册
        saveImageToGallery(data);

        // 释放图像资源
        image.close();
    };

    private void saveImageToGallery(byte[] data) {
        // 定义图片的保存路径和文件名
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + File.separator + fileName;

        // 创建文件输出流
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(data);
            fos.close();

            // 通知图库更新
            MediaScannerConnection.scanFile(this, new String[]{filePath}, null, null);

            // 在某些设备上，可能需要发送广播通知才能使图片立即出现在相册中
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(filePath))));

            // 显示保存成功的提示
            Toast.makeText(this, "图片保存成功", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            // 显示保存失败的提示
            Toast.makeText(this, "图片保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private MediaRecorder mMediaRecorder;
    private int videoWidth = 1920;
    private int videoHeight = 1080;
    private File recorderFile;
    private String recorderPath;
    private boolean isRecording = false;

    private void initRecording() {
        recorderFile = new File(Camera2Activity.this.getExternalFilesDir(null).getAbsolutePath() + "/video/");
        if (!recorderFile.exists()) {
            recorderFile.mkdir();
        }
        recorderPath = recorderFile.getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp4";
        Log.e(TAG, "视频路径：" + recorderPath);
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(recorderPath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(videoWidth, videoHeight);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        // 开始录制
        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startRecordingVideo() {
        // 创建录制视频请求
        try {
            CaptureRequest.Builder recordRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            recordRequestBuilder.addTarget(mMediaRecorder.getSurface()); // 设置录制目标Surface

            captureSession.setRepeatingRequest(recordRequestBuilder.build(), null, null); // 开始录制视频
            isRecording = true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopRecordingVideo() {
        // 停止录制视频
        try {
            mMediaRecorder.stop();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } finally {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        isRecording = false;

        // 关闭相机预览会话
//        if (captureSession != null) {
//            captureSession.close();
//            captureSession = null;
//        }

        //图库更新
        addToGallery(recorderPath);
    }

    private void addToGallery(String videoFilePath) {
        // 发送广播通知图库更新
        MediaScannerConnection.scanFile(this, new String[]{videoFilePath}, null,
                (path, uri) -> {
                    // 添加到相册成功的回调
                    Toast.makeText(this, "视频已保存至相册", Toast.LENGTH_SHORT).show();
                });
    }
}
