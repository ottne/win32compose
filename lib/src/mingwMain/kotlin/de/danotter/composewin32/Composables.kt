package de.danotter.composewin32

import androidx.compose.runtime.*
import kotlinx.cinterop.*
import platform.windows.*

private const val WindowDefault = CW_USEDEFAULT

class WindowScope internal constructor(
    internal val windowHandle: WindowHandle
)

@Composable
fun Window(
    title: String? = null,
    x: Int = WindowDefault,
    y: Int = WindowDefault,
    width: Int = WindowDefault,
    height: Int = WindowDefault,
    content: @Composable WindowScope.() -> Unit
) {
    val node = remember {
        WindowNode(
            x = x,
            y = y,
            width = width,
            height = height
        )
    }

    val windowScope = remember(node) { WindowScope(node.windowHandle) }
    ComposeNode<WindowNode, WindowsApplier>(
        factory = {
            node
        },
        update = {
            set(title) {
                this.title = it
            }
        },
        content =  {
            windowScope.content()
        }
    )
}

@Composable
fun WindowScope.Button(
    title: String? = null,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    onClick: (() -> Unit)? = null
) {
    ComposeNode<ChildNode, WindowsApplier>(
        factory = {
            ChildNode(
                parentHwnd = windowHandle.rawValue,
                className = "BUTTON",
                style = WS_TABSTOP or WS_VISIBLE or WS_CHILD or BS_PUSHBUTTON,
                exStyle = 0,
                x = x,
                y = y,
                width = width,
                height = height,
                initialTitle = title,
                onWidgetCreated = { hButton ->
                    memScoped {
                        val bi: BUTTON_IMAGELIST = alloc()
                        bi.himl = null
                        bi.uAlign = BUTTON_IMAGELIST_ALIGN_LEFT.toUInt()
                        SendMessage(hButton, BCM_SETIMAGELIST.toUInt(), 0UL, bi.ptr.rawValue.toLong())
                    }
                },
                onCommand = { _, command, _ ->
                    if (command == BN_CLICKED) {
                        onClick?.invoke()
                    }
                },
            )
        },
        update = {
            set(title) {
                this.title = it
            }
            set(x) {
                this.x = x
            }
            set(y) {
                this.y = y
            }
            set(width) {
                this.width = width
            }
            set(height) {
                this.height = height
            }
        }
    )
}

@Composable
fun WindowScope.Label(
    title: String? = null,
    x: Int,
    y: Int,
    width: Int,
    height: Int
) {
    ComposeNode<ChildNode, WindowsApplier>(
        factory = {
            ChildNode(
                parentHwnd = windowHandle.rawValue,
                className = "STATIC",
                style = WS_VISIBLE or WS_CHILD or SS_LEFT,
                x = x,
                y = y,
                width = width,
                height = height,
                initialTitle = title,
            )
        },
        update = {
            set(title) {
                this.title = it
            }
            set(x) {
                this.x = x
            }
            set(y) {
                this.y = y
            }
            set(width) {
                this.width = width
            }
            set(height) {
                this.height = height
            }
        }
    )
}

@Composable
fun WindowScope.ListBox(
    itemCount: Int,
    title: (n: Int) -> String,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    onSelectIndex: ((index: Int) -> Unit)? = null
) {
    var hCurrentListBox by remember { mutableLongStateOf(0) }
    val lastTitle by rememberUpdatedState(title)
    LaunchedEffect(hCurrentListBox, itemCount) {
        if (hCurrentListBox == 0L) return@LaunchedEffect

        // Todo check if title callback reads states with SnapshotStateObserver
        for (i in 0..<itemCount) {
            val titleString = lastTitle(i)
            titleString.usePinned { pinnedItem ->
                SendMessageW(
                    hWnd = hCurrentListBox.toCPointer(),
                    Msg = LB_ADDSTRING.toUInt(),
                    wParam = 0u,
                    lParam = pinnedItem.addressOf(0).toLong()
                )
            }
        }
    }

    ComposeNode<ChildNode, WindowsApplier>(
        factory = {
            ChildNode(
                parentHwnd = windowHandle.rawValue,
                className = "LISTBOX",
                style = WS_CHILD or WS_VISIBLE or WS_VSCROLL or LBS_NOTIFY,
                exStyle = WS_EX_CLIENTEDGE,
                x = x,
                y = y,
                width = width,
                height = height,
                initialTitle = null,
                onWidgetCreated = { hListBox ->
                    hCurrentListBox = hListBox.toLong()
                },
                onCommand = { hChild, cmdCode, _ ->
                    if (cmdCode == LBN_SELCHANGE) {
                        val selectedIndex = SendMessage(hChild, LB_GETCURSEL.toUInt(), 0u, 0).toInt()
                        if (selectedIndex != LB_ERR) {
                            onSelectIndex?.invoke(selectedIndex)
                        }
                    }
                },
            )
        },
        update = {
            set(x) {
                this.x = x
            }
            set(y) {
                this.y = y
            }
            set(width) {
                this.width = width
            }
            set(height) {
                this.height = height
            }
        }
    )
}

@Composable
fun WindowScope.TabControl(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    onSelectIndex: (Int) -> Unit
) {
    ComposeNode<ChildNode, WindowsApplier>(
        factory = {
            ChildNode(
                windowHandle.rawValue,
                className = WC_TABCONTROLA,
                style = WS_CHILD or WS_VISIBLE,
                exStyle = 0,
                x = x,
                y = y,
                width = width,
                height = height,
                onWidgetCreated = { hTabCtrl ->
                    memScoped {
                        val tie: TCITEMW = alloc()
                        tie.mask = TCIF_TEXT.toUInt()

                        tie.pszText = "Tab 1".wcstr.ptr
                        SendMessage(hTabCtrl, TCM_INSERTITEM.toUInt(), 0UL, tie.ptr.toLong())

                        tie.pszText = "Tab 2".wcstr.ptr
                        SendMessage(hTabCtrl, TCM_INSERTITEM.toUInt(), 1UL, tie.ptr.toLong())
                    }
                },
                onNotify = { hTabCtrl, nmhdr ->
                    when (nmhdr.code) {
                        TCN_SELCHANGE -> {
                            val selectedIndex = SendMessage(hTabCtrl, TCM_GETCURSEL.toUInt(), 0U, 0).toInt()
                            onSelectIndex(selectedIndex)
                        }
                    }
                }
            )
        },
        update = {
            set(x) {
                this.x = x
            }
            set(y) {
                this.y = y
            }
            set(width) {
                this.width = width
            }
            set(height) {
                this.height = height
            }
        }
    )
}
