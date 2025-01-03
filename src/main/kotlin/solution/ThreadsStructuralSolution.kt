package pl.edu.pw.solution

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread
import kotlin.math.sqrt

class ThreadsStructuralSolution(override val tolerance: Double) : Solution(tolerance) {
    private val VERBOSE = true
    private val THREAD_COUNT = 2

    override suspend fun solve(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Companion.RoundResult {
        val startTime = System.nanoTime()

        var xMatrix = Array(bMatrix.size) { DoubleArray(1) }
        var r = subtractIND(bMatrix, multiplyIND(aMatrix, xMatrix))
        var rNormSquared = r.normSquared()
        var rNorm: Double

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

            beta = rNormSquared / rPrevNormSquared
            val threadAddINDX = thread { xMatrix = addIND(xMatrix, multiplyINDByScalar(p, alfa)) }
            val threadAddINDP = thread { p = addIND(r, multiplyINDByScalar(p, beta)) }

          threadAddINDX.join()
          threadAddINDP.join()

            if (i % 100 == 0 && VERBOSE) {
                println(
                    "Iteration $i: Norm = ${rNorm}, Time elapsed = ${
                        "%.2f".format(getElapsedTime(startTime))
                    } seconds"
                )
            }
        } while (rNorm > tolerance)
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
        val threads = createThreadsWithChunks(rowsA) { i ->
            for (j in 0 until colsB) {
                for (k in 0 until colsA) {
                    result[i][j] += aMatrix[i][k] * bMatrix[k][j]
                }
            }
        }

        threads.forEach { it.join() }
        return result
    }

    private fun multiplyINDByScalar(matrix: Array<DoubleArray>, scalar: Double): Array<DoubleArray> {
        val rows = matrix.size
        val result = Array(rows) { DoubleArray(matrix[0].size) }

        val threads = createThreadsWithChunks(rows) { i ->
            for (j in matrix[i].indices) {
                result[i][j] = matrix[i][j] * scalar
            }
        }

        threads.forEach { it.join() }
        return result
    }

    private fun addIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
        if (aMatrix.size != bMatrix.size || aMatrix[0].size != bMatrix[0].size)
            throw IllegalArgumentException("Shapes are not equal.")

        val result = Array(aMatrix.size) { DoubleArray(bMatrix[0].size) }

        val threads = createThreadsWithChunks(aMatrix.size) { row ->
            for (col in 0 until aMatrix[row].size) {
                result[row][col] = aMatrix[row][col] + bMatrix[row][col]
            }
        }

        threads.forEach { it.join() }
        return result
    }

    private fun subtractIND(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
        if (aMatrix.size != bMatrix.size || aMatrix[0].size != bMatrix[0].size)
            throw IllegalArgumentException("Shapes are not equal.")

        val result = Array(aMatrix.size) { DoubleArray(bMatrix[0].size) }

        val threads = createThreadsWithChunks(aMatrix.size) { row ->
            for (col in 0 until aMatrix[row].size) {
                result[row][col] = aMatrix[row][col] - bMatrix[row][col]
            }
        }

        threads.forEach { it.join() }
        return result
    }

    private fun transposeIND(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val rows = matrix.size
        val cols = matrix[0].size

        return Array(cols) { i -> DoubleArray(rows) { j -> matrix[j][i] } }
    }

    private fun createThreadsWithChunks(count: Int, operation: (Int) -> Unit): List<Thread> {
        // Divide (count / thread_count) and ceil
        val chunkSize = (count + THREAD_COUNT - 1) / THREAD_COUNT

        return (0 until THREAD_COUNT ).map { threadIndex ->
          thread {
              val currentChunk = threadIndex * chunkSize
              for (i in currentChunk until (currentChunk + chunkSize).coerceAtMost(count)) {
                operation(i)
              }
          }

        }
    }
}
