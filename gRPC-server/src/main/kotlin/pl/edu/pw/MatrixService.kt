package pl.edu.pw

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class MatrixService : MatrixServiceGrpcKt.MatrixServiceCoroutineImplBase() {
  override fun add(requests: Flow<MatrixRequest>): Flow<MatrixResponse> = flow {
    requests.collect { request ->
      val response = doOperation(request) { a, b -> a + b }
      emit(response)
    }
  }

  override fun sub(requests: Flow<MatrixRequest>): Flow<MatrixResponse> = flow {
    requests.collect { request ->
      val response = doOperation(request) { a, b -> a - b }
      emit(response)
    }
  }

  override fun multiply(requests: Flow<MatrixRequest>): Flow<MatrixResponse> = flow {
    requests.collect { request ->
      try {
        val aMatrix = request.aMatrixList
        val bMatrix = request.bMatrixList

        val resultMatrixList = mutableListOf<DoubleArrayM>()

        for (i in aMatrix.indices) {
          val resultRow = mutableListOf<Double>()
          for (j in bMatrix[0].valuesList.indices) {
            var sum = 0.0
            for (k in aMatrix[i].valuesList.indices) {
              sum += aMatrix[i].valuesList[k] * bMatrix[k].valuesList[j]
            }
            resultRow.add(sum)
          }
          resultMatrixList.add(DoubleArrayM.newBuilder().addAllValues(resultRow).build())
        }

        emit(
          MatrixResponse.newBuilder()
            .addAllResultMatrix(resultMatrixList)
            .build()
        )
      } catch (e: Exception) {
        throw e
      }
    }
  }

  override fun multiplyByScalar(requests: Flow<SingleMatrixWithScalarRequest>): Flow<MatrixResponse> = flow {
    requests.collect { request ->
      val matrix = request.matrixList
      val scalar = request.scalar
      val resultMatrixList = mutableListOf<DoubleArrayM>()

      for (i in matrix.indices) {
        val aRow = matrix[i].valuesList
        val resultRow = aRow.map { cell: Double -> cell * scalar }

        val resultDoubleArray = DoubleArrayM.newBuilder()
          .addAllValues(resultRow)
          .build()

        resultMatrixList.add(resultDoubleArray!!)
      }

      emit(
        MatrixResponse.newBuilder()
          .addAllResultMatrix(resultMatrixList)
          .build()
      )
    }
  }

  override fun transpose(requests: Flow<SingleMatrixRequest>): Flow<MatrixResponse> = flow {
    requests.collect { request ->
      val aMatrix = request.matrixList
      val resultMatrixList = mutableListOf<DoubleArrayM>()
      for (colIndex in 0 until aMatrix[0].valuesList.size) {
        val transposedRow = aMatrix.map { it.valuesList[colIndex] }
        resultMatrixList.add(DoubleArrayM.newBuilder().addAllValues(transposedRow).build())
      }

      emit(
        MatrixResponse.newBuilder()
          .addAllResultMatrix(resultMatrixList)
          .build()
      )
    }
  }


  private fun doOperation(request: MatrixRequest, operation: (Double, Double) -> (Double)): MatrixResponse {
    val aMatrix = request.aMatrixList
    val bMatrix = request.bMatrixList
    val resultMatrixList = mutableListOf<DoubleArrayM>()

    for (i in aMatrix.indices) {
      val aRow = aMatrix[i].valuesList
      val bRow = bMatrix[i].valuesList

      val resultRow = aRow.zip(bRow) { a, b -> operation(a, b) }

      val resultDoubleArray = DoubleArrayM.newBuilder()
        .addAllValues(resultRow)
        .build()

      resultMatrixList.add(resultDoubleArray!!)
    }

    return MatrixResponse.newBuilder()
      .addAllResultMatrix(resultMatrixList)
      .build()
  }
}
