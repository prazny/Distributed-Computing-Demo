package pl.edu.pw

fun main() {
  val port = 5000

  val grpcServer = GRpcServer()
  grpcServer.startServer(port)
  println("Server started on port $port")
}
