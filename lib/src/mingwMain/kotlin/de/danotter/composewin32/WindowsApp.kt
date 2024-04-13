package de.danotter.composewin32

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.*
import platform.windows.MSG
import platform.windows.TranslateMessage

private var hasFrameWaiters = false
internal val clock = BroadcastFrameClock(onNewAwaiters = {
    hasFrameWaiters = true
})

fun runWindowsApp(
    content: @Composable () -> Unit
) = runBlocking {
    val snapshotJob = launch {
        SnapshotManager.ensureStarted()
    }

    val job = Job(coroutineContext[Job])
    val composeContext = coroutineContext + clock + job
    val recomposer = Recomposer(composeContext)

    val root = ApplicationNode()
    val composition = Composition(WindowsApplier(root), recomposer)

    composition.setContent(content)

    launch(context = clock, start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }

    memScoped {
        // This part is the "message loop". This loop ensures the application keeps running and makes the window able to receive messages
        // in the WndProc function. You must have this piece of code in your GUI application if you want it to run properly.
        val msg = alloc<MSG>()
        while (GetMessage(msg.ptr, null, 0u, 0u) > 0) {
            TranslateMessage(msg.ptr)
            DispatchMessage(msg.ptr)
            yield()
        }
    }

    job.cancel()
    snapshotJob.cancel()
}
