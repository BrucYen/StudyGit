# Keep serializable data models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.studyguard.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.studyguard.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
