package dev.crashteam.uzumspace.service.encryption

interface PasswordEncryptor {
    fun encryptPassword(password: String): ByteArray

    fun decryptPassword(password: ByteArray): String
}
