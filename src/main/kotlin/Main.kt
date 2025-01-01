package pl.edu.pw

import pl.edu.pw.experiment.ExperimentWrapper
import pl.edu.pw.solution.ParallelStructuralSolution
import pl.edu.pw.solution.SyncStructuralSolution


fun main() {
    val config = ConfigurationProvider(1000, 1e-6, 1)
    val solutions = listOf(
        SyncStructuralSolution(config.toleranceValue),
        ParallelStructuralSolution(config.toleranceValue),
    )
    val experiment = ExperimentWrapper(config, solutions)
    experiment.proceed()
}

