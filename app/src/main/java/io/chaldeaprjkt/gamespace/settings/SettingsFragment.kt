/*
 * Copyright (C) 2021 Chaldeaprjkt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.chaldeaprjkt.gamespace.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import dagger.hilt.android.AndroidEntryPoint
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.preferences.AppListPreferences
import io.chaldeaprjkt.gamespace.preferences.appselector.AppSelectorActivity
import javax.inject.Inject

import com.libremobileos.providers.LMOSettings

import vendor.lineage.fastcharge.V1_0.IFastCharge

@AndroidEntryPoint(PreferenceFragmentCompat::class)
class SettingsFragment : Hilt_SettingsFragment(), Preference.OnPreferenceChangeListener {
    @Inject
    lateinit var settings: SystemSettings

    private val TAG = "GameSpaceSettingsFragment"

    private var apps: AppListPreferences? = null

    private val selectorResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            apps?.useSelectorResult(it)
        }

    private val perAppResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            apps?.usePerAppResult(it)
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        apps = findPreference(LMOSettings.System.GAMESPACE_GAME_LIST)
        apps?.onRegisteredAppClick {
            perAppResult.launch(Intent(context, PerAppSettingsActivity::class.java).apply {
                putExtra(PerAppSettingsActivity.EXTRA_PACKAGE, it)
            })
        }

        findPreference<Preference>(AppListPreferences.KEY_ADD_GAME)
            ?.setOnPreferenceClickListener {
                selectorResult.launch(Intent(context, AppSelectorActivity::class.java))
                return@setOnPreferenceClickListener true
            }

        findPreference<SwitchPreference>(LMOSettings.System.GAMESPACE_SUPPRESS_FULLSCREEN_INTENT)?.apply {
            isChecked = settings.suppressFullscreenIntent
            onPreferenceChangeListener = this@SettingsFragment
        }

        findPreference<SwitchPreference>(AppSettings.KEY_EDGE_CUTOUT)?.apply {
            try {
                context?.resources?.getString(com.android.internal.R.string.config_edge_cutout_overlay_package)?.let {
                    context?.packageManager?.getPackageInfo(it, 0)
                }
                    ?.let {
                        isVisible = true
                    } ?: run {
                        isVisible = false
                    }
            } catch (e: PackageManager.NameNotFoundException) {
                isVisible = false
            }
        }

        findPreference<SwitchPreference>(AppSettings.KEY_FAST_CHARGE_ENABLER)?.apply {
            setOnPreferenceChangeListener { preference, newValue ->
                val isChecked = newValue as Boolean
                if (isChecked) {
                    AlertDialog.Builder(context)
                        .setTitle(R.string.fast_charge_enabled_warning_title)
                        .setMessage(R.string.fast_charge_enabled_warning_message)
                        .setIcon(R.drawable.ic_battery_alert)
                        .setCancelable(false)
                        .setPositiveButton(R.string.fast_charge_enabled_warning_confirm) { _, _ ->
                            // do nothing
                        }
                        .setNegativeButton(R.string.fast_charge_enabled_warning_cancel) { _, _ ->
                            (preference as SwitchPreference).isChecked = false
                        }
                        .show()
                }
                true
            }
            try {
                val fastCharge = IFastCharge.getService()
                isVisible = fastCharge != null
                // consider disable if not supported by device.
                // since we enable it by default, it avoids trying to
                // set fastcharge state in unsupported devices.
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to get IFastCharge service", e)
                isVisible = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        apps?.updateAppList()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            LMOSettings.System.GAMESPACE_SUPPRESS_FULLSCREEN_INTENT -> {
                settings.suppressFullscreenIntent = newValue as Boolean
                return true
            }
        }
        return false
    }
}
