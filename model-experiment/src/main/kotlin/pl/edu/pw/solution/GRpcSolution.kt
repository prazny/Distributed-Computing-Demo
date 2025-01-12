package pl.edu.pw.solution

import kotlinx.coroutines.*
import pl.edu.pw.solution.dto.RoundResult
import pl.edu.pw.solution.grpc.MatrixClient
import kotlin.math.ceil
import kotlin.math.sqrt

class GRpcSolution(
  override val tolerance: Double,
  private val threadCount: Int,
  private val serverCount: Int,
  private val maxMessageSize: Int,
  private val grpcClients: List<MatrixClient>
) : Solution(tolerance) {
  private val VERBOSE = false

  @OptIn(DelicateCoroutinesApi::class)
  private val dispatcher = newFixedThreadPoolContext(threadCount, "ParallelThreadPool")

  override suspend fun solve(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): RoundResult {
    val startTime = System.nanoTime()

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

      if (i % 100 == 0 && VERBOSE) {
        println(
          "Iteration $i: Norm = ${rNorm}, Time elapsed = ${
            "%.2f".format(getElapsedTime(startTime))
          } seconds"
        )
      }
    } while (rNorm > tolerance)
    val elapsedTime = getElapsedTime(startTime)
    return RoundResult(i, elapsedTime, rNorm)
  }

  /**
   * This should be in separate class but, the exercise conditions require avoiding it.
   */
  private suspend fun dotProduct(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Double {
    val result = multiplyIND(aMatrix, bMatrix)
    return result.sumOf { it.sum() }
  }

  /**
   * aMatrix is always of size (n,n) or (1,n)
   * bMatrix is always of size (n, 1) size
   * To distribute multiplication, we can divide aMatrix into segments
   * and always send a segment of aMatrix along with the full bMatrix.
   */
  private suspend fun multiplyIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    val count = ceil(aMatrix.size.toDouble() / maxMessageSize.toDouble()).toInt()
    require(aMatrix.isNotEmpty() && bMatrix.isNotEmpty()) {
      "The input matrices must not be empty. aMatrix size: ${aMatrix.size}, maxMessageSize: $maxMessageSize"
    }
    require(aMatrix[0].size == bMatrix.size) {
      "Input matrices are not compatible for multiplication: aMatrix columns (${aMatrix[0].size}) != bMatrix rows (${bMatrix.size})."
    }

    val aSubMatrices = divideMatrixHorizontally(aMatrix, count)

    return coroutineScope {
      aSubMatrices.map { aFragment ->
        async {
          getClient().multiplyMatrixes(aFragment, bMatrix)
        }
      }
    }.awaitAll()
      .toTypedArray()
      .flatten()
      .toTypedArray()
  }


  private fun multiplyINDByScalar(matrix: Array<DoubleArray>, scalar: Double): Array<DoubleArray> {
    return getClient().multiplyMatrixByScalar(matrix, scalar)
  }

  private suspend fun addIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    if (aMatrix.size != bMatrix.size || aMatrix[0].size != bMatrix[0].size)
      throw IllegalArgumentException("Shapes are not equal.")

    return makeRequestWithDividedMatrixes(aMatrix, bMatrix) { a, b ->
      getClient().addMatrixes(a, b)
    }
  }

  private suspend fun subtractIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    if (aMatrix.size != bMatrix.size || aMatrix[0].size != bMatrix[0].size)
      throw IllegalArgumentException("Shapes are not equal.")

    return makeRequestWithDividedMatrixes(aMatrix, bMatrix) { a, b ->
      getClient().subMatrixes(a, b)
    }
  }

  private fun transposeIND(matrix: Array<DoubleArray>): Array<DoubleArray> {
    return getClient().transposeMatrix(matrix)
  }

  private suspend fun makeRequestWithDividedMatrixes(
    aMatrix: Array<DoubleArray>,
    bMatrix: Array<DoubleArray>,
    operation: (Array<DoubleArray>, Array<DoubleArray>) -> Array<DoubleArray>
  ): Array<DoubleArray> {
    val aMatrixDivided = divideIntoSubMatrixes(aMatrix.size / maxMessageSize + 1, aMatrix)
    val bMatrixDivided = divideIntoSubMatrixes(aMatrix.size / maxMessageSize + 1, bMatrix)

    val results = mutableListOf<Deferred<Array<DoubleArray>>>()

    coroutineScope {
      for (i in aMatrixDivided.indices) {
        val deferredResult = async {
          operation(aMatrixDivided[i], bMatrixDivided[i])
        }
        results.add(deferredResult)
      }
    }

    return combineResults(results, aMatrix.size, bMatrix[0].size)
  }

  private suspend fun combineResults(
    results: List<Deferred<Array<DoubleArray>>>,
    totalRows: Int,
    totalCols: Int
  ): Array<DoubleArray> {
    val combinedResult = Array(totalRows) { DoubleArray(totalCols) }

    var currentRow = 0
    for (resultDeferred in results) {
      val result = resultDeferred.await()

      for (i in result.indices) {
        for (j in result[i].indices) {
          combinedResult[currentRow + i][j] = result[i][j]
        }
      }
      currentRow += result.size
    }

    return combinedResult
  }

  private fun divideIntoSubMatrixes(count: Int, matrix: Array<DoubleArray>): Array<Array<DoubleArray>> {
    val rowsPerChunk = (matrix.size + count - 1) / count

    return (0 until count).map { chunkIndex ->
      val startRow = chunkIndex * rowsPerChunk
      val endRow = minOf(startRow + rowsPerChunk, matrix.size)
      matrix.copyOfRange(startRow, endRow)
    }.toTypedArray()
  }

  private fun divideMatrixHorizontally(matrix: Array<DoubleArray>, count: Int): Array<Array<DoubleArray>> {
    val chunkSize = matrix.size / count + if (matrix.size % count > 0) 1 else 0
    return (0 until count).map { i ->
      matrix.copyOfRange(i * chunkSize, minOf((i + 1) * chunkSize, matrix.size))
    }.toTypedArray()
  }

  private fun getClient() =
    grpcClients.random()
}
