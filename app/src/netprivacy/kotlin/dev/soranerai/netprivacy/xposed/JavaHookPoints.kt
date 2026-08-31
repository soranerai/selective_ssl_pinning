package dev.soranerai.netprivacy.xposed

/** Named hook points kept separate from policy and certificate verification. */
object JavaHookPoints {
    const val WEBVIEW_FACTORY = "android.webkit.WebViewFactory"
    const val WEBVIEW_PROVIDER_METHOD = "getProvider"
    const val X509_UTIL = "org.chromium.net.X509Util"
    const val ANDROID_NETWORK_LIBRARY = "org.chromium.net.AndroidNetworkLibrary"
    const val VERIFY_METHOD = "verifyServerCertificates"
    const val WEBVIEW_CLIENT = "android.webkit.WebViewClient"
    const val SSL_ERROR_METHOD = "onReceivedSslError"
}

enum class VerificationHookMode {
    DIAGNOSTIC,
    SELECTIVE_CA,
}
