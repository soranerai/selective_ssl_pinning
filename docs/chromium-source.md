# Chromium source baseline

The project keeps a local, shallow sparse checkout of official Chromium at
`third_party/chromium-src`. It is intentionally ignored by git.

- Remote: `https://chromium.googlesource.com/chromium/src.git`
- Revision: `e0e14699` (recorded 2026-08-31)
- Sparse paths: `android_webview`, `base/android`, `build/config/chromium`, `chrome/android`,
  `content/browser`, `net`, `services/network`.

For Android WebView, the certificate-verification route to review before compatibility changes is:

```text
net/cert/cert_verify_proc_android.cc
  -> net/android/network_library.cc::VerifyX509CertChain
  -> org.chromium.net.AndroidNetworkLibrary.verifyServerCertificates
  -> org.chromium.net.X509Util.verifyServerCertificates
```

Installed Chrome/WebView providers can differ from this source revision and may R8-obfuscate
their Java classes. Any provider-specific candidate must first be validated by a diagnostic hook.

## Chrome Android M152

The exact installed Chrome tag `152.0.7977.64` was fetched locally as commit `506c834ec`.
Its `net/features.gni` declares `chrome_root_store_optional = is_android && !is_cronet_build`
and explicitly notes that WebView, unlike Chrome Android, uses the Android cert verifier.

Chrome Android's active certificate route is therefore expected to be native:

```text
net::CertVerifier
  -> CertVerifyProc::CreateBuiltinWithChromeRootStore
  -> net::CertVerifyProcBuiltin::VerifyInternal
```

`CertVerifyProcBuiltin::VerifyInternal` is implemented in
`net/cert/cert_verify_proc_builtin.cc` (line 1648 in tag `152.0.7977.64`). It is a stripped,
native `libchrome.so` target, so it is not covered by the WebView Java hook implementation.

Runtime validation on Chrome `152.0.7977.64` recorded
`CERT_VERIFY_PROC_CHROME_ROOT_STORE_VERSION` events in Chrome NetLog while loading a
self-signed test endpoint. Those events are emitted by the builtin verifier path, confirming
that the Chrome Java candidates (`AndroidNetworkLibrary`, `mou`, and `iou`) are not the active
certificate-verification path for this build.
