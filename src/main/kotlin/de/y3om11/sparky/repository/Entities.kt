package de.y3om11.sparky.repository

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class TGUser (@Id @GeneratedValue(strategy = GenerationType.AUTO) var userId: Long = 0L,
                                    var count: Int = 0,
                                    var trusted: Boolean = false)

@Entity
class Feedback (@Id @GeneratedValue(strategy = GenerationType.AUTO) var feedbackId: Long = 0L,
                var userId: Long = 0L,
                var chatId: Long = 0L,
                var data: String = "")

@Entity
class Blacklist (@Id @GeneratedValue(strategy = GenerationType.AUTO) var blackListId: Long = 0L,
                 var hash: String = "")