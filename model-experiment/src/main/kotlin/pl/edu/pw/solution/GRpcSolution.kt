package pl.edu.pw.solution

import Matrix
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

    // Convert input arrays to Matrix objects
    var aMatrix = Matrix.fromDoubleArrayArray(aMatrix)
    var bMatrix = Matrix.fromDoubleArrayArray(bMatrix)
    var xMatrix = Matrix.fromDoubleArrayArray(Array(bMatrix.rowCount()) { DoubleArray(1) })

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

      // Parallelize the updates for xMatrix and p
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
   * Calculates the dot product of two matrices.
   */
  private suspend fun dotProduct(aMatrix: Matrix, bMatrix: Matrix): Double {
    val result = multiplyIND(aMatrix, bMatrix)
    return result.toDoubleArrayList().sumOf { it.sum() }
  }

  /**
   * Multiplies aMatrix and bMatrix.
   * The operation is split into chunks, and a segment of aMatrix is sent to gRPC server for multiplication.
   */
  private suspend fun multiplyIND(aMatrix: Matrix, bMatrix: Matrix): Matrix {
    val count = ceil(aMatrix.rowCount().toDouble() / maxMessageSize.toDouble()).toInt()
    require(aMatrix.rowCount() > 0 && bMatrix.rowCount() > 0) {
      "The input matrices must not be empty."
    }
    require(aMatrix.columnCount() == bMatrix.rowCount()) {
      "Input matrices are not compatible for multiplication: " +
        "aMatrix.columns (${aMatrix.columnCount()}) != bMatrix.rows (${bMatrix.rowCount()})"
    }

    val aSubMatrices = divideMatrixHorizontally(aMatrix, count)

    val results = coroutineScope {
      aSubMatrices.map { aFragment ->
        async {
          getClient().multiplyMatrixes(aFragment, bMatrix)
        }
      }
    }.awaitAll()

    // Flatten all the resulting matrices into a single matrix
    val flattenedResults = results.flatMap { it.toDoubleArrayList() }

    // Return the combined matrix
    return Matrix.fromGMatrixRows(flattenedResults)
  }

  private fun multiplyINDByScalar(matrix: Matrix, scalar: Double): Matrix {
    return getClient().multiplyMatrixByScalar(matrix, scalar)
  }

  /**
   * Adds two matrices element-wise.
   */
  private suspend fun addIND(aMatrix: Matrix, bMatrix: Matrix): Matrix {
    if (aMatrix.rowCount() != bMatrix.rowCount() || aMatrix.columnCount() != bMatrix.columnCount())
      throw IllegalArgumentException("Matrix shapes are not equal.")
    return makeRequestWithDividedMatrixes(aMatrix, bMatrix) { a, b ->
      getClient().addMatrixes(a, b)
    }
  }

  /**
   * Subtracts two matrices element-wise.
   */
  private suspend fun subtractIND(aMatrix: Matrix, bMatrix: Matrix): Matrix {
    if (aMatrix.rowCount() != bMatrix.rowCount() || aMatrix.columnCount() != bMatrix.columnCount())
      throw IllegalArgumentException("Matrix shapes are not equal.")
    return makeRequestWithDividedMatrixes(aMatrix, bMatrix) { a, b ->
      getClient().subMatrixes(a, b)
    }
  }

  /**
   * Transposes a matrix.
   */
  private fun transposeIND(matrix: Matrix): Matrix {
    return getClient().transposeMatrix(matrix)
  }

  /**
   * Divides the matrices into smaller sub-matrices for processing in parallel.
   */
  private suspend fun makeRequestWithDividedMatrixes(
    aMatrix: Matrix,
    bMatrix: Matrix,
    operation: (Matrix, Matrix) -> Matrix
  ): Matrix {
    val aMatrixDivided = divideIntoSubMatrixes(aMatrix.rowCount() / maxMessageSize + 1, aMatrix)
    val bMatrixDivided = divideIntoSubMatrixes(bMatrix.rowCount() / maxMessageSize + 1, bMatrix)

    val results = mutableListOf<Deferred<Matrix>>()

    coroutineScope {
      for (i in aMatrixDivided.indices) {
        val deferredResult = async {
          operation(aMatrixDivided[i], bMatrixDivided[i])
        }
        results.add(deferredResult)
      }
    }

    return combineResults(results, aMatrix.rowCount(), bMatrix.columnCount())
  }

  /**
   * Combines results from parallel matrix operations.
   */
  private suspend fun combineResults(
    results: List<Deferred<Matrix>>,
    totalRows: Int,
    totalCols: Int
  ): Matrix {
    val combinedResult = Array(totalRows) { DoubleArray(totalCols) }

    var currentRow = 0
    for (resultDeferred in results) {
      val result = resultDeferred.await()
      val resultArray = result.toDoubleArrayList()

      for (i in resultArray.indices) {
        for (j in resultArray[i].indices) {
          combinedResult[currentRow + i][j] = resultArray[i][j]
        }
      }
      currentRow += result.rowCount()
    }

    return Matrix.fromDoubleArrayArray(combinedResult)
  }

  /**
   * Divides a matrix into sub-matrices (rows).
   */
  private fun divideIntoSubMatrixes(count: Int, matrix: Matrix): Array<Matrix> {
    val rowsPerChunk = (matrix.rowCount() + count - 1) / count
    return (0 until count).map { chunkIndex ->
      val startRow = chunkIndex * rowsPerChunk
      val endRow = minOf(startRow + rowsPerChunk, matrix.rowCount())
      matrix.copyOfRange(startRow, endRow)
    }.toTypedArray()
  }

  /**
   * Divides a matrix horizontally (row chunks).
   */
  private fun divideMatrixHorizontally(matrix: Matrix, count: Int): Array<Matrix> {
    val chunkSize = matrix.rowCount() / count + if (matrix.rowCount() % count > 0) 1 else 0
    return (0 until count).map { i ->
      matrix.copyOfRange(i * chunkSize, minOf((i + 1) * chunkSize, matrix.rowCount()))
    }.toTypedArray()
  }

  private fun getClient() =
    grpcClients.random()
}
