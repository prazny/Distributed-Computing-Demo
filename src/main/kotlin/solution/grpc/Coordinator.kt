package pl.edu.pw.solution.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import pl.edu.pw.*

class Coordinator(private val workerAddresses: List<Int>) {

  private var currentIndex = 0
  private val stubs: List<MatrixServiceGrpcKt.MatrixServiceCoroutineStub>

  init {
    stubs = workerAddresses.map { address ->
      val channel = ManagedChannelBuilder.forAddress("localhost", address).usePlaintext().build()
      MatrixServiceGrpcKt.MatrixServiceCoroutineStub(channel)
    }
  }

  private fun getNextStub(): MatrixServiceGrpcKt.MatrixServiceCoroutineStub {
    val stub = stubs[currentIndex]
    currentIndex = (currentIndex + 1) % stubs.size
    return stub
  }

  suspend fun <T : Any> distributeTask(taskType: TaskType, request: Any): T {
    val stub = getNextStub()

    return when (taskType) {
      TaskType.MULTIPLY_MATRIX_VECTOR -> {
        val multiplyRequest = request as Flow<StreamMultiplyRequest>
        stub.multiplyMatrixVector(multiplyRequest) as T
      }
      TaskType.MULTIPLY_VECTOR_SCALAR -> {
        val ScalarMultiplyStreamRequest = request as ScalarMultiplyRequest
        stub.multiplyVectorScalar(ScalarMultiplyStreamRequest) as T
      }
      TaskType.ADD_VECTORS -> {
        val addRequest = request as VectorOpRequest
        stub.addVectors(addRequest) as T
      }
      TaskType.SUB_VECTORS -> {
        val subRequest = request as VectorOpRequest
        stub.subVectors(subRequest) as T
      }
      TaskType.TRANSPOSE_MATRIX -> {
        val transposeRequest = request as TransposeRequest
        stub.transpose(transposeRequest) as T
      }
    }
  }

  fun shutdownChannels() {
    stubs.forEach { stub ->
      (stub.channel as ManagedChannel).shutdown()
    }
  }
}

enum class TaskType {
  MULTIPLY_MATRIX_VECTOR,
  MULTIPLY_VECTOR_SCALAR,
  ADD_VECTORS,
  SUB_VECTORS,
  TRANSPOSE_MATRIX
}
