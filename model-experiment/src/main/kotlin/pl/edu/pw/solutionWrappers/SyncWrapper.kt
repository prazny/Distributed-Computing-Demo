package pl.edu.pw.solutionWrappers

import getElapsedTime
import kotlinx.coroutines.runBlocking
import pl.edu.pw.solution.Solution
import pl.edu.pw.solution.dto.ExperimentResult


class SyncWrapper : SolutionWrapper() {
  override fun solve(
    solution: Solution, matrices: List<Pair<Array<DoubleArray>, Array<DoubleArray>>>
  ): ExperimentResult {
    val startTime = System.nanoTime()
    val results = runBlocking {
      matrices.map { matrix -> solution.solve(matrix.first, matrix.second) }
    }
    val elapsedTime = getElapsedTime(startTime)
    return ExperimentResult(elapsedTime, results[0].isSync)
  }

  override fun toString(): String {
    return "Sync"
  }
}