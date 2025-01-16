package pl.edu.pw

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

internal class MatrixService : MatrixServiceGrpcKt.MatrixServiceCoroutineImplBase() {
  override fun add(requests: Flow<MatrixRequest>): Flow<GMatrixRow> = flow {
    requests.collect { request ->
      // withContext(Dispatchers.Default) {
      emit(doOperation(request) { a, b -> a + b })
      //}
    }
  }

  override fun sub(requests: Flow<MatrixRequest>): Flow<GMatrixRow> = flow {
    requests.collect { request ->
      //withContext(Dispatchers.Default) {
      emit(doOperation(request) { a, b -> a - b })
    }
    // }
  }


  override fun multiply(requests: Flow<MultiplyMatrixRequest>): Flow<GMatrixRow> = flow {
    var bMatrix: GMatrix? = null

    requests.collect { request ->
      when (request.requestTypeCase) {
        MultiplyMatrixRequest.RequestTypeCase.BMATRIX -> {
          bMatrix = request.bMatrix
        }

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

  override fun multiplyByScalar(requests: Flow<SingleMatrixWithScalarRequest>): Flow<GMatrixRow> = flow {
    requests.collect { request ->
      //withContext(Dispatchers.Default) {
      val matrixRows = request.matrixRow
      val scalar = request.scalar

      val resultValues = matrixRows.valuesList.map { it * scalar }

      emit(GMatrixRow.newBuilder().addAllValues(resultValues).build())
    }
    // }
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


  private fun doOperation(request: MatrixRequest, operation: (Double, Double) -> (Double)): GMatrixRow {

    val aRow = request.aMatrixRow.valuesList
    val bRow = request.bMatrixRow.valuesList
    val resultRow = DoubleArray(aRow.size) { colIndex -> operation(aRow[colIndex], bRow[colIndex]) }

    return GMatrixRow.newBuilder().addAllValues(resultRow.asList()).build()
  }

}
