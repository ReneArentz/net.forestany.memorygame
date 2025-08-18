package net.forestany.memorygame.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import net.forestany.memorygame.R
import net.forestany.memorygame.utils.Util.errorSnackbar
import net.forestany.memorygame.utils.Util.notifySnackbar

class SettingsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment(this, findViewById(android.R.id.content), null))
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        // deactivate standard back button
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    /* execute anything, e.g. finish() - if nothing is here, nothing happens pushing main back button */
                    finish()
                }
            }
        )
    }

    class SettingsFragment(private val context: Context, private val view: View, private val anchorView: View?) : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
        private val sharedPreferencesHistory = mutableMapOf<String, Any?>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // store all shared preferences key values in a map as history
            PreferenceManager.getDefaultSharedPreferences(context).all.forEach {
                sharedPreferencesHistory[it.key] = it.value
                //Log.v(TAG, "${it.key} -> ${it.value.toString()}")
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            return when (preference.key) {
                "special_form_element" -> { true }
                else -> super.onPreferenceTreeClick(preference)
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (sharedPreferences == null || key == null) return

            var wrongValue = false
            var exceptionMessage = "Wrong value for settings."
            val value: Any? = sharedPreferences.all[key]

            // do nothing if current value is equal to history value
            if (this.sharedPreferencesHistory[key].toString().contentEquals(value.toString())) return

            //Log.v(TAG, "old $key -> ${this.sharedPreferencesHistory[key].toString()} < - - - > ${value.toString()}")

            // check values
            if (key.contentEquals("general_locale")) {
                // setting is 'de' or 'en'
                if ((value.toString().contentEquals("de")) || (value.toString().contentEquals("en"))) {
                    // restart app with new language setting after 1 second
                    notifySnackbar(message = getString(R.string.settings_language_changed, 5), view = view, anchorView = anchorView)

                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        requireActivity().finish()
                        Runtime.getRuntime().exit(0)
                    }, 5000)
                }
            }

            // entered value is wrong
            if (wrongValue) {
                // show error snackbar
                errorSnackbar(message = exceptionMessage, view = view, anchorView = anchorView)

                // manually update setting UI component
                when (val preference = findPreference<Preference>(key)) {
                    is EditTextPreference -> {
                        preference.text = sharedPreferencesHistory[key].toString()
                    }
                    is ListPreference -> {
                        preference.value = sharedPreferencesHistory[key].toString()
                    }
                    is Preference -> {
                        // nothing to do for password fields
                    }
                }
            } else {
                // update value is shared preferences history
                sharedPreferencesHistory[key] = value
            }
        }
    }
}