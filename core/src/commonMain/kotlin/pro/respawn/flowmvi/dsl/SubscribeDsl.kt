package pro.respawn.flowmvi.dsl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.ActionConsumer
import pro.respawn.flowmvi.api.FlowMVIDSL
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.StateConsumer
import pro.respawn.flowmvi.api.Store

/**
 * Subscribe to the [store] and invoke [consume] and [render] in parallel in the provided scope.
 * This function does **not** handle the lifecycle of the UI layer. For that, see platform implementations.
 * @see [Store.subscribe]
 */
@FlowMVIDSL
public inline fun <S : MVIState, I : MVIIntent, A : MVIAction> CoroutineScope.subscribe(
    store: Store<S, I, A>,
    noinline consume: (suspend (action: A) -> Unit)?,
    crossinline render: suspend (state: S) -> Unit,
): Job = with(store) {
    subscribe outer@{
        coroutineScope inner@{
            consume?.let { consume ->
                launch {
                    actions.collect { consume(it) }
                }
            }
            launch {
                states.collect { render(it) }
            }
        }
    }
}

/**
 * Subscribe to the [store] and invoke [ActionConsumer.consume] and [StateConsumer.render] in parallel in the provided scope.
 * This function does **not** handle the lifecycle of the UI layer. For that, see platform implementations.
 * @see [Store.subscribe]
 */
@FlowMVIDSL
public fun <S : MVIState, I : MVIIntent, A : MVIAction, T> T.subscribe(
    store: Store<S, I, A>,
    scope: CoroutineScope
): Job where T : ActionConsumer<A>, T : StateConsumer<S> = with(scope) {
    subscribe(store, ::consume, ::render)
}

/**
 * Subscribe to the [store] and invoke [StateConsumer.render] in the provided scope.
 * This function does **not** handle the lifecycle of the UI layer. For that, see platform implementations.
 * @see [Store.subscribe]
 */
@FlowMVIDSL
public fun <S : MVIState, I : MVIIntent, A : MVIAction, T> StateConsumer<S>.subscribe(
    store: Store<S, I, A>,
    scope: CoroutineScope
): Job where T : StateConsumer<S> = with(scope) {
    subscribe(store, null, ::render)
}
