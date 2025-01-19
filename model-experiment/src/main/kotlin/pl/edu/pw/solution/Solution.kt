package pl.edu.pw.solution

import pl.edu.pw.solution.dto.RoundResult

abstract class Solution(
  open val tolerance: Double
) {

  abstract suspend fun solve(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): RoundResult

  protected fun Array<DoubleArray>.normSquared(): Double {
    return this.sumOf { row -> row.sumOf { it * it } }
  }

  abstract suspend fun checkSolution(
    aMatrix: Array<DoubleArray>,
    xMatrix: Array<DoubleArray>,
    bMatrix: Array<DoubleArray>
  ): Double;

}
