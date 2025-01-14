package pl.edu.pw

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class MatrixService : MatrixServiceGrpcKt.MatrixServiceCoroutineImplBase() {
  override fun add(requests: Flow<MatrixRequest>): Flow<MatrixResponse> = flow {
    requests.collect { request ->
      val response = withContext(Dispatchers.Default) {
        doOperation(request) { a, b -> a + b }
      }
      emit(response)
    }
  }

  override fun sub(requests: Flow<MatrixRequest>): Flow<MatrixResponse> = flow {
    requests.collect { request ->
      val response = withContext(Dispatchers.Default) {
        doOperation(request) { a, b -> a - b }
      }
      emit(response)
    }
  }

  override fun multiply(requests: Flow<MatrixRequest>): Flow<MatrixResponse> = flow {
    requests.collect { request ->
      val response = withContext(Dispatchers.Default) {
        computeMatrixProduct(request)
      }
      emit(response)
    }
  }

  private suspend fun computeMatrixProduct(request: MatrixRequest): MatrixResponse =
    withContext(Dispatchers.Default) { // Ensure computation runs on the Default dispatcher
      val aMatrix = request.aMatrixList.map { it.valuesList.toDoubleArray() }
      val bMatrixTransposed = Array(request.bMatrixList[0].valuesList.size) { j ->
        DoubleArray(request.bMatrixList.size) { k -> request.bMatrixList[k].valuesList[j] }
      }

      val resultMatrixList = aMatrix.map { row ->
        val resultRow = bMatrixTransposed.map { col ->
          row.indices.sumOf { k -> row[k] * col[k] }
        }
        DoubleArrayM.newBuilder().addAllValues(resultRow).build()
      }

      MatrixResponse.newBuilder()
        .addAllResultMatrix(resultMatrixList)
        .build()
    }


  override fun multiplyByScalar(requests: Flow<SingleMatrixWithScalarRequest>): Flow<MatrixResponse> = flow {
    requests.collect { request ->
      val response = withContext(Dispatchers.Default) {
        val matrix = request.matrixList
        val scalar = request.scalar

// Use a single loop and process each row efficiently
        val resultMatrixList = matrix.map { row ->
          val resultValues = row.valuesList.map { it * scalar } // Scale each element
          DoubleArrayM.newBuilder().addAllValues(resultValues).build() // Build DoubleArrayM
        }

        MatrixResponse.newBuilder()
          .addAllResultMatrix(resultMatrixList)
          .build()
      }

      emit(response)
    }
  }

  override fun transpose(requests: Flow<SingleMatrixRequest>): Flow<MatrixResponse> = flow {
    requests.collect { request ->
      val response = withContext(Dispatchers.Default) {
        val aMatrix = request.matrixList

// Transpose the matrix efficiently
        val columnCount = aMatrix[0].valuesList.size
        val rowCount = aMatrix.size

        val resultMatrixList = (0 until columnCount).map { colIndex ->
          val transposedRow = DoubleArray(rowCount) { rowIndex -> aMatrix[rowIndex].valuesList[colIndex] }
          DoubleArrayM.newBuilder().addAllValues(transposedRow.toList()).build()
        }

        MatrixResponse.newBuilder()
          .addAllResultMatrix(resultMatrixList)
          .build()
      }


      emit(response)
    }
  }


  private fun doOperation(request: MatrixRequest, operation: (Double, Double) -> (Double)): MatrixResponse {
    val aMatrix = request.aMatrixList
    val bMatrix = request.bMatrixList

// Efficiently compute the result matrix
    val resultMatrixList = aMatrix.indices.map { rowIndex ->
      val aRow = aMatrix[rowIndex].valuesList
      val bRow = bMatrix[rowIndex].valuesList

      // Perform the operation element-wise using indexed iteration
      val resultRow = DoubleArray(aRow.size) { colIndex -> operation(aRow[colIndex], bRow[colIndex]) }

      DoubleArrayM.newBuilder().addAllValues(resultRow.toList()).build()
    }

    return MatrixResponse.newBuilder()
      .addAllResultMatrix(resultMatrixList)
      .build()
  }
}
