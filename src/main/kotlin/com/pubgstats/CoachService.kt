package com.pubgstats

import org.springframework.stereotype.Service

/** One piece of coaching advice for a metric where the user trails the pro baseline. */
data class Suggestion(
    val metric: String,
    val userValue: Double,
    val baselineValue: Double,
    /** Fraction below baseline, 0.0..1.0 (0.4 = user is 40% below the pro median). */
    val gap: Double,
    /** Priority score; higher = further behind = fix first. Equals gap * 100 rounded. */
    val priority: Int,
    val tip: String,
)

@Service
class CoachService {

    /**
     * Compare the user's stats against the pro baseline and return suggestions ranked by how far
     * below baseline the user is (biggest gaps first). Metrics at or above baseline are skipped.
     */
    fun coach(user: PlayerStats, baseline: ProBaseline?): List<Suggestion> {
        if (baseline == null) return emptyList()
        return METRICS.mapNotNull { m ->
            val userValue = m.userValue(user)
            val baselineValue = m.baselineValue(baseline)
            if (baselineValue <= 0.0 || userValue >= baselineValue) return@mapNotNull null
            val gap = (baselineValue - userValue) / baselineValue
            Suggestion(m.label, userValue, baselineValue, gap, Math.round(gap * 100).toInt(), m.tip)
        }.sortedByDescending { it.gap }
    }

    private data class Metric(
        val label: String,
        val tip: String,
        val userValue: (PlayerStats) -> Double,
        val baselineValue: (ProBaseline) -> Double,
    )

    companion object {
        // ponytail: these metric labels + tips are hand-tuned heuristics, not learned;
        // upgrade path is a trained model producing per-player advice.
        private val METRICS = listOf(
            Metric("K/D", "Pick fights you can win and trade smarter — avoid overextending into third parties.",
                { it.kd }, { it.kd }),
            Metric("Avg Survival Time", "Play more cautiously: land safer, rotate early, and use cover on the move.",
                { it.avgSurvivalTimeSeconds.toDouble() }, { it.avgSurvivalTimeSeconds }),
            Metric("Headshot Ratio", "Spend time on aim training and pull for the head at close range.",
                { it.headshotRatio }, { it.headshotRatio }),
            Metric("Win Ratio", "Prioritise placement over kills — position for the final circles.",
                { it.winRatio }, { it.winRatio }),
            Metric("Avg Damage", "Be more aggressive in winnable fights and tag enemies through cover to build damage.",
                { it.avgDamage }, { it.avgDamage }),
        )
    }
}
