package com.soundryn.music.core.design.motion

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun systemAnimationScale(context: Context): Flow<Float> = callbackFlow {
    val resolver = context.applicationContext.contentResolver
    fun readScale(): Float = Settings.Global.getFloat(
        resolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    trySend(readScale())
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            trySend(readScale())
        }
    }
    val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
    if (uri != null) {
        resolver.registerContentObserver(uri, false, observer)
    }
    awaitClose { resolver.unregisterContentObserver(observer) }
}.distinctUntilChanged()
