import kotlin.concurrent.thread

fun main(args: Array<String>) {
    thread(start = true) {
        val socky = Socky.Server(4444)

        while (true) {
            val out = socky.receive()
            out.forEach {
                println(String(it.second))
            }
        }
    }

    thread(start = true) {
        val socky = Socky.Client("localhost", 4444)
        while (true) {
            socky.send("Hello World".toByteArray())
        }
    }
}