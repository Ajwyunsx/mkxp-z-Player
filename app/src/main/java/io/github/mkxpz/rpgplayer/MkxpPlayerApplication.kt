package io.github.mkxpz.rpgplayer

import android.app.Application
import io.github.mkxpz.rpgplayer.data.AppContainer

class MkxpPlayerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
