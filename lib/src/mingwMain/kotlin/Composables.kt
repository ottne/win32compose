package sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.remember
import kotlinx.cinterop.*
import platform.windows.*

private const val WindowDefault = CW_USEDEFAULT

class WindowScope internal constructor(
    internal val hwnd: HWND
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

    val windowScope = remember(node) { WindowScope(node.hwnd) }
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
                parentHwnd = hwnd,
                className = "BUTTON",
                style = WS_TABSTOP or WS_VISIBLE or WS_CHILD or BS_PUSHBUTTON,
                exStyle = 0,
                x = x,
                y = y,
                width = width,
                height = height,
                initialTitle = title,
                onCustomizeWidget = { hButton ->
                    memScoped {
                        val bi: BUTTON_IMAGELIST = alloc()
                        bi.himl = null
                        bi.uAlign = BUTTON_IMAGELIST_ALIGN_LEFT.toUInt()
                        SendMessage(hButton, BCM_SETIMAGELIST.toUInt(), 0UL, bi.ptr.rawValue.toLong())
                    }
                },
                onCommand = { _, command ->
                    if (command == BN_CLICKED) {
                        onClick?.invoke()
                    }
                }
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
                parentHwnd = hwnd,
                className = "STATIC",
                style = WS_VISIBLE or WS_CHILD or SS_LEFT,
                x = x,
                y = y,
                width = width,
                height = height,
                initialTitle = title
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
    items: List<String>,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    onSelectIndex: ((index: Int) -> Unit)? = null
) {
    ComposeNode<ChildNode, WindowsApplier>(
        factory = {
            ChildNode(
                parentHwnd = hwnd,
                className = "LISTBOX",
                style = WS_CHILD or WS_VISIBLE or WS_VSCROLL or LBS_NOTIFY,
                exStyle = WS_EX_CLIENTEDGE,
                x = x,
                y = y,
                width = width,
                height = height,
                initialTitle = null,
                onCustomizeWidget = { hListBox ->
                    items.forEach { item ->
                        item.usePinned { pinnedItem ->
                            SendMessageW(
                                hWnd = hListBox,
                                Msg = LB_ADDSTRING.toUInt(),
                                wParam = 0u,
                                lParam = pinnedItem.addressOf(0).toLong()
                            )
                        }
                    }
                },
                onCommand = { hChild, cmdCode ->
                    if (cmdCode == LBN_SELCHANGE) {
                        val selectedIndex = SendMessage(hChild, LB_GETCURSEL.toUInt(), 0u, 0).toInt()
                        if (selectedIndex != LB_ERR) {
                            onSelectIndex?.invoke(selectedIndex)
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
