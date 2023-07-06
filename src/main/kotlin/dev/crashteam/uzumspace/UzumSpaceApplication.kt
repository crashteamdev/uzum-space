package dev.crashteam.uzumspace

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class UzumSpaceApplication

fun main(args: Array<String>) {
    runApplication<UzumSpaceApplication>(*args)
}
