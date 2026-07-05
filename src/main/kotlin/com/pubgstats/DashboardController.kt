package com.pubgstats

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping

@Controller
class DashboardController(
    private val repository: PlayerStatsRepository,
    private val ingestion: StatsIngestionService,
) {
    @GetMapping("/")
    fun dashboard(model: Model): String {
        model.addAttribute("you", repository.findByPlayerName("you"))
        model.addAttribute("pros", repository.findByIsProTrue())
        return "dashboard"
    }

    /** Force re-ingestion (still respects the cache TTL), then back to the dashboard. */
    @PostMapping("/refresh")
    fun refresh(): String {
        ingestion.ingestAll()
        return "redirect:/"
    }
}
