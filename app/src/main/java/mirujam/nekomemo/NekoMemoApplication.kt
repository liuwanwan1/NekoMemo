package mirujam.nekomemo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import mirujam.nekomemo.ui.shared.SharedDataStore

@HiltAndroidApp
class NekoMemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SharedDataStore.init(this)
    }
}
