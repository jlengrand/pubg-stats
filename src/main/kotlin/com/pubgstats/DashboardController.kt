package com.pubgstats

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Controller
class DashboardController(
    private val repository: PlayerStatsRepository,
    private val ingestion: StatsIngestionService,
    private val baselineService: ProBaselineService,
    private val coach: CoachService,
    private val props: PubgApiProperties,
) {
    private val tsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z").withZone(ZoneId.systemDefault())

    @GetMapping("/")
    fun dashboard(model: Model): String {
        val you = repository.findByPlayerName("you")
        val baseline = baselineService.compute()
        model.addAttribute("you", you)
        model.addAttribute("pros", repository.findByIsProTrue())
        model.addAttribute("suggestions", if (you != null) coach.coach(you, baseline) else emptyList<Suggestion>())
        model.addAttribute("youFetchedAt", you?.let { tsFormat.format(it.fetchedAt) })
        // "fresh" = still within the cache TTL, so a refresh would reuse it; otherwise it is stale/cached.
        model.addAttribute(
            "fresh",
            you != null && Duration.between(you.fetchedAt, Instant.now()) < Duration.ofHours(props.cacheTtlHours),
        )
        return "dashboard"
    }

    /** Force re-ingestion (still respects the cache TTL), then back to the dashboard. */
    @PostMapping("/refresh")
    fun refresh(redirect: RedirectAttributes): String {
        ingestion.refresh()?.let { redirect.addFlashAttribute("banner", it) }
        return "redirect:/"
    }
}
