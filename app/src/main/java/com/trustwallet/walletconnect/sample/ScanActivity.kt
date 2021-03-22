package com.trustwallet.walletconnect.sample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import cn.bingoogolapple.qrcode.core.QRCodeView
import com.trustwallet.walletconnect.sample.MainActivity.Companion.logger
import kotlinx.android.synthetic.main.activity_scan.*

class ScanActivity : AppCompatActivity(), QRCodeView.Delegate {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        zxing_view?.setDelegate(this)
    }

    override fun onStart() {
        super.onStart()

        // 打开后置摄像头开始预览，但是并未开始识别
        zxing_view?.startCamera()

        // 显示扫描框，并开始识别
        zxing_view?.startSpotAndShowRect()
    }

    override fun onStop() {
        // 关闭摄像头预览，并且隐藏扫描框
        zxing_view?.stopCamera()
        super.onStop()
    }

    override fun onDestroy() {
        // 销毁二维码扫描控件
        zxing_view?.onDestroy()
        super.onDestroy()
    }

    override fun onScanQRCodeSuccess(result: String?) {
        if (result == null || result.isEmpty()) {
            // 重新开始识别
            zxing_view?.startSpot()
            return
        }
        val intent = Intent()
        intent.putExtra("result", result)
        logger("result = $result")
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {}

    override fun onScanQRCodeOpenCameraError() {}
}