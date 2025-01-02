package pl.edu.pw.solution

import kotlin.math.sqrt

class SyncStructuralSolution(override val tolerance: Double) : Solution(tolerance) {
    private val VERBOSE = true

    override suspend fun solve(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Companion.RoundResult {
        val startTime = System.nanoTime()

        var xMatrix = Array(bMatrix.size) { DoubleArray(1) }
        var r = subtractIND(bMatrix, multiplyIND(aMatrix, xMatrix))
        var rNormSquared = r.normSquared()
        var rNorm = sqrt(rNormSquared)
        var p = r
        var beta: Double

        var i = 0
        do {
            i++
            val q = multiplyIND(aMatrix, p)

            val alfa = rNormSquared / dotProduct(transposeIND(p), q)

            val rPrevNormSquared = rNormSquared
            r = subtractIND(r, multiplyINDByScalar(q, alfa))
            rNormSquared = r.normSquared()
            rNorm = sqrt(rNormSquared)

            xMatrix = addIND(xMatrix, multiplyINDByScalar(p, alfa))
            beta = rNormSquared / rPrevNormSquared

            p = addIND(r, multiplyINDByScalar(p, beta))

            if (i % 100 == 0 && VERBOSE) {
                println(
                    "Iteration $i: Norm = ${rNorm}, Time elapsed = ${
                        "%.2f".format(getElapsedTime(startTime))
                    } seconds"
                )
            }
        } while (i < 100000 && rNorm > tolerance)
        return Companion.RoundResult(i, getElapsedTime(startTime), rNorm)
    }

    /**
     * This should be in separate class but, the exercise conditions require avoiding it.
     */

    private fun dotProduct(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Double {
        val result = multiplyIND(aMatrix, bMatrix)
        return result.sumOf { it.sum() }
    }

    private fun multiplyIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
        val rowsA = aMatrix.size
        val colsA = aMatrix[0].size
        val rowsB = bMatrix.size
        val colsB = bMatrix[0].size

        if (colsA != rowsB) throw IllegalArgumentException("Columns are not equal.")

        val result = Array(rowsA) { DoubleArray(colsB) }
        for (i in 0 until rowsA) {
            for (j in 0 until colsB) {
                for (k in 0 until colsA) {
                    result[i][j] += aMatrix[i][k] * bMatrix[k][j]
                }
            }
        }
        return result
    }

    private fun multiplyINDByScalar(matrix: Array<DoubleArray>, scalar: Double): Array<DoubleArray> {
        return matrix.map { row -> row.map { it * scalar }.toDoubleArray() }.toTypedArray()
    }

    private fun addIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
        if (aMatrix.size != bMatrix.size || aMatrix[0].size != bMatrix[0].size)
            throw IllegalArgumentException("Shapes are not equal.")

        return Array(aMatrix.size) { i ->
            DoubleArray(aMatrix[0].size) { j -> aMatrix[i][j] + bMatrix[i][j] }
        }
    }

    private fun subtractIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
        if (aMatrix.size != bMatrix.size || aMatrix[0].size != bMatrix[0].size)
            throw IllegalArgumentException("Shapes are not equal.")

        return Array(aMatrix.size) { i ->
            DoubleArray(aMatrix[0].size) { j -> aMatrix[i][j] - bMatrix[i][j] }
        }
    }

    private fun transposeIND(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val rows = matrix.size
        val cols = matrix[0].size

        return Array(cols) { i -> DoubleArray(rows) { j -> matrix[j][i] } }
    }
}
