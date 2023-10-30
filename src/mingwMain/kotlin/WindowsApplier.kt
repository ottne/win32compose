package sample

import androidx.compose.runtime.AbstractApplier
import kotlinx.cinterop.*
import platform.windows.*

private val clickListeners = mutableMapOf<HWND, () -> Unit>()

val SendMessage = (platform.windows.SendMessage)!!
val GetMessage = (platform.windows.GetMessage)!!
val DispatchMessage = (platform.windows.DispatchMessage)!!
val DefWindowProc = (platform.windows.DefWindowProc)!!
val GetModuleHandle = (platform.windows.GetModuleHandle)!!
val RegisterClassEx = (platform.windows.RegisterClassEx)!!

class WindowsApplier(root: WindowsNode) : AbstractApplier<WindowsNode>(root) {

    override fun insertBottomUp(index: Int, instance: WindowsNode) {

    }

    override fun insertTopDown(index: Int, instance: WindowsNode) {
        println("Inserting $instance at index $index into $current")
        current.insert(index, instance)
    }

    override fun move(from: Int, to: Int, count: Int) {
        //currentWindowNode.children.move(from, to, count)
    }

    override fun onClear() {
        println("Clearing $current")
        current.clear()
    }

    override fun remove(index: Int, count: Int) {
        println("Removing $count elements at $index from $current")
        current.remove(index, count)
    }
}

sealed class WindowsNode {
    open fun insert(index: Int, instance: WindowsNode) {}
    open fun clear() {}
    open fun remove(index: Int, count: Int) {}
}

class ApplicationNode : WindowsNode() {
    private val applicationWindows = mutableListOf<WindowNode>()

    override fun insert(index: Int, instance: WindowsNode) {
        applicationWindows.add(instance as WindowNode)
    }

    override fun clear() {
        applicationWindows.clear()
    }

    override fun remove(index: Int, count: Int) {
        applicationWindows.remove(index, count)
    }
}

class WindowNode(
    x: Int = CW_USEDEFAULT,
    y: Int = CW_USEDEFAULT,
    width: Int = CW_USEDEFAULT,
    height: Int = CW_USEDEFAULT,
) : WindowsNode() {
    private val hwnd: HWND

    init {
        val windowClassName = "ComposedWindow"
        val success = memScoped {
            val hInstance = GetModuleHandle(null)

            // In order to be able to create a window you need to have a window class available. A window class can be created for your
            // application by registering one. The following struct declaration and fill provides details for a new window class.
            val wc = alloc<WNDCLASSEX>().apply {
                cbSize = sizeOf<WNDCLASSEX>().toUInt()
                style = 0u
                lpfnWndProc = staticCFunction(::WndProc)
                cbClsExtra = 0
                cbWndExtra = 0
                this.hInstance = hInstance
                hIcon = null
                hCursor = (LoadCursor!!)(hInstance, IDC_ARROW)
                //wc.hbrBackground = HBRUSH(COLOR_WINDOW+1)
                hbrBackground = (COLOR_WINDOW.toLong() + 1).toCPointer()
                lpszMenuName = null
                lpszClassName = windowClassName.wcstr.ptr
                hIconSm = null
            }

            // This function actually registers the window class. If the information specified in the 'wc' struct is correct,
            // the window class should be created and no error is returned.
            RegisterClassEx(wc.ptr) != 0u.toUShort()
        }
        check(success) { "Failed registering window class" }

        val hInstance = GetModuleHandle(null)
        hwnd = requireNotNull(
            // This function creates the first window. It uses the window class registered in the first part, and takes a title,
            // style and position/size parameters. For more information about style-specific definitions, refer to the MSDN where
            // extended documentation is available.
            CreateWindowExA(
                dwExStyle = WS_EX_CLIENTEDGE.toUInt(),
                lpClassName = windowClassName,
                lpWindowName = "Win32 C Window application by Danni Otterbach",
                dwStyle = (WS_OVERLAPPED or WS_CAPTION or WS_SYSMENU or WS_MINIMIZEBOX or WS_THICKFRAME).toUInt(),
                X = x,
                Y = y,
                nWidth = width,
                nHeight = height,
                hWndParent = null,
                hMenu = null,
                hInstance = hInstance,
                lpParam = NULL
            )
        ) { "Unable to create window" }

        ShowWindow(hwnd, 1)
        UpdateWindow(hwnd)

        // todo check to use multimedia timer instead to avoid having to use a Window
        SetTimer(
            hWnd = hwnd,
            nIDEvent = 1u,
            uElapse = 16u,
            lpTimerFunc = null
        )
    }

    override fun insert(index: Int, instance: WindowsNode) {
        check(instance is ChildNode) { "Illegal node type" }
        instance.createForWindow(hwnd)

        UpdateWindow(hwnd)
    }

    override fun clear() {
        val childList = getChildWindows(hwnd)

        childList.forEach { hChild ->
            DestroyWindow(hChild)
            clickListeners.remove(hChild)
        }
    }

    override fun remove(index: Int, count: Int) {
        val childWindows = getChildWindows(hwnd)
        childWindows.subList(index, index + count).forEach { hChild ->
            DestroyWindow(hChild)
            clickListeners.remove(hChild)
        }
    }

    var title: String?
        set(value) {
            SetWindowTextW(hwnd, value)
        }
        get() {
            val titleLength = GetWindowTextLengthW(hwnd)
            if (titleLength == 0) return ""
            memScoped {
                val title = allocArray<WCHARVar>(titleLength + 1)

                GetWindowTextW(hwnd, title, titleLength + 1)
                return title.toKString()
            }
        }
}

class ChildNode(
    private val className: String,
    private val style: Int,
    private val exStyle: Int = 0,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val initialTitle: String? = null,
    private val onCustomizeWidget: ((hwnd: HWND) -> Unit)? = null
) : WindowsNode() {

    private var hButton: HWND? = null

    var x: Int = x
        set(value) {
            field = value
            updatePositionIfNecessary()
        }

    var y: Int = y
        set(value) {
            field = value
            updatePositionIfNecessary()
        }

    var width: Int = width
        set(value) {
            field = value
            updatePositionIfNecessary()
        }

    var height: Int = height
        set(value) {
            field = value
            updatePositionIfNecessary()
        }

    var title: String?
        set(value) {
            if (hButton != null) {
                SetWindowTextW(hButton, value)
            }
        }
        get() {
            if (hButton != null) {
                return null
            }
            val titleLength = GetWindowTextLengthW(hButton)
            if (titleLength == 0) return ""
            memScoped {
                val title = allocArray<WCHARVar>(titleLength + 1)

                GetWindowTextW(hButton, title, titleLength + 1)
                return title.toKString()
            }
        }

    private var _onClick: (() -> Unit)? = null

    var onClick: (() -> Unit)?
        set(value) {
            _onClick = value

            if (hButton != null) {
                if (value != null) {
                    clickListeners[hButton!!] = value
                } else {
                    clickListeners.remove(hButton)
                }
            }
        }
        get() = _onClick

    private fun updatePositionIfNecessary() {
        if (hButton != null) {
            SetWindowPos(
                hWnd = hButton,
                hWndInsertAfter = null,
                X = x,
                Y = y,
                cx = width,
                cy = height,
                uFlags = 0u
            )
        }
    }

    internal fun createForWindow(hParent: HWND) {
        val hButton = requireNotNull(
            value = CreateWindowExW(
                dwExStyle = exStyle.toUInt(),
                lpClassName = className,
                lpWindowName = initialTitle,
                dwStyle = style.toUInt(),
                //dwStyle = (WS_TABSTOP or WS_VISIBLE or WS_CHILD).toUInt(),
                X = x, Y = y,
                nWidth = width, nHeight = height,
                hWndParent = hParent,
                hMenu = null,
                hInstance = (GetWindowLongPtr!!)(hParent, GWLP_HINSTANCE).toCPointer<HINSTANCE__>()!!.reinterpret(),
                lpParam = null
            )
        ) {
            "Unable to create window"
        }
        this.hButton = hButton

        val onClick = onClick
        if (onClick != null) {
            clickListeners[hButton] = onClick
        }

        onCustomizeWidget?.invoke(hButton)
    }
}

private fun <T> MutableList<T>.remove(index: Int, count: Int) {
    if (count == 1) {
        removeAt(index)
    } else {
        subList(index, index + count).clear()
    }
}

@ExperimentalUnsignedTypes
private fun WndProc(hwnd: HWND?, msg: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
    // This switch block differentiates between the message type that could have been received. If you want to
    // handle a specific type of message in your application, just define it in this block.
    when (msg.toInt()) {
        // This message type is used by the OS to close a window. Just closes the window using DestroyWindow(hwnd)
        WM_CLOSE -> DestroyWindow(hwnd)

        // This message type is part of the WM_CLOSE case. After the DestroyWindow(hwnd) function is called, a
        // WM_DESTROY message is sent to the window, which actually closes it.
        WM_DESTROY -> PostQuitMessage(0)

        // This message type is an important one for GUI programming. It symbolizes an event for a button for example.
        WM_COMMAND -> {
            // To differentiate between controls, compare the HWND of, for example, the button to the HWND that is passed
            // into the LPARAM parameter. This way you can establish control-specific actions.
            //if (lParam == button.objcPtr().toLong() && (wParam == BN_CLICKED.toULong()))
            //{
            //    // The button was clicked, this is your proof.
            //    MessageBoxA(hwnd, "Button is pressed!", "test", MB_ICONINFORMATION)
            //}

            if (wParam == BN_CLICKED.toULong()) {
                clickListeners[lParam.toCPointer()]?.invoke()
            }
        }

        WM_TIMER -> {
            val timeNanos = getCurrentTimeNanoseconds()
            clock.sendFrame(timeNanos)
        }

        // When no message type is handled in your application, return the default window procedure. In this case the message
        // will be handled elsewhere or not handled at all.
        else -> return DefWindowProc(hwnd, msg, wParam, lParam)
    }

    return 0
}