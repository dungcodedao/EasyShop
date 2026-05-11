package com.example.easyshop.util

import org.junit.Test
import org.junit.Assert.*

class FormValidationTest {

    // --- isEmailValid ---

    @Test
    fun `isEmailValid - valid email returns true`() {
        assertTrue(FormValidation.isEmailValid("user@example.com"))
    }

    @Test
    fun `isEmailValid - blank email returns false`() {
        assertFalse(FormValidation.isEmailValid(""))
    }

    @Test
    fun `isEmailValid - no domain returns false`() {
        assertFalse(FormValidation.isEmailValid("user@"))
    }

    @Test
    fun `isEmailValid - no at sign returns false`() {
        assertFalse(FormValidation.isEmailValid("userexample.com"))
    }

    // --- isPasswordValid ---

    @Test
    fun `isPasswordValid - valid password returns true`() {
        assertTrue(FormValidation.isPasswordValid("abc123"))
    }

    @Test
    fun `isPasswordValid - too short returns false`() {
        assertFalse(FormValidation.isPasswordValid("a1"))
    }

    @Test
    fun `isPasswordValid - no digit returns false`() {
        assertFalse(FormValidation.isPasswordValid("abcdef"))
    }

    @Test
    fun `isPasswordValid - no letter returns false`() {
        assertFalse(FormValidation.isPasswordValid("123456"))
    }

    // --- isNameValid ---

    @Test
    fun `isNameValid - valid name returns true`() {
        assertTrue(FormValidation.isNameValid("Nguyen Van A"))
    }

    @Test
    fun `isNameValid - blank name returns false`() {
        assertFalse(FormValidation.isNameValid("   "))
    }

    @Test
    fun `isNameValid - empty name returns false`() {
        assertFalse(FormValidation.isNameValid(""))
    }

    // --- isPasswordMatch ---

    @Test
    fun `isPasswordMatch - matching passwords returns true`() {
        assertTrue(FormValidation.isPasswordMatch("abc123", "abc123"))
    }

    @Test
    fun `isPasswordMatch - different passwords returns false`() {
        assertFalse(FormValidation.isPasswordMatch("abc123", "xyz789"))
    }

    // --- isSignUpFormValid ---

    @Test
    fun `isSignUpFormValid - all valid returns true`() {
        assertTrue(FormValidation.isSignUpFormValid("user@test.com", "Nguyen Van A", "pass123", "pass123"))
    }

    @Test
    fun `isSignUpFormValid - invalid email returns false`() {
        assertFalse(FormValidation.isSignUpFormValid("bad-email", "Name", "pass123", "pass123"))
    }

    @Test
    fun `isSignUpFormValid - blank name returns false`() {
        assertFalse(FormValidation.isSignUpFormValid("user@test.com", "  ", "pass123", "pass123"))
    }

    @Test
    fun `isSignUpFormValid - weak password returns false`() {
        assertFalse(FormValidation.isSignUpFormValid("user@test.com", "Name", "123", "123"))
    }

    @Test
    fun `isSignUpFormValid - password mismatch returns false`() {
        assertFalse(FormValidation.isSignUpFormValid("user@test.com", "Name", "pass123", "pass456"))
    }
}
