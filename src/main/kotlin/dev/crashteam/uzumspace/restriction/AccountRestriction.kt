package dev.crashteam.uzumspace.restriction

interface AccountRestriction {

    fun uzumAccountLimit(): Int

    fun itemPoolLimit(): Int

    fun itemCompetitorLimit(): Int
}
