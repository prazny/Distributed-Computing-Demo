package pl.edu.pw

import pl.edu.pw.experiment.ExperimentWrapper
import pl.edu.pw.solution.OptimizedSimpleStructuralSolution
import pl.edu.pw.solution.SimpleStructuralSolution


fun main() {
    val config = ConfigurationProvider(300, 1e-6, 1)
    println(config.aMatrix)
    println(config.bMatrix)
    val solutions = listOf(
        SimpleStructuralSolution(config.toleranceValue),
        OptimizedSimpleStructuralSolution(config.toleranceValue)
    )
    val experiment = ExperimentWrapper(config, solutions)
    experiment.proceed()
}

