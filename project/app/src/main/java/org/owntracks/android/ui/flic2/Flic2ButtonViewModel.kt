package org.owntracks.android.ui.flic2

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.owntracks.android.preferences.types.MonitoringMode
import javax.inject.Inject

@HiltViewModel
class Flic2ButtonViewModel @Inject constructor(private val flicButtonRepo: FlicButtonRepo) : ViewModel() {

    val connectionState: LiveData<Int>
        get() = flicButtonRepo.buttonConnectState

    fun connect(onSendLocation: () -> Unit) {
        flicButtonRepo.connect(onSendLocation)
    }

    fun disconnect() {
        flicButtonRepo.disconnect()
    }
}
