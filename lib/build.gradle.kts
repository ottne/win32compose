import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    mingwX64("mingw")

    applyDefaultHierarchyTemplate()

    sourceSets {
        mingwMain.configure {
            dependencies {
                implementation(compose.runtime)
            }

            resources.srcDir("src/mingwMain/resources")
        }
        mingwTest {
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
