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

fun makeLParam(wparam: Long, lparam: Long): ULong {
    return ((lparam shl 16) or wparam).toULong()
}

fun WPARAM.loword(): Short {
    return (this.toInt() and 0xFFFF).toShort()
}

fun WPARAM.hiword(): Short {
    return (this shr 16).toShort()
}
