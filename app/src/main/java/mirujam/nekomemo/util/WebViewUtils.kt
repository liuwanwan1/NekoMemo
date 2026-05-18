package mirujam.nekomemo.util

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

fun clearWebViewData(context: Context) {
    WebView(context).apply {
        clearCache(true)
        clearHistory()
        clearFormData()
    }
    WebStorage.getInstance().deleteAllData()
    CookieManager.getInstance().removeAllCookies(null)
    CookieManager.getInstance().flush()
}
