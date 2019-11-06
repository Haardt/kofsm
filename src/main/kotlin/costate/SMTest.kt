package costate

class SMTest: StateMachine<String>({

    fun idle() {
        io {
            println("Idle")
        }
    }

    suspend fun waitForNextThing() {
        println("make thing and wait again")
        test3@ while(true) when(waitForEvent()) {
            "Test3" -> println("Hallo")
            "Test4" -> break@test3
            else -> println("Unknown event...")
        }
    }

    idle()
    idle@ while(true) when(waitForEvent()) {
        "Test1" -> println("Test1")
        "Test2" -> waitForNextThing()
        "Test5" -> break@idle
    }
    println("Done")

})