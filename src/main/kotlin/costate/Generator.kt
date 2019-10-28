package costate

import java.io.Serializable
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.intrinsics.*

@RestrictsSuspension
@SinceKotlin("1.3")
public abstract class GeneratorScope<in T> constructor() {
    public abstract suspend fun waitForEvent()
}


private typealias MyState = Int

private const val State_NotReady: MyState = 0
private const val State_ManyNotReady: MyState = 1
private const val State_ManyReady: MyState = 2
private const val State_Ready: MyState = 3
private const val State_Done: MyState = 4
private const val State_Failed: MyState = 5

class GeneratorIterator<T> : GeneratorScope<T>(), Continuation<Unit>, Serializable {
    private var state = State_NotReady
    private var nextValue: T? = null
    private var nextIterator: Iterator<T>? = null
    var nextStep: Continuation<Unit>? = null
    var event = "init"
    lateinit var smContext: Context

    fun fireEvent(event: String, context: Context): Boolean {
        this.event = event
        this.smContext = context
        while (true) {
            when (state) {
                State_NotReady -> {}
                State_ManyNotReady ->
                    if (nextIterator!!.hasNext()) {
                        state = State_ManyReady
                        return true
                    } else {
                        nextIterator = null
                    }
                State_Done -> return false
                State_Ready, State_ManyReady -> {
                    state = State_NotReady
                    return true
                }
                else -> throw exceptionalState()
            }

            state = State_Failed
            val step = nextStep!!
            nextStep = null
            step.resumeWith(Result.success(Unit))
        }
    }

    private fun exceptionalState(): Throwable = when (state) {
        State_Done -> NoSuchElementException()
        State_Failed -> IllegalStateException("Iterator has failed.")
        else -> IllegalStateException("Unexpected state of the iterator: $state")
    }


    override suspend fun waitForEvent() {
        state = State_Ready
        val x: Unit = suspendCoroutineUninterceptedOrReturn<Unit> { c ->
            nextStep = c
            COROUTINE_SUSPENDED
        }
    }

    // Completion continuation implementation
    override fun resumeWith(result: Result<Unit>) {
        println("CurrentEvent ${result.getOrThrow()}") // just rethrow exception if it is there
        state = State_Done
    }

    override val context: CoroutineContext
        get() = EmptyCoroutineContext
}
