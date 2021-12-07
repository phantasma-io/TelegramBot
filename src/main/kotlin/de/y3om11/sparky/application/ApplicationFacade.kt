package de.y3om11.sparky.application

import com.github.kotlintelegrambot.Bot
import de.y3om11.sparky.application.logic.BotModes
import de.y3om11.sparky.application.model.BotConfiguration

import org.springframework.stereotype.Component

@Component
class ApplicationFacade(private val mode: BotModes,
                        private val configuration: BotConfiguration) {

    fun executePollingBot() {
        getBotByMode().startPolling()
    }

    private fun getBotByMode(): Bot {
        when(configuration.mode){
            "admin" -> {
                return mode.administrationBot
            }
        }
        return mode.administrationBot
    }
}