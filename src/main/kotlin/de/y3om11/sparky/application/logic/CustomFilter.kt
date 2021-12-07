package de.y3om11.sparky.application.logic

import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.extensions.filters.Filter
import de.y3om11.sparky.application.model.BotConfiguration
import de.y3om11.sparky.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class CustomFilter(
    val configuration: BotConfiguration,
    val userRepository: UserRepository
) {

    val isTrustCommand = object : Filter {
        override fun Message.predicate(): Boolean {
            return text?.contains("!trust") ?: false
        }
    }

    val isForgiveCommand = object : Filter {
        override fun Message.predicate(): Boolean {
            return text?.contains("!forgive") ?: false
        }
    }

    val isMuteOneMinuteCommand = object : Filter {
        override fun Message.predicate(): Boolean {
            return text?.contains("!mute1") ?: false
        }
    }

    val isMuteOneDayCommand = object : Filter {
        override fun Message.predicate(): Boolean {
            return text?.contains("!mute24") ?: false
        }
    }

    val isMuteTwoDaysCommand = object : Filter {
        override fun Message.predicate(): Boolean {
            return text?.contains("!mute48") ?: false
        }
    }

    val isWarnCommand = object : Filter {
        override fun Message.predicate(): Boolean {
            return text?.contains("!warn") ?: false
        }
    }

    val isBanCommand = object : Filter {
        override fun Message.predicate(): Boolean {
            return text?.contains("!ban") ?: false
        }
    }

    val isUnbanCommand = object : Filter {
        override fun Message.predicate(): Boolean {
            return text?.contains("!unban") ?: false
        }
    }

    val isUnmuteCommand = object : Filter {
        override fun Message.predicate(): Boolean {
            return text?.contains("!unmute") ?: false
        }
    }

    val isNextCommand = object : Filter {
        override fun Message.predicate(): Boolean {
            return text?.contains("#nextcommand") ?: false
        }
    }

    val isPromoCommand = object : Filter {
        override fun Message.predicate(): Boolean {
            return text?.contains("#promo") ?: false
        }
    }

    val isTargetChat = object : Filter {
        override fun Message.predicate(): Boolean {
            return configuration.chatsToWatch.contains(chat.id)
        }
    }

    val isVerificationChat = object : Filter {
        override fun Message.predicate(): Boolean {
            return configuration.verificationChat == chat.id
        }
    }

    val isAdmin = object : Filter {
        override fun Message.predicate(): Boolean {
            return configuration.admins.contains(from!!.id)
        }
    }

    val isNotWhitelisted = object : Filter {
        override fun Message.predicate(): Boolean {
            return if (configuration.admins.contains(from!!.id)) {
                false
            } else {
                !userRepository.findById(from!!.id)
                    .map { user -> user.trusted }
                    .orElse(false)
            }
        }
    }

    val hasEntities = object : Filter {
        override fun Message.predicate(): Boolean {
            return entities != null && entities!!.isNotEmpty()
        }
    }

    val hasDocument = object : Filter {
        override fun Message.predicate(): Boolean {
            if (document == null) return false
            val isMimeType = configuration.botMimetypes.contains(document?.mimeType.toString())
            val isFileName = configuration.botFilenames.contains(document?.fileName.toString())
            return !(isMimeType || isFileName)
        }
    }

    val hasForwardedMessage = object : Filter {
        override fun Message.predicate(): Boolean {
            return forwardFrom != null
        }
    }
}
