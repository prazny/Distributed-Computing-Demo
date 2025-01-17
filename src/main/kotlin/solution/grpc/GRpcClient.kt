//package pl.edu.pw.solution.grpc
//
//import io.grpc.ManagedChannelBuilder
//import pl.edu.pw.*
//
//class MatrixClient {
//  private var stub: MatrixServiceGrpc.MatrixServiceBlockingStub
//
//  companion object {
//    private fun Array<DoubleArray>.toDoubleArrayM(): DoubleArrayM {
//      val flatValues = this.flatMap { it.asList() }
//      return DoubleArrayM.newBuilder()
//        .addAllValues(flatValues)
//        .build()
//    }
//
//    private fun List<DoubleArrayM>.toArrayDoubleArray(): Array<DoubleArray> =
//      this.map { it.valuesList.toDoubleArray() }.toTypedArray()
//  }
//
//  init {
//    val channel = ManagedChannelBuilder.forAddress("localhost", 15001)
//      .usePlaintext()
//      .build()
//
//    stub = MatrixServiceGrpc.newBlockingStub(channel)
//  }
//
//  fun addMatrixes(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
//    return stub.add(
//      MatrixRequest
//        .newBuilder()
//        .addAMatrix(aMatrix.toDoubleArrayM())
//        .addBMatrix(bMatrix.toDoubleArrayM())
//        .build()
//    ).resultMatrixList
//      .toArrayDoubleArray()
//  }
//
//  fun subMatrixes(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
//    return stub.sub(
//      MatrixRequest
//        .newBuilder()
//        .addAMatrix(aMatrix.toDoubleArrayM())
//        .addBMatrix(bMatrix.toDoubleArrayM())
//        .build()
//    ).resultMatrixList
//      .toArrayDoubleArray()
//  }
//
//  fun multiplyMatrixes(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
//    return stub.multiply(
//      MatrixRequest
//        .newBuilder()
//        .addAMatrix(aMatrix.toDoubleArrayM())
//        .addBMatrix(bMatrix.toDoubleArrayM())
//        .build()
//    ).resultMatrixList
//      .toArrayDoubleArray()
//  }
//
//  fun multiplyMatrixByScalar(matrix: Array<DoubleArray>, scalar: Double): Array<DoubleArray> {
//    return stub.multiplyByScalar(
//      SingleMatrixWithScalarRequest
//        .newBuilder()
//        .addMatrix(matrix.toDoubleArrayM())
//        .setScalar(scalar)
//        .build()
//    ).resultMatrixList
//      .toArrayDoubleArray()
//  }
//
//
//  fun transposeMatrix(aMatrix: Array<DoubleArray>): Array<DoubleArray> {
//    return stub.transpose(
//      SingleMatrixRequest
//        .newBuilder()
//        .addMatrix(aMatrix.toDoubleArrayM())
//        .build()
//    ).resultMatrixList
//      .toArrayDoubleArray()
//  }
//}
