import com.android.build.api.dsl.Packaging

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "space.webkombinat.a_server"
    compileSdk = 36

    defaultConfig {
        applicationId = "space.webkombinat.a_server"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    packagingOptions {
        resources.excludes += setOf(
            "META-INF/INDEX.LIST",
            "META-INF/io.netty.versions.properties"
        )
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.documentfile)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("io.ktor:ktor-server-core:2.3.2") {
        exclude(group= "org.jetbrains.kotlin", module= "kotlin-stdlib-jdk8")
    }
    implementation("io.ktor:ktor-server-netty:2.3.2") {
        exclude( group="org.jetbrains.kotlin", module= "kotlin-stdlib-jdk8")
    }
    implementation("io.ktor:ktor-server-html-builder:2.3.2") {
        exclude(group = "org.jetbrains.kotlin", module="kotlin-stdlib-jdk8")
    }

    implementation("io.ktor:ktor-server-cors:2.3.12"){
        exclude(group = "org.jetbrains.kotlin", module="kotlin-stdlib-jdk8")
    }
    implementation("io.ktor:ktor-server-content-negotiation:2.3.2") {
        exclude(group = "org.jetbrains.kotlin", module="kotlin-stdlib-jdk8")
    }

    implementation(libs.ktor.serialization.kotlinx.json){
        exclude(group = "org.jetbrains.kotlin", module="kotlin-stdlib-jdk8")
    }
    implementation ("org.slf4j:slf4j-android:1.7.36")


    implementation(libs.navigation.compose)
    //  アイコン
    implementation(libs.androidx.material.icons.extended)

    //  Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)


    implementation (libs.androidx.datastore.preferences)

    implementation(libs.lifecycle.viewmodel.savedstate)

}