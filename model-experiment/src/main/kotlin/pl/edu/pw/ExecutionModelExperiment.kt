package pl.edu.pw

import pl.edu.pw.experiment.ExperimentWrapper
import pl.edu.pw.solution.GRpcSolution
import pl.edu.pw.solution.SyncSolution
import pl.edu.pw.solution.grpc.MatrixClient

fun startExperiment(grpcClients: List<MatrixClient>) {
  val grpcServerCount = grpcClients.count()


  val configs = listOf(
    ConfigurationProvider(1000, 1e-6, 1, 3, 2, 400),
  )
  configs.forEach { config ->
    require(grpcServerCount >= config.instanceCount)

    println("\nConfiguration: $config")
    val solutions = listOf(
      GRpcSolution(config.toleranceValue, config.threadCount, config.instanceCount, config.maxMessageSize, grpcClients.take(config.instanceCount)),
      SyncSolution(config.toleranceValue),
      //ParallelSolution(config.toleranceValue, config.threadCount),
      //    ThreadsStructuralSolution(config.toleranceValue, config.threadCount),
    )
    val experiment = ExperimentWrapper(config, solutions)
    experiment.proceed()
  }
}

fun main() {
  startExperiment((5000..5003).map { port ->
    MatrixClient(port)
  })
}
