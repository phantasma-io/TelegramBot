package de.y3om11.sparky.repository

import org.springframework.data.repository.CrudRepository

interface FeedbackRepository: CrudRepository<Feedback, Long> {
    fun findFirstByUserId(userId: Long): Feedback?
}