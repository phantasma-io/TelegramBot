package de.y3om11.sparky.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface FeedbackRepository: CrudRepository<Feedback, Long> {
    fun findFirstByUserId(userId: Long): Feedback?
}