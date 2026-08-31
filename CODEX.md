# Selective WebView CA Trust — project context

Android/LSPosed module for selective custom-CA trust in Chromium-based embedded
Android WebView. It targets Android 13+ and uses Kotlin with JDK 17.

## Current milestone

- `NetPrivacyHookEntry.kt` runs only in packages selected through LSPosed scope.
- `xposed/WebViewHookInstaller.kt` waits for `WebViewFactory.getProvider()`,
  obtains the provider ClassLoader and hooks compatible
  `org.chromium.net.X509Util.verifyServerCertificates` overloads. It falls back
  to `AndroidNetworkLibrary` only when the primary class has no compatible
  method.
- The hook is after-only and diagnostic. It must not modify `param.result`.
- `chromium/ChromiumMethodResolver.kt` discovers the DER chain plus the
  auth-type and host parameters without depending on the trailing Chromium
  arguments.
- `policy/DomainMatcher.kt` performs IDN-aware, label-boundary matching and
  selects the most-specific enabled rule. Unit tests live under
  `app/src/netprivacyTest`.
- `config/` provides storage-independent models for the later UI and trust
  implementation. The default state must remain disabled.

## Security boundaries

Do not hook `HostnameVerifier`, `SslErrorHandler`, OkHttp, Retrofit, Cronet,
or native BoringSSL. Do not alter the system trust store. Any reflection,
configuration, parsing, or trust-verification failure must leave Chromium's
original result untouched.

## Build

Run `./gradlew :app:testDebugUnitTest` from the repository root.
