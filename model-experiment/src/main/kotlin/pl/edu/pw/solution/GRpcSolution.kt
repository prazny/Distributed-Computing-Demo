package pl.edu.pw.solution

import kotlinx.coroutines.*
import pl.edu.pw.GRpcServer
import pl.edu.pw.solution.dto.RoundResult
import pl.edu.pw.solution.grpc.MatrixClient
import kotlin.math.sqrt

class GRpcSolution(
  override val tolerance: Double,
  private val threadCount: Int,
  private val serverCount: Int
) : Solution(tolerance) {
  private val VERBOSE = false

  @OptIn(DelicateCoroutinesApi::class)
  private val dispatcher = newFixedThreadPoolContext(threadCount, "ParallelThreadPool")
  private var clients: List<MatrixClient> = emptyList()
  private val server: GRpcServer? = null

  override suspend fun solve(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): RoundResult {
    this.prepareRound()
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
    this.finalizeRound()
    return RoundResult(i, elapsedTime, rNorm)
  }

  private fun prepareRound() {
    val server = GRpcServer()
    val ports = (0 until serverCount).map { i -> 5000 + i }
    server.startServers(ports)
    Thread.sleep(100)
    this.clients = ports.map { port -> MatrixClient(port) }
  }

  private fun finalizeRound() {
    server?.stopServers()
  }

  /**
   * This should be in separate class but, the exercise conditions require avoiding it.
   */
  private fun dotProduct(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Double {
    val result = multiplyIND(aMatrix, bMatrix)
    return result.sumOf { it.sum() }
  }

  private fun multiplyIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    if (aMatrix[0].size != bMatrix.size)
      throw IllegalArgumentException("Columns and rows are not equal.")

    return getClient().multiplyMatrixes(aMatrix, bMatrix)
  }

  private fun multiplyINDByScalar(matrix: Array<DoubleArray>, scalar: Double): Array<DoubleArray> {
    return getClient().multiplyMatrixByScalar(matrix, scalar)
  }

  private suspend fun addIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    if (aMatrix.size != bMatrix.size || aMatrix[0].size != bMatrix[0].size)
      throw IllegalArgumentException("Shapes are not equal.")

    return makeRequestWithDividedMatrixes(aMatrix, bMatrix) { a, b ->
      getClient().addMatrixes(a, b)
    }  }

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
    val aMatrixDivided = divideIntoSquareSubMatrixes(serverCount, aMatrix)
    val bMatrixDivided = divideIntoSquareSubMatrixes(serverCount, bMatrix)

    val results = mutableListOf<Deferred<Array<DoubleArray>>>()

    coroutineScope {
      for (i in 0 until serverCount) {
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

  private fun divideIntoSquareSubMatrixes(count: Int, matrix: Array<DoubleArray>): Array<Array<DoubleArray>> {
    val rowsPerChunk = (matrix.size + count - 1) / count

    return (0 until count).map { chunkIndex ->
      val startRow = chunkIndex * rowsPerChunk
      val endRow = minOf(startRow + rowsPerChunk, matrix.size)
      matrix.copyOfRange(startRow, endRow)
    }.toTypedArray()
  }

  private fun getClient() =
    clients.random()
}