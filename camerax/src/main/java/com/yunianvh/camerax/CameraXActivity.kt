package com.yunianvh.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Surface.ROTATION_90
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.hjq.permissions.XXPermissions
import com.yunianvh.camerax.databinding.ActivityCameraXBinding
import java.io.File

@SuppressLint("RestrictedApi")
class CameraXActivity : AppCompatActivity() {
    private val TAG = "CameraXActivity"
    private var binding: ActivityCameraXBinding? = null
    private val permissionArray = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraXBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        binding!!.cameraxTakePicture.setOnClickListener {
            takePicture()
        }
        binding!!.cameraxRecording.setOnClickListener {
            if (!isRecording) startRecording() else stopRecording()
        }

        XXPermissions.with(this).permission(*permissionArray)
            .request { _: List<String?>?, allGranted: Boolean ->
                if (!allGranted) {
                    Toast.makeText(this@CameraXActivity, "请务必开启所有权限", Toast.LENGTH_LONG).show()
                    return@request
                }
                //获取权限成功
                startCamera()
            }
    }

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("RestrictedApi")
    private fun bindCameraUseCases() {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        //previewView预览
        preview = Preview.Builder().build()
        //拍照
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        //录像
        videoCapture = VideoCapture.Builder()//录像用例配置
            .setTargetAspectRatio(AspectRatio.RATIO_16_9) //设置高宽比
            .setTargetRotation(ROTATION_90)//设置旋转角度
            .setAudioRecordSource(MediaRecorder.AudioSource.MIC)//设置音频源麦克风
            .build()

        //解绑用例
        cameraProvider.unbindAll()
        //绑定用例
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture)

        //开始预览 previewView
        preview.setSurfaceProvider(binding!!.previewView.surfaceProvider)
    }

    //拍照
    private var imageCapture: ImageCapture? = null
    private fun takePicture() {
        val outputFile = File(this.getExternalFilesDir(null), "${System.currentTimeMillis()}.jpg")
        Log.e(TAG, "照片文件: ${outputFile.absolutePath}")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // 图片保存成功后的处理逻辑
                    Toast.makeText(this@CameraXActivity, "拍照成功", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    // 拍照失败的处理逻辑
                    Toast.makeText(this@CameraXActivity, "拍照失败", Toast.LENGTH_SHORT).show()
                }
            })
    }

    //录像
    private var isRecording = false
    private var videoCapture: VideoCapture? = null
    private fun startRecording() {
        val outputFile = File(this.getExternalFilesDir(null), "${System.currentTimeMillis()}.mp4")
        Log.e(TAG, "视频文件: ${outputFile.absolutePath}")
        val outputOptions = VideoCapture.OutputFileOptions.Builder(outputFile).build()
        videoCapture?.startRecording(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    // 录制保存成功后的处理逻辑
                    Toast.makeText(this@CameraXActivity, "录制保存成功", Toast.LENGTH_SHORT).show()
                    isRecording = false
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    // 录制保存失败的处理逻辑
                    Toast.makeText(this@CameraXActivity, "录制保存失败", Toast.LENGTH_SHORT).show()
                    isRecording = false
                }
            })
        isRecording = true
    }

    private fun stopRecording() {
        videoCapture?.stopRecording()
        isRecording = false
    }
}