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
package io.chaldeaprjkt.gamespace.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import io.chaldeaprjkt.gamespace.utils.GameModeUtils
import javax.inject.Inject

import com.libremobileos.providers.LMOSettings

class SystemSettings @Inject constructor(
    context: Context,
    private val gameModeUtils: GameModeUtils
) {

    private val resolver = context.contentResolver

    private val handler = Handler(Looper.getMainLooper())
    private var edgeCutoutRunnable: Runnable? = null

    var headsUp
        get() =
            Settings.Global.getInt(resolver, Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED, 1) == 1
        set(it) {
            Settings.Global.putInt(
                resolver,
                Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED,
                it.toInt()
            )
        }

    var autoBrightness
        get() =
            Settings.System.getIntForUser(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
                UserHandle.USER_CURRENT
            ) ==
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        set(auto) {
            Settings.System.putIntForUser(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (auto) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                UserHandle.USER_CURRENT
            )
        }

    var threeScreenshot
        get() = Settings.System.getIntForUser(
            resolver, LMOSettings.System.THREE_FINGER_GESTURE, 0,
            UserHandle.USER_CURRENT
        ) == 1
        set(it) {
            Settings.System.putIntForUser(
                resolver, LMOSettings.System.THREE_FINGER_GESTURE,
                it.toInt(), UserHandle.USER_CURRENT
            )
        }

    var suppressFullscreenIntent
        get() = Settings.System.getIntForUser(
            resolver,
            LMOSettings.System.GAMESPACE_SUPPRESS_FULLSCREEN_INTENT,
            0,
            UserHandle.USER_CURRENT
        ) == 1
        set(it) {
            Settings.System.putIntForUser(
                resolver,
                LMOSettings.System.GAMESPACE_SUPPRESS_FULLSCREEN_INTENT,
                it.toInt(),
                UserHandle.USER_CURRENT
            )
        }

    var userGames
        get() =
            Settings.System.getStringForUser(
                resolver, LMOSettings.System.GAMESPACE_GAME_LIST,
                UserHandle.USER_CURRENT
            )
                ?.split(";")
                ?.toList()?.filter { it.isNotEmpty() }
                ?.map { UserGame.fromSettings(it) } ?: emptyList()
        set(games) {
            Settings.System.putStringForUser(
                resolver,
                LMOSettings.System.GAMESPACE_GAME_LIST,
                if (games.isEmpty()) "" else
                    games.joinToString(";") { it.toString() },
                UserHandle.USER_CURRENT
            )
            gameModeUtils.setupBatteryMode(games.isNotEmpty())
        }

    var edgeCutout: Boolean
        get() {
            val value = Settings.Secure.getIntForUser(
                resolver, LMOSettings.Secure.EDGE_CUTOUT, 0,
                UserHandle.USER_CURRENT
            ) == 1
            // If the edge cutout last update is executed successfully
            // then give the actual value.
            // Otherwise return false.
            // because we are updating setting with a delay.
            // So it may read the setting value before updating.
            // For example, closing and opening the game too quickly
            // will make gamespace consider edge cutout enabled in device by
            // default. In that case, it will enable the edge cutout system-wide
            // instead of only for the game.
            return edgeCutoutRunnable == null && value
        }
        set(value) {
            // Cancel if there is any previous runnable is waiting
            // to execute
            if (edgeCutoutRunnable != null) {
                handler.removeCallbacks(edgeCutoutRunnable!!)
                edgeCutoutRunnable = null
                return
            }
            edgeCutoutRunnable = Runnable {
                Settings.Secure.putIntForUser(
                    resolver, LMOSettings.Secure.EDGE_CUTOUT,
                    value.toInt(), UserHandle.USER_CURRENT
                )
                edgeCutoutRunnable = null
            }
            handler.postDelayed(edgeCutoutRunnable!!, 1000)
        }

    var doubleTapToSleep
        get() = Settings.System.getIntForUser(
                resolver, LMOSettings.System.DOUBLE_TAP_SLEEP_GESTURE,1,
                UserHandle.USER_CURRENT
            )==1
            set(it){
                Settings.System.putIntForUser(
                    resolver,LMOSettings.System.DOUBLE_TAP_SLEEP_GESTURE,
                    it.toInt(),UserHandle.USER_CURRENT
            )
        }

    private fun Boolean.toInt() = if (this) 1 else 0
}