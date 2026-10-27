import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.angel.ibanez.dehabitos"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.angel.ibanez.dehabitos"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        create("release") {
            val props = Properties()
            val localProps = rootProject.file("local.properties")
            if (localProps.exists()) {
                props.load(localProps.inputStream())
            }
            storeFile = file(
                props["RELEASE_STORE_FILE"]?.toString()
                    ?: System.getenv("RELEASE_STORE_FILE")
                    ?: "release.keystore"
            )
            storePassword = props["RELEASE_STORE_PASSWORD"]?.toString()
                ?: System.getenv("RELEASE_STORE_PASSWORD")
                ?: ""
            keyAlias = props["RELEASE_KEY_ALIAS"]?.toString()
                ?: System.getenv("RELEASE_KEY_ALIAS")
                ?: "release"
            keyPassword = props["RELEASE_KEY_PASSWORD"]?.toString()
                ?: System.getenv("RELEASE_KEY_PASSWORD")
                ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        viewBinding = true
    }
    sourceSets {
        getByName("main").assets {
            directories += "src/main/res/drawable/ICONOS"
        }
    }
    androidResources {
        noCompress += "tflite"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.android.material)
    implementation(libs.glide)
    implementation(libs.gson)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.sqlcipher)
    implementation(libs.androidx.sqlite)
    implementation(libs.tensorflow.lite)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.google.firebase.bom))
    implementation(libs.google.firebase.auth)
    implementation(libs.google.firebase.firestore)
    implementation(libs.google.firebase.messaging)

    implementation(libs.mikepenz.iconics.core)
    implementation(libs.mikepenz.iconics.views)
    implementation(libs.mikepenz.google.material.typeface.rounded) {
        artifact { type = "aar" }
    }

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
