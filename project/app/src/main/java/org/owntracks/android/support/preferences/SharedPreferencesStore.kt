package org.owntracks.android.support.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.qualifier.AppContext
import org.owntracks.android.injection.scopes.PerApplication
import org.owntracks.android.services.MessageProcessorEndpointHttp
import org.owntracks.android.services.MessageProcessorEndpointMqtt
import java.util.*
import javax.inject.Inject

/***
 * Implements a PreferencesStore that uses a SharedPreferecnces as a backend.
 */
@PerApplication
class SharedPreferencesStore @Inject constructor(@AppContext c: Context) : PreferencesStore {
    private lateinit var sharedPreferencesName: String
    private val activeSharedPreferencesChangeListener = LinkedList<OnModeChangedPreferenceChangedListener>()
    private val FILENAME_PRIVATE = "org.owntracks.android.preferences.private"
    private val FILENAME_HTTP = "org.owntracks.android.preferences.http"

    private lateinit var activeSharedPreferences: SharedPreferences
    private val commonSharedPreferences: SharedPreferences

    private val privateSharedPreferences: SharedPreferences
    private val httpSharedPreferences: SharedPreferences

    init {
        commonSharedPreferences = PreferenceManager.getDefaultSharedPreferences(c) // only used for modeId and firstStart keys
        privateSharedPreferences = c.getSharedPreferences(FILENAME_PRIVATE, Context.MODE_PRIVATE)
        httpSharedPreferences = c.getSharedPreferences(FILENAME_HTTP, Context.MODE_PRIVATE)
    }


    override fun putString(key: String, value: String) {
        activeSharedPreferences.edit().putString(key, value).apply()
    }

    override fun getString(key: String, default: String): String? {
        return activeSharedPreferences.getString(key, default)
    }

    override fun remove(key: String) {
        activeSharedPreferences.edit().remove(key).apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return activeSharedPreferences.getBoolean(key, default)
    }

    override fun putBoolean(key: String, value: Boolean) {
        activeSharedPreferences.edit().putBoolean(key, value).apply()
    }

    override fun getInt(key: String, default: Int): Int {
        return activeSharedPreferences.getInt(key, default)
    }

    override fun putInt(key: String, value: Int) {
        activeSharedPreferences.edit().putInt(key, value).apply()
    }

    override fun getSharedPreferencesName(): String {
        return sharedPreferencesName
    }

    override fun getInitMode(key: String, default: Int): Int {
        return commonSharedPreferences.getInt(key, default)
    }

    override fun setMode(key: String, mode: Int) {
        detachAllActivePreferenceChangeListeners()
        when (mode) {
            MessageProcessorEndpointMqtt.MODE_ID -> {
                activeSharedPreferences = privateSharedPreferences
                sharedPreferencesName = FILENAME_PRIVATE
            }
            MessageProcessorEndpointHttp.MODE_ID -> {
                activeSharedPreferences = httpSharedPreferences
                sharedPreferencesName = FILENAME_HTTP
            }
        }
        commonSharedPreferences.edit().putInt(key, mode).apply()
        // Mode switcher reads from currently active sharedPreferences, so we commit the value to all
        privateSharedPreferences.edit().putInt(key, mode).apply()
        httpSharedPreferences.edit().putInt(key, mode).apply()

        attachAllActivePreferenceChangeListeners()
    }

    override fun registerOnSharedPreferenceChangeListener(listenerModeChanged: OnModeChangedPreferenceChangedListener) {
        activeSharedPreferences.registerOnSharedPreferenceChangeListener(listenerModeChanged)
        activeSharedPreferencesChangeListener.push(listenerModeChanged)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listenerModeChanged: OnModeChangedPreferenceChangedListener) {
        activeSharedPreferences.unregisterOnSharedPreferenceChangeListener(listenerModeChanged)
        activeSharedPreferencesChangeListener.remove(listenerModeChanged)
    }

    private fun detachAllActivePreferenceChangeListeners() {
        activeSharedPreferencesChangeListener.forEach { activeSharedPreferences.unregisterOnSharedPreferenceChangeListener(it) }
    }

    private fun attachAllActivePreferenceChangeListeners() {
        activeSharedPreferencesChangeListener.forEach {
            activeSharedPreferences.registerOnSharedPreferenceChangeListener(it)
            it.onAttachAfterModeChanged()
        }
    }
}


@Module
abstract class SharedPreferencesStoreModule {
    @Binds
    abstract fun bindSharedPreferencesStoreModule(sharedPreferencesStore: SharedPreferencesStore): PreferencesStore
}