# BouncyCastle — keep crypto provider classes used via reflection
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# KotlinX Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Compose — keep @Composable metadata
-keep class androidx.compose.** { *; }

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep the app's model classes used in serialization
-keep class org.github.keepasscompose.core.model.** { *; }
