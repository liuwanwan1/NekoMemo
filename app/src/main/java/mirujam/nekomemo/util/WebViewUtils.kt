package mirujam.nekomemo.util

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage

fun clearWebViewData(context: Context) {
    WebStorage.getInstance().deleteAllData()
    CookieManager.getInstance().removeAllCookies(null)
    CookieManager.getInstance().flush()
    deleteWebViewCache(context)
}

private fun deleteWebViewCache(context: Context) {
    try {
        val cacheDir = context.cacheDir
        val webviewCache = java.io.File(cacheDir, "webview")
        if (webviewCache.exists()) {
            webviewCache.deleteRecursively()
        }
    } catch (_: Exception) {
    }
}
