package pro.respawn.flowmvi.plugins

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pro.respawn.flowmvi.api.FlowMVIDSL
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.StoreBuilder
import pro.respawn.flowmvi.util.CappedMutableList

/**
 * A plugin that allows to undo and redo any actions happening in the [pro.respawn.flowmvi.api.Store].
 * Keep a reference to the plugin instance to call [undo], [redo], and [invoke].
 */
public class UndoRedoPlugin<S : MVIState, I : MVIIntent, A : MVIAction>(
    private val maxQueueSize: Int,
    name: String? = null,
) : AbstractStorePlugin<S, I, A>(name) {

    private val queue by atomic<CappedMutableList<Event>>(CappedMutableList(maxQueueSize))
    private val _index = MutableStateFlow(-1)
    private val lock = Mutex()

    /**
     * Current index of the queue.
     * Larger value means newer, but not larger than [maxQueueSize]
     */
    public val index: StateFlow<Int> = _index.asStateFlow()

    /**
     * Whether the intent queue is empty. Does not take [index] into account
     */
    public val isQueueEmpty: Boolean get() = queue.isEmpty()

    /**
     * The current queue size of the plugin.
     * This queue size does **not** consider current index and allows to get the total amount of events stored
     */
    public val queueSize: Int get() = queue.size

    /**
     * Whether the plugin can [undo] at this moment.
     */
    public val canUndo: Boolean get() = !isQueueEmpty && index.value != -1

    /**
     * Whether the plugin can [redo] at this moment.
     */
    public val canRedo: Boolean get() = !isQueueEmpty && index.value != queue.lastIndex

    /**
     * Add a given [undo] and [redo] to the queue.
     * If [doImmediately] is true, then [redo] will be executed **before** the queue is modified!
     * **You cannot call [UndoRedoPlugin.undo] or [UndoRedoPlugin.redo] in [redo] or [undo]!**
     */
    @FlowMVIDSL
    public suspend operator fun invoke(
        doImmediately: Boolean = true,
        redo: suspend () -> Unit,
        undo: suspend () -> Unit,
    ): Int = lock.withLock {
        with(queue) {
            if (doImmediately) redo()
            val range = index.value.coerceAtLeast(0) + 1..lastIndex
            if (!range.isEmpty()) removeAll(slice(range))
            add(Event(redo, undo))
            lastIndex.also { _index.value = it }
        }
    }

    /**
     * Add the [intent] to the queue with specified [undo] and **immediately** execute the [intent].
     * **You cannot call [UndoRedoPlugin.undo] or [UndoRedoPlugin.redo] in [intent] or [undo]!**
     */
    @FlowMVIDSL
    public suspend operator fun PipelineContext<S, I, A>.invoke(
        intent: I,
        undo: suspend () -> Unit,
    ): Int = invoke(redo = { intent(intent) }, undo = undo, doImmediately = true)

    /**
     * Undo the event at current [index].
     * **You cannot undo and redo while another undo/redo is running!**
     */
    public suspend fun undo(require: Boolean = false): Int = lock.withLock {
        if (!canUndo) {
            require(!require) { "Tried to undo but nothing was in the queue" }
            -1
        } else {
            val i = index.value.coerceIn(queue.indices)
            queue[i].undo()
            (i - 1).also { _index.value = it }
        }
    }

    /**
     * Redo the event at current [index].
     * **You cannot undo and redo while another undo/redo is running!**
     */
    public suspend fun redo(require: Boolean = false): Int = lock.withLock {
        if (!canRedo) {
            require(!require) { "Tried to redo but queue already at the last index of ${queue.lastIndex}" }
            queue.lastIndex
        } else {
            val i = index.value.coerceIn(queue.indices)
            queue[i].redo()
            (i + 1).also { _index.value = it }
        }
    }

    /**
     * Clear the queue of events and reset [index] to 0
     */
    public fun reset(): Unit = _index.update {
        queue.clear()
        -1
    }

    // reset because pipeline context captured in Events is no longer running
    override suspend fun PipelineContext<S, I, A>.onStart(): Unit = reset()
    override fun onStop(e: Exception?): Unit = reset()

    /**
     * An event happened in the [UndoRedoPlugin].
     */
    public data class Event internal constructor(
        internal val redo: suspend () -> Unit,
        internal val undo: suspend () -> Unit,
    ) {

        override fun toString(): String = "UndoRedoPlugin.Event"
    }
}

/**
 * Creates a new [UndoRedoPlugin]
 */
@FlowMVIDSL
public fun <S : MVIState, I : MVIIntent, A : MVIAction> undoRedoPlugin(
    maxQueueSize: Int,
    name: String? = null,
): UndoRedoPlugin<S, I, A> = UndoRedoPlugin(maxQueueSize, name)

/**
 * Creates and installs a new [UndoRedoPlugin]
 * @return an instance that was created. Make sure to keep it to use the plugin's api.
 */
@FlowMVIDSL
public fun <S : MVIState, I : MVIIntent, A : MVIAction> StoreBuilder<S, I, A>.undoRedo(
    maxQueueSize: Int,
    name: String? = null,
): UndoRedoPlugin<S, I, A> = UndoRedoPlugin<S, I, A>(maxQueueSize, name).also { install(it) }
