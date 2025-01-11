package pl.edu.pw

internal class MatrixService : MatrixServiceGrpcKt.MatrixServiceCoroutineImplBase() {
  override suspend fun add(request: MatrixRequest): MatrixResponse {
    return doOperation(request) { a, b -> a + b }
  }

  override suspend fun sub(request: MatrixRequest): MatrixResponse {
    return doOperation(request) { a, b -> a - b }
  }

  override suspend fun multiply(request: MatrixRequest): MatrixResponse {
    try {
      val aMatrix = request.aMatrixList
      val bMatrix = request.bMatrixList

      if(aMatrix.isEmpty() && bMatrix.isEmpty()) {
        throw IllegalArgumentException("Macierze wejściowe nie mogą być puste.") }
      if(aMatrix[0].valuesList.size != bMatrix.size) {
        throw IllegalArgumentException(
          "Liczba kolumn macierzy A (${aMatrix[0].valuesList.size}) musi być równa liczbie wierszy macierzy B (${bMatrix.size})."

        )
      }
      val bMatrixCleaned = bMatrix.filter { it.valuesList.isNotEmpty() }
      require(bMatrixCleaned.size == bMatrix.size) { "Nie wszystkie wiersze macierzy B są poprawnie zainicjalizowane." }

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

      return MatrixResponse.newBuilder()
        .addAllResultMatrix(resultMatrixList)
        .build()
    } catch (e: Exception) {
      throw e
    }

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
    for (colIndex in 0 until aMatrix[0].valuesList.size) {
      val transposedRow = aMatrix.map { it.valuesList[colIndex] }
      resultMatrixList.add(DoubleArrayM.newBuilder().addAllValues(transposedRow).build())
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
