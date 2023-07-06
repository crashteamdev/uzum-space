package dev.crashteam.uzumspace.repository.redis

import dev.crashteam.uzumspace.repository.redis.entity.UserTokenEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UzumUserTokenRepository : CrudRepository<UserTokenEntity, String>
