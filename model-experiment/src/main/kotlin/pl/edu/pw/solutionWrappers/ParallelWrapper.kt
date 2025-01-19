package pl.edu.pw.solutionWrappers

import getElapsedTime
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import pl.edu.pw.solution.Solution
import pl.edu.pw.solution.dto.ExperimentResult

class ParallelWrapper : SolutionWrapper() {
  override fun solve(
    solution: Solution, matrices: List<Pair<Array<DoubleArray>, Array<DoubleArray>>>
  ): ExperimentResult {
    val startTime = System.nanoTime()
    val results = runBlocking {
      matrices.map { matrix ->
        async { solution.solve(matrix.first, matrix.second) }
      }.map { it.await() }
    }
    val elapsedTime = getElapsedTime(startTime)
    return ExperimentResult(elapsedTime, false)
  }

  override fun toString(): String {
    return "Parallel"
  }
}