package pl.edu.pw.experiment

import kotlinx.coroutines.runBlocking
import pl.edu.pw.ConfigurationProvider
import pl.edu.pw.solution.Solution

class ExperimentWrapper(
    private val config: ConfigurationProvider,
    private val solutions: List<Solution>
) {

  fun printResults (result: Map<Solution, Solution.Companion.RoundResult>) {
      for (solution in result.keys) {
        println(solution.javaClass.toString() + ": " + result[solution].toString())
      }
    }

    fun proceed() {
        runBlocking {
            val results = solutions.associate {
                it to it.solve(config.aMatrix, config.bMatrix)
            }
          printResults(results)
        }
    }
}
