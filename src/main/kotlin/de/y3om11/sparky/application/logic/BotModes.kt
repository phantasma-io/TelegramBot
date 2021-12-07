package de.y3om11.sparky.application.logic

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ChatPermissions
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import de.y3om11.sparky.application.model.BotConfiguration
import de.y3om11.sparky.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.String.format
import java.security.MessageDigest
import java.time.Instant
import java.util.stream.Collectors
import kotlin.math.log

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
            message(hasNextCommand()) {
                handleNextCommand(bot, message)
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
                handleWhitelistUser(message)
            }
            message(hasWarnCommand()) {
                handleWarnCommand(bot, message)
            }
            message(hasForgiveCommand()) {
                handleForgiveCommand(bot, message)
            }
            message(hasMuteOneMinuteCommand()) {
                handleMuteOneMinuteCommand(bot, message)
            }
            message(hasMuteOneDayCommand()) {
                handleMuteOneDayCommand(bot, message)
            }
            message(hasMuteTwoDaysCommand()) {
                handleMuteTwoDaysCommand(bot, message)
            }
            message(hasUnmuteCommand()) {
                handleUnmuteCommand(bot, message)
            }
            message(hasBanCommand()) {
                handleBanCommand(bot, message)
            }
            message(hasUnbanCommand()) {
                handleUnbanCommand(bot, message)
            }
            message(hasTrustCommand()) {
                handleTrustCommand(message)
            }
        }
    }

    private fun hasPhoto(): Filter {
        return f.isTargetChat
            .and(f.isNotWhitelisted)
            .and(Filter.Photo)
    }

    private fun hasDocument(): Filter {
        return f.isTargetChat
            .and(f.isNotWhitelisted)
            .and(f.hasDocument)
    }

    private fun hasPromoCommand(): Filter {
        return f.isTargetChat
            .and(f.isPromoCommand)
    }

    private fun hasNextCommand(): Filter {
        return f.isTargetChat
            .and(f.isNextCommand)
    }

    private fun hasWarnCommand(): Filter {
        return f.isTargetChat
            .and(f.isAdmin)
            .and(f.isWarnCommand)
    }

    private fun hasForgiveCommand(): Filter {
        return f.isTargetChat
            .and(f.isAdmin)
            .and(f.isForgiveCommand)
    }

    private fun hasMuteOneMinuteCommand(): Filter {
        return f.isTargetChat
            .and(f.isAdmin)
            .and(f.isMuteOneMinuteCommand)
    }

    private fun hasMuteOneDayCommand(): Filter {
        return f.isTargetChat
            .and(f.isAdmin)
            .and(f.isMuteOneDayCommand)
    }

    private fun hasMuteTwoDaysCommand(): Filter {
        return f.isTargetChat
            .and(f.isAdmin)
            .and(f.isMuteTwoDaysCommand)
    }

    private fun hasUnmuteCommand(): Filter {
        return f.isTargetChat
            .and(f.isAdmin)
            .and(f.isUnmuteCommand)
    }

    private fun hasBanCommand(): Filter {
        return f.isTargetChat
            .and(f.isAdmin)
            .and(f.isBanCommand)
    }

    private fun hasUnbanCommand(): Filter {
        return f.isTargetChat
            .and(f.isAdmin)
            .and(f.isUnbanCommand)
    }

    private fun hasTrustCommand(): Filter {
        return f.isTargetChat
            .and(f.isAdmin)
            .and(f.isTrustCommand)
    }

    private fun hasLinks(): Filter {
        return f.isTargetChat
            .and(f.isNotWhitelisted)
            .and(f.hasEntities)
    }

    private fun forwardedMessageToWhitelist(): Filter {
        return f.isVerificationChat
            .and(f.hasForwardedMessage)
    }

    private fun handleTrustCommand(message: Message) {
        userRepository.findById(message.replyToMessage!!.from!!.id).ifPresentOrElse({
            user -> user.trusted = true
            userRepository.save(user)
        },
        {
            userRepository.save(TGUser(message.replyToMessage!!.from!!.id, 0, true))
        })
    }

    private fun handleUnbanCommand(bot: Bot, message: Message) {
        bot.unbanChatMember(ChatId.fromId(message.chat.id), message.replyToMessage!!.from!!.id)
    }

    private fun handleBanCommand(bot: Bot, message: Message) {
        bot.kickChatMember(ChatId.fromId(message.chat.id), message.replyToMessage!!.from!!.id, Instant.MAX.epochSecond)
    }

    private fun handleUnmuteCommand(bot: Bot, message: Message) {
        bot.restrictChatMember(ChatId.fromId(message.chat.id), message.replyToMessage!!.from!!.id,
            ChatPermissions(canSendMessages = true, canSendMediaMessages = true, canSendOtherMessages = true),
            Instant.MAX.epochSecond)
    }

    private fun handleMuteTwoDaysCommand(bot: Bot, message: Message) {
        restrictForTime(bot, message, 172800)
    }

    private fun handleMuteOneDayCommand(bot: Bot, message: Message) {
        restrictForTime(bot, message, 86400)
    }

    private fun handleMuteOneMinuteCommand(bot: Bot, message: Message) {
        restrictForTime(bot, message, 3600)
    }

    private fun handleForgiveCommand(bot: Bot, message: Message) {
        userRepository.findById(message.replyToMessage!!.from!!.id).ifPresent {
            user -> user.count = 0
            userRepository.save(user)
        }
        bot.sendMessage(
            ChatId.fromId(message.chat.id),
            text = String.format("All is forgiven, %s, I love you! (again)", message.replyToMessage!!.from?.firstName ?: "dude")
        )
    }

    private fun handleWarnCommand(bot: Bot, message: Message) {
        var currentCount = 1
        val targetUserId = message.replyToMessage!!.from!!.id
        userRepository.findById(targetUserId).ifPresentOrElse({
            user -> user.count += 1
            currentCount = user.count
            userRepository.save(user)
        },
        {
            userRepository.save(TGUser(targetUserId, currentCount, false))
        })
        when (currentCount) {
            1 -> {
                bot.sendMessage(
                    ChatId.fromId(message.chat.id),
                    text = String.format(configuration.firstWarning, message.replyToMessage!!.from?.firstName ?: "dude")
                )
            }
            2 -> {
                bot.sendMessage(
                    ChatId.fromId(message.chat.id),
                    text = String.format(configuration.secondWarning, message.replyToMessage!!.from?.firstName ?: "dude")
                )
            }
            3 -> {
                bot.sendMessage(
                    ChatId.fromId(message.chat.id),
                    text = String.format(configuration.thirdWarning, message.replyToMessage!!.from?.firstName ?: "dude")
                )
                bot.kickChatMember(ChatId.fromId(message.chat.id), targetUserId, Instant.MAX.epochSecond)
            }
        }
    }

    private fun handleNextCommand(bot: Bot, message: Message) {
        bot.sendMessage(
            ChatId.fromId(message.chat.id),
            text = String.format("Hey there, %s %s", message.from?.firstName ?: "dude", configuration.nextCommand)
        )
    }

    private fun handleWhitelistUser(message: Message) {
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

    private fun handlePromoCommand(bot: Bot, message: Message) {
        bot.sendMessage(
            ChatId.fromId(message.chat.id),
            text = String.format("Hey there, %s %s", message.from?.firstName ?: "dude", configuration.promo)
        )
    }

    private fun restrictForTime(bot: Bot, message: Message, time: Long) {
        logger.info(format("User %s will be restricted for %s seconds", message.replyToMessage!!.from!!.id, time))
        bot.restrictChatMember(
            ChatId.fromId(message.chat.id), message.replyToMessage!!.from!!.id,
            ChatPermissions(canSendMessages = false, canSendMediaMessages = false, canSendOtherMessages = false),
            Instant.now().epochSecond + time
        )
    }

    private fun handleNotWhitelistedLinks(bot: Bot, message: Message) {
        bot.deleteMessage(ChatId.fromId(message.chat.id), message.messageId)
        message.entities!!.forEach { e ->
            if (e.type.name.contains("link") || e.type.name.contains("url")) {
                if (e.url != null) checkForBlacklist(e.url.toString(), message, bot)
            } else if (e.type.name.contains("mention")) {
                checkForBlacklist(message.messageId.toString(), message, bot)
            }
        }
    }

    private fun handleNotWhitelistedPhoto(bot: Bot, message: Message) {
        bot.deleteMessage(ChatId.fromId(message.chat.id), message.messageId)
        val photoHash = message.photo?.stream()
            ?.map { p -> p.width.toString() + p.height.toString() + p.fileSize.toString() }
            ?.collect(Collectors.joining())
        if (photoHash != null) {
            checkForBlacklist(photoHash, message, bot)
        }
    }

    private fun handleNotWhitelistedDocument(bot: Bot, message: Message) {
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