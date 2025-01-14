package pl.edu.pw.solution.grpc

import Matrix
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import pl.edu.pw.*

class MatrixClient(port: Int) {
  private val stub: MatrixServiceGrpcKt.MatrixServiceCoroutineStub

  init {
    val channel = ManagedChannelBuilder.forAddress("localhost", port)
      .usePlaintext()
      .build()

    stub = MatrixServiceGrpcKt.MatrixServiceCoroutineStub(channel)
  }

  fun addMatrixes(aMatrix: Matrix, bMatrix: Matrix): Matrix = runBlocking {
    val request = MatrixRequest
      .newBuilder()
      .addAllAMatrix(aMatrix.toDoubleArrayMList())
      .addAllBMatrix(bMatrix.toDoubleArrayMList())
      .build()

    val responseFlow = stub.add(listOf(request).asFlow())

    val resultMatrixList = mutableListOf<DoubleArrayM>()
    responseFlow.collect { response ->
      resultMatrixList.addAll(response.resultMatrixList)
    }

    Matrix(resultMatrixList)
  }

  fun subMatrixes(aMatrix: Matrix, bMatrix: Matrix): Matrix = runBlocking {
    val request = MatrixRequest
      .newBuilder()
      .addAllAMatrix(aMatrix.toDoubleArrayMList())
      .addAllBMatrix(bMatrix.toDoubleArrayMList())
      .build()

    val responseFlow = stub.sub(listOf(request).asFlow())

    val resultMatrixList = mutableListOf<DoubleArrayM>()
    responseFlow.collect { response ->
      resultMatrixList.addAll(response.resultMatrixList)
    }

    Matrix(resultMatrixList)
  }

  fun multiplyMatrixes(aMatrix: Matrix, bMatrix: Matrix): Matrix = runBlocking {
    val request = MatrixRequest
      .newBuilder()
      .addAllAMatrix(aMatrix.toDoubleArrayMList())
      .addAllBMatrix(bMatrix.toDoubleArrayMList())
      .build()

    val responseFlow = stub.multiply(listOf(request).asFlow())

    val resultMatrixList = mutableListOf<DoubleArrayM>()
    responseFlow.collect { response ->
      resultMatrixList.addAll(response.resultMatrixList)
    }

    Matrix(resultMatrixList)
  }

  fun multiplyMatrixByScalar(matrix: Matrix, scalar: Double): Matrix = runBlocking {
    val request = SingleMatrixWithScalarRequest
      .newBuilder()
      .addAllMatrix(matrix.toDoubleArrayMList())
      .setScalar(scalar)
      .build()

    val responseFlow = stub.multiplyByScalar(listOf(request).asFlow())

    val resultMatrixList = mutableListOf<DoubleArrayM>()
    responseFlow.collect { response ->
      resultMatrixList.addAll(response.resultMatrixList)
    }

    Matrix(resultMatrixList)
  }

  fun transposeMatrix(aMatrix: Matrix): Matrix = runBlocking {
    val request = SingleMatrixRequest
      .newBuilder()
      .addAllMatrix(aMatrix.toDoubleArrayMList())
      .build()

    val responseFlow = stub.transpose(listOf(request).asFlow())

    val resultMatrixList = mutableListOf<DoubleArrayM>()
    responseFlow.collect { response ->
      resultMatrixList.addAll(response.resultMatrixList)
    }

    Matrix(resultMatrixList)
  }
}
