package pro.respawn.flowmvi.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import pro.respawn.flowmvi.MVIIntent
import pro.respawn.flowmvi.MVIState
import pro.respawn.flowmvi.base.IntentReceiver
import pro.respawn.flowmvi.base.StateProvider
import pro.respawn.flowmvi.base.StateReceiver

/**
 * A central business logic unit for handling [MVIIntent]s, [MVIAction]s, and [MVIState]s.
 * Usually not subclassed but used with a corresponding builder (see [lazyStore], [launchedStore]).
 * A store functions independently of any subscribers.
 * MVIStore is the base implementation of [MVIProvider].
 */
public interface Store<S : MVIState, in I : MVIIntent> :
    StateProvider<S>,
    StateReceiver<S>,
    IntentReceiver<I> {

    /**
     * Starts store intent processing in a new coroutine in the given [scope].
     * Intents are processed as long as the parent scope is active.
     * **Starting store processing when it is already started will result in an exception.**
     * Although not advised, store can be launched multiple times, provided you cancel the job used before.
     * @return a [Job] that the store is running on that can be cancelled later.
     */
    public fun start(scope: CoroutineScope): Job
}
