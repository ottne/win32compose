import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
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
    mingwX64("mingw") {
        binaries {
            executable {
                // Change to specify fully qualified name of your application"s entry point:
                entryPoint = "de.danotter.composewin32.sample.main"
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

    applyDefaultHierarchyTemplate()

    sourceSets {
        // Note: To enable common source sets please comment out "kotlin.import.noCommonSourceSets" property
        // in gradle.properties file and re-import your project in IDE.
        mingwMain.configure {
            dependencies {
                implementation(project(":lib"))
                implementation(compose.runtime)
            }

            resources.srcDir("src/mingwMain/resources")
        }
        mingwTest.configure {
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
