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

open class StateMachine<T>(block: suspend StateMachine<T>.() -> Unit) : GeneratorScope<T>(), Continuation<Unit>, Serializable {
    private var state = State_NotReady
    private var nextStep: Continuation<Unit>? = null
    var event = ""
    lateinit var smContext: Context

    companion object {
        private const val State_NotReady: MyState = 0
        private const val State_Ready: MyState = 1
        private const val State_Done: MyState = 2
        private const val State_Failed: MyState = 3

//        suspend fun <T> create(block: suspend StateMachine<T>.() -> Unit): StateMachine<T> {
//            val sm = StateMachine<T>()
//            sm.nextStep = block.createCoroutineUnintercepted<StateMachine<T>, Unit>(receiver = sm, completion = sm)
//            return sm
//        }
    }

    init {
        nextStep = block.createCoroutineUnintercepted<StateMachine<T>, Unit>(receiver = this, completion = this)
    }

    fun io(block: Context.() -> Unit) {
        block(smContext)
    }

    fun fireEvent(event: String, context: Context): Boolean {
        this.event = event
        this.smContext = context
        while (state != State_Done) {
            when (state) {
                State_NotReady -> {
                }
                State_Done -> return false
                State_Ready -> {
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
        return false
    }

    private fun exceptionalState(): Throwable = when (state) {
        State_Done -> NoSuchElementException()
        State_Failed -> IllegalStateException("state machine error.")
        else -> IllegalStateException("Unexpected state of the sm: $state")
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
        println("CurrentEvent ${result}") // just rethrow exception if it is there
        state = State_Done
    }

    override val context: CoroutineContext
        get() = EmptyCoroutineContext
}
