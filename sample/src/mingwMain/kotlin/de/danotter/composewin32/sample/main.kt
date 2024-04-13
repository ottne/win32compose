package de.danotter.composewin32.sample

import kotlinx.cinterop.*
import platform.windows.*
import de.danotter.composewin32.runWindowsApp

fun main() {

    memScoped {
        val icex = alloc<INITCOMMONCONTROLSEX>()
        icex.dwSize = sizeOf<INITCOMMONCONTROLSEX>().toUInt()
        icex.dwICC = (ICC_STANDARD_CLASSES or ICC_LINK_CLASS).toUInt()
        InitCommonControlsEx(icex.ptr)
    }

    runWindowsApp {
        SampleApp()
    }
}
