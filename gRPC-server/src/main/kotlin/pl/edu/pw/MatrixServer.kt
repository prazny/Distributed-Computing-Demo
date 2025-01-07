package pl.edu.pw

import io.grpc.Server
import io.grpc.ServerBuilder

class MatrixServer(port: Int) {
  private val server: Server =
    ServerBuilder
      .forPort(port)
      .addService(MatrixService())
      .maxInboundMessageSize(16 * 1024 * 1024)
      .build()

  fun start() {
    server.start()
    server.awaitTermination()
  }

  fun stop() {
    server.shutdown()
  }

  fun blockUntilShutdown() {
    server.awaitTermination()
  }
}