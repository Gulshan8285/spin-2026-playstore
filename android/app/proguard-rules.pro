# Keep Firebase & Data Models for serialization
-keep class com.spinwin.rewards.data.model.** { *; }

# AdMob Lite
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Google Sign In
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep interface com.google.android.gms.auth.api.signin.** { *; }
-dontwarn com.google.android.gms.auth.api.signin.**

# Firebase Services
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class io.grpc.** { *; }
-dontwarn io.grpc.**
