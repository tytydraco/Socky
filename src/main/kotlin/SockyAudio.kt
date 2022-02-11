import java.io.BufferedInputStream
import java.io.InputStream
import javax.sound.sampled.AudioSystem

class SockyAudio {
    companion object {
        const val DEFAULT_SERVER_BUFFER_SIZE = 2048
    }

    class Server(val server: Socky.Server, bufferSize: Int = DEFAULT_SERVER_BUFFER_SIZE) {
        private val buffer = ByteArray(bufferSize)

        /**
         * Serve audio from stream to all target sockets until empty
         */
        fun serve(audioStream: InputStream) {
            while (true) {
                val read = audioStream.read(buffer)
                if (read == -1)
                    break
                server.clientSocketHelpers.forEach {
                    it
                        .outputStream
                        .write(buffer, 0, read)
                }
            }
        }
    }

    class Client(val client: Socky.Client) {
        private val audioClip = AudioSystem.getClip()

        fun init() {
            audioClip.open(AudioSystem.getAudioInputStream(BufferedInputStream(client.socketHelper.inputStream)))
        }

        /**
         * Play audio from server
         */
        fun play() {
            with(audioClip) {
                start()
                drain()
            }
        }

        fun pause() = audioClip.stop()

        fun reset() {
            pause()
            seek(0)
            init()
        }

        fun seek(time: Int) {
            val microseconds = time * 1_000_000L
            audioClip.microsecondPosition = microseconds
        }
    }
}