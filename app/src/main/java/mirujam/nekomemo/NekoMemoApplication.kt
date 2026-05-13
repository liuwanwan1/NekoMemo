package mirujam.nekomemo

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mirujam.nekomemo.ui.shared.SharedDataStore
import javax.inject.Inject

@HiltAndroidApp
class NekoMemoApplication : Application() {

    @Inject
    lateinit var sharedDataStore: SharedDataStore

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            try {
                val cleanedCount = sharedDataStore.cleanupOldFiles()
                if (cleanedCount > 0) {
                    Log.d("NekoMemoApplication", "Cleaned up $cleanedCount old temporary files")
                }

                Log.d("NekoMemoApplication", "Data file size: ${sharedDataStore.getDataSize()} bytes")
                Log.d("NekoMemoApplication", "Data available: ${sharedDataStore.isDataAvailable()}")
            } catch (e: Exception) {
                Log.e("NekoMemoApplication", "Error during cleanup", e)
            }
        }
    }
}
