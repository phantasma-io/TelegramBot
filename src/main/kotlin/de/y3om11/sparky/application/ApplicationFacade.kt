package de.y3om11.sparky.application

import com.github.kotlintelegrambot.Bot
import de.y3om11.sparky.application.logic.BotModes

import org.springframework.stereotype.Component

@Component
class ApplicationFacade(private val mode: BotModes) {

    fun executePollingBot() {
        getBotByMode().startPolling()
    }

    private fun getBotByMode(): Bot {
        return mode.administrationBot
    }
}