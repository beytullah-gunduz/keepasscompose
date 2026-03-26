package org.github.keepasscompose

import android.app.Application
import org.github.keepasscompose.core.common.AndroidContextHolder
import org.github.keepasscompose.di.initKoin

class KeePassApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContextHolder.init(this)
        initKoin()
    }
}
