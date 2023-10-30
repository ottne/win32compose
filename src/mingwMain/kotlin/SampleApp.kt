@file:OptIn(ExperimentalForeignApi::class)

package sample

import androidx.compose.runtime.*
import kotlinx.cinterop.*
import platform.windows.*


@Composable
fun SampleApp() {

    var counter1 by remember { mutableStateOf(0) }
    var counter2 by remember { mutableStateOf(0) }

    Window(
        title = "Compose Win32",
        width = 500,
        height = 600,
    ) {
        Label(
            title = "Hello world!",
            x = counter1,
            y = 60,
            width = 100,
            height = 30,
        )

        Button(
            title = "Button1: $counter1",
            x = 0,
            y = 0,
            width = 100,
            height = 50,
            onClick = {
                counter1++
            }
        )

        Button(
            title = "Button2: $counter2",
            x = 120,
            y = 0,
            width = 100,
            height = 50,
            onClick = {
                counter2++
            }
        )

        val listItems = remember {
            List(50) { i ->
                "Item $i"
            }
        }
        ListBox(
            items = listItems,
            x = 300,
            y = 300,
            width = 150,
            height = 200,
        )
    }
}