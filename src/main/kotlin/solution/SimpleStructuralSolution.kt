package pl.edu.pw.solution

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

class SimpleStructuralSolution(override val tolerance: Double) : Solution(tolerance) {
    private val VERBOSE = true

    override fun solve(aMatrix: INDArray, bMatrix: INDArray): Companion.RoundResult {
        val startTime = System.nanoTime()

        var xMatrix = Nd4j.zeros(bMatrix.rows(), 1)
        var r = bMatrix.sub(aMatrix.mmul(xMatrix))
        var p = r
        var beta: Double;

        var i = 0;
        do {
            i++;
            val q = aMatrix.mmul(p)

            val alfa = r.norm() / (p.transpose().mmul(q)).getDouble(0)

            val rPrev = r
            r = r.sub(q.mul(alfa))

            xMatrix = xMatrix.add(p.mul(alfa))
            beta = r.norm() / rPrev.norm()

            p = r.add(p.mul(beta))

            if (i % 1000 == 0 && VERBOSE) {
                println(
                    "Iteration $i: Residual norm = ${Math.sqrt(r.norm())}, Time elapsed = ${
                        "%.2f".format(
                            getElapsedTime(startTime)
                        )
                    } seconds"
                )
            }
        } while (i < 1000000 && r.norm() > tolerance);

        return Companion.RoundResult(i, getElapsedTime(startTime), r.norm())
    }
}