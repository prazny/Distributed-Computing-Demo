fun Array<DoubleArray>.normSquared(): Double {
  return this.sumOf { row -> row.sumOf { it * it } }
}

fun getElapsedTime(startTime: Long): Double = (System.nanoTime() - startTime) / 1_000_000_000.0
