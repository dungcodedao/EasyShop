package com.example.easyshop.util

object FormValidation {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isEmailValid(email: String): Boolean =
        email.isNotBlank() && EMAIL_REGEX.matches(email)

    fun isPasswordValid(password: String): Boolean =
        password.length >= 6 && password.any { it.isLetter() } && password.any { it.isDigit() }

    fun isPasswordMatch(password: String, confirmPassword: String): Boolean =
        password == confirmPassword

    fun isNameValid(name: String): Boolean =
        name.trim().isNotBlank()

    fun isSignUpFormValid(
        email: String,
        name: String,
        password: String,
        confirmPassword: String
    ): Boolean =
        isEmailValid(email) &&
                isNameValid(name) &&
                isPasswordValid(password) &&
                isPasswordMatch(password, confirmPassword)
}
