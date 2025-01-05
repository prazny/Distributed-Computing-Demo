package pl.edu.pw

import pl.edu.pw.experiment.ExperimentWrapper
import pl.edu.pw.solution.ParallelStructuralSolution
import pl.edu.pw.solution.SyncStructuralSolution
import pl.edu.pw.solution.ThreadsStructuralSolution


fun main() {

    val configs = listOf(
        ConfigurationProvider(100, 1e-12, 1, 2),
        ConfigurationProvider(100, 1e-12, 1, 3)
    )
    configs.forEach { config ->
        println("\nConfiguration: $config")
        val solutions = listOf(
            SyncStructuralSolution(config.toleranceValue),
            ParallelStructuralSolution(config.toleranceValue, config.threadCount),
            ThreadsStructuralSolution(config.toleranceValue, config.threadCount),
        )
        val experiment = ExperimentWrapper(config, solutions)
        experiment.proceed()
    }
}

