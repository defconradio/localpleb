# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
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

# --- Essential Proguard rules for typical Android/Kotlin/Compose/Hilt/Serialization projects ---

# Keep class names for kotlinx.serialization only (Gson and Moshi not used in this project)
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep data classes for serialization
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Compose generated classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Hilt classes
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**
-keep class javax.inject.** { *; }
-dontwarn javax.inject.**

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep all classes with @Keep annotation
-keep @androidx.annotation.Keep class * {*;}
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Keep Application class
-keep class com.example.pleb2.Pleb2Application { *; }

# Add more rules as needed for other libraries you use

# Keep errorprone annotations, which are sometimes needed by other libraries at runtime
-keep class com.google.errorprone.annotations.** { *; }
-dontwarn com.google.errorprone.annotations.**

# Keep SLF4J classes, as it's a common logging facade
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# Keep all classes in the z1 package and their members
-keep class z1.** { *; }

# Keep all classes with static initializers
-keepclassmembers class * {
    static <clinit>();
}

# Keep all classes in obfuscated packages seen in the stack trace
-keep class z1.** { *; }
-keep class y1.** { *; }
-keep class a.a.** { *; }
-keep class b1.** { *; }
-keep class q2.** { *; }
-keep class K2.** { *; }

# Keep all classes in your app's package
-keep class com.example.pleb2.** { *; }

# DEBUG: Keep all classes and members (for debugging only, remove after issue is found)
-keep class * { *; }

# Add missing dontwarn rules from missing_rules.txt
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn java.awt.Component
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Window
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn org.joda.time.Instant
