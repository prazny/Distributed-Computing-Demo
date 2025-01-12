package pl.edu.pw


fun main(args: Array<String>) {
  if (args.isEmpty()) {
    println("Please provide a port number.")
    return
  }

  val port = args[0].toIntOrNull()
  if (port == null) {
    println("Invalid port number.")
    return
  }

  val grpcServer = GRpcServer()
  grpcServer.startServer(port)
  println("Server started on port $port")
}