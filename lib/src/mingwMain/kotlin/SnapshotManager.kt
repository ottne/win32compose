package sample

import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal object SnapshotManager {
    private val started = atomic(false)

    /**
     * Registers an observer to global snapshot states and sends an apply notification
     * for each callback if it has not already started.
     */
    suspend fun ensureStarted() = coroutineScope {
        if (started.compareAndSet(expect = false, update = true)) {
            val channel = Channel<Unit>(Channel.CONFLATED)
            launch {
                channel.consumeEach {
                    Snapshot.sendApplyNotifications()
                }
            }
            Snapshot.registerGlobalWriteObserver {
                channel.trySend(Unit)
            }
        }
    }
}
