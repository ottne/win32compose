# Win32Compose

An experimental Kotlin/Native library for Win32 that lets you write Win32 user interfaces using the Compose compiler + runtime.

This was born out of personal interest for several topics:

* **Kotlin/Native** for compiling Kotlin code to native machine code
* the **Compose runtime**, which is separate from Compose UI, the library that is available to write user interfaces for Android and Skia
* **Win32**, the classic framework for writing user interfaces for the Windows operating system

The library is very incomplete with respect to supported features. Not all widgets are available yet. Notably, no 
layout helpers are available, which means that all widgets must be sized and placed manually.

## Sample code

```kotlin
import androidx.compose.runtime.*
import de.danotter.composewin32.*

fun main() {
    runWindowsApp {
        MyWindowsApp()
    }
}

@Composable
fun MyWindowsApp() {
    var counter by remember { mutableStateOf(0) }
   
    Window(
        title = "Compose Win32",
        width = 500,
        height = 600,
    ) {
       Label(
          title = "Hello world!",
          x = counter,
          y = 160,
          width = 100,
          height = 30,
       )

       Button(
          title = "Button1: $counter",
          x = 0,
          y = 100,
          width = 100,
          height = 50,
          onClick = {
             counter++
          }
       )
    }
}
```

A more complex example is contained inside the `sample` directory.

## License

Win32Compose is licensed under the MIT License. See the [LICENSE](LICENSE) file for more information.
