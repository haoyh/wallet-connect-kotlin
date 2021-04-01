package com.trustwallet.walletconnect.sample

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.trustwallet.walletconnect.models.WCPeerMeta
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    data class PeerData(val peerId: String, val peerMeta: WCPeerMeta)

    companion object {
        private val TAG = "hyh"
        fun logger(info: String) {
            Log.d(TAG, info)
        }
    }

    private val REQUEST_CODE_SCAN_QRCODE = 0x31
    private val PERMISSION_CAMERA_REQUESTCODE = 0x42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_go_scan?.setOnClickListener {
            // check permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.CAMERA),
                    PERMISSION_CAMERA_REQUESTCODE
                )
            } else {
                startScan()
            }
        }
    }

    private fun startScan() {
        startActivityForResult(
            Intent(this, ScanActivity::class.java), REQUEST_CODE_SCAN_QRCODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CAMERA_REQUESTCODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SCAN_QRCODE) {
            data?.getStringExtra("result")?.let { info ->
                val intent = Intent(this@MainActivity, LoadAndShowDappInfoActivity::class.java)
                intent.putExtra(LoadAndShowDappInfoActivity.PARAMS_INFO, info)
                startActivity(intent)
            }

        }
    }
}
