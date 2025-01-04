package pl.edu.pw

import pl.edu.pw.experiment.ExperimentWrapper
import pl.edu.pw.solution.ParallelStructuralSolution
import pl.edu.pw.solution.SyncStructuralSolution
import pl.edu.pw.solution.ThreadsStructuralSolution


fun main() {
    val config = ConfigurationProvider(500, 1e-12, 3)

    val solutions = listOf(
        SyncStructuralSolution(config.toleranceValue),
        ParallelStructuralSolution(config.toleranceValue),
        ThreadsStructuralSolution(config.toleranceValue),
    )
    val experiment = ExperimentWrapper(config, solutions)
    experiment.proceed()
}

