package org.github.keepasscompose.core.common

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
object AndroidContextHolder {
    lateinit var applicationContext: Context
        private set

    private var activityRef: WeakReference<ComponentActivity>? = null

    val activity: ComponentActivity?
        get() = activityRef?.get()

    fun init(context: Context) {
        applicationContext = context.applicationContext
        if (context is ComponentActivity) {
            activityRef = WeakReference(context)
        }
    }
}
