package pl.edu.pw.solution.dto

import pl.edu.pw.solution.Solution
import pl.edu.pw.solutionWrappers.SolutionWrapper

data class RoundResult(
  val iterations: Int,
  val elapsedTime: Double,
  val norm: Double,
  val isSync: Boolean = false,
  val solutionChecked: Double = 0.0,
) {
  companion object {
//    fun printResults(results: List<RoundResult>) {
//      val elapsedTime = results.sumOf { it.elapsedTime }
//
//
//      val syncResult = results.values.find { it.isSync }
//      val timeSync = syncResult?.elapsedTime ?: 0.0
//
//      results.forEach { (solution, result) ->
//        println(
//          String.format(
//            "%-55s iterations = %-10d time elapsed [s] = %-10.2f norm = %-10.2e S(n, p) = %.2f",
//            solution.javaClass, result.iterations, result.elapsedTime, result.norm, timeSync / result.elapsedTime
//          )
//        )
//      }
//    }
//
//    fun prepareAccumulatedResults(
//      results: Map<Pair<SolutionWrapper, Solution>, List<RoundResult>>,
//      accumulatedResults: MutableMap<Solution, RoundResult>
//    ) {
//      results.forEach { (solution, roundResult) ->
//        val current = accumulatedResults.getOrPut(solution) {
//          RoundResult(0, 0.0, 0.0, roundResult.isSync)
//        }
//
//        accumulatedResults[solution] = RoundResult(
//          iterations = current.iterations + roundResult.iterations,
//          elapsedTime = current.elapsedTime + roundResult.elapsedTime,
//          norm = current.norm + roundResult.norm,
//          isSync = roundResult.isSync
//        )
//      }
//    }
  }
}