package pl.edu.pw.solution

import kotlinx.coroutines.*
import pl.edu.pw.GRpcServer
import pl.edu.pw.solution.dto.RoundResult
import pl.edu.pw.solution.grpc.MatrixClient
import kotlin.math.ceil
import kotlin.math.sqrt

class GRpcSolution(
  override val tolerance: Double,
  private val threadCount: Int,
  private val serverCount: Int,
  private val maxMessageSize: Int
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
    val ports = (0 until serverCount).map { i -> 6000 + i }
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
  private suspend fun dotProduct(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Double {
    val result = multiplyIND(aMatrix, bMatrix)
    return result.sumOf { it.sum() }
  }

  private suspend fun multiplyIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    val size = aMatrix[0].size
    for (row in aMatrix) {
      if (row.size != size)
        throw IllegalArgumentException("Columns and rows are not equal.")
    }
    if (size != bMatrix.size)
      throw IllegalArgumentException("Columns and rows are not equal.")

    val result = multiplyDistributed(aMatrix, bMatrix)
    if(result.size != aMatrix.size || bMatrix[0].size != result[0].size) {
      throw IllegalArgumentException("Wrong sizes")
    }
    return result
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

  private suspend fun multiplyDistributed(
    aMatrix: Array<DoubleArray>,
    bMatrix: Array<DoubleArray>,
  ): Array<DoubleArray> {
    val count = ceil(aMatrix.size.toDouble() / maxMessageSize.toDouble()).toInt()
    if (count == 0) {
      throw IllegalArgumentException("The matrix is empty." + aMatrix.size + " - " + maxMessageSize)
    }
    if (aMatrix[0].size != bMatrix.size) {
      throw IllegalArgumentException("Entry matrixes are not in shape.")
    }


    val aSubMatrices = divideMatrixVertically(aMatrix, count)
    val bSubMatrices = divideMatrixHorizontally(bMatrix, count)
    for (i in aSubMatrices.indices) {
      if (aSubMatrices[i][0].size != bSubMatrices[i].size) {
        throw IllegalArgumentException("The matrices are not in shape." + aMatrix + bMatrix)
      }
    }
    val results = mutableListOf<Deferred<Array<DoubleArray>>>()

    coroutineScope {
      for (i in 0 until count) {
        val aFragment = aSubMatrices[i]
        val bFragment = bSubMatrices[i]

        if (aFragment[0].size != bFragment.size) {
          throw IllegalArgumentException("The matrixes are not in shape.")
        }

        results.add(async {
          val res = getClient().multiplyMatrixes(aFragment, bFragment)

          if(res.size != aFragment.size || bFragment[0].size != res[0].size) {
            throw IllegalArgumentException("Wrong sizes")
          }
          res
        })
      }
    }
    return combineMatrixFragments(results.map { it.await() })
  }

  private fun combineMatrixFragments(fragments: List<Array<DoubleArray>>): Array<DoubleArray> {
    val totalRows = fragments[0].size
    val totalCols = fragments[0][0].size

    for (fragment in fragments) {
      require(fragment.size == totalRows && fragment[0].size == totalCols) {
        "All matrices must have the same dimensions."
      }
    }

    return fragments.reduce { acc, matrix ->
      acc.zip(matrix) { row1, row2 -> // Combine rows
        row1.zip(row2) { element1, element2 -> element1 + element2 }.toDoubleArray()
      }.toTypedArray()
    }

  }

  private fun divideMatrixHorizontally(matrix: Array<DoubleArray>, count: Int): Array<Array<DoubleArray>> {
    val chunkSize = matrix.size / count + if (matrix.size % count > 0) 1 else 0
    return (0 until count).map { i ->
      matrix.copyOfRange(i * chunkSize, minOf((i + 1) * chunkSize, matrix.size))
    }.toTypedArray()
  }


  private fun divideMatrixVertically(matrix: Array<DoubleArray>, count: Int): Array<Array<DoubleArray>> {

    val chunkSize = matrix[0].size / count + if (matrix[0].size % count > 0) 1 else 0
    return (0 until count).map { i ->
      matrix.map { row ->
        row.copyOfRange(i * chunkSize, minOf((i + 1) * chunkSize, row.size))
      }.toTypedArray()
    }.toTypedArray()
  }


  private fun getClient() =
    clients.random()
}
