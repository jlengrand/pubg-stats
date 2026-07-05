package com.pubgstats

import org.springframework.data.jpa.repository.JpaRepository

interface PlayerStatsRepository : JpaRepository<PlayerStats, Long> {
    fun findByPlayerName(playerName: String): PlayerStats?
    fun findByIsProTrue(): List<PlayerStats>
}
