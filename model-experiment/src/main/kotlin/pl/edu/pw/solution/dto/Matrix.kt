import pl.edu.pw.DoubleArrayM

class Matrix(private val rows: List<DoubleArrayM>) {

  companion object {
    // Factory methods to create Matrix from DoubleArray or List<DoubleArray>
    fun fromDoubleArrayList(data: List<DoubleArray>): Matrix {
      val doubleArrayMList = data.map { row ->
        DoubleArrayM.newBuilder().addAllValues(row.asList()).build()
      }
      return Matrix(doubleArrayMList)
    }

    fun fromDoubleArrayArray(data: Array<DoubleArray>): Matrix {
      val doubleArrayMList = data.map { row ->
        DoubleArrayM.newBuilder().addAllValues(row.asList()).build()
      }
      return Matrix(doubleArrayMList)
    }

    fun fromDoubleArrayMList(data: List<DoubleArrayM>): Matrix {
      return Matrix(data)
    }
  }

  // Convert back to List<DoubleArray>
  fun toDoubleArrayList(): List<DoubleArray> {
    return rows.map { it.valuesList.toDoubleArray() }
  }

  // Access the gRPC-compatible internal representation
  fun toDoubleArrayMList(): List<DoubleArrayM> {
    return rows
  }

  // Get number of rows and columns
  fun rowCount(): Int = rows.size
  fun columnCount(): Int = if (rows.isNotEmpty()) rows[0].valuesCount else 0

  // normSquared method
  fun normSquared(): Double {
    return rows.sumOf { row ->
      row.valuesList.sumOf { value -> value * value }
    }
  }

  // Element-wise addition with another Matrix
  fun add(other: Matrix): Matrix {
    require(this.rowCount() == other.rowCount() && this.columnCount() == other.columnCount()) {
      "Matrix dimensions do not match for addition."
    }

    val resultRows = mutableListOf<DoubleArrayM>()
    for (i in 0 until this.rowCount()) {
      val resultRow = this.row(i).zip(other.row(i)) { a, b -> a + b }
      resultRows.add(DoubleArrayM.newBuilder().addAllValues(resultRow).build())
    }
    return Matrix(resultRows)
  }

  // Helper function to get a row from the matrix
  private fun row(i: Int): List<Double> = rows[i].valuesList

  // Helper function to get a column from the matrix
  private fun column(j: Int): List<Double> = rows.map { it.valuesList[j] }


  fun copyOfRange(fromRow: Int, toRow: Int): Matrix {
    if (fromRow < 0 || toRow > rowCount() || fromRow >= toRow) {
      throw IllegalArgumentException("Invalid row range.")
    }

    // Copy rows within the specified range
    val subMatrix = rows.subList(fromRow, toRow)

    return Matrix(subMatrix)
  }
}
