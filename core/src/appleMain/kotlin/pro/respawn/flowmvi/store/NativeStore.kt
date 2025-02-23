package pro.respawn.flowmvi.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import pro.respawn.flowmvi.api.DelicateStoreApi
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.subscribe

/**
 * This is an experimental wrapper for the store to support native platforms which do not have the coroutines api,
 * such as iOS. Please fil an issue if you face any problems with this.
 * You **must** call [close] when you are done working with the store. This stops the store.
 * @param autoStart whether to start the store immediately, false by default.
 */
@OptIn(ExperimentalStdlibApi::class)
@DelicateStoreApi
public class NativeStore<S : MVIState, I : MVIIntent, A : MVIAction>(
    private val store: Store<S, I, A>,
    autoStart: Boolean = false,
) : AutoCloseable {

    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Get the initial state of the store. Changed using [pro.respawn.flowmvi.dsl.StoreBuilder]
     */
    public val initial: S = store.initial

    /**
     * Get the name of the store. Changed using [pro.respawn.flowmvi.dsl.StoreBuilder]
     */
    public val name: String? = store.name

    init {
        if (autoStart) store.start(scope)
    }

    /**
     * Same as [Store.subscribe] but does not manage the scope for you. [close] the store manually.
     * @return an [AutoCloseable] that you can close when you want to unsubscribe from the store.
     */
    public fun subscribe(
        onAction: (action: A) -> Unit,
        onState: (state: S) -> Unit,
    ): AutoCloseable = object : AutoCloseable {
        val job: Job = scope.subscribe(store, onAction, onState)
        override fun close() = job.cancel()
    }

    /**
     * See [pro.respawn.flowmvi.api.IntentReceiver.send]
     */
    public fun send(intent: I): Unit = store.send(intent)

    /**
     * See [pro.respawn.flowmvi.api.IntentReceiver.send]
     */
    public fun intent(intent: I): Unit = store.intent(intent)

    /**
     * Stop the store and its coroutine scope. The store **cannot** be used again after this call!
     */
    override fun close(): Unit = scope.cancel()
}
