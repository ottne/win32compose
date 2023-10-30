package sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import kotlinx.cinterop.*
import platform.windows.*

private const val WindowDefault = CW_USEDEFAULT

@Composable
fun Window(
    title: String? = null,
    x: Int = WindowDefault,
    y: Int = WindowDefault,
    width: Int = WindowDefault,
    height: Int = WindowDefault,
    content: @Composable () -> Unit
) {
    ComposeNode<WindowNode, WindowsApplier>(
        factory = {
            WindowNode(
                x = x,
                y = y,
                width = width,
                height = height
            )
        },
        update = {
            set(title) {
                this.title = it
            }
        },
        content = content
    )
}

@Composable
fun Button(
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
                className = "BUTTON",
                style = WS_TABSTOP or WS_VISIBLE or WS_CHILD,
                exStyle = WS_EX_CLIENTEDGE,
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
                }
            )
        },
        update = {
            set(title) {
                this.title = it
            }
            set(onClick) {
                this.onClick = it
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
fun Label(
    title: String? = null,
    x: Int,
    y: Int,
    width: Int,
    height: Int
) {
    ComposeNode<ChildNode, WindowsApplier>(
        factory = {
            ChildNode(
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
fun ListBox(
    items: List<String>,
    x: Int,
    y: Int,
    width: Int,
    height: Int
) {
    ComposeNode<ChildNode, WindowsApplier>(
        factory = {
            ChildNode(
                className = "LISTBOX",
                style = WS_CHILD or WS_VISIBLE or ES_AUTOVSCROLL,
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
