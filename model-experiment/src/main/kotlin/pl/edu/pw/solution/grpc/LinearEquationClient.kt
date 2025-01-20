package pl.edu.pw.solution.grpc

import io.grpc.ManagedChannelBuilder
import pl.edu.pw.GMatrix
import pl.edu.pw.GMatrixRow
import pl.edu.pw.LinearEquationGrpcKt
import pl.edu.pw.SolveRequest

class LinearEquationClient(ports: List<Int>) {
  private val stubs: List<LinearEquationGrpcKt.LinearEquationCoroutineStub> = ports.map { port ->
    val channel = ManagedChannelBuilder.forAddress("localhost", port)
      .usePlaintext()
      .build()

    LinearEquationGrpcKt.LinearEquationCoroutineStub(channel)
  }


  suspend fun solveLinearEquation(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>, threads: Int, tolerance: Double): Array<DoubleArray> {
    return getStub().solve(
      SolveRequest.newBuilder()
        .setAMatrix(aMatrix.toGMatrix())
        .setBMatrix(bMatrix.toGMatrix())
        .setThreadCount(threads)
        .setTolerance(tolerance)
        .build()
    ).toArray()
  }

  private fun Array<DoubleArray>.toGMatrix(): GMatrix {
    val rows = this.map { row ->
      GMatrixRow.newBuilder().addAllValues(row.asList()).build()
    }
    return GMatrix.newBuilder()
      .addAllRow(rows)
      .build()
  }

  private fun GMatrix.toArray(): Array<DoubleArray> {
    return this.rowList.map { row ->
      row.valuesList.toDoubleArray()
    }.toTypedArray()
  }

  var currentIndex = 0;
  private fun getStub(): LinearEquationGrpcKt.LinearEquationCoroutineStub {
    currentIndex = (currentIndex + 1) % stubs.size
    return stubs[currentIndex]
  }
}
