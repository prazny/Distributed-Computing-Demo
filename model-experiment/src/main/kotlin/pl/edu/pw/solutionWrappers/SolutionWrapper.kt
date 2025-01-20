package pl.edu.pw.solutionWrappers

import pl.edu.pw.solution.Solution
import pl.edu.pw.solution.dto.ExperimentResult

abstract class SolutionWrapper {
  abstract fun solve(
    solution: Solution, matrices: List<Pair<Array<DoubleArray>, Array<DoubleArray>>>
  ): ExperimentResult
}