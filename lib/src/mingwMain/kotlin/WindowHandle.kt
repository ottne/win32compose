package sample

import kotlinx.cinterop.*
import platform.windows.*
import kotlin.native.concurrent.ThreadLocal

value class WindowHandle(val rawValue: HWND) {

    fun getClassName(): String {
        val buffer = ByteArray(256)

        return buffer.usePinned { pinned ->
            GetClassNameW(rawValue, pinned.addressOf(0).reinterpret(), buffer.size)
            pinned.addressOf(0).reinterpret<ShortVar>().toKStringFromUtf16()
        }
    }

    fun setDefaultFont() = memScoped {
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
                rawValue,
                WM_SETFONT.toUInt(),
                font.toLong().toULong(),
                makeLParam(TRUE.toLong(), 0).toLong()
            )

        //DeleteObject(font)
    }

    fun getChildWindows(): List<HWND> {

        tmpChildWindowList.clear()

        EnumChildWindows(
            rawValue,
            staticCFunction { hChild, _ ->
                if (hChild != null) {
                    tmpChildWindowList.add(hChild)
                }

                TRUE
            },
            0
        )

        tmpChildWindowList.reverse()

        return tmpChildWindowList.toList()
    }
}

// https://stackoverflow.com/q/59707350/823483
@ThreadLocal
private val tmpChildWindowList: MutableList<HWND> = ArrayList(10)

fun HWND.asWindowHandle(): WindowHandle {
    return WindowHandle(this)
}


