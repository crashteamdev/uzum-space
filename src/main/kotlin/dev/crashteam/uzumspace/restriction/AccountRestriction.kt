package dev.crashteam.uzumspace.restriction

interface AccountRestriction {

    fun keAccountLimit(): Int

    fun itemPoolLimit(): Int

    fun itemCompetitorLimit(): Int
}
