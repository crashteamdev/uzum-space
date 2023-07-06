package dev.crashteam.uzumspace.repository.redis

import dev.crashteam.uzumspace.repository.redis.entity.CookieEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CookieRepository : CrudRepository<CookieEntity, String>
