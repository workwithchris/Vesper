# Preserve reflection target for StatusBarManager notification shade gesture
-dontwarn android.app.StatusBarManager
-keepclassmembers class android.app.StatusBarManager {
    public void expandNotificationsPanel();
}

# Preserve line numbers and source file names for crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
