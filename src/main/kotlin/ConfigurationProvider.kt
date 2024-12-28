package pl.edu.pw

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

class ConfigurationProvider(
    private var n: Int,
    private var tolerance: Double,
    private var rounds: Int,
) {
    private val nValue get() = n
    val toleranceValue get() = tolerance

    var aMatrix: INDArray
        private set

    var bMatrix: INDArray
        private set

    init {
        aMatrix = Nd4j.rand(nValue, nValue).let { it.transpose().mul(it) }
        bMatrix = Nd4j.rand(nValue, 1)
    }
}