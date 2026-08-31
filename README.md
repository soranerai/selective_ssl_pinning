# Selective WebView CA Trust

LSPosed module for observing and, in later milestones, selectively extending
certificate trust inside embedded Chromium Android WebView.

The first milestone only installs an after-hook on Chromium's certificate
verifier and logs the hostname, authentication type, result status and chain
length. It does not alter certificate verification or WebView behaviour.

The module never installs a certificate into the system trust store, bypasses
hostname verification, disables certificate validation, or hooks non-WebView
HTTP clients.

## Build

```bash
./gradlew :app:testDebugUnitTest
```

JDK 17 and an Android SDK are required.

The Zygisk build scaffold is documented in
[`docs/implementation-plan.md`](docs/implementation-plan.md).
