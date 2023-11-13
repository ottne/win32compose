package sample

import kotlinx.cinterop.*
import platform.windows.*

fun main() {

    memScoped {
        // Todo doesn't work because we need to add something to the app manifest
        val icex = alloc<INITCOMMONCONTROLSEX>()
        icex.dwSize = sizeOf<INITCOMMONCONTROLSEX>().toUInt()
        icex.dwICC = (ICC_STANDARD_CLASSES or ICC_LINK_CLASS).toUInt()
        InitCommonControlsEx(icex.ptr)

        //ncm = alloc()

        // Modify the font settings in ncm
        //ncm.lfMessageFont.lfHeight = 16; // Set the desired font size
        //ncm.lfMessageFont.lfWeight = FW_NORMAL; // Set the desired font weight
        // Modify other font properties as needed
        //SystemParametersInfo?.invoke(
        //    SPI_SETNONCLIENTMETRICS.toUInt(),
        //    sizeOf<NONCLIENTMETRICS>().toUInt(),
        //    ncm.reinterpret(),
        //    (SPIF_UPDATEINIFILE or SPIF_SENDCHANGE).toUInt()
        //)
    }

    runWindowsApp {
        SampleApp()
    }
}
