@file:OptIn(ExperimentalForeignApi::class)

package de.danotter.composewin32

import kotlinx.cinterop.*
import platform.posix.fdopen
import platform.posix.fprintf_s
import platform.windows.*

val SendMessage = (platform.windows.SendMessage)!!
val GetMessage = (platform.windows.GetMessage)!!
val DispatchMessage = (platform.windows.DispatchMessage)!!
val DefWindowProc = (platform.windows.DefWindowProc)!!
val GetModuleHandle = (platform.windows.GetModuleHandle)!!
val RegisterClassEx = (platform.windows.RegisterClassEx)!!
val SystemParametersInfo = (platform.windows.SystemParametersInfo)!!

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

fun getLastError(): String? {
    val errorCode = GetLastError()

    memScoped {
        val lpMsgBuf = alloc<LPWSTRVar>()
        val bufferLength = FormatMessageW(
            (FORMAT_MESSAGE_ALLOCATE_BUFFER or FORMAT_MESSAGE_FROM_SYSTEM or FORMAT_MESSAGE_IGNORE_INSERTS).toUInt(),
            null,
            errorCode,
            makelangid(LANG_NEUTRAL.toUShort(), SUBLANG_DEFAULT.toUShort()),
            lpMsgBuf.ptr.reinterpret(),
            0u,
            null
        )

        if (bufferLength > 0u) {
            val message = lpMsgBuf.value?.toKString()
            LocalFree(lpMsgBuf.value)

            return message
        } else {
            fprintf_s(fdopen(2, "w"), "Unknown error")
            return null
        }
    }
}

private fun makelangid(primary: UShort, sub: UShort): UInt {
    return ((sub.toUInt() shl 10) or primary.toUInt())
}
