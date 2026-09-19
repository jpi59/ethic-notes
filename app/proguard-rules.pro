# Ethic Notes: Zero-permission offline notes application
# Strip all verbose and debug logs in release builds to eliminate data leakage
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
