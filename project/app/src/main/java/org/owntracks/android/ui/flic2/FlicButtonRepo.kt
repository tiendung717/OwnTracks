package org.owntracks.android.ui.flic2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2ButtonListener
import io.flic.flic2libandroid.Flic2Manager
import io.flic.flic2libandroid.Flic2ScanCallback
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlicButtonRepo @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        const val FEATURE_ENABLED = true
    }

    private var flic2Button: Flic2Button? = null
    val buttonConnectState = MutableLiveData<Int>(Flic2Button.CONNECTION_STATE_DISCONNECTED)

    fun connect(onSendLocation: () -> Unit) {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasBluetoothScanPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val hasBluetoothConnectPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            hasBluetoothConnectPermission && hasBluetoothScanPermission
        } else {
            true
        }

        if (hasPermission) {
            Flic2Manager.getInstance().startScan(object : Flic2ScanCallback {
                override fun onDiscoveredAlreadyPairedButton(button: Flic2Button?) {
                    flic2Log("Button paired. Address: ${button?.bdAddr}")
                    setupFlic2Button(
                        button = button,
                        onSendLocation = onSendLocation
                    )
                }

                override fun onDiscovered(bdAddr: String?) {
                    flic2Log("Button discovered. Address: $bdAddr")
                }

                override fun onConnected() {
                    flic2Log("Button connected!")
                }

                override fun onComplete(result: Int, subCode: Int, button: Flic2Button?) {
                    if (result == Flic2ScanCallback.RESULT_SUCCESS) {
                        setupFlic2Button(
                            button = button,
                            onSendLocation = onSendLocation
                        )
                    } else {
                        flic2Log("Button scan failed: $subCode")
                    }
                }
            })
        } else {
            flic2Log("Button scan failed due to not enough permission!!")
        }
    }

    private fun setupFlic2Button(button: Flic2Button?, onSendLocation: () -> Unit) {
        // Stop scanning
        stopScan()

        // Update connection state
        updateConnectionState(button)

        flic2Button = button
        flic2Button?.addListener(object : Flic2ButtonListener() {
            override fun onButtonUpOrDown(
                button: Flic2Button?,
                wasQueued: Boolean,
                lastQueued: Boolean,
                timestamp: Long,
                isUp: Boolean,
                isDown: Boolean
            ) {
                when {
                    isUp -> {
                        flic2Log("Button up!")
                    }
                    isDown -> {
                        flic2Log("Button down! Send location")
                        onSendLocation()
                    }
                }
            }

            override fun onDisconnect(button: Flic2Button?) {
                super.onDisconnect(button)
                // Update connection state
                updateConnectionState(button)
            }
        })
    }

    private fun stopScan() {
        Flic2Manager.getInstance().stopScan()
    }

    private fun updateConnectionState(button: Flic2Button?) {
        button?.let { buttonConnectState.postValue(button.connectionState) }
    }

    fun disconnect() {
        if (flic2Button?.connectionState != Flic2Button.CONNECTION_STATE_DISCONNECTED) {
            flic2Button?.disconnectOrAbortPendingConnection()
            // Flic2Manager.getInstance().forgetButton(flic2Button)
        }
    }



    private fun flic2Log(message: String) {
        Timber.tag("nt.dung").d(message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
