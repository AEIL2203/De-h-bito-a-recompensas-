# Se conservan todas las clases de la app: Firestore, Gson, Navigation,
# WorkManager e Iconics usan reflexion sobre nombres de clase.
-keep class com.angel.ibanez.dehabitos.** { *; }

# Kotlin metadata necesaria para reflexion de data classes
-keep class kotlin.Metadata { *; }

# SQLCipher: libsqlcipher.so accede por JNI a campos como mNativeHandle
# (crashea con NoSuchFieldError si R8 los renombra)
-keep class net.sqlcipher.** { *; }
-keep class net.zetetic.** { *; }

# TensorFlow Lite: la JNI nativa referencia clases/campos de Java
-keep class org.tensorflow.** { *; }
