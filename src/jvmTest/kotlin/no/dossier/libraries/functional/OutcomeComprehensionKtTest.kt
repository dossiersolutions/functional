package no.dossier.libraries.functional

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OutcomeComprehensionKtTest {

    @Test
    fun `should succeed if all outcomes succeed`() {
        val outcome = composeOutcome<String, String> {
            val a = !Success("a")
            val b = !Success("b")
            Success("${a}${b}")
        }

        assertTrue(outcome is Success)
        assertEquals("ab", outcome.getOrElse { throw Exception() })
    }

    @Test
    fun `should fail if first outcome is a failure`() {
        val outcome = composeOutcome<String, String> {
            val a = !(Failure("failed") as Outcome<String, String>)
            val b = !Success("b")
            Success("${a}${b}")
        }
        assertTrue(outcome is Failure)
        assertEquals("failed", (outcome as Failure<String>).error)
    }

    @Test
    fun `should fail if second outcome is a failure`() {
        val outcome = composeOutcome<String, String> {
            val a = !Success("a")
            val b = !(Failure("failed") as Outcome<String, String>)
            Success("${a}${b}")
        }
        assertTrue(outcome is Failure)
        assertEquals("failed", (outcome as Failure<String>).error)
    }

    @Test
    fun `should fail if all outcomes are failures`() {
        val outcome = composeOutcome<String, String> {
            val a = !(Failure("failed a") as Outcome<String, String>)
            val b = !(Failure("failed b") as Outcome<String, String>)
            Success("${a}${b}")
        }
        assertTrue(outcome is Failure)
        assertEquals("failed a", (outcome as Failure<String>).error)
    }
}