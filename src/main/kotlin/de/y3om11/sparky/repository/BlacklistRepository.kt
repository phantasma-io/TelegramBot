package de.y3om11.sparky.repository

import org.springframework.data.repository.CrudRepository

interface BlacklistRepository : CrudRepository<Blacklist, Long> {
    fun findFirstByHash(hash: String) : Blacklist?
}