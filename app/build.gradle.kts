plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.exemple.manettes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.exemple.manettes"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
```[span_0](start_span)[span_0](end_span)

### 2. `build.gradle.kts` (à la racine du dépôt)
```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
```[span_1](start_span)[span_1](end_span)

### 3. `settings.gradle.kts` (à la racine du dépôt)
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "manettes-app"
include(":app")
```[span_2](start_span)[span_2](end_span)

### 4. `.github/workflows/build.yml`
```yaml
name: Build APK
on: [push, workflow_dispatch]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: "8.9"
      - run: gradle assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: apk
          path: app/build/outputs/apk/debug/*.apk
```[span_3](start_span)[span_3](end_span)

http://googleusercontent.com/lmdx_content/lcWIPDTpxnTTjKPqSbPZJwRVZtMUhgXgpZeOhAVplWeAjGznFUYDYGnBGRIrfstGkrgyImXAFinyksTxrzLWsvCrdUMPiTWskMbbJhdacpmsJemtIrpHUrbvYoPmcTLhPHyKPsICZKPlAhfepltIbfPQYcbjJUsfqCCPohCvaMHXzqtIZGkQmLwvkAXeQJRGxVeluFNbjBQWcAJVPCLPeGrceOpHKPpnHzaBliCFDmbWayVGdCJGxaBkTZyPRrcfXjGsCYLJITaUfFEIHxGFoRsjWApkzzSWvIBIcrrKhGxsoEKnRTPDzOcyDfMDPxE195306
