plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.project"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.project"
        minSdk = 33
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src\\main\\assets\\fonts", "src\\main\\assets\\fonts")
            }
        }
    }
}
dependencies {
    //implementation("com.jakewharton:butterknife:10.2.3")
    implementation("androidx.databinding:databinding-runtime:8.4.1")
    //annotationProcessor("com.jakewharton:butterknife-compiler:10.2.3")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.github.niqdev:ipcam-view:2.4.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
