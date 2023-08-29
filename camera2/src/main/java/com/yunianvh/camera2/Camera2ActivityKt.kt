package com.yunianvh.camera2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.XXPermissions
import com.yunianvh.camera2.databinding.Camera2ActivityBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer


class Camera2ActivityKt : AppCompatActivity() {
    private val permissionArray = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private var binding: Camera2ActivityBinding? = null
    val TAG: String = "Camera2ActivityKt"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = Camera2ActivityBinding.inflate(layoutInflater)
        setContentView(binding!!.root)


        //给一个按钮点击事件进行拍照
        binding!!.camera2TakePicture.setOnClickListener { v -> takePicture() }

        //录制
        binding!!.camera2Recording.setOnClickListener { v ->
            if (!isRecording) startRecordingVideo() else stopRecordingVideo()
            binding!!.camera2Recording.text = if (isRecording) "关闭录制" else "开启录制"
        }

        XXPermissions.with(this).permission(*permissionArray)
            .request { permissions: List<String?>?, allGranted: Boolean ->
                if (!allGranted) {
                    Toast.makeText(this@Camera2ActivityKt, "请务必开启所有权限", Toast.LENGTH_LONG).show()
                    return@request
                }
                //获取权限成功
                initCamera2()
            }

    }

    @SuppressLint("MissingPermission")
    fun initCamera2() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIdList = cameraManager.cameraIdList

        cameraManager.openCamera(cameraIdList[1], object : CameraDevice.StateCallback() {
            override fun onOpened(cameraDevice: CameraDevice) {
                // 相机打开成功，可以进行后续操作
                createCaptureSession(cameraDevice)
            }

            override fun onDisconnected(cameraDevice: CameraDevice) {
                // 相机断开连接
                mCameraDevice!!.close()
                mCameraDevice = null
            }

            override fun onError(cameraDevice: CameraDevice, error: Int) {
                // 打开相机发生错误
                mCameraDevice!!.close()
                mCameraDevice = null
            }
        }, null)
    }

    private var mCameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private val imageWidth = 1920
    private val imageHeight = 1080
    private var mImageReader: ImageReader? = null

    private fun createCaptureSession(cameraDevice: CameraDevice) {
        mCameraDevice = cameraDevice
        val surfaces: MutableList<Surface> = ArrayList()

        // 预览Surface
        val surfaceHolder: SurfaceHolder = binding!!.camera2SurfaceView.holder
        val previewSurface: Surface = surfaceHolder.surface
        surfaces.add(previewSurface)

        // 创建ImageReader对象(拍照)
        mImageReader = ImageReader.newInstance(imageWidth, imageHeight, ImageFormat.JPEG, 1)
        mImageReader!!.setOnImageAvailableListener(mImageReaderListener, null)
        surfaces.add(mImageReader!!.surface)

        //添加录制Surface
        initRecording()
        surfaces.add(mMediaRecorder!!.surface)
        try {
            cameraDevice.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            // 预览会话已创建成功，可以开始预览
                            captureSession = session

                            // 创建预览请求
                            val previewRequestBuilder =
                                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                            previewRequestBuilder.addTarget(previewSurface) // 设置预览目标Surface

                            // 开启连续预览
                            captureSession!!.setRepeatingRequest(
                                previewRequestBuilder.build(),
                                null,
                                null
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        // 预览会话创建失败
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun takePicture() {
        try {
            // 创建拍照请求
            val captureBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(mImageReader!!.surface)

            // 设置自动对焦
            captureBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_AUTO
            )

            // 设置闪光灯
            captureBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )

            // 获取设备方向
            val rotation = windowManager.defaultDisplay.rotation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation)
            captureSession!!.capture(captureBuilder.build(), null, null)

            // 播放拍照音效或显示闪光灯动画等
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val mImageReaderListener = OnImageAvailableListener { reader: ImageReader ->
        // 获取到拍照的图像数据
        val image: Image = reader.acquireLatestImage()

        // 获取图片的字节数组
        val buffer: ByteBuffer = image.getPlanes().get(0).getBuffer()
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        // 保存图片到相册
        saveImageToGallery(data)

        // 释放图像资源
        image.close()
    }

    private fun saveImageToGallery(data: ByteArray) {
        // 定义图片的保存路径和文件名
        val fileName = "IMG_" + System.currentTimeMillis() + ".jpg"
        val filePath: String =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                .absolutePath + File.separator.toString() + fileName

        // 创建文件输出流
        try {
            val fos = FileOutputStream(filePath)
            fos.write(data)
            fos.close()

            // 通知图库更新
            MediaScannerConnection.scanFile(this, arrayOf(filePath), null, null)

            // 在某些设备上，可能需要发送广播通知才能使图片立即出现在相册中
            sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(File(filePath))
                )
            )

            // 显示保存成功的提示
            Toast.makeText(this, "图片保存成功", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            // 显示保存失败的提示
            Toast.makeText(this, "图片保存失败", Toast.LENGTH_SHORT).show()
        }
    }

    private var mMediaRecorder: MediaRecorder? = null
    private val videoWidth = 1920
    private val videoHeight = 1080
    private var recorderFile: File? = null
    private var recorderPath: String? = null
    private var isRecording = false

    private fun initRecording() {
        recorderFile =
            File(this.getExternalFilesDir(null)!!.absolutePath + "/video/")
        if (!recorderFile!!.exists()) {
            recorderFile!!.mkdir()
        }
        recorderPath = recorderFile!!.absolutePath + "/" + System.currentTimeMillis() + ".mp4"
        Log.e(TAG, "视频路径：$recorderPath")
        mMediaRecorder = MediaRecorder()
        mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mMediaRecorder!!.setOutputFile(recorderPath)
        mMediaRecorder!!.setVideoEncodingBitRate(10000000)
        mMediaRecorder!!.setVideoFrameRate(30)
        mMediaRecorder!!.setVideoSize(videoWidth, videoHeight)
        mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        // 开始录制
        try {
            mMediaRecorder!!.prepare()
            mMediaRecorder!!.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun startRecordingVideo() {
        // 创建录制视频请求
        try {
            val recordRequestBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            recordRequestBuilder.addTarget(mMediaRecorder!!.surface) // 设置录制目标Surface
            captureSession!!.setRepeatingRequest(recordRequestBuilder.build(), null, null) // 开始录制视频
            isRecording = true
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun stopRecordingVideo() {
        // 停止录制视频
        try {
            mMediaRecorder!!.stop()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } finally {
            mMediaRecorder!!.reset()
            mMediaRecorder!!.release()
            mMediaRecorder = null
        }
        isRecording = false

        // 关闭相机预览会话
//        if (captureSession != null) {
//            captureSession.close();
//            captureSession = null;
//        }

        //图库更新
        addToGallery(recorderPath)
    }

    private fun addToGallery(videoFilePath: String?) {
        // 发送广播通知图库更新
        MediaScannerConnection.scanFile(
            this, arrayOf(videoFilePath), null
        ) { path: String?, uri: Uri? ->
            // 添加到相册成功的回调
            Toast.makeText(this, "视频已保存至相册", Toast.LENGTH_SHORT).show()
        }
    }

}