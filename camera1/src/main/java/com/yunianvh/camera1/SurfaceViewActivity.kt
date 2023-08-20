package com.yunianvh.camera1

import android.Manifest
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.yunianvh.camera1.databinding.ActivitySurfaceViewBinding

class SurfaceViewActivity : AppCompatActivity() , SurfaceHolder.Callback {
    private val permissionArray = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    private var binding: ActivitySurfaceViewBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySurfaceViewBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        XXPermissions.with(this)
            .permission(*permissionArray)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, allGranted: Boolean) {
                    if (!allGranted) {
                        Toast.makeText(applicationContext, "请务必开启所有权限", Toast.LENGTH_LONG).show()
                        return
                    }
                    binding!!.cameraSurfaceView.holder.addCallback(this@SurfaceViewActivity)
                }

                override fun onDenied(permissions: List<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {
                        Toast.makeText(applicationContext, "被永久拒绝授权，请手动授予权限", Toast.LENGTH_LONG)
                            .show()
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(applicationContext, permissions)
                    } else {
                        Toast.makeText(applicationContext, "获取权限失败", Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseCamera()
    }
    // 定义相机实例
    var mCamera: Camera? = null
    val width = 1920
    val height = 1080

    // 初始化相机方法，注意要想让代码跑起来，要先申请相机权限
    private fun initCamera() {
        try {
            // 获取相机实例
            mCamera = Camera.open()

            // 设置相机参数
            val parameters = mCamera?.parameters
            // 设置自动对焦模式
            parameters?.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
            // 设置相机预览图像的尺寸
            parameters?.setPreviewSize(width, height)
            // 设置其他相机参数...

            // 将参数应用到相机
            mCamera?.parameters = parameters

            //寻转角度
            mCamera?.setDisplayOrientation(getDisplayOrientation())

            mCamera?.setPreviewDisplay(binding?.cameraSurfaceView?.holder)
            // 启动相机预览
            mCamera?.startPreview()
        } catch (e: Exception) {
            // 处理相机初始化异常
            e.printStackTrace()
        }
    }

    // 停止相机预览并释放相机资源
    private fun releaseCamera() {
        mCamera?.apply {
            stopPreview()
            release()
        }
        mCamera = null
    }

    /**
     * 自适应相机角度
     * @return
     */
    private fun getDisplayOrientation(): Int {
        val windowManager = this.getSystemService(WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val rotation = display.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        val cameraInfo = CameraInfo()
        Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, cameraInfo)
        var result: Int
        if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else {
            result = (cameraInfo.orientation - degrees + 360) % 360
        }
        return result
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        initCamera()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releaseCamera()
    }
}