import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.9.0"
    id("org.jetbrains.compose") version "1.5.3"
}

repositories {
    mavenCentral()
}

val resFile = file("$buildDir/res/kotlinwin32.res")
val konanUserDir = System.getenv("KONAN_DATA_DIR") ?: "${System.getProperty("user.home")}/.konan"

tasks.register<Exec>("windowsResources") {
    val rcFile = file("main.rc")
    val path = System.getenv("PATH")

    val windresDir = "$konanUserDir/dependencies/msys2-mingw-w64-x86_64-2/bin"

    commandLine("$windresDir/windres", "-i", rcFile, "-o", resFile)
    environment("PATH", "$windresDir;$path")

    inputs.file(rcFile)
    outputs.file(resFile)
}

kotlin {
    // For ARM, should be changed to iosArm32 or iosArm64
    // For Linux, should be changed to e.g. linuxX64
    // For MacOS, should be changed to e.g. macosX64
    // For Windows, should be changed to e.g. mingwX64
    mingwX64("mingw") {
        binaries {
            executable {
                // Change to specify fully qualified name of your application"s entry point:
                entryPoint = "sample.main"
                // Specify command-line arguments, if necessary:
                runTask?.args("")
                linkerOpts("-Wl,--subsystem,windows")
                linkerOpts("$resFile")
            }
        }
        compilations.configureEach {
            tasks.named("compileKotlinMingw").configure {
                dependsOn("windowsResources")
                inputs.files(resFile)
            }
        }
    }
    sourceSets {
        // Note: To enable common source sets please comment out "kotlin.import.noCommonSourceSets" property
        // in gradle.properties file and re-import your project in IDE.
        val mingwMain by getting {
            dependencies {
                implementation(project(":lib"))
                implementation(compose.runtime)
            }

            resources.srcDir("src/mingwMain/resources")
        }
        val mingwTest by getting {
        }
    }
}

// Use the following Gradle tasks to run your application:
// :runReleaseExecutableMingw - without debug symbols
// :runDebugExecutableMingw - with debug symbols

tasks.named("compileKotlinMingw", KotlinCompilationTask::class.java) {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xopt-in=kotlinx.cinterop.ExperimentalForeignApi",
        )
    }
}
