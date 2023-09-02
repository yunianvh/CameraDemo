package com.yunianvh.uvccamera

import android.Manifest
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.XXPermissions
import com.jiangdg.usbcamera.UVCCameraHelper
import com.serenegiant.usb.common.AbstractUVCCameraHandler.OnEncodeResultListener
import com.serenegiant.usb.encoder.RecordParams
import com.serenegiant.usb.widget.CameraViewInterface
import com.yunianvh.ucvcamera.databinding.ActivityUvcCameraBinding
import java.io.File


class UVCCameraActivity : AppCompatActivity() {
    private val TAG = "UVCCameraActivity"
    private var binding: ActivityUvcCameraBinding? = null
    private val permissionArray = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUvcCameraBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        binding!!.uvcTakePicture.setOnClickListener {
            takePicture()
        }
        binding!!.uvcRecording.setOnClickListener {
            videoRecord(!mCameraHelper!!.isPushing)
        }

        XXPermissions.with(this).permission(*permissionArray)
            .request { _: List<String?>?, allGranted: Boolean ->
                if (!allGranted) {
                    Toast.makeText(this@UVCCameraActivity, "请务必开启所有权限", Toast.LENGTH_LONG).show()
                    return@request
                }
                //获取权限成功
                initUVCCamera()
            }
    }

    private var mCameraHelper: UVCCameraHelper? = null
    private var mUVCCameraView: CameraViewInterface? = null
    private var isPreview = false

    // step.1 initialize UVCCameraHelper
    fun initUVCCamera() {
        mUVCCameraView = binding!!.cameraView
        mUVCCameraView!!.setCallback(cameraCallback)
        mCameraHelper = UVCCameraHelper.getInstance()
        mCameraHelper!!.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG)
        mCameraHelper!!.initUSBMonitor(this, mUVCCameraView, object :
            UVCCameraHelper.OnMyDevConnectListener {
            override fun onAttachDev(p0: UsbDevice?) {
                if (mCameraHelper != null) {
                    mCameraHelper!!.requestPermission(0)
                }
            }

            override fun onDettachDev(p0: UsbDevice?) {
                mCameraHelper!!.closeCamera()
            }

            override fun onConnectDev(p0: UsbDevice?, p1: Boolean) {
                isPreview = p1
            }

            override fun onDisConnectDev(p0: UsbDevice?) {
            }
        })
    }

    private val cameraCallback = object : CameraViewInterface.Callback {
        override fun onSurfaceCreated(p0: CameraViewInterface?, p1: Surface?) {
            if (!isPreview && mCameraHelper!!.isCameraOpened) {
                mCameraHelper!!.startPreview(mUVCCameraView)
                isPreview = true
            }
        }

        override fun onSurfaceChanged(
            p0: CameraViewInterface?,
            p1: Surface?,
            p2: Int,
            p3: Int
        ) {
        }

        override fun onSurfaceDestroy(p0: CameraViewInterface?, p1: Surface?) {
            if (isPreview && mCameraHelper!!.isCameraOpened) {
                mCameraHelper!!.stopPreview()
                isPreview = false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper!!.registerUSB()
            isPreview = true
            mCameraHelper!!.startPreview(mUVCCameraView)
        }
    }

    override fun onStop() {
        super.onStop()
        // step.3 unregister USB event broadcast
        if (mCameraHelper != null) {
            isPreview = false
            mCameraHelper!!.stopPreview()
            mCameraHelper!!.unregisterUSB()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // step.4 release uvc camera resources
        if (mCameraHelper != null) {
            mCameraHelper!!.release()
        }
    }
    /**
     * 拍照
     */
    private fun takePicture() {
        val picPath =
            File(getExternalFilesDir(null)!!.absolutePath + "/" + System.currentTimeMillis() + ".jpg")
        mCameraHelper!!.capturePicture(picPath.absolutePath) {
            Log.e(TAG, "图片路径：$it")
        }
    }

    /**
     * 录制
     */
    private fun videoRecord(isRecord: Boolean) {
        if (!isRecord) {
            mCameraHelper!!.stopPusher()
        } else {
            val videoPath =
                File(getExternalFilesDir(null)!!.absolutePath + "/" + System.currentTimeMillis() + ".mp4")
            val params = RecordParams()
            params.recordPath = videoPath.absolutePath
            params.recordDuration = 0 // auto divide saved,default 0 means not divided
            params.isVoiceClose = false // is close voice
            Log.e(TAG, "录制视频路径：${videoPath.absolutePath}")
            mCameraHelper!!.startPusher(params, object : OnEncodeResultListener {
                override fun onEncodeResult(
                    data: ByteArray,
                    offset: Int,
                    length: Int,
                    timestamp: Long,
                    type: Int
                ) {
                    // type = 1,h264 video stream
                    // type = 0,aac audio stream
                }

                override fun onRecordResult(videoPath: String) {
                    //录制完成返回的videoPath
                    Log.e(TAG, "录制完成：${videoPath}")
                }
            })
        }
    }
}