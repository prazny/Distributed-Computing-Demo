import pl.edu.pw.*

internal class MatrixService : MatrixServiceGrpcKt.MatrixServiceCoroutineImplBase() {
  override suspend fun add(request: MatrixRequest): MatrixResponse {
    return doOperation(request) { a, b -> a + b }
  }

  override suspend fun sub(request: MatrixRequest): MatrixResponse {
    return doOperation(request) { a, b -> a - b }
  }

  override suspend fun multiply(request: MatrixRequest): MatrixResponse {
    return doOperation(request) { a, b -> a * b }
  }

  override suspend fun multiplyByScalar(request: SingleMatrixWithScalarRequest): MatrixResponse {
    val matrix = request.matrixList
    val resultMatrixList = mutableListOf<DoubleArrayM>()

    for (i in matrix.indices) {
      val aRow = matrix[i].valuesList

      val scalar = request.scalar
      val resultRow = aRow.map { cell: Double -> cell * scalar }

      val resultDoubleArray = DoubleArrayM.newBuilder()
        .addAllValues(resultRow)
        .build()

      resultMatrixList.add(resultDoubleArray!!)
    }

    return MatrixResponse.newBuilder()
      .addAllResultMatrix(resultMatrixList)
      .build()
  }

  override suspend fun transpose(request: SingleMatrixRequest): MatrixResponse {
    val aMatrix = request.matrixList
    val resultMatrixList = mutableListOf<DoubleArrayM>()

    for (i in aMatrix.indices.reversed()) {
      val row = aMatrix[i].valuesList
      resultMatrixList.add(DoubleArrayM.newBuilder().addAllValues(row).build())
    }

    return MatrixResponse.newBuilder()
      .addAllResultMatrix(resultMatrixList)
      .build()
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