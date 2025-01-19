fun main(args: Array<String>) {
  val server = MatrixServer(args[0].toInt())
  server.start()
  server.blockUntilShutdown()
}
