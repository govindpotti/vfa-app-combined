# The app has no reflection-based entry points beyond the framework's own, so the
# defaults do the job. These keep the two things R8 can't see:

# org.json is used by VfaBackend to parse the analyzer/verifier responses.
-dontwarn org.json.**

# Compose keeps its own rules via consumer files; CameraX loads camera2 implementation
# classes by name at runtime.
-keep class androidx.camera.camera2.** { *; }
