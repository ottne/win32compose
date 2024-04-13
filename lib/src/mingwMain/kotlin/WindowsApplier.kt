package sample

import androidx.compose.runtime.AbstractApplier
import kotlinx.cinterop.*
import platform.windows.*

class WindowsApplier(root: WindowsNode) : AbstractApplier<WindowsNode>(root) {

    override fun insertBottomUp(index: Int, instance: WindowsNode) {

    }

    override fun insertTopDown(index: Int, instance: WindowsNode) {
        println("Inserting $instance at index $index into $current")
        current.insert(index, instance)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.move(from, to, count)
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

    open fun move(from: Int, to: Int, count: Int) {}

    internal open fun onReceiveMsg(msg: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
        return 0
    }
}

class ApplicationNode : WindowsNode() {
    private val applicationWindows = mutableListOf<WindowNode>()

    override fun insert(index: Int, instance: WindowsNode) {
        applicationWindows.add(instance as WindowNode)
    }

    override fun clear() {
        applicationWindows.clear()
    }

    override fun move(from: Int, to: Int, count: Int) {
        applicationWindows.move(from, to, count)
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
    internal val windowHandle: WindowHandle

    private val stableThis: StableRef<WindowsNode>

    init {
        windowHandle = createAndShowWindow(x, y, width, height).asWindowHandle()

        stableThis = StableRef.create(this)

        (SetWindowLongPtr!!)(windowHandle.rawValue, GWLP_USERDATA, stableThis.asCPointer().toLong())
    }

    private fun createAndShowWindow(x: Int, y: Int, width: Int, height: Int): HWND {
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
        val hwnd = requireNotNull(
            // This function creates the first window. It uses the window class registered in the first part, and takes a title,
            // style and position/size parameters. For more information about style-specific definitions, refer to the MSDN where
            // extended documentation is available.
            CreateWindowExA(
                dwExStyle = WS_EX_CLIENTEDGE.toUInt(),
                lpClassName = windowClassName,
                lpWindowName = "Win32 C Window application by Danni Otterbach",
                dwStyle = (WS_OVERLAPPED or WS_CAPTION or WS_SYSMENU or WS_MINIMIZEBOX).toUInt(),
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

        setFont(hwnd)

        ShowWindow(hwnd, 1)
        UpdateWindow(hwnd)

        // todo check to use multimedia timer instead to avoid having to use a Window
        SetTimer(
            hWnd = hwnd,
            nIDEvent = 1u,
            uElapse = 16u,
            lpTimerFunc = null
        )

        return hwnd
    }

    override fun insert(index: Int, instance: WindowsNode) {
        check(instance is ChildNode) { "Illegal node type" }

        UpdateWindow(windowHandle.rawValue)
    }

    override fun clear() {
        val childList = windowHandle.getChildWindows()

        childList.forEach { hChild ->
            DestroyWindow(hChild)
        }
    }

    override fun move(from: Int, to: Int, count: Int) {
        val childWindows = windowHandle.getChildWindows()

        // Todo this was generated by ChatGPT so I have now idea if this really works
        if (from in 0 until count && to in 0 until count) {
            // Create an array to reorder the child windows
            val reorderedWindows = childWindows.toMutableList()

            // Reorder the windows
            reorderedWindows[to] = childWindows[from] // Place the "from" window at the "to" position
            if (from < to) {
                for (i in from until to) {
                    reorderedWindows[i] = childWindows[i + 1] // Shift windows down
                }
            } else {
                for (i in from downTo to + 1) {
                    reorderedWindows[i] = childWindows[i - 1] // Shift windows up
                }
            }

            // Update the child windows' z-order
            var previousHWND: HWND? = null
            for (childWindow in reorderedWindows) {
                SetWindowPos(childWindow, previousHWND, 0, 0, 0, 0, (SWP_NOMOVE or SWP_NOSIZE).toUInt())
                previousHWND = childWindow
            }
        }
    }

    override fun remove(index: Int, count: Int) {
        val childWindows = windowHandle.getChildWindows()

        childWindows.subList(index, index + count).forEach { hChild ->
            DestroyWindow(hChild)
        }
    }

    override fun onReceiveMsg(msg: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
        when (msg.toInt()) {
            // This message type is used by the OS to close a window. Just closes the window using DestroyWindow(hwnd)
            WM_CLOSE -> DestroyWindow(windowHandle.rawValue)

            // This message type is part of the WM_CLOSE case. After the DestroyWindow(hwnd) function is called, a
            // WM_DESTROY message is sent to the window, which actually closes it.
            WM_DESTROY -> {
                PostQuitMessage(0)
            }

            WM_TIMER -> {
                val timeNanos = getCurrentTimeNanoseconds()
                clock.sendFrame(timeNanos)
            }

            else -> return DefWindowProc(windowHandle.rawValue, msg, wParam, lParam)
        }

        return super.onReceiveMsg(msg, wParam, lParam)
    }

    var title: String?
        set(value) {
            SetWindowTextW(windowHandle.rawValue, value)
        }
        get() {
            val titleLength = GetWindowTextLengthW(windowHandle.rawValue)
            if (titleLength == 0) return ""
            memScoped {
                val title = allocArray<WCHARVar>(titleLength + 1)

                GetWindowTextW(windowHandle.rawValue, title, titleLength + 1)
                return title.toKString()
            }
        }
}

class ChildNode(
    parentHwnd: HWND,
    private val className: String,
    private val style: Int,
    private val exStyle: Int = 0,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val initialTitle: String? = null,
    private val onWidgetCreated: ((hwnd: HWND) -> Unit)? = null,
    val onCommand: ((hChild: HWND, notificationCode: Int, lParam: LPARAM) -> Unit)? = null,
    val onNotify: ((hChild: HWND, nmhdr: NMHDR) -> Unit)? = null
) : WindowsNode() {

    private var hChild: HWND

    private val stableThis: StableRef<ChildNode>

    init {
        hChild = createChildWindow(parentHwnd)

        // TODO dispose this ref
        stableThis = StableRef.create(this)

        (SetWindowLongPtr!!)(hChild, GWLP_USERDATA, stableThis.asCPointer().toLong())
    }

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
            SetWindowTextW(hChild, value)
        }
        get() {
            val titleLength = GetWindowTextLengthW(hChild)
            if (titleLength == 0) return ""
            memScoped {
                val title = allocArray<WCHARVar>(titleLength + 1)

                GetWindowTextW(hChild, title, titleLength + 1)
                return title.toKString()
            }
        }

    private fun updatePositionIfNecessary() {
        SetWindowPos(
            hWnd = hChild,
            hWndInsertAfter = null,
            X = x,
            Y = y,
            cx = width,
            cy = height,
            uFlags = 0u
        )
    }

    private fun createChildWindow(hParent: HWND): HWND {
        val hChild = requireNotNull(
            CreateWindowExW(
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
        this.hChild = hChild

        setFont(hChild)

        onWidgetCreated?.invoke(hChild)

        return hChild
    }

    override fun onReceiveMsg(msg: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {

        when (msg.toInt()) {
            WM_COMMAND -> {
                onCommand?.invoke(hChild, wParam.hiword().toInt(), lParam)
            }
            WM_NOTIFY -> {
                val nmhdr: NMHDR = lParam.toCPointer<NMHDR>()!![0]
                onNotify?.invoke(hChild, nmhdr)
            }
            else -> return DefWindowProc(hChild, msg, wParam, lParam)
        }

        return super.onReceiveMsg(msg, wParam, lParam)
    }

    override fun toString(): String {
        return "${super.toString()} (${WindowHandle(hChild).getClassName()}"
    }
}

private fun setFont(hChild: HWND) = memScoped {
    val metrics = alloc<NONCLIENTMETRICS>()
    metrics.cbSize = sizeOf<NONCLIENTMETRICS>().toUInt()

    SystemParametersInfo?.invoke(
        SPI_GETNONCLIENTMETRICS.toUInt(),
        sizeOf<NONCLIENTMETRICS>().toUInt(),
        metrics.reinterpret(),
        0u
    )

    val font = (CreateFontIndirect!!)(metrics.lfMessageFont.reinterpret())

    val result =
        SendMessage(
            hChild,
            WM_SETFONT.toUInt(),
            font.toLong().toULong(),
            makeLParam(TRUE.toLong(), 0).toLong()
        )

    //DeleteObject(font)
}

private fun <T> MutableList<T>.remove(index: Int, count: Int) {
    if (count == 1) {
        removeAt(index)
    } else {
        subList(index, index + count).clear()
    }
}

private fun <T> MutableList<T>.move(from: Int, to: Int, count: Int) {
    val dest = if (from > to) to else to - count
    if (count == 1) {
        if (from == to + 1 || from == to - 1) {
            // Adjacent elements, perform swap to avoid backing array manipulations.
            val fromEl = get(from)
            val toEl = set(to, fromEl)
            set(from, toEl)
        } else {
            val fromEl = removeAt(from)
            add(dest, fromEl)
        }
    } else {
        val subView = subList(from, from + count)
        val subCopy = subView.toMutableList()
        subView.clear()
        addAll(dest, subCopy)
    }
}

@ExperimentalUnsignedTypes
private fun WndProc(hwnd: HWND?, msg: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
    // This switch block differentiates between the message type that could have been received. If you want to
    // handle a specific type of message in your application, just define it in this block.

    val targetHwnd = when (msg.toInt()) {
        WM_COMMAND -> {
            // todo need to check for accelerator or menu here in wParam high word
            lParam.toCPointer()
        }
        WM_NOTIFY -> {
            val nmhdr: NMHDR = lParam.toCPointer<NMHDR>()!![0]
            nmhdr.hwndFrom
        }
        else -> {
            hwnd
        }
    }

    val node = (GetWindowLongPtr!!)(targetHwnd, GWLP_USERDATA).toCPointer<CPointed>()?.asStableRef<WindowsNode>()?.get()

    if (node != null) {
        return node.onReceiveMsg(msg, wParam, lParam)
    }

    return DefWindowProc(hwnd, msg, wParam, lParam)
}