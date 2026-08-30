# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep public JavaScript interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep @android.webkit.JavascriptInterface class * { *; }

# Keep our entry points
-keep class com.github.catvod.spider.WebHome { *; }
-keep class com.fongmi.web.FmBridge { *; }
-keep class com.fongmi.web.FmActionHandler { *; }
-keep class com.fongmi.web.DefaultFmActionHandler { *; }
-keep class com.fongmi.web.PlayerGateway { *; }
-keep class com.fongmi.web.FmController { *; }
-keep class com.fongmi.web.FmHttpResponse { *; }
-keep class com.fongmi.web.WebHomeChrome { *; }
-keep class com.fongmi.web.WebResource { *; }

# Keep public methods of public types
-keepclassmembers class com.fongmi.web.** {
    public *;
}
-keepclassmembers class com.github.catvod.spider.** {
    public *;
}
