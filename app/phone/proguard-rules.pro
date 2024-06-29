# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepnames class org.askartv.phone.models.PlayerItem

# ProGuard thinks all SettingsFragments are unused
-keep class org.askartv.phone.fragments.SettingsLanguageFragment
-keep class org.askartv.phone.fragments.SettingsAppearanceFragment
-keep class org.askartv.phone.fragments.SettingsDownloadsFragment
-keep class org.askartv.phone.fragments.SettingsPlayerFragment
-keep class org.askartv.phone.fragments.SettingsDeviceFragment
-keep class org.askartv.phone.fragments.SettingsCacheFragment
-keep class org.askartv.phone.fragments.SettingsNetworkFragment

# These classes are from okhttp and are not used in Android
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE