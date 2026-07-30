# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.nimbus.weather.data.model.** { *; }
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
