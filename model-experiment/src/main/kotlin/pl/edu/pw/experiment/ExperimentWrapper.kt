package pl.edu.pw.experiment

import pl.edu.pw.MatrixProvider
import pl.edu.pw.solution.Solution
import pl.edu.pw.solution.dto.ExperimentResult
import pl.edu.pw.solutionWrappers.SolutionWrapper

class ExperimentWrapper(
  private val matrices: List<MatrixProvider>,
  private val solutions: List<Solution>,
  private val wrappers: List<SolutionWrapper>
) {
  fun proceed() {


    val experimentResults: MutableList<Triple<SolutionWrapper, Solution, ExperimentResult>> = mutableListOf()
    wrappers.forEach { wrapper ->
      solutions.forEach { solution ->
        val result = wrapper.solve(solution, matrices.map { Pair(it.aMatrix, it.bMatrix) })
        experimentResults.add(Triple(wrapper, solution, result))
      }
    }

    printResults(experimentResults)
  }

  private fun printResults(experimentResults: List<Triple<SolutionWrapper, Solution, ExperimentResult>>) {
    println("Configuration: ${matrices[0].nValue} × ${matrices[0].nValue} matrices run ${matrices.size} times.")

    val sourceTime = experimentResults.find { it.third.isSync }!!.third.elapsedTime
    experimentResults.forEach { result ->
      println(
        String.format(
          "Wrapper: %-10s Solution: %-10s Time: %5.2f P: %5.2f",
          result.first,
          result.second,
          result.third.elapsedTime,
          sourceTime / result.third.elapsedTime
        )
      )
    }
  }
}
