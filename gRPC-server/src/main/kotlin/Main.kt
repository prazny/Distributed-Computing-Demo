import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun main() {
  coroutineScope {
    (6000..6006).forEach { port ->


        launch {
          val server = MatrixServer(port)
          server.start()
          server.blockUntilShutdown()
        }


    }
  }



}
