# Project-specific R8 rules.
# Compose, AndroidX, Room and Kotlin ship their own rules.

# The code is open source anyway, so keep real class/method names and line numbers:
# crash reports then point at the exact line. Unused code is still removed (small APK).
-dontobfuscate
-keepattributes SourceFile,LineNumberTable
