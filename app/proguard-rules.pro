-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class mirujam.nekomemo.data.local.entity.** { *; }
-keep class mirujam.nekomemo.data.local.Converters { *; }

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

-dontwarn androidx.room.**

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

-keep class org.jsoup.** { *; }
-keep interface org.jsoup.** { *; }
-dontwarn org.jsoup.**

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}
