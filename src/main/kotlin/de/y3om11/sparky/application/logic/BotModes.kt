package de.y3om11.sparky.application.logic

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import de.y3om11.sparky.application.model.BotConfiguration
import de.y3om11.sparky.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Instant
import java.util.stream.Collectors

@Component
class BotModes(
    private val f: CustomFilter,
    private val blacklistRepository: BlacklistRepository,
    private val feedbackRepository: FeedbackRepository,
    private val userRepository: UserRepository,
    private val configuration: BotConfiguration
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val administrationBot = bot {
        token = configuration.botToken
        dispatch {
            logger.info("Bot is running in administration mode")
            message(hasPromoCommand()) {
                handlePromoCommand(bot, message)
            }
            message(hasLinks()) {
                handleNotWhitelistedLinks(bot, message)
            }
            message(hasDocument()) {
                handleNotWhitelistedDocument(bot, message)
            }
            message(hasPhoto()) {
                handleNotWhitelistedPhoto(bot, message)
            }
            message(forwardedMessageToWhitelist()) {
                handleWhitelistUser(bot, message)
            }
        }
    }

    private fun handleWhitelistUser(bot: Bot, message: Message) {
        val userIdToWhitelist = message.forwardFrom?.id ?: 0L
        userRepository.findById(userIdToWhitelist)
            .ifPresentOrElse(
                {
                    logger.info("User {} already present - ignore insert", userIdToWhitelist)},
                {
                    userRepository.save(TGUser(userIdToWhitelist, 0, true))
                    logger.info("Whitelisted User {}", userIdToWhitelist)
                }
            )
    }

    fun hasPhoto(): Filter {
        return f.isTargetChat
            .and(f.isNotWhitelisted)
            .and(Filter.Photo)
    }

    fun hasDocument(): Filter {
        return f.isTargetChat
            .and(f.isNotWhitelisted)
            .and(f.hasDocument)
    }

    fun hasPromoCommand(): Filter {
        return f.isTargetChat
            .and(f.isPromoCommand)
    }

    fun hasLinks(): Filter {
        return f.isTargetChat
            .and(f.isNotWhitelisted)
            .and(f.hasEntities)
    }

    fun forwardedMessageToWhitelist(): Filter {
        return f.isVerificationChat
            .and(f.hasForwardedMessage)
    }

    fun handlePromoCommand(bot: Bot, message: Message) {
        bot.sendMessage(
            ChatId.fromId(message.chat.id),
            text = String.format("Hey there, %s %s", message.from?.firstName ?: "dude", configuration.promo)
        )
    }

    fun handleNotWhitelistedLinks(bot: Bot, message: Message) {
        bot.deleteMessage(ChatId.fromId(message.chat.id), message.messageId)
        message.entities!!.forEach { e ->
            if (e.type.name.contains("link") || e.type.name.contains("url")) {
                if (e.url != null) checkForBlacklist(e.url.toString(), message, bot)
            } else if (e.type.name.contains("mention")) {
                checkForBlacklist(message.messageId.toString(), message, bot)
            }
        }
    }

    fun handleNotWhitelistedPhoto(bot: Bot, message: Message) {
        bot.deleteMessage(ChatId.fromId(message.chat.id), message.messageId)
        val photoHash = message.photo?.stream()
            ?.map { p -> p.width.toString() + p.height.toString() + p.fileSize.toString() }
            ?.collect(Collectors.joining())
        if (photoHash != null) {
            checkForBlacklist(photoHash, message, bot)
        }
    }

    fun handleNotWhitelistedDocument(bot: Bot, message: Message) {
        bot.deleteMessage(ChatId.fromId(message.chat.id), message.messageId)
        val docHash = (message.document?.fileId
                + message.document?.fileName
                + message.document?.mimeType.toString()
                + message.document?.fileSize.toString())
            .toMD5()
        checkForBlacklist(docHash, message, bot)
    }

    private fun checkForBlacklist(hash: String, message: Message, bot: Bot) {
        val isBlacklisted = blacklistRepository.findFirstByHash(hash) != null
        if (isBlacklisted) {
            banSender(message, bot)
        } else {
            forwardMessageToVerify(hash, message, bot)
        }
    }

    private fun banSender(message: Message, bot: Bot) {
        bot.kickChatMember(ChatId.fromId(message.chat.id), message.from!!.id, Instant.MAX.epochSecond)
    }

    private fun forwardMessageToVerify(hash: String, message: Message, bot: Bot) {
        bot.forwardMessage(
            ChatId.fromId(configuration.verificationChat),
            ChatId.fromId(message.chat.id),
            message.messageId
        )
        generateFeedback(hash, message, bot)
    }

    private fun generateFeedback(hash: String, message: Message, bot: Bot) {

        val feedback = Feedback(0L, message.chat.id, message.from!!.id, hash)
        feedbackRepository.save(feedback)
        val feedbackByUserId = feedbackRepository.findFirstByUserId(message.from!!.id)
        val feedbackId = feedbackByUserId?.feedbackId ?: 0L
        val blacklistButton = InlineKeyboardButton.CallbackData(
            "Blacklist content and ban",
            FeedbackAction.BAN.value + "," + feedbackId
        )
        val whitelistButton = InlineKeyboardButton.CallbackData(
            "Add User to whitelist",
            FeedbackAction.WHITELIST.value + "," + feedbackId
        )
        val ignoreButton = InlineKeyboardButton.CallbackData(
            "Ignore",
            FeedbackAction.IGNORE.value + ",0"
        )
        val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
            listOf(
                listOf(blacklistButton),
                listOf(whitelistButton),
                listOf(ignoreButton)
            )
        )

        bot.sendMessage(
            ChatId.fromId(configuration.verificationChat),
            "This post was shared",
            null,
            null,
            null,
            null,
            null,
            inlineKeyboardMarkup
        )
    }

    fun String.toMD5(): String {
        val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
        return bytes.toHex()
    }

    fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

    enum class FeedbackAction(val value: String) {
        BAN("ban"),
        WHITELIST("whitelist"),
        IGNORE("ignore");
    }
}