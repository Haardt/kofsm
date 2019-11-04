package costate

class SMTest: StateMachine<String>({

    fun idle() {
        io {

        }
    }

    idle()
    waitForEvent()
    idle@ while(true) when(event) {
        "Test1" -> println("Test1")
        "Test2" -> {
            test3@ while(true) when(waitForEvent().run { event }) {
                "Test3" -> println("Hallo")
                "Test4" -> break@idle
                else -> println("Unknown event...")
            }
        }
    }
    println("Done")

})