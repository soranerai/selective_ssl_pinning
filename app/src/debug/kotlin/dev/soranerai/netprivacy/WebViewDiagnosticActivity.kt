package dev.soranerai.netprivacy

import android.app.Activity
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

/** Debug-only integration surface for exercising the provider ClassLoader hook. */
class WebViewDiagnosticActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(WebView(this).apply {
            settings.javaScriptEnabled = false
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            loadUrl(intent?.dataString ?: "https://self-signed.badssl.com/")
        })
    }
}
