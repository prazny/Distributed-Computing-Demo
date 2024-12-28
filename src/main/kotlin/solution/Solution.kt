package pl.edu.pw.solution

import org.nd4j.linalg.api.ndarray.INDArray

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

    abstract suspend fun solve(aMatrix: INDArray, bMatrix: INDArray): RoundResult
    protected fun INDArray.norm(): Double {
        val norm = this.norm2Number().toDouble()
        return norm
    }

    protected fun getElapsedTime(startTime: Long): Double =
        (System.nanoTime() - startTime) / 1_000_000_000.0

}