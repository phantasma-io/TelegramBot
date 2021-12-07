package de.y3om11.sparky.repository

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
open class TGUser (@Id open var userId: Long = 0L,
                   open var count: Int = 0,
                   open var trusted: Boolean = false){
    override fun toString(): String {
        return "TGUser(userId=$userId, count=$count, trusted=$trusted)"
    }
}

@Entity
open class Feedback (@Id @GeneratedValue(strategy = GenerationType.AUTO) open var feedbackId: Long = 0L,
                open var userId: Long = 0L,
                open var chatId: Long = 0L,
                open var data: String = ""){
    override fun toString(): String {
        return "Feedback(feedbackId=$feedbackId, userId=$userId, chatId=$chatId, data='$data')"
    }
}

@Entity
open class Blacklist (@Id @GeneratedValue(strategy = GenerationType.AUTO) open var blackListId: Long = 0L,
                 open var hash: String = ""){
    override fun toString(): String {
        return "Blacklist(blackListId=$blackListId, hash='$hash')"
    }
}