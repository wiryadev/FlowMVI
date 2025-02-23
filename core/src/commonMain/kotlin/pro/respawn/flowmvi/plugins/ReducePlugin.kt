package pro.respawn.flowmvi.plugins

import pro.respawn.flowmvi.api.FlowMVIDSL
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.StorePlugin
import pro.respawn.flowmvi.dsl.StoreBuilder
import pro.respawn.flowmvi.dsl.plugin

/**
 * Default name for [reducePlugin].
 * This is hardcoded so that multiple [reduce] invocations are not allowed w/o
 * explicit consent of the user as most often multiple reducers will conflict with each other.
 * Provide your own name if you want to have multiple reducers.
 */
public const val ReducePluginName: String = "ReducePlugin"

/**
 * An operation that processes incoming [MVIIntent]s
 */
public typealias Reduce<S, I, A> = suspend PipelineContext<S, I, A>.(intent: I) -> Unit

/**
 * Create and install a [reducePlugin].
 *
 * Name is hardcoded because usually multiple reducers are not used.
 *
 * Provide your own name if you want to have multiple reducers.
 *
 * Events will be consumed (not passed along the chain of plugins) if [consume] is true (true by default).
 */
@FlowMVIDSL
public inline fun <S : MVIState, I : MVIIntent, A : MVIAction> StoreBuilder<S, I, A>.reduce(
    consume: Boolean = true,
    name: String = ReducePluginName,
    crossinline reduce: Reduce<S, I, A>,
): Unit = install(reducePlugin(consume, name, reduce))

/**
 * Create  a new plugin that simply invokes [StorePlugin.onIntent], processes it and does not change the intent.
 *
 * To change the intent, either create your own [plugin] or use [PipelineContext] to manage the store.
 *
 * Name is hardcoded because usually multiple reducers are not used.
 *
 * Provide your own name if you want to have multiple reducers.
 *
 * Events will be consumed (not passed along the chain of plugins) if [consume] is true (true by default).
 **/
@FlowMVIDSL
public inline fun <S : MVIState, I : MVIIntent, A : MVIAction> reducePlugin(
    consume: Boolean = true,
    name: String = ReducePluginName,
    crossinline reduce: Reduce<S, I, A>,
): StorePlugin<S, I, A> = plugin {
    this.name = name
    onIntent {
        reduce(it)
        it.takeUnless { consume }
    }
}
