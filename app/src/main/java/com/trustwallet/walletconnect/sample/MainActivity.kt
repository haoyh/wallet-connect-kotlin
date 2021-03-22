package com.trustwallet.walletconnect.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import com.trustwallet.walletconnect.models.session.WCSession
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class MainActivity : AppCompatActivity() {

    data class PeerData(val peerId: String, val peerMeta: WCPeerMeta)

    companion object {
        private val TAG = "hyh"
        fun logger(info: String) {
            Log.d(TAG, info)
        }
    }

    private val REQUEST_CODE_SCAN_QRCODE = 1

    private var mInfo = "wc:3778840c-bc5d-468e-8c9f-801a9ae9ee2c@1?bridge=https%3A%2F%2Fbridge.walletconnect.org&key=26abb36c0062e638a914d4069b08d0a2f42264bc31852034613ee4ec39ae6f00"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv_info?.text = mInfo

        initWcClient()

        btn_go_scan?.setOnClickListener {
            startActivityForResult(Intent(this, ScanActivity::class.java), REQUEST_CODE_SCAN_QRCODE)
        }

        // 该方法应该在扫码成功之后调用，并且传入扫码获取到的信息
        btn_connect?.setOnClickListener {
            wcClient.startConnect()
        }

        // 断开连接
        btn_disconnect?.setOnClickListener {
            wcClient.killSession()
        }

        // 同意连接
        btn_approve_connect?.setOnClickListener {
            wcClient.approveSession(listOf("0x704125Bb597c54A54dE3C57Ce5Efb08Fd5F8DB84"), 1)
        }

        // 拒绝连接
        btn_reject_connect?.setOnClickListener {
            wcClient.rejectSession("cancel hyh")
        }
    }

    val client = OkHttpClient()
    var mPeerMeta: WCPeerMeta? = null
    private lateinit var wcClient: WCClient

    var remotePeerData: PeerData? = null

    private fun initWcClient() {
        mPeerMeta = WCPeerMeta(name = "aolink", url = "https://aolink.io/")
        wcClient = WCClient(GsonBuilder(), client)

        wcClient.apply {
            addSocketListener(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "onOpen")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.d(TAG, "onFailure")
                }
            })

            onSessionRequest = { id: Long, peer: WCPeerMeta ->
                Log.d(TAG, "onSessionRequest id = $id remotePeerId = $remotePeerId")
                wcClient.remotePeerId?.let {
                    remotePeerData = PeerData(it, peer)
                    val info = " id = ${remotePeerData?.peerId}" +
                            "\n name = ${remotePeerData?.peerMeta?.name}" +
                            "\n url = ${remotePeerData?.peerMeta?.url}" +
                            "\n description = ${remotePeerData?.peerMeta?.description}"
                    "\n icons = ${remotePeerData?.peerMeta?.icons?.joinToString("--")}"
                    Log.d(TAG, "info = $info")
                    runOnUiThread {
                        tv_dapp_info_return?.text = info
                    }
                }
            }

            onDisconnect = { code: Int, reason: String ->
                Log.d(TAG, "onDisconnect reason = $reason")
                wcClient.killSession()
            }

            onEthSign = { id: Long, message: WCEthereumSignMessage ->
                Log.d(TAG, "onEthSign message.data = ${message.data}")
            }

            onEthSignTransaction = { id: Long, transaction: WCEthereumTransaction ->
                Log.d(TAG, "onEthSignTransaction transaction.to = ${transaction.to}")
            }

            onEthSendTransaction = { id: Long, transaction: WCEthereumTransaction ->
                Log.d(TAG, "onEthSendTransaction transaction.to = ${transaction.to}")
            }

            onGetAccounts = { id: Long ->
                Log.d(TAG, "onGetAccounts")
            }
        }
    }

    private fun WCClient.startConnect() {
        connect(session = WCSession.from(mInfo)!!, peerMeta = mPeerMeta!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SCAN_QRCODE) {

            data?.getStringExtra("result")?.apply {
                mInfo = this
                tv_info?.text = mInfo
                wcClient.startConnect()
            }
        }
    }
}
