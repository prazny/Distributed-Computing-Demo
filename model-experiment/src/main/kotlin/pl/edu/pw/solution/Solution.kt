package pl.edu.pw.solution

import pl.edu.pw.solution.dto.RoundResult

abstract class Solution(
  open val tolerance: Double
) {

  abstract suspend fun solve(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): RoundResult

  protected fun Array<DoubleArray>.normSquared(): Double {
    return this.sumOf { row -> row.sumOf { it * it } }
  }

  protected fun getElapsedTime(startTime: Long): Double = (System.nanoTime() - startTime) / 1_000_000_000.0
}
