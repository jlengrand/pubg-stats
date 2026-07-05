package com.pubgstats

import org.springframework.stereotype.Service

/** One median (baseline) value per pro metric. */
data class ProBaseline(
    val kd: Double,
    val avgSurvivalTimeSeconds: Double,
    val headshotRatio: Double,
    val winRatio: Double,
    val avgDamage: Double,
)

@Service
class ProBaselineService(private val repository: PlayerStatsRepository) {

    /** Load all pro stats and compute the median of each metric. */
    fun compute(): ProBaseline? {
        val pros = repository.findByIsProTrue()
        if (pros.isEmpty()) return null
        return ProBaseline(
            kd = median(pros.map { it.kd }),
            avgSurvivalTimeSeconds = median(pros.map { it.avgSurvivalTimeSeconds.toDouble() }),
            headshotRatio = median(pros.map { it.headshotRatio }),
            winRatio = median(pros.map { it.winRatio }),
            avgDamage = median(pros.map { it.avgDamage }),
        )
    }

    companion object {
        /** Median of a non-empty list; averages the two middle values for even counts. */
        fun median(values: List<Double>): Double {
            require(values.isNotEmpty()) { "median of empty list" }
            val sorted = values.sorted()
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
        }
    }
}
