package pl.edu.pw

import pl.edu.pw.experiment.ExperimentWrapper
import pl.edu.pw.solution.GRpcSolution
import pl.edu.pw.solution.ParallelSolution
import pl.edu.pw.solution.SyncSolution
import pl.edu.pw.solution.grpc.LinearEquationClient
import pl.edu.pw.solutionWrappers.ParallelWrapper
import pl.edu.pw.solutionWrappers.SyncWrapper

fun startExperiment(toleranceValue: Double, threadCount: Int, grpcClient: LinearEquationClient) {

  val matrixProvider = MatrixProvider(1700)
  val matrices = List(1) { matrixProvider }
  val solutions = listOf(
    SyncSolution(toleranceValue),
    ParallelSolution(toleranceValue, threadCount),
    GRpcSolution(toleranceValue, threadCount, grpcClient),
  )

  val wrappers = listOf(
    SyncWrapper(),
    ParallelWrapper(),
  )

  val experiment = ExperimentWrapper(matrices, solutions, wrappers)
  experiment.proceed()
}


const val TOLERANCE = 1e-6
const val THREAD_COUNT = 3

fun main() {
  startExperiment(TOLERANCE, THREAD_COUNT, LinearEquationClient(listOf(5000,)))
}
