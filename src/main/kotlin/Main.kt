import java.io.FileInputStream
import kotlin.concurrent.thread


fun main(args: Array<String>) {
    //echoTest()
    audioStreamTest()
}

fun audioStreamTest() {
    thread(start = true) {
        val socky = Socky.Server(4444)
        Thread.sleep(100)
        val audio = SockyAudio.Server(socky)
        audio.serve(FileInputStream("C:\\Users\\tyler\\Desktop\\wav.wav"))
    }

    thread(start = true) {
        val socky = Socky.Client("localhost", 4444)
        val audio = SockyAudio.Client(socky)
        audio.play()
        Thread.sleep(5000)
        audio.pause()
    }
}

fun echoTest() {
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