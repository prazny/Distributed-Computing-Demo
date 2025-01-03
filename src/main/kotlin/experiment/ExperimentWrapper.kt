package pl.edu.pw.experiment

import kotlinx.coroutines.runBlocking
import pl.edu.pw.ConfigurationProvider
import pl.edu.pw.solution.Solution
import pl.edu.pw.solution.SyncStructuralSolution

class ExperimentWrapper(
  private val config: ConfigurationProvider, private val solutions: List<Solution>
) {

  private fun printResults(results: Map<Solution, Solution.Companion.RoundResult>) {
    var timeSync = 0.0
    for (solution in results.keys) {
      if (solution is SyncStructuralSolution) {
        timeSync = results[solution]?.elapsedTime ?: 0.0
      }

      println(
        "${solution.javaClass}: iterations = ${results[solution]?.iterations}, " + "time elapsed = ${
          "%.2f".format(
            results[solution]?.elapsedTime
          )
        } seconds, " + "norm = ${results[solution]?.norm}, " + "S(n, p) = ${"%.2f".format(timeSync / results[solution]?.elapsedTime!!)}"
      )
    }
  }

  private fun prepareAccumulatedResults(
    results: Map<Solution, Solution.Companion.RoundResult>,
    accumulatedResults: MutableMap<Solution, Solution.Companion.RoundResult>
  ) {
    for (result in results.keys) {
      if (!accumulatedResults.containsKey(result)) {
        accumulatedResults[result] = Solution.Companion.RoundResult(0, 0.0, 0.0)
      }

      accumulatedResults[result]?.iterations =
        accumulatedResults[result]?.iterations?.plus(results[result]?.iterations ?: 0)!!
      accumulatedResults[result]?.elapsedTime =
        accumulatedResults[result]?.elapsedTime?.plus(results[result]?.elapsedTime ?: 0.0)!!
      accumulatedResults[result]?.norm = accumulatedResults[result]?.norm?.plus(results[result]?.norm ?: 0.0)!!
    }

  }

  fun proceed() {
    var accumulatedResults: MutableMap<Solution, Solution.Companion.RoundResult> = mutableMapOf()

    (0 until config.roundsValue).map {
      runBlocking {
        val results = solutions.associate {
          it to it.solve(config.aMatrix, config.bMatrix)
        }

        prepareAccumulatedResults(results, accumulatedResults)
      }
    }

    for (result in accumulatedResults.keys) {
      accumulatedResults[result]?.iterations = accumulatedResults[result]?.iterations?.div(config.roundsValue)!!
      accumulatedResults[result]?.elapsedTime = accumulatedResults[result]?.elapsedTime?.div(config.roundsValue)!!
      accumulatedResults[result]?.norm = accumulatedResults[result]?.norm?.div(config.roundsValue)!!
    }

    printResults(accumulatedResults)

  }
}
