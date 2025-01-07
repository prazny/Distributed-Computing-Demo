package pl.edu.pw

class ConfigurationProvider(
    private var n: Int,
    private var tolerance: Double,
    private var rounds: Int,
    private var threads: Int,
    private var instances: Int,
) {
    private val nValue get() = n
    val toleranceValue get() = tolerance
    val roundsValue get() = rounds
    val threadCount get() = threads
    val instanceCount get() = instances

    var aMatrix: Array<DoubleArray>
        private set

    var bMatrix: Array<DoubleArray>
        private set

    init {
        aMatrix = generateSymmetricPositiveDefiniteMatrix(nValue)
        bMatrix = Array(nValue) { DoubleArray(1) { Math.random() } }
    }

    private fun generateSymmetricPositiveDefiniteMatrix(size: Int): Array<DoubleArray> {
        val randomMatrix = Array(size) { DoubleArray(size) { Math.random() } }
        val transpose = Array(size) { i -> DoubleArray(size) { j -> randomMatrix[j][i] } }

        return multiplyMatrixes(transpose, randomMatrix)
    }

    private fun multiplyMatrixes(aMatrix: Array<DoubleArray>, bMatrix: Array<DoubleArray>): Array<DoubleArray> {
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

    override fun toString(): String {
        return "Configuration {n=$n, tolerance=$tolerance, threads=$threadCount, rounds=$rounds}"
    }
}
