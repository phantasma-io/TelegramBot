package de.y3om11.sparky.application.configuration

import com.google.gson.Gson
import de.y3om11.sparky.application.model.BotConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class AppConfig(val gson: Gson) {

    @Bean
    fun getAppConfiguration(): BotConfiguration {
        val configJson = File("./config.json").readText()
        return gson.fromJson(configJson, BotConfiguration::class.java)
    }
}