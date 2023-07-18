package pro.respawn.flowmvi.api

// https://youtrack.jetbrains.com/issue/KTIJ-7642
@Suppress("FUN_INTERFACE_WITH_SUSPEND_FUNCTION")
public fun interface ActionReceiver<in A : MVIAction> {

    /**
     * Send a new side-effect to be processed by subscribers, only once.
     * Actions not consumed will await in the queue with max capacity given as an implementation detail.
     * Actions that make the capacity overflow will be dropped, starting with the oldest.
     * How actions will be distributed depends on [ActionShareBehavior].
     * @See MVIProvider
     */
    public suspend fun send(action: A)
    public suspend fun action(action: A): Unit = send(action)
}
