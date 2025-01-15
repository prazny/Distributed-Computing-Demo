package pl.edu.pw

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

internal class MatrixService : MatrixServiceGrpcKt.MatrixServiceCoroutineImplBase() {
  override fun add(requests: Flow<MatrixRequest>): Flow<GMatrix> = flow {
    requests.collect { request ->
      val response = withContext(Dispatchers.Default) {
        doOperation(request) { a, b -> a + b }
      }
      emit(response)
    }
  }

  override fun sub(requests: Flow<MatrixRequest>): Flow<GMatrix> = flow {
    requests.collect { request ->
      val response = withContext(Dispatchers.Default) {
        doOperation(request) { a, b -> a - b }
      }
      emit(response)
    }
  }


  override fun multiply(requests: Flow<MultiplyMatrixRequest>): Flow<GMatrixRow> = flow {
    var bMatrix: GMatrix? = null

    requests.collect { request ->
      when (request.requestTypeCase) {
        MultiplyMatrixRequest.RequestTypeCase.BMATRIX -> bMatrix = request.bMatrix
        MultiplyMatrixRequest.RequestTypeCase.AMATRIXROW -> {
            val aMatrixRow = request.aMatrixRow

            val resultRow = DoubleArray(bMatrix!!.rowList[0].valuesList.size) { colIndex ->
              aMatrixRow.valuesList.indices.sumOf { k ->
                aMatrixRow.valuesList[k] * bMatrix!!.rowList[k].valuesList[colIndex]
              }
            }

          emit(GMatrixRow.newBuilder().addAllValues(resultRow.asList()).build())

        }

        MultiplyMatrixRequest.RequestTypeCase.REQUESTTYPE_NOT_SET -> throw RuntimeException()
      }
    }
  }

  override fun multiplyByScalar(requests: Flow<SingleMatrixWithScalarRequest>): Flow<GMatrix> = flow {
    requests.collect { request ->
      val response = withContext(Dispatchers.Default) {
        val matrixRows = request.matrix.rowList
        val scalar = request.scalar

        val resultMatrixList = matrixRows.map { row ->
          val resultValues = row.valuesList.map { it * scalar } // Scale each element
          GMatrixRow.newBuilder().addAllValues(resultValues).build()
        }

        GMatrix.newBuilder().addAllRow(resultMatrixList).build()
      }

      emit(response)
    }
  }


  override fun transpose(requests: Flow<GMatrix>): Flow<GMatrix> = flow {
    requests.collect { request ->
      val response = withContext(Dispatchers.Default) {
        val aMatrixRows = request.rowList

        val columnCount = aMatrixRows[0].valuesList.size
        val rowCount = aMatrixRows.size

        val resultMatrixList = (0 until columnCount).map { colIndex ->
          val transposedRow = DoubleArray(rowCount) { rowIndex -> aMatrixRows[rowIndex].valuesList[colIndex] }
          GMatrixRow.newBuilder().addAllValues(transposedRow.toList()).build()
        }

        GMatrix.newBuilder().addAllRow(resultMatrixList).build()
      }

      emit(response)
    }
  }



  private fun doOperation(request: MatrixRequest, operation: (Double, Double) -> (Double)): GMatrix {
    val resultMatrixList = request.aMatrix.rowList.indices.map { rowIndex ->
      val aRow = request.aMatrix.rowList[rowIndex].valuesList
      val bRow = request.bMatrix.rowList[rowIndex].valuesList

      val resultRow = DoubleArray(aRow.size) { colIndex -> operation(aRow[colIndex], bRow[colIndex]) }

      GMatrixRow.newBuilder().addAllValues(resultRow.asList()).build()
    }

    return GMatrix.newBuilder().addAllRow(resultMatrixList).build()
  }
}
