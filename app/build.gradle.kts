plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.barangay.bdms"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.barangay.bdms"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            val debugBaseUrl = (project.findProperty("BDMS_DEBUG_API_BASE_URL") as String?)
                ?: "http://10.0.2.2:5000/api/"
            buildConfigField("String", "API_BASE_URL", "\"$debugBaseUrl\"")
        }
        release {
            val releaseBaseUrl = (project.findProperty("BDMS_RELEASE_API_BASE_URL") as String?)
                ?: "https://REPLACE_WITH_PRODUCTION_API_URL/api/"
            buildConfigField("String", "API_BASE_URL", "\"$releaseBaseUrl\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.socketio.client) {
        exclude(group = "org.json", module = "json")
    }
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.security.crypto)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
