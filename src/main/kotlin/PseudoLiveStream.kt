import java.io.InputStream
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class PseudoLiveStream {
    class Server(port: Int) {
        val socky = Socky.Server(port)
        val sockyAudio = SockyAudio.Server(socky)

        var currentTime = AtomicInteger(0)

        init {
            startTimer()
        }

        fun startTimer() {
            Timer().scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    currentTime.getAndIncrement()
                }
            }, 0, 1000)
        }

        fun hookCurTimeReqs() {
            socky.clientSocketHelpers.forEach {
                thread(start = true) {
                    while (true) {
                        val bytes = it.receive()
                        if (String(bytes) == "time") {
                            it.send(byteArrayOf(currentTime.get().toByte()))
                        }
                    }
                }
            }
        }

        fun queue(audioStream: InputStream) {
            sockyAudio.serve(audioStream)
        }
    }

    class Client(hostname: String, port: Int) {
        val socky = Socky.Client(hostname, port)
        val sockyAudio = SockyAudio.Client(socky)

        fun updateCurTime() {
            socky.send("time".toByteArray())
            val bytes = socky.receive()
            val time = bytes[0].toInt()
            sockyAudio.seek(time)
        }

        fun play() {
            sockyAudio.init()
            updateCurTime()
            sockyAudio.play()
        }

        fun pause() = sockyAudio.pause()
    }
}