package pl.edu.pw.experiment

import kotlinx.coroutines.runBlocking
import pl.edu.pw.ConfigurationProvider
import pl.edu.pw.solution.Solution
import pl.edu.pw.solution.dto.RoundResult

class ExperimentWrapper(
  private val config: ConfigurationProvider, private val solutions: List<Solution>
) {

  fun proceed() {
    val accumulatedResults: MutableMap<Solution, RoundResult> = mutableMapOf()

    (0 until config.roundsValue).map {
      runBlocking {
        val results = solutions.associateWith { it.solve(config.aMatrix, config.bMatrix) }

        RoundResult.prepareAccumulatedResults(results, accumulatedResults)
      }
    }

    RoundResult.calculateAverageResults(accumulatedResults, config.roundsValue)
    RoundResult.printResults(accumulatedResults)
  }
}
