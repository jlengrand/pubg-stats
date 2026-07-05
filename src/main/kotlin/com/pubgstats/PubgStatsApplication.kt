package com.pubgstats

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class PubgStatsApplication

fun main(args: Array<String>) {
    runApplication<PubgStatsApplication>(*args)
}
