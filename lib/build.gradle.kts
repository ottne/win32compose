import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    mingwX64("mingw") {
    }
    sourceSets {
        val mingwMain by getting {
            dependencies {
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
