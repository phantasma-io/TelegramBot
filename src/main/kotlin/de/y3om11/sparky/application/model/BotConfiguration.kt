package de.y3om11.sparky.application.model

data class BotConfiguration(val botToken: String,
                            val botMimetypes: String,
                            val botFilenames: String,
                            val admins: List<Long>,
                            val chatsToWatch: List<Long>,
                            val verificationChat: Long,
                            val mode: String,
                            val promo: String,
                            val nextCommand: String,
                            val firstWarning: String,
                            val secondWarning: String,
                            val thirdWarning: String)
