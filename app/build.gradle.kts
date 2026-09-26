plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}
android {
    namespace = "com.froydinger.breeze"
    compileSdk { version = release(37) { minorApiLevel = 1 } }
    defaultConfig {
        applicationId = "com.froydinger.breeze"
        minSdk = 29
        targetSdk = 36
        ndk { abiFilters += "arm64-v8a" }
        versionCode = 19
        versionName = "0.1.18"
        val supabaseKey = providers.environmentVariable("SUPABASE_ANON_KEY").orNull
            ?: rootProject.file(".supabase-anon-key").takeIf { it.isFile }?.readText()?.trim().orEmpty()
        require(supabaseKey.matches(Regex("[A-Za-z0-9._~-]*"))) { "Invalid Supabase public key format" }
        buildConfigField("String", "SUPABASE_URL", "\"https://sbvjjseitpahdpewsqqc.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseKey\"")
        manifestPlaceholders["authScheme"] = "com.froydinger.breeze"
        buildConfigField("String", "AUTH_CALLBACK_URI", "\"com.froydinger.breeze://auth-callback\"")
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".dev"; versionNameSuffix = "-dev"
            manifestPlaceholders["authScheme"] = "com.froydinger.breeze.dev"
            buildConfigField("String", "AUTH_CALLBACK_URI", "\"com.froydinger.breeze.dev://auth-callback\"")
            val token = providers.environmentVariable("BREEZE_CLIENT_TOKEN").orNull
                ?: rootProject.file(".breeze-client-token").takeIf { it.isFile }?.readText()?.trim().orEmpty()
            require(token.matches(Regex("[A-Za-z0-9._~+/=-]*"))) { "Invalid local development credential format" }
            buildConfigField("String", "CLOUD_TOKEN", "\"$token\"")
            buildConfigField("String", "CLOUD_URL", "\"https://breeze-chat.jakefroydinger.workers.dev/v1/mobile/responses\"")
        }
        release {
            manifestPlaceholders["authScheme"] = "com.froydinger.breeze"
            buildConfigField("String", "AUTH_CALLBACK_URI", "\"com.froydinger.breeze://auth-callback\"")
            isMinifyEnabled = false
            val token = providers.environmentVariable("BREEZE_CLIENT_TOKEN").orNull
                ?: rootProject.file(".breeze-client-token").takeIf { it.isFile }?.readText()?.trim().orEmpty()
            require(token.isNotEmpty()) { "Local Breeze Cloud client credential required for release" }
            require(token.matches(Regex("[A-Za-z0-9._~+/=-]+"))) { "Invalid local client credential format" }
            buildConfigField("String", "CLOUD_TOKEN", "\"$token\"")
            buildConfigField("String", "CLOUD_URL", "\"https://breeze-chat.jakefroydinger.workers.dev/v1/mobile/responses\"")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.browser:browser:1.10.0")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.webkit:webkit:1.15.0")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("org.mozilla.geckoview:geckoview:156.0.20260921121718")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
