package pro.respawn.flowmvi.api

public interface StorePlugin<S : MVIState, I : MVIIntent, A : MVIAction> {

    public suspend fun PipelineContext<S, I, A>.onState(old: S, new: S): S? = new
    public suspend fun PipelineContext<S, I, A>.onIntent(intent: I): I? = intent
    public suspend fun PipelineContext<S, I, A>.onAction(action: A): A? = action
    public suspend fun PipelineContext<S, I, A>.onException(e: Exception): Exception? = e
    public suspend fun PipelineContext<S, I, A>.onStart(): Unit = Unit
    public suspend fun PipelineContext<S, I, A>.onSubscribe(): Unit = Unit
    public fun onStop(): Unit = Unit

    public val name: String
    override fun hashCode(): Int
    override fun equals(other: Any?): Boolean
}
