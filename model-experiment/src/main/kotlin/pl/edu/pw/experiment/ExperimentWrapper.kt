package pl.edu.pw.experiment

import kotlinx.coroutines.runBlocking
import pl.edu.pw.ConfigurationProvider
import pl.edu.pw.solution.Solution

class ExperimentWrapper(
  private val config: ConfigurationProvider, private val solutions: List<Solution>
) {

  fun proceed() {
    val accumulatedResults: MutableMap<Solution, Solution.Companion.RoundResult> = mutableMapOf()

    (0 until config.roundsValue).map {
      runBlocking {
        val results = solutions.associate {
          it to it.solve(config.aMatrix, config.bMatrix)
        }

        Solution.Companion.RoundResult.prepareAccumulatedResults(results, accumulatedResults)
      }
    }

    Solution.Companion.RoundResult.calculateAverageResults(accumulatedResults, config.roundsValue)

    Solution.Companion.RoundResult.printResults(accumulatedResults)

  }
}
