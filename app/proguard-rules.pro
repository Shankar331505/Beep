# TensorFlow Lite uses JNI and reflection internally.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
