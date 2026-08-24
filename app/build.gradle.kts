plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val generatedReaderAssets = layout.buildDirectory.dir("generated/moriReaderAssets")
val prepareReaderAssets by tasks.registering(Sync::class) {
    from("src/main/assets")
    into(generatedReaderAssets)
    filesMatching("foliate-js/paginator.js") {
        filter { line ->
            if (line.trim() == "this.#iframe.src = src") {
                """                // MoriReader Android WebView compatibility: blob iframe navigation
                // can remain at about:blank, so materialize the same document via srcdoc.
                fetch(src).then(response => response.text()).then(html => {
                    this.#iframe.srcdoc = html
                }).catch(() => {
                    this.#iframe.src = src
                })"""
            } else {
                line
            }
        }
    }
    doLast {
        val paginator = generatedReaderAssets.get().file("foliate-js/paginator.js").asFile
        check(paginator.readText().contains("MoriReader Android WebView compatibility")) {
            "foliate-js paginator compatibility patch was not applied"
        }
    }
}

android {
    namespace = "io.github.shuixingqianfeng.morireader"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.shuixingqianfeng.morireader"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions.jvmTarget = "17"

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    sourceSets.getByName("main").assets.setSrcDirs(listOf(generatedReaderAssets))
}

tasks.named("preBuild").configure { dependsOn(prepareReaderAssets) }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    // 1.5.3 is the newest Haze line compatible with compileSdk 35. Newer
    // releases are built against Android 36 / Compose 1.10.
    implementation("dev.chrisbanes.haze:haze:1.5.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}
