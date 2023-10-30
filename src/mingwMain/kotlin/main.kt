package sample

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import platform.windows.ICC_LINK_CLASS
import platform.windows.ICC_STANDARD_CLASSES
import platform.windows.INITCOMMONCONTROLSEX
import platform.windows.InitCommonControlsEx

fun main() {

    memScoped {
        // Todo doesn't work because we need to add something to the app manifest
        val icex = alloc<INITCOMMONCONTROLSEX>()
        icex.dwSize = sizeOf<INITCOMMONCONTROLSEX>().toUInt()
        icex.dwICC = (ICC_STANDARD_CLASSES or ICC_LINK_CLASS).toUInt()
        InitCommonControlsEx(icex.ptr)
    }

    runWindowsApp {
        SampleApp()
    }
}
