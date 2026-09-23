package dev.gtnh.intellij.contribution

import junit.framework.TestCase

class GtnhPrDraftServiceTest : TestCase() {
    fun testDoesNotInventTestingIssueOrHumanClaims() {
        val draft = GtnhPrDraftService.generate(GtnhPrDraftInput("Improve navigation", listOf("Add reverse lookup")))
        assertTrue(draft.contains("- Not run."))
        assertTrue(draft.contains("- None specified."))
        assertTrue(draft.contains("- [ ] DevEnv tested"))
        assertFalse(draft.contains("Fixes #"))
    }

    fun testOnlyExplicitIssueCanCloseAndConfirmationsRemainExplicit() {
        val draft = GtnhPrDraftService.generate(GtnhPrDraftInput(
            "Summary", emptyList(), listOf("test" to true), 123, true,
            setOf(GtnhHumanConfirmation.AI_POLICY_ACKNOWLEDGED)
        ))
        assertTrue(draft.contains("Fixes #123"))
        assertTrue(draft.contains("- [x] test"))
        assertTrue(draft.contains("- [x] AI policy acknowledged"))
        assertTrue(draft.contains("- [ ] Fullpack tested"))
    }
}
