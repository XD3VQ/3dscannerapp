# ARCore
-keep class com.google.ar.** { *; }
-dontwarn com.google.ar.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# TensorFlow Lite
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# OpenCV
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# SceneView
-keep class io.github.sceneview.** { *; }
-dontwarn io.github.sceneview.**

# Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Models
-keep class com.wallscanner.pro.model.** { *; }
