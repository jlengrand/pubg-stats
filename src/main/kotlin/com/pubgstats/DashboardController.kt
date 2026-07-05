package com.pubgstats

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class DashboardController(private val repository: PlayerStatsRepository) {
    @GetMapping("/")
    fun dashboard(model: Model): String {
        model.addAttribute("you", repository.findByPlayerName("you"))
        model.addAttribute("pros", repository.findByIsProTrue())
        return "dashboard"
    }
}
