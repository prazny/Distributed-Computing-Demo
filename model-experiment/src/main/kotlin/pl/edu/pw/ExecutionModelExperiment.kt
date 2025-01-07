package pl.edu.pw

import pl.edu.pw.experiment.ExperimentWrapper
import pl.edu.pw.solution.GRpcSolution
import pl.edu.pw.solution.ParallelSolution
import pl.edu.pw.solution.SyncSolution

fun startExperiment() {

  val configs = listOf(
    ConfigurationProvider(3000, 1e-11, 1, 6, 3, 300),

  )
  configs.forEach { config ->
    println("\nConfiguration: $config")
    val solutions = listOf(
      GRpcSolution(config.toleranceValue, config.threadCount, config.instanceCount, config.maxMessageSize),
      SyncSolution(config.toleranceValue),
      //ParallelSolution(config.toleranceValue, config.threadCount),
  //    ThreadsStructuralSolution(config.toleranceValue, config.threadCount),
    )
    val experiment = ExperimentWrapper(config, solutions)
    experiment.proceed()
  }
}

