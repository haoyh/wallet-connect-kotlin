package com.trustwallet.walletconnect.sample

import android.content.DialogInterface
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import com.trustwallet.walletconnect.models.session.WCSession
import kotlinx.android.synthetic.main.activity_load_and_show_dapp_info.*
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.InterruptedIOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class LoadAndShowDappInfoActivity : AppCompatActivity() {
    
    companion object {
        val PARAMS_INFO = "info"
    }
    
    private lateinit var mInfo: String
    private var mState = State.CLOSED

    enum class State {
        OPEN,
        CLOSED,
        FAILURE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_and_show_dapp_info)

        showLoadingDialog()

        initWcClient()

        mInfo = intent.getStringExtra(PARAMS_INFO)
        
        initView()

        // 该方法应该在扫码成功之后调用，并且传入扫码获取到的信息
        wcClient.startConnect(mInfo)
    }
    
    private fun initView() {

        tv_scan_info?.text = mInfo

        btn_approve_connect?.setOnClickListener {
            // todo need check network statue
            if (mState == State.OPEN && null != remotePeerData) {
                wcClient.approveSession(
                    // your wallet address
                    accounts = listOf("0x7ebbc8aa2ff2fbf2881275c350aeedb81cb380db"),
                    // todo need setting chainId
                    chainId = 65
                )
                btn_disconnect?.visibility = VISIBLE
                tv_scan_info?.visibility = GONE
                ll_connect?.visibility = GONE
            }
        }

        btn_reject_connect?.setOnClickListener {
            if (mState == State.OPEN && null != remotePeerData) {
                wcClient.rejectSession("cancel hyh")
            }
            finish()
        }

        btn_disconnect?.setOnClickListener {
            close(kill = false)
        }
    }

    private var mLoadingDialog: AlertDialog? = null

    private fun showLoadingDialog() {
        mLoadingDialog = null
        mLoadingDialog = AlertDialog.Builder(this).apply {
            setMessage("loading...")
        }.create().apply {
            if (!isFinishing) {
                show()
            }
        }
    }

    private fun dismissLoadingDialog() {
        if (!isFinishing) {
            mLoadingDialog?.dismiss()
        }
    }

    val TIMEOUT_SECOND = 10L

    val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECOND, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECOND, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECOND, TimeUnit.SECONDS)
            .callTimeout(TIMEOUT_SECOND, TimeUnit.SECONDS).build()

    var mPeerMeta: WCPeerMeta? = null
    private lateinit var wcClient: WCClient

    private var remotePeerData: MainActivity.PeerData? = null

    private fun initWcClient() {
        mPeerMeta = WCPeerMeta(name = "aolink", url = "https://aolink.io/")
        wcClient = WCClient(GsonBuilder(), client)

        wcClient.apply {
            addSocketListener(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    MainActivity.logger("onOpen   response.message = ${response.message}")
                    mState = State.OPEN
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosed(webSocket, code, reason)
                    mState = State.CLOSED
                    MainActivity.logger("onClosed reason = $reason")
                    finish()
                }
            })

            onSessionRequest = { id: Long, peer: WCPeerMeta ->
                MainActivity.logger( "onSessionRequest id = $id remotePeerId = $remotePeerId")
                wcClient.remotePeerId?.let {
                    remotePeerData = MainActivity.PeerData(it, peer).apply {
                        val info = " id = ${peerId}" +
                                "\n name = ${peerMeta.name}" +
                                "\n url = ${peerMeta.url}" +
                                "\n description = ${peerMeta.description}" +
                                "\n icons = ${peerMeta.icons[0]}"
                        MainActivity.logger("info = $info")
                        runOnUiThread {
                            dismissLoadingDialog()
                            tv_dapp_info?.text = info
                        }
                    }
                }
            }

            onDisconnect = { code: Int, reason: String ->
                MainActivity.logger( "onDisconnect reason = $reason")
            }

            onEthSign = { id: Long, message: WCEthereumSignMessage ->
                MainActivity.logger( "onEthSign message.data = $message")
                showRequestInfoDialog("onEthSign", id)

            }

            onEthSignTransaction = { id: Long, transaction: WCEthereumTransaction ->
                MainActivity.logger( "onEthSignTransaction transaction.to = $transaction")
                showRequestInfoDialog("onEthSignTransaction", id)
            }

            onEthSendTransaction = { id: Long, transaction: WCEthereumTransaction ->
                MainActivity.logger( "onEthSendTransaction transaction.to = $transaction")
                // return info： WCEthereumTransaction(from=0x7ebbc8aa2ff2fbf2881275c350aeedb81cb380db, to=0x4d5ef58aac27d99935e5b6b4a6778ff292059991, nonce=null, gasPrice=0x266ac43200, gas=0x13880, gasLimit=null, value=null, data=0x095ea7b3000000000000000000000000b93b505ed567982e2b6756177ddd23ab5745f309ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff)
                //  show value        return key
                //  value           transaction.value
                //  gasLimit        transaction.gas
                //  gasPrice        by yourself
                //  from address    transaction.from
                //  to address      transaction.to

                // get nonce，create、sign、send transaction, then give the transactionHash to dapp
                showRequestInfoDialog("onEthSendTransaction", id)
            }

            onGetAccounts = { id: Long ->
                MainActivity.logger( "onGetAccounts")
                showRequestInfoDialog("onGetAccounts", id)
            }

            onFailure = {
                MainActivity.logger("onFailure ---  msg = ${it.message}")
                it.printStackTrace()
                mState = State.FAILURE
                var info: String? = null
                when(it) {
                    is UnknownHostException -> {
                        info = "UnknownHostException  msg = ${it.message}"
                    }
                    is InterruptedIOException -> {
                        info = "InterruptedIOException  msg = ${it.message}"

                    }
                }
                info?.let { str ->
                    showInfoDialog(str, "confirm") {
                        finish()
                    }
                }
            }
        }
    }

    private fun showRequestInfoDialog(methodName: String, id: Long) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setMessage(methodName)
                .setPositiveButton("OK") { dialog, which ->
                    // 返回值：
                    approveRequest(id, "")
                }
                .setNegativeButton("Cancel") { dialog: DialogInterface?, which: Int ->
                    rejectRequest(id)
                }
                .create().show()
        }
    }

    private fun <T> approveRequest(id: Long, result: T) {
        wcClient.approveRequest(id, result)
    }

    private fun rejectRequest(id: Long) {
        wcClient.rejectRequest(id, "cancel by user")
    }

    private fun showInfoDialog(msg: String, btnText: String, onClick: () -> Unit) {
        runOnUiThread {
            if (!isFinishing) {
                AlertDialog.Builder(this).setMessage(msg)
                    .setNegativeButton(btnText) { dialog, which ->
                        onClick()
                    }.create().show()
            }
        }
    }

    private fun WCClient.startConnect(info: String) {
        connect(session = WCSession.from(info)!!, peerMeta = mPeerMeta!!)
    }

    private fun close(kill: Boolean = true) {
        if (mState == State.OPEN) {
            if (kill) {
                wcClient.killSession()
            } else {
                wcClient.rejectSession("disconnect")
                finish()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        close()
    }

}