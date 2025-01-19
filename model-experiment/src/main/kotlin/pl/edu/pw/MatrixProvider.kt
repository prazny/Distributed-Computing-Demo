package pl.edu.pw

import kotlinx.coroutines.*
import kotlin.random.Random

class MatrixProvider(
    private var n: Int,
) {
    private val threadCount = 5
    @OptIn(DelicateCoroutinesApi::class)
    private val dispatcher = newFixedThreadPoolContext(threadCount, "ParallelThreadPool")

    private val random = Random(8347569)

    val nValue get() = n

    var aMatrix: Array<DoubleArray>
        private set

    var bMatrix: Array<DoubleArray>
        private set

    init {
        aMatrix = generateSymmetricPositiveDefiniteMatrix(nValue)
        bMatrix = Array(nValue) { DoubleArray(1) { random.nextDouble() } }
    }

    private fun generateSymmetricPositiveDefiniteMatrix(size: Int): Array<DoubleArray> {
        val randomMatrix = Array(size) { DoubleArray(size) { random.nextDouble() } }
        val transpose = Array(size) { i -> DoubleArray(size) { j -> randomMatrix[j][i] } }

        return multiplyMatrices(transpose, randomMatrix)
    }

    private  fun multiplyMatrices(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
        val rowsA = aMatrix.size
        val colsA = aMatrix[0].size
        val colsB = bMatrix[0].size

        return runBlocking {
            val result = Array(rowsA) { DoubleArray(colsB) }
            applyCoroutineScopeWithChunks(rowsA) { i ->
                for (j in 0 until colsB) {
                    for (k in 0 until colsA) {
                        result[i][j] += aMatrix[i][k] * bMatrix[k][j]
                    }
                }
            }
            result
        }
    }

    private suspend fun applyCoroutineScopeWithChunks(count: Int, operation: suspend (Int) -> Unit) {
        // Divide (count / thread_count) and ceil
        val chunkSize = (count + threadCount - 1) / threadCount

        return coroutineScope {
            (0 until count step chunkSize).map { startRow ->
                launch(dispatcher) {
                    for (i in startRow until (startRow + chunkSize).coerceAtMost(count)) {
                        operation(i)
                    }
                }
            }.joinAll()
        }
    }

    override fun toString(): String {
        return "Configuration {n=$n}"
    }
}
