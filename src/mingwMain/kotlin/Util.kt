@file:OptIn(ExperimentalForeignApi::class)

package sample

import kotlinx.cinterop.*
import platform.windows.*
import kotlin.native.concurrent.ThreadLocal

val SendMessage = (platform.windows.SendMessage)!!
val GetMessage = (platform.windows.GetMessage)!!
val DispatchMessage = (platform.windows.DispatchMessage)!!
val DefWindowProc = (platform.windows.DefWindowProc)!!
val GetModuleHandle = (platform.windows.GetModuleHandle)!!
val RegisterClassEx = (platform.windows.RegisterClassEx)!!

fun getCurrentTimeNanoseconds(): Long = memScoped {
    val counter = alloc<LARGE_INTEGER>()

    QueryPerformanceFrequency(counter.ptr)
    val freq = counter.QuadPart

    QueryPerformanceCounter(counter.ptr)
    val count = counter.QuadPart

    return (count.toDouble() / freq.toDouble() * 1_000_000_000).toLong()
}


// https://stackoverflow.com/q/59707350/823483
@ThreadLocal
private val childWindowList: MutableList<HWND> = mutableListOf()

fun getChildWindows(hwnd: HWND): List<HWND> {

    childWindowList.clear()

    EnumChildWindows(
        hwnd,
        staticCFunction { hChild, _ ->
            if (hChild != null) {
                childWindowList.add(hChild)
            }

            TRUE
        },
        0
    )

    return childWindowList.toList()
}

fun makeLParam(wparam: Long, lparam: Long): ULong {
    return ((lparam shl 16) or wparam).toULong()
}

fun Int.loword(): Short {
    return (this and 0xFFFF).toShort()
}
