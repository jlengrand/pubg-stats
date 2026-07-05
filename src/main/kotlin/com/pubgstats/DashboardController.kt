package com.pubgstats

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping

@Controller
class DashboardController(
    private val repository: PlayerStatsRepository,
    private val ingestion: StatsIngestionService,
    private val baselineService: ProBaselineService,
    private val coach: CoachService,
) {
    @GetMapping("/")
    fun dashboard(model: Model): String {
        val you = repository.findByPlayerName("you")
        val baseline = baselineService.compute()
        model.addAttribute("you", you)
        model.addAttribute("pros", repository.findByIsProTrue())
        model.addAttribute("suggestions", if (you != null) coach.coach(you, baseline) else emptyList<Suggestion>())
        return "dashboard"
    }

    /** Force re-ingestion (still respects the cache TTL), then back to the dashboard. */
    @PostMapping("/refresh")
    fun refresh(): String {
        ingestion.ingestAll()
        return "redirect:/"
    }
}
