plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.expensetrackingsystem"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.kabuoanexpensetrackingsystem"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}