package pl.edu.pw

import pl.edu.pw.experiment.ExperimentWrapper
import pl.edu.pw.solution.GRpcSolution
import pl.edu.pw.solution.ParallelStructuralSolution
import pl.edu.pw.solution.SyncStructuralSolution
import pl.edu.pw.solution.ThreadsStructuralSolution


fun main() {

    val configs = listOf(
        ConfigurationProvider(1000, 1e-6, 5, 3),
//        ConfigurationProvider(2950, 1e-12, 3, 4),
//        ConfigurationProvider(2950, 1e-12, 3, 6),
//        ConfigurationProvider(2950, 1e-12, 3, 8),
//        ConfigurationProvider(2950, 1e-12, 3, 10),
    )
    configs.forEach { config ->
        println("\nConfiguration: $config")
        val solutions = listOf(

//            ParallelStructuralSolution(config.toleranceValue, config.threadCount),
//            ThreadsStructuralSolution(config.toleranceValue, config.threadCount),
             GRpcSolution(config.toleranceValue, config.threadCount, listOf(6000, 6001, 6002, 6003)),
          SyncStructuralSolution(config.toleranceValue),
        )
        val experiment = ExperimentWrapper(config, solutions)
        experiment.proceed()
    }
}

