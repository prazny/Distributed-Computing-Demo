package pl.edu.pw.solution

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

class SyncStructuralSolution(override val tolerance: Double) : Solution(tolerance) {
    private val VERBOSE = true

    override suspend fun solve(aMatrix: INDArray, bMatrix: INDArray): Companion.RoundResult {
        val startTime = System.nanoTime()

        var xMatrix = Nd4j.zeros(bMatrix.rows(), 1)
        var r = subtractIND(bMatrix, multiplyIND(aMatrix, xMatrix))
        var rNorm = r.norm()
        var p = r
        var beta: Double

        var i = 0
        do {
            i++
            val q = multiplyIND(aMatrix, p)

            val alfa = r.norm() / (multiplyIND(transposeIND(p), q)).getDouble(0)

            val rPrevNorm = rNorm
            r = subtractIND(r, multiplyINDByScalar(q, alfa))
            rNorm = r.norm()

            xMatrix = addIND(xMatrix, multiplyINDByScalar(p, alfa))
            beta = rNorm / rPrevNorm

            p = addIND(r, multiplyINDByScalar(p, beta))

            if (i % 1000 == 0 && VERBOSE) {
                println(
                    "Iteration $i: Norm = ${rNorm}, Time elapsed = ${
                        "%.2f".format(getElapsedTime(startTime))
                    } seconds"
                )
            }
        } while (i < 10000 && rNorm > tolerance)

        return Companion.RoundResult(i, getElapsedTime(startTime), rNorm)
    }

    /**
     * This should be in separate class but, the exercise conditions require avoiding it.
     */

    private fun multiplyIND(aMatrix: INDArray, bMatrix: INDArray): INDArray {
        val rowsA = aMatrix.rows()
        val colsA = aMatrix.columns()
        val rowsB = bMatrix.rows()
        val colsB = bMatrix.columns()

        if (colsA != rowsB)
            throw IllegalArgumentException("Columns are not equal.")

        val result = Nd4j.zeros(rowsA, colsB)

        for (i in 0L until rowsA) {
            for (j in 0L until colsB) {
                var sum = 0.0
                for (k in 0L until colsA) {
                    sum += aMatrix.getDouble(i, k) * bMatrix.getDouble(k, j)
                }
                result.putScalar(i, j, sum)
            }
        }

        return result
    }

    private fun multiplyINDByScalar(matrix: INDArray, scalar: Double): INDArray {
        val (rows, cols) = arrayOf(matrix.rows(), matrix.columns())

        val result = Nd4j.zeros(rows, cols)

        for (i in 0L until rows) {
            for (j in 0L until cols) {
                val value = matrix.getDouble(i, j)
                result.putScalar(i, j, value * scalar)
            }
        }
        return result
    }

    private fun addIND(aMatrix: INDArray, bMatrix: INDArray): INDArray {
        if (!aMatrix.shape().contentEquals(bMatrix.shape()))
            throw IllegalArgumentException("Shapes are not equal.")

        val (rows, cols) = arrayOf(aMatrix.rows(), aMatrix.columns())

        val result = Nd4j.zeros(rows, cols)
        for (i in 0L until rows) {
            for (j in 0L until cols) {
                val sum = aMatrix.getDouble(i, j) + bMatrix.getDouble(i, j)
                result.putScalar(i, j, sum)
            }
        }

        return result
    }

    private fun subtractIND(aMatrix: INDArray, bMatrix: INDArray): INDArray {
        if (!aMatrix.shape().contentEquals(bMatrix.shape()))
            throw IllegalArgumentException("Shapes are not equal.")

        val (rows, cols) = arrayOf(aMatrix.rows(), aMatrix.columns())

        val result = Nd4j.zeros(rows, cols)
        for (i in 0L until rows) {
            for (j in 0L until cols) {
                val difference = aMatrix.getDouble(i, j) - bMatrix.getDouble(i, j)
                result.putScalar(i, j, difference)
            }
        }

        return result
    }

    private fun transposeIND(matrix: INDArray): INDArray {
        val (rows, cols) = arrayOf(matrix.rows(), matrix.columns())
        val transposed = Nd4j.zeros(cols, rows)

        for (i in 0L until rows) {
            for (j in 0L until cols) {
                val value = matrix.getDouble(i, j)
                transposed.putScalar(j, i, value)
            }
        }

        return transposed
    }
}