package org.owntracks.android.ui.mixins

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.owntracks.android.R
import timber.log.Timber

class LocationPermissionRequester(
    caller: ActivityResultCaller,
    private val permissionGrantedCallback: (code: Int) -> Unit,
    private val permissionDeniedCallback: (code: Int) -> Unit
) {

    private val permissionRequest =
        caller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Timber.d("Notification permission callback, result=$permissions ")
            val hasLocationPermission = permissions[ACCESS_COARSE_LOCATION] ?: false || permissions[ACCESS_FINE_LOCATION] ?: false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val hasBluetoothPermission = permissions[BLUETOOTH_CONNECT] ?: false && permissions[BLUETOOTH_SCAN] ?: false

                when {
                    hasLocationPermission && hasBluetoothPermission -> {
                        permissionGrantedCallback(this.code)
                    }
                    else -> {
                        permissionDeniedCallback(this.code)
                    }
                }
            } else {
                when {
                    hasLocationPermission -> {
                        permissionGrantedCallback(this.code)
                    }
                    else -> {
                        permissionDeniedCallback(this.code)
                    }
                }
            }
        }

    private var code: Int = 0

    /**
     * Request location permissions, optionally showing a request dialog first
     *
     * @param code that's passed back to the callback
     */
    fun requestLocationPermissions(
        code: Int = 0,
        context: Context,
        showPermissionRationale: (permissions: String) -> Boolean
    ) {
        Timber.d("Requesting Location Permissions with code=$code ")
        this.code = code
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                ACCESS_FINE_LOCATION,
                ACCESS_COARSE_LOCATION,
                BLUETOOTH_SCAN,
                BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                ACCESS_FINE_LOCATION,
                ACCESS_COARSE_LOCATION
            )
        }
        if (showPermissionRationale(ACCESS_FINE_LOCATION)) {
            // The user may have denied us once already, so show a rationale
            Timber.d("Showing Location permission rationale")
            val locationPermissionsRationaleAlertDialog = MaterialAlertDialogBuilder(context).setCancelable(true)
                .setIcon(R.drawable.ic_baseline_location_disabled_24)
                .setTitle(R.string.locationPermissionRequestDialogTitle)
                .setMessage(R.string.locationPermissionRequestDialogMessage)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    permissionRequest.launch(permissions, ActivityOptionsCompat.makeBasic())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    permissionDeniedCallback(this.code)
                }
                .create()
            if (!locationPermissionsRationaleAlertDialog.isShowing) {
                locationPermissionsRationaleAlertDialog.show()
            }
        } else {
            Timber.d("launching location permission request")
            // No need to show rationale, just request
            permissionRequest.launch(permissions)
        }
    }
}
