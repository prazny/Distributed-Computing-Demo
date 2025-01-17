package pl.edu.pw.solution

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pl.edu.pw.*
import pl.edu.pw.solution.grpc.Coordinator
import pl.edu.pw.solution.grpc.TaskType
import kotlin.math.sqrt

class GRpcSolution(override val tolerance: Double, val threadCount: Int, workerAddresses: List<Int>) : Solution(tolerance) {
  private val VERBOSE = true

  @OptIn(DelicateCoroutinesApi::class)
  private val dispatcher = newFixedThreadPoolContext(threadCount, "ParallelThreadPool")
//  private val matrixClient = MatrixClient()
  private val coordinator = Coordinator(workerAddresses)

  override suspend fun solve(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Companion.RoundResult {
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

//    coordinator.shutdownChannels()

    return Companion.RoundResult(i, getElapsedTime(startTime), rNorm)
  }

  /**
   * This should be in separate class but, the exercise conditions require avoiding it.
   */

  private suspend fun dotProduct(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Double {
    val result = multiplyIND(aMatrix, bMatrix)
    return result.sumOf { it.sum() }
  }

  private suspend fun multiplyIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    if (aMatrix[0].size != bMatrix.size)
      throw IllegalArgumentException("Columns or rows are not equal.")

    val result = Array(aMatrix.size) {DoubleArray(bMatrix[0].size) }

    val requests: Flow<StreamMultiplyRequest> = flow {
      emit(StreamMultiplyRequest.newBuilder()
        .setVector(Vector.newBuilder().addAllValues(bMatrix.flatMap { it.asIterable() }.toList()).build())
        .build())

      emit(StreamMultiplyRequest.newBuilder()
        .setRowSize(aMatrix.size)
        .build())

        aMatrix.forEach { row ->
            val request = StreamMultiplyRequest.newBuilder()
              .setRow(MatrixRow.newBuilder().addAllValues(row.toList()).build())
              .build()

            emit(request)
      }
    }.flowOn(Dispatchers.Default)

    val responses = coordinator.distributeTask<StreamMultiplyResponse>(TaskType.MULTIPLY_MATRIX_VECTOR, requests)

    responses.resultsList.forEachIndexed { index, row ->
      result[index] = DoubleArray(1) {row}
    }

    return result
  }

  private suspend fun multiplyINDByScalar(matrix: Array<DoubleArray>, scalar: Double): Array<DoubleArray> {
    val rows = matrix.size
    val cols = matrix[0].size

    val result = Array(rows) { DoubleArray(cols) }

    val request = ScalarMultiplyRequest.newBuilder()
      .setVector(Vector.newBuilder().addAllValues(matrix.flatMap { it.asIterable() }.toList()).build())
      .setScalar(scalar)
      .build()

    val response = coordinator.distributeTask<ScalarMultiplyResponse>(TaskType.MULTIPLY_VECTOR_SCALAR, request)

    response.result.valuesList.forEachIndexed { index, row ->
      result[index] = DoubleArray(1) {row}
    }

    return result;
  }

  private suspend fun addIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    if (aMatrix.size != bMatrix.size || aMatrix[0].size != bMatrix[0].size)
      throw IllegalArgumentException("Shapes are not equal.")

    val result = Array(aMatrix.size) { DoubleArray(bMatrix[0].size) }

    val request = VectorOpRequest.newBuilder()
      .setVector1(Vector.newBuilder().addAllValues(aMatrix.flatMap { it.asIterable() }.toList()).build())
      .setVector2(Vector.newBuilder().addAllValues(bMatrix.flatMap { it.asIterable() }.toList()).build())
      .build()

    val response = coordinator.distributeTask<VectorOpResponse>(TaskType.ADD_VECTORS, request)

    response.result.valuesList.forEachIndexed { index, row ->
      result[index] = DoubleArray(1) {row}
    }

    return result;
  }

  private suspend fun subtractIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    if (aMatrix.size != bMatrix.size || aMatrix[0].size != bMatrix[0].size)
      throw IllegalArgumentException("Shapes are not equal.")

    val result = Array(aMatrix.size) { DoubleArray(bMatrix[0].size) }

    val request = VectorOpRequest.newBuilder()
      .setVector1(Vector.newBuilder().addAllValues(aMatrix.flatMap { it.asIterable() }.toList()).build())
      .setVector2(Vector.newBuilder().addAllValues(bMatrix.flatMap { it.asIterable() }.toList()).build())
      .build()

    val response = coordinator.distributeTask<VectorOpResponse>(TaskType.SUB_VECTORS, request)

    response.result.valuesList.forEachIndexed { index, row ->
      result[index] = DoubleArray(1) {row}
    }

    return result;
  }

  private suspend fun transposeIND(matrix: Array<DoubleArray>): Array<DoubleArray> {
    val request = TransposeRequest.newBuilder()
    val result = Array(matrix.size) { DoubleArray(matrix[0].size) }

    matrix.forEach {row ->
      run {
        request.addMatrix(MatrixRow.newBuilder().addAllValues(row.asIterable()).build())
      }
    }

    val response = coordinator.distributeTask<TransposeResponse>(TaskType.TRANSPOSE_MATRIX, request.build())

    response.transposedMatrixList.forEachIndexed { index, row ->
      result[index] = row.valuesList.toDoubleArray()
    }

    return result
  }
}
