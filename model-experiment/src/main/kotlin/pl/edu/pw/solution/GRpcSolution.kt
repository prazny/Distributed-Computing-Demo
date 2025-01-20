package pl.edu.pw.solution

import getElapsedTime
import pl.edu.pw.solution.dto.RoundResult
import pl.edu.pw.solution.grpc.LinearEquationClient
import kotlin.math.sqrt

class GRpcSolution(override val tolerance: Double, val threadCount: Int, val client: LinearEquationClient) :
  Solution(tolerance) {

  override suspend fun solve(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): RoundResult {
    val startTime = System.nanoTime()
    val res = client.solveLinearEquation(aMatrix, bMatrix, threadCount, tolerance)
    val elapsedTime = getElapsedTime(startTime)
    return RoundResult(0, elapsedTime, 0.0, false, checkSolution(aMatrix, res, bMatrix))
  }

  private fun subtractIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    if (aMatrix.size != bMatrix.size || aMatrix[0].size != bMatrix[0].size)
      throw IllegalArgumentException("Shapes are not equal.")

    return Array(aMatrix.size) { i ->
      DoubleArray(aMatrix[0].size) { j -> aMatrix[i][j] - bMatrix[i][j] }
    }
  }

  override suspend fun checkSolution(
    aMatrix: Array<DoubleArray>,
    xMatrix: Array<DoubleArray>,
    bMatrix: Array<DoubleArray>
  ): Double {
    val result = subtractIND(multiplyIND(aMatrix, xMatrix), bMatrix)

    return sqrt(result.normSquared())
  }


  private fun multiplyIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    val result: Array<DoubleArray>
    //  val executionTime = measureNanoTime {
    val rowsA = aMatrix.size
    val colsA = aMatrix[0].size
    val rowsB = bMatrix.size
    val colsB = bMatrix[0].size

    if (colsA != rowsB) throw IllegalArgumentException("Columns are not equal.")

    result = Array(rowsA) { DoubleArray(colsB) }
    for (i in 0 until rowsA) {
      for (j in 0 until colsB) {
        for (k in 0 until colsA) {
          result[i][j] += aMatrix[i][k] * bMatrix[k][j]
        }
      }
    }

    //}
    // println("Czas wykonania funkcji sync: $executionTime ns")
    return result
  }

  override fun toString(): String {
    return "gRPC"
  }
}