package org.owntracks.android.ui.flic2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2ButtonListener
import io.flic.flic2libandroid.Flic2Manager
import io.flic.flic2libandroid.Flic2ScanCallback
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Flic2Button @Inject constructor() {

    companion object {
        const val FEATURE_ENABLED = true
    }

    private var flic2Button: Flic2Button? = null

    fun startScan(context: Context, onButtonSingleOrDoubleClickOrHold: () -> Unit) {
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
                    setupFlic2Button(
                        button = button,
                        onButtonSingleOrDoubleClickOrHold = onButtonSingleOrDoubleClickOrHold
                    )
                }

                override fun onDiscovered(bdAddr: String?) {
                    Timber.tag("nt.dung").d("Flic2Button discovered. Address: $bdAddr")
                }

                override fun onConnected() {
                    Timber.tag("nt.dung").d("Flic2Button connected!")
                }

                override fun onComplete(result: Int, subCode: Int, button: Flic2Button?) {
                    if (result == Flic2ScanCallback.RESULT_SUCCESS) {
                        setupFlic2Button(
                            button = button,
                            onButtonSingleOrDoubleClickOrHold = onButtonSingleOrDoubleClickOrHold
                        )
                    } else {
                        Timber.tag("nt.dung").e("Flic2Button scan failed: $subCode")
                    }
                }
            })
        } else {
            Timber.tag("nt.dung").e("Not enough permission!!")
        }
    }

    fun setupFlic2Button(button: Flic2Button?, onButtonSingleOrDoubleClickOrHold: () -> Unit) {
        flic2Button = button
        flic2Button?.addListener(object : Flic2ButtonListener() {
            override fun onButtonSingleOrDoubleClickOrHold(
                button: Flic2Button?,
                wasQueued: Boolean,
                lastQueued: Boolean,
                timestamp: Long,
                isSingleClick: Boolean,
                isDoubleClick: Boolean,
                isHold: Boolean
            ) {
                super.onButtonSingleOrDoubleClickOrHold(
                    button,
                    wasQueued,
                    lastQueued,
                    timestamp,
                    isSingleClick,
                    isDoubleClick,
                    isHold
                )
                onButtonSingleOrDoubleClickOrHold()
            }
        })
    }
}
