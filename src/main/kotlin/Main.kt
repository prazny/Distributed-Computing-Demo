package pl.edu.pw

import pl.edu.pw.experiment.ExperimentWrapper
import pl.edu.pw.solution.SimpleStructuralSolution
import pl.edu.pw.solution.Solution


fun main() {
    val config = ConfigurationProvider(1000, 1e-6, 1)
    val solutions = listOf<Solution>(
        SimpleStructuralSolution(config.toleranceValue)
    )
    val experiment = ExperimentWrapper(config, solutions)
    experiment.proceed()

}

