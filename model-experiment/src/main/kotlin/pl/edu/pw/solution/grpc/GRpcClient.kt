package pl.edu.pw.solution.grpc

import io.grpc.ManagedChannelBuilder
import pl.edu.pw.*

class MatrixClient(port: Int) {
  private var stub: MatrixServiceGrpc.MatrixServiceBlockingStub

  companion object {
    private fun Array<DoubleArray>.toDoubleArrayM(): List<DoubleArrayM> {
      return this.map { row ->
        DoubleArrayM.newBuilder()
          .addAllValues(row.toList())
          .build()
      }
    }

    private fun List<DoubleArrayM>.toArrayDoubleArray(): Array<DoubleArray> =
      this.map { it.valuesList.toDoubleArray() }.toTypedArray()
  }

  init {
    val channel = ManagedChannelBuilder.forAddress("localhost", port)
      .usePlaintext()
      .build()

    stub = MatrixServiceGrpc.newBlockingStub(channel)
  }

  fun addMatrixes(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    return stub.add(
      MatrixRequest
        .newBuilder()
        .addAllAMatrix(aMatrix.toDoubleArrayM())
        .addAllBMatrix(bMatrix.toDoubleArrayM())
        .build()
    ).resultMatrixList
      .toArrayDoubleArray()
  }

  fun subMatrixes(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
    return stub.sub(
      MatrixRequest
        .newBuilder()
        .addAllAMatrix(aMatrix.toDoubleArrayM())
        .addAllBMatrix(bMatrix.toDoubleArrayM())
        .build()
    ).resultMatrixList
      .toArrayDoubleArray()
  }

  fun multiplyMatrixes(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {

    return stub.multiply(
      MatrixRequest
        .newBuilder()
        .addAllAMatrix(aMatrix.toDoubleArrayM())
        .addAllBMatrix(bMatrix.toDoubleArrayM())
        .build()
    ).resultMatrixList
      .toArrayDoubleArray()
  }

  fun multiplyMatrixByScalar(matrix: Array<DoubleArray>, scalar: Double): Array<DoubleArray> {
    return stub.multiplyByScalar(
      SingleMatrixWithScalarRequest
        .newBuilder()
        .addAllMatrix(matrix.toDoubleArrayM())
        .setScalar(scalar)
        .build()
    ).resultMatrixList
      .toArrayDoubleArray()
  }


  fun transposeMatrix(aMatrix: Array<DoubleArray>): Array<DoubleArray> {
    return stub.transpose(
      SingleMatrixRequest
        .newBuilder()
        .addAllMatrix(aMatrix.toDoubleArrayM())
        .build()
    ).resultMatrixList
      .toArrayDoubleArray()
  }
}
