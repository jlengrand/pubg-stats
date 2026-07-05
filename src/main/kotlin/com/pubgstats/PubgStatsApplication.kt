package com.pubgstats

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PubgStatsApplication

fun main(args: Array<String>) {
    runApplication<PubgStatsApplication>(*args)
}
