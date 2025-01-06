fun main() {
  val port =  50051
  val server = MatrixServer(port)
  server.start()
  server.blockUntilShutdown()
}