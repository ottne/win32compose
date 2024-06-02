package de.danotter.composewin32.sample

import androidx.compose.runtime.*
import de.danotter.composewin32.*


@Composable
fun SampleApp() {

    var counter1 by remember { mutableStateOf(0) }
    var counter2 by remember { mutableStateOf(0) }

    var selectedTab by remember { mutableIntStateOf(0) }

    var showDetailWindow by remember { mutableStateOf(false) }

    Window(
        title = "Compose Win32",
        width = 500,
        height = 600,
    ) {
        TabControl(
            0, 0, 500, 600,
            onSelectIndex = { selectedIndex ->
                println("Selected tab: $selectedIndex")

                selectedTab = selectedIndex
            }
        )

        val firstTab = movableContentOf {
            Label(
                title = "Hello world!",
                x = counter1,
                y = 160,
                width = 100,
                height = 30,
            )

            Button(
                title = "Button1: $counter1",
                x = 20,
                y = 100,
                width = 100,
                height = 50,
                onClick = {
                    counter1++
                }
            )

            Button(
                title = "Button2: $counter2",
                x = 140,
                y = 100,
                width = 100,
                height = 50,
                onClick = {
                    counter2++
                }
            )

            Button(
                title = "Open detail window",
                x = 20,
                y = 300,
                width = 100,
                height = 30,
                onClick = {
                    showDetailWindow = true
                }
            )
        }

        val secondTab = movableContentOf {
            var selectedListIndex by remember {
                mutableStateOf<Int?>(null)
            }
            Label(
                title = "Selected index: ${selectedListIndex ?: "none"}",
                x = 150,
                y = 300,
                width = 130,
                height = 20
            )

            val listItems = remember {
                List(50) { i ->
                    "Item $i"
                }
            }
            ListBox(
                listItems.size,
                title = { i ->
                    listItems[i]
                },
                x = 300,
                y = 300,
                width = 150,
                height = 200,
                onSelectIndex = {
                    selectedListIndex = it
                }
            )

            var isChecked by remember { mutableStateOf(false) }
            CheckBox(
                rect = Rect(
                    x = 100,
                    y = 100,
                    width = 150,
                    height = 30
                ),
                isChecked = false,
                title = "Checkbox (${if (isChecked) "checked" else "unchecked"})",
                onCheckedChange = {
                    isChecked = !isChecked
                }
            )
        }

        when (selectedTab) {
            0 -> {
                firstTab()
            }

            1 -> {
                secondTab()
            }
        }
    }

    if (showDetailWindow) {
        SecondWindow()
    }
}

@Composable
fun SecondWindow() {
    Window(
        title = "Detail window",
        width = 200,
        height = 100
    ) {
        Label(
            "Second window content",
            0,
            0,
            100,
            100
        )
    }
}
