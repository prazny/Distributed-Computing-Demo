package pl.edu.pw

import kotlin.concurrent.thread


class GRpcServer {
  private val servers = mutableListOf<MatrixServer>()

  fun startServer(port: Int) {
    thread {
      val server = MatrixServer(port)
      server.start()
      server.blockUntilShutdown()
      servers.add(server)
    }
  }

  fun startServers(ports: List<Int>) {
    ports.forEach { port ->
      startServer(port)
    }
  }

  fun stopServers() {
    servers.forEach { it.stop() }
    servers.clear()
  }
}
