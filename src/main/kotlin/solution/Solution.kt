package pl.edu.pw.solution

abstract class Solution(
  open val tolerance: Double
) {
  companion object {
    class RoundResult(
      val iterations: Int,
      val elapsedTime: Double,
      val norm: Double,
      val isSync: Boolean = false,
    ) {
      companion object {
        fun printResults(results: Map<Solution, RoundResult>) {
          val syncResult = results.values.find { it.isSync }
          val timeSync = syncResult?.elapsedTime ?: 0.0

          results.forEach { (solution, result) ->
            println(
              String.format(
                "%-55s iterations = %-10d time elapsed [s] = %-10.2f norm = %-10.2e S(n, p) = %.2f",
                solution.javaClass, result.iterations, result.elapsedTime, result.norm, timeSync / result.elapsedTime
              )
            )
          }
        }

        fun prepareAccumulatedResults(
          results: Map<Solution, RoundResult>, accumulatedResults: MutableMap<Solution, RoundResult>
        ) {
          results.forEach { (solution, roundResult) ->
            val current = accumulatedResults.getOrPut(solution) {
              RoundResult(0, 0.0, 0.0, roundResult.isSync)
            }

            accumulatedResults[solution] = RoundResult(
              iterations = current.iterations + roundResult.iterations,
              elapsedTime = current.elapsedTime + roundResult.elapsedTime,
              norm = current.norm + roundResult.norm,
              isSync = roundResult.isSync
            )
          }
        }

        fun calculateAverageResults(
          accumulatedResults: MutableMap<Solution, RoundResult>, roundsValue: Int
        ) {
          accumulatedResults.forEach { (solution, roundResult) ->
            accumulatedResults[solution] = Solution.Companion.RoundResult(
              iterations = roundResult.iterations / roundsValue,
              elapsedTime = roundResult.elapsedTime / roundsValue,
              norm = roundResult.norm / roundsValue,
              isSync = roundResult.isSync
            )
          }
        }
      }
    }
  }

  abstract suspend fun solve(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): RoundResult

  protected fun Array<DoubleArray>.normSquared(): Double {
    return this.sumOf { row -> row.sumOf { it * it } }
  }

  protected fun getElapsedTime(startTime: Long): Double = (System.nanoTime() - startTime) / 1_000_000_000.0
}
