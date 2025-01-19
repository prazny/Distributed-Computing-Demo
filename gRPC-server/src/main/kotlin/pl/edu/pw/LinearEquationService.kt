package pl.edu.pw

import kotlinx.coroutines.*
import normSquared
import kotlin.math.sqrt

internal class LinearEquationService : LinearEquationGrpcKt.LinearEquationCoroutineImplBase() {
  val VERBOSE = false
  val THREAD_COUNT = 3

  @OptIn(DelicateCoroutinesApi::class)
  private val dispatcher = newFixedThreadPoolContext(THREAD_COUNT, "ParallelThreadPool")

  fun GMatrix.toDoubleArray(): Array<DoubleArray> {
    return this.rowList.map { it ->
      it.valuesList.map { it }.toDoubleArray()
    }.toTypedArray()
  }

  override suspend fun solve(request: SolveRequest): GMatrix {
    val aMatrix = request.aMatrix.toDoubleArray()
    val bMatrix = request.bMatrix.toDoubleArray()
    val tolerance = request.tolerance

    var xMatrix = Array(bMatrix.size) { DoubleArray(1) }
    var r = subtractIND(bMatrix, multiplyIND(aMatrix, xMatrix))
    var rNormSquared = r.normSquared()
    var rNorm: Double

    var p = r
    var beta: Double

    var i = 0
    do {
      i++
      val q = multiplyIND(aMatrix, p)

      val alfa = rNormSquared / dotProduct(transposeIND(p), q)

      val rPrevNormSquared = rNormSquared
      r = subtractIND(r, multiplyINDByScalar(q, alfa))
      rNormSquared = r.normSquared()
      rNorm = sqrt(rNormSquared)

      beta = rNormSquared / rPrevNormSquared

      coroutineScope {
        launch(dispatcher) {
          xMatrix = addIND(xMatrix, multiplyINDByScalar(p, alfa))
        }
        launch(dispatcher) {
          p = addIND(r, multiplyINDByScalar(p, beta))
        }
      }
    } while (rNorm > tolerance)

    return GMatrix.newBuilder().build()
  }

  /**
   * This should be in separate class but, the exercise conditions require avoiding it.
   */

  private suspend fun dotProduct(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Double {
    val result = multiplyIND(aMatrix, bMatrix)
    return result.sumOf { it.sum() }
  }

  private suspend fun multiplyIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    val rowsA = aMatrix.size
    val colsA = aMatrix[0].size
    val rowsB = bMatrix.size
    val colsB = bMatrix[0].size

    if (colsA != rowsB) throw IllegalArgumentException("Columns are not equal.")

    val result = Array(rowsA) { DoubleArray(colsB) }
    applyCoroutineScopeWithChunks(rowsA) { i ->
      for (j in 0 until colsB) {
        for (k in 0 until colsA) {
          result[i][j] += aMatrix[i][k] * bMatrix[k][j]
        }
      }
    }
    return result
  }

  private suspend fun multiplyINDByScalar(matrix: Array<DoubleArray>, scalar: Double): Array<DoubleArray> {
    val rows = matrix.size
    val result = Array(rows) { DoubleArray(matrix[0].size) }

    applyCoroutineScopeWithChunks(rows) { i ->
      for (j in matrix[i].indices) {
        result[i][j] = matrix[i][j] * scalar
      }
    }
    return result
  }

  private suspend fun addIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    if (aMatrix.size != bMatrix.size || aMatrix[0].size != bMatrix[0].size)
      throw IllegalArgumentException("Shapes are not equal.")

    val result = Array(aMatrix.size) { DoubleArray(bMatrix[0].size) }

    applyCoroutineScopeWithChunks(aMatrix.size) { row ->
      for (col in 0 until aMatrix[row].size) {
        result[row][col] = aMatrix[row][col] + bMatrix[row][col]
      }
    }
    return result
  }

  private suspend fun subtractIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    if (aMatrix.size != bMatrix.size || aMatrix[0].size != bMatrix[0].size)
      throw IllegalArgumentException("Shapes are not equal.")

    val result = Array(aMatrix.size) { DoubleArray(bMatrix[0].size) }

    applyCoroutineScopeWithChunks(aMatrix.size) { row ->
      for (col in 0 until aMatrix[row].size) {
        result[row][col] = aMatrix[row][col] - bMatrix[row][col]
      }
    }
    return result
  }

  private fun transposeIND(matrix: Array<DoubleArray>): Array<DoubleArray> {
    val rows = matrix.size
    val cols = matrix[0].size

    return Array(cols) { i -> DoubleArray(rows) { j -> matrix[j][i] } }
  }

  private suspend fun applyCoroutineScopeWithChunks(count: Int, operation: suspend (Int) -> Unit) {
    // Divide (count / thread_count) and ceil
    val chunkSize = (count + THREAD_COUNT - 1) / THREAD_COUNT

    return coroutineScope {
      (0 until count step chunkSize).map { startRow ->
        launch(dispatcher) {
          for (i in startRow until (startRow + chunkSize).coerceAtMost(count)) {
            operation(i)
          }
        }
      }.joinAll()
    }
  }
}
