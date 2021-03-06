import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread


class Socky {
    companion object {
        const val DEFAULT_SOCKET_TIMEOUT = 10_000
    }

    /**
     * Helper class to automatically open and close streams for us
     */
    class SocketHelper(val socket: Socket): Closeable {
        val inputStream: InputStream by lazy { socket.getInputStream() }
        val inputBufferedReader: BufferedReader by lazy { inputStream.bufferedReader() }
        val outputStream: OutputStream by lazy { socket.getOutputStream() }
        val outputBufferedReader: BufferedWriter by lazy { outputStream.bufferedWriter() }

        @Synchronized
        fun send(byteArray: ByteArray) {
            outputStream.write(byteArray.size)
            outputStream.write(byteArray)
            outputStream.flush()
        }

        @Synchronized
        fun receive(): ByteArray {
            val sizeArray = inputStream.readNBytes(1)
            val size = sizeArray[0].toInt()

            return inputStream.readNBytes(size)
        }

        override fun close() {
            inputStream.close()
            inputBufferedReader.close()
            outputStream.close()
            outputBufferedReader.close()
        }
    }

    class Server(port: Int, timeout: Int = DEFAULT_SOCKET_TIMEOUT): Closeable {
        private val serverSocket = ServerSocket(port).also { it.soTimeout = timeout }
        var clientSocketHelpers = mutableListOf<SocketHelper>()
        private val runLoop = AtomicBoolean(true)
        private val loopThread: Thread

        init {
            loopThread = thread(start = true) {
                serverLoop()
            }
        }

        /**
         * Constantly acquire new clients (synchronized)
         */
        private fun serverLoop() {
            while (runLoop.get()) {
                synchronized(clientSocketHelpers) {
                    try {
                        clientSocketHelpers += SocketHelper(serverSocket.accept())
                    } catch (_: Exception) {}
                }
            }
        }

        fun send(byteArray: ByteArray) {
            clientSocketHelpers.forEach {
                it.send(byteArray)
            }
        }

        fun receive() = clientSocketHelpers.map {
            it to it.receive()
        }

        override fun close() {
            /* Wait for thread to die */
            runLoop.set(false)
            loopThread.join()

            /* Disconnect all clients */
            clientSocketHelpers.forEach { it.close() }

            /* Close server itself */
            serverSocket.close()
        }
    }

    class Client(hostname: String, port: Int, timeout: Int = DEFAULT_SOCKET_TIMEOUT): Closeable {
        val socketHelper = SocketHelper(
            Socket(hostname, port).also { it.soTimeout = timeout }
        )

        fun send(byteArray: ByteArray) = socketHelper.send(byteArray)
        fun receive() = socketHelper.receive()

        override fun close() {
            socketHelper.close()
        }
    }
}