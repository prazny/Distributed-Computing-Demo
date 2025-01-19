import pl.edu.pw.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class MatrixService : MatrixServiceGrpcKt.MatrixServiceCoroutineImplBase() {
  override suspend fun addVectors(request: VectorOpRequest): VectorOpResponse {
    return doOperation(request) { a, b -> a + b }
  }

  override suspend fun subVectors(request: VectorOpRequest): VectorOpResponse {
    return doOperation(request) { a, b -> a - b }
  }

  override suspend fun multiplyVectorScalar(request: ScalarMultiplyRequest): ScalarMultiplyResponse {
    val vector = request.vector.valuesList
    val scalar = request.scalar

    val result = vector.map { it * scalar }

    val response = ScalarMultiplyResponse.newBuilder().setResult(Vector.newBuilder().addAllValues(result).build()).build()

    return response
  }

  override suspend fun multiplyMatrixVector(requests: Flow<StreamMultiplyRequest>): StreamMultiplyResponse {
    var vector: List<Double> = listOf()
    var rowSize: Int = 0
    var multiplyResult: MutableList<Double> = mutableListOf()
    var currIndex: Int = 0

    val responses = requests.collect {request ->
      when {
        request.hasVector() -> {
          vector = request.vector.valuesList
        }
        request.hasRowSize() -> {
          rowSize = request.rowSize
          multiplyResult = MutableList(size = rowSize) {0.0}
        }
        request.hasRow() -> {
          val matrixRow = request.row.valuesList
//          println(matrixRow)
          val result = matrixRow.zip(vector).sumOf { it.first * it.second }

          multiplyResult[currIndex++] = result
        }
      }
    }

    return StreamMultiplyResponse.newBuilder().addAllResults(multiplyResult).build()
  }

  override suspend fun transpose(request: TransposeRequest): TransposeResponse {

    val matrix = request.matrixList

    val numRows = matrix.size
    val numCols = matrix[0].valuesList.size
    val transposedMatrix = MutableList(numCols) { MutableList(numRows) { 0.0 } }


    for (i in 0 until numRows) {
      for (j in 0 until numCols) {
        transposedMatrix[j][i] = matrix[i].valuesList[j]
      }
    }

    val response = TransposeResponse.newBuilder()

    for (transposedRow in transposedMatrix) {
      response.addTransposedMatrix(MatrixRow.newBuilder().addAllValues(transposedRow).build())
    }

    return response.build()
  }

  private fun doOperation(request: VectorOpRequest, operation: (Double, Double) -> (Double)): VectorOpResponse {
    val aVector = request.vector1.valuesList
    val bVector = request.vector2.valuesList

    val resultRow = aVector.zip(bVector) { a, b -> operation(a, b) }

    val response = VectorOpResponse.newBuilder().setResult(Vector.newBuilder().addAllValues(resultRow).build()).build()

    return response
  }
}
