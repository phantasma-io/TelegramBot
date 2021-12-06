package de.y3om11.sparky

import de.y3om11.sparky.application.ApplicationFacade
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationEvent
import org.springframework.context.event.EventListener

@SpringBootApplication
class SparkyApplication(private val applicationFacade: ApplicationFacade) {

    @EventListener(ApplicationReadyEvent::class)
    fun startup(event: ApplicationEvent) {
        applicationFacade.executePollingBot()
    }
}

fun main(args: Array<String>) {
    runApplication<SparkyApplication>(*args)
}
