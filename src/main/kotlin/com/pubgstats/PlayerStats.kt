package com.pubgstats

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant

@Entity
class PlayerStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val playerName: String,
    val kd: Double,
    val avgSurvivalTimeSeconds: Int,
    val headshotRatio: Double,
    val winRatio: Double,
    val avgDamage: Double,
    val isPro: Boolean,
    val fetchedAt: Instant,
)
