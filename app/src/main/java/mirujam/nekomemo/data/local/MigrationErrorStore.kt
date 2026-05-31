package mirujam.nekomemo.data.local

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationErrorStore @Inject constructor() {
    @Volatile
    private var lastError: String? = null

    @Volatile
    private var hasMigrationFailed: Boolean = false

    fun recordError(error: String) {
        lastError = error
        hasMigrationFailed = true
        Timber.e("Database migration failed: $error")
    }

    fun hasFailed(): Boolean = hasMigrationFailed

    fun getLastError(): String? = lastError

    fun clearError() {
        lastError = null
        hasMigrationFailed = false
    }
}
