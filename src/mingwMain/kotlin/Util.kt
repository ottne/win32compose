@file:OptIn(ExperimentalForeignApi::class)

package sample

import kotlinx.cinterop.*
import platform.windows.*
import kotlin.native.concurrent.ThreadLocal

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
