package pl.edu.pw.solution

abstract class Solution(
    open val tolerance: Double
) {
    companion object {
        data class RoundResult(
            val iterations: Int,
            val elapsedTime: Double,
            val norm: Double
        )
    }

    abstract suspend fun solve(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): RoundResult

    protected fun Array<DoubleArray>.normSquared(): Double {
        return this.sumOf { row -> row.sumOf { it * it } }
    }

    protected fun getElapsedTime(startTime: Long): Double =
        (System.nanoTime() - startTime) / 1_000_000_000.0
}
