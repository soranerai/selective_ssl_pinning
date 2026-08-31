package dev.soranerai.netprivacy.xposed

import android.net.http.SslError
import android.webkit.SslErrorHandler
import dev.soranerai.netprivacy.NetPrivacyLog

/** Optional diagnostics only. It deliberately never calls proceed(). */
object WebViewSslErrorDiagnostic {
    fun onReceivedSslError(url: String?, error: SslError?, handler: SslErrorHandler?) {
        NetPrivacyLog.info("WebView SSL diagnostic url=${url.orEmpty()} primaryError=${error?.primaryError}")
        // Handler intentionally unused: certificate decisions belong to Chromium verification.
    }
}
