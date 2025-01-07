package pl.edu.pw

import kotlin.concurrent.thread


class GRpcServer {
  private val servers = mutableListOf<MatrixServer>()
  fun startServers(ports: List<Int>) {
    ports.forEach { port ->
     thread {
        val server = MatrixServer(port)
        server.start()
        server.blockUntilShutdown()
        servers.add(server)
      }
    }
  }

  fun stopServers() {
    servers.forEach { it.stop() }
    servers.clear()
  }
}
