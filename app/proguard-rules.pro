# LibXposed discovers the entry point from META-INF/xposed/java_init.list. R8 cannot see
# that reference, so keep the module's code and names intact in release APKs.
# This also protects reflection-based Android API hooks from being stripped.
-keep class dev.soranerai.netprivacy.** { *; }

# Keep Xposed API types
-keep class io.github.libxposed.api.** { *; }
-dontwarn io.github.libxposed.annotation.**

# JNA — UniFFI Kotlin bindings call Rust via JNA. Native.initIDs looks up
# com.sun.jna.Pointer.peer via JNI at init time; R8 renaming these classes
# or their fields makes it fail with UnsatisfiedLinkError: "Can't obtain
# peer field ID for class com.sun.jna.Pointer". Rules lifted from the
# JNA FAQ (https://github.com/java-native-access/jna/blob/master/www/
# FrequentlyAskedQuestions.md#jna-on-android). Gobley generates the same
# rules into build/generated/uniffi/.../generated-proguard-rules.txt but
# its auto-wiring into the app's R8 configuration doesn't fire for pure
# Android (non-KMP) applications, so we keep them here directly.
-dontwarn java.awt.*
-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }
