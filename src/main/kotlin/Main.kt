package pl.edu.pw

import pl.edu.pw.experiment.ExperimentWrapper
import pl.edu.pw.solution.OptimizedSimpleStructuralSolution
import pl.edu.pw.solution.SimpleStructuralSolution


fun main() {
    val config = ConfigurationProvider(1000, 1e-6, 1)
    val solutions = listOf(
        SimpleStructuralSolution(config.toleranceValue),
        OptimizedSimpleStructuralSolution(config.toleranceValue)
    )
    val experiment = ExperimentWrapper(config, solutions)
    experiment.proceed()
}

