package pl.edu.pw.solution.grpc

import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import pl.edu.pw.*

class MatrixClient(port: Int) {
  private val stub: MatrixServiceGrpcKt.MatrixServiceCoroutineStub

  companion object {
    private fun Array<DoubleArray>.toDoubleArrayM(): List<DoubleArrayM> {
      return this.map { row ->
        DoubleArrayM.newBuilder()
          .addAllValues(row.toList())
          .build()
      }
    }

    private fun List<DoubleArrayM>.toArrayDoubleArray(): Array<DoubleArray> =
      this.map { it.valuesList.toDoubleArray() }.toTypedArray()
  }

  init {
    val channel = ManagedChannelBuilder.forAddress("localhost", port)
      .usePlaintext()
      .build()

    stub = MatrixServiceGrpcKt.MatrixServiceCoroutineStub(channel)
  }

  fun addMatrixes(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> = runBlocking {
    val request = MatrixRequest
      .newBuilder()
      .addAllAMatrix(aMatrix.toDoubleArrayM())
      .addAllBMatrix(bMatrix.toDoubleArrayM())
      .build()

    val responseFlow = stub.add(listOf(request).asFlow())

    val resultMatrixList = mutableListOf<DoubleArrayM>()
    responseFlow.collect { response ->
      resultMatrixList.addAll(response.resultMatrixList)
    }

    resultMatrixList.toArrayDoubleArray()
  }

  fun subMatrixes(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> = runBlocking {
    val request = MatrixRequest
      .newBuilder()
      .addAllAMatrix(aMatrix.toDoubleArrayM())
      .addAllBMatrix(bMatrix.toDoubleArrayM())
      .build()

    val responseFlow = stub.sub(listOf(request).asFlow())

    val resultMatrixList = mutableListOf<DoubleArrayM>()
    responseFlow.collect { response ->
      resultMatrixList.addAll(response.resultMatrixList)
    }

    resultMatrixList.toArrayDoubleArray()
  }

  fun multiplyMatrixes(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> = runBlocking {
    val request = MatrixRequest
      .newBuilder()
      .addAllAMatrix(aMatrix.toDoubleArrayM())
      .addAllBMatrix(bMatrix.toDoubleArrayM())
      .build()

    val responseFlow = stub.multiply(listOf(request).asFlow())

    val resultMatrixList = mutableListOf<DoubleArrayM>()
    responseFlow.collect { response ->
      resultMatrixList.addAll(response.resultMatrixList)
    }

    resultMatrixList.toArrayDoubleArray()
  }

  fun multiplyMatrixByScalar(matrix: Array<DoubleArray>, scalar: Double): Array<DoubleArray> = runBlocking {
    val request = SingleMatrixWithScalarRequest
      .newBuilder()
      .addAllMatrix(matrix.toDoubleArrayM())
      .setScalar(scalar)
      .build()

    val responseFlow = stub.multiplyByScalar(listOf(request).asFlow())

    val resultMatrixList = mutableListOf<DoubleArrayM>()
    responseFlow.collect { response ->
      resultMatrixList.addAll(response.resultMatrixList)
    }

    resultMatrixList.toArrayDoubleArray()
  }

  fun transposeMatrix(aMatrix: Array<DoubleArray>): Array<DoubleArray> = runBlocking {
    val request = SingleMatrixRequest
      .newBuilder()
      .addAllMatrix(aMatrix.toDoubleArrayM())
      .build()

    val responseFlow = stub.transpose(listOf(request).asFlow())

    val resultMatrixList = mutableListOf<DoubleArrayM>()
    responseFlow.collect { response ->
      resultMatrixList.addAll(response.resultMatrixList)
    }

    resultMatrixList.toArrayDoubleArray()
  }
}
