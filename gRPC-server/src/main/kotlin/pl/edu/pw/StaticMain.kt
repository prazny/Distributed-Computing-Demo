package pl.edu.pw

fun main() {
  val port = 5000

  val grpcServer = GRPCServer(port)
  grpcServer.start()
  println("Server started on port $port")
}
