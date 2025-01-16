package pl.edu.pw.solution.grpc

import Matrix
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import pl.edu.pw.*
import kotlinx.coroutines.flow.flow

class MatrixClient(port: Int) {
  private val stub: MatrixServiceGrpcKt.MatrixServiceCoroutineStub

  init {
    val channel = ManagedChannelBuilder.forAddress("localhost", port)
      .usePlaintext()
      .build()

    stub = MatrixServiceGrpcKt.MatrixServiceCoroutineStub(channel)
  }

  fun addMatrixes(aMatrix: Matrix, bMatrix: Matrix): Matrix = runBlocking {
    val flow = (0 until aMatrix.rows.size).map { i ->
      MatrixRequest
        .newBuilder()
        .setAMatrixRow(aMatrix.rows[i])
        .setBMatrixRow(bMatrix.rows[i])
        .build()
    }.asFlow()

    val responseFlow = stub.add(flow)

    val resultMatrixList = mutableListOf<GMatrixRow>()
    responseFlow.collect { response ->
      resultMatrixList.add(response)
    }

    Matrix(resultMatrixList)
  }

  fun subMatrixes(aMatrix: Matrix, bMatrix: Matrix): Matrix = runBlocking {
    val flow = (0 until aMatrix.rows.size).map { i ->
      MatrixRequest
        .newBuilder()
        .setAMatrixRow(aMatrix.rows[i])
        .setBMatrixRow(bMatrix.rows[i])
        .build()
    }.asFlow()

    val responseFlow = stub.sub(flow)

    val resultMatrixList = mutableListOf<GMatrixRow>()
    responseFlow.collect { response ->
      resultMatrixList.add(response)
    }

    Matrix(resultMatrixList)
  }

  fun multiplyMatrixes(aMatrix: Matrix, bMatrix: Matrix): Matrix = runBlocking {
    val requestFlow = flow<MultiplyMatrixRequest>{
      emit(
        MultiplyMatrixRequest.newBuilder()
          .setBMatrix(bMatrix.toGMatrix())
          .build()
      )

      aMatrix.rows.forEach { aRow ->
        emit(
          MultiplyMatrixRequest.newBuilder()
            .setAMatrixRow(aRow)
            .build()
        )
      }
    }

    val responseFlow = stub.multiply(requestFlow)

    val resultMatrixList = mutableListOf<GMatrixRow>()
    responseFlow.collect { response ->
      resultMatrixList.add(response)
    }

    Matrix(resultMatrixList)
  }

  fun multiplyMatrixByScalar(matrix: Matrix, scalar: Double): Matrix = runBlocking {
    val flow = (0 until matrix.rows.size).map { i ->
      SingleMatrixWithScalarRequest
        .newBuilder()
        .setMatrixRow(matrix.rows[i]).setScalar(scalar)
        .build()
    }.asFlow()


    val responseFlow = stub.multiplyByScalar(flow)

    val resultMatrixList = mutableListOf<GMatrixRow>()
    responseFlow.collect { response ->
      resultMatrixList.add(response)
    }

    Matrix(resultMatrixList)
  }

  fun transposeMatrix(matrix: Matrix): Matrix = runBlocking {
    val responseFlow = stub.transpose(listOf(matrix.toGMatrix()).asFlow())

    val resultMatrixList = mutableListOf<GMatrixRow>()
    responseFlow.collect { response ->
      resultMatrixList.addAll(response.rowList)
    }

    Matrix(resultMatrixList)
  }
}
