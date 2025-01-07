package pl.edu.pw

import pl.edu.pw.experiment.ExperimentWrapper
import pl.edu.pw.solution.GRpcSolution
import pl.edu.pw.solution.ParallelSolution
import pl.edu.pw.solution.SyncSolution

fun startExperiment() {

  val configs = listOf(
    ConfigurationProvider(2000, 1e-12, 1, 2, 3),

  )
  configs.forEach { config ->
    println("\nConfiguration: $config")
    val solutions = listOf(
      SyncSolution(config.toleranceValue),
      ParallelSolution(config.toleranceValue, config.threadCount),
  //    ThreadsStructuralSolution(config.toleranceValue, config.threadCount),
      GRpcSolution(config.toleranceValue, 2, config.threadCount),
    )
    val experiment = ExperimentWrapper(config, solutions)
    experiment.proceed()
  }
}

