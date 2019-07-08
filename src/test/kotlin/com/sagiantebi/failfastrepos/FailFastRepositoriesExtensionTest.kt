package com.sagiantebi.failfastrepos

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.util.*

class FailFastRepositoriesExtensionTest {

    @Test
    fun `when inserting a single failsafe it should be present`() {
        val e = FailFastRepositoriesExtension(ProjectBuilder.builder().build())
        e.failFast(Repo("http://repo1.com"))
        assertEquals(e.repos.size, 1)
    }

    @Test
    fun `when inserting multiple failsafes insertion order should presist`() {
        val e = FailFastRepositoriesExtension(ProjectBuilder.builder().build())
        e.failFast(Repo("http://repo1.com"))
        e.failFast(Repo("http://repo2.com"))
        assertEquals(e.repos.size, 2)
        assertEquals(e.repos.poll().invokationCount(), 0)
        assertEquals(e.repos.poll().invokationCount(), 1)
    }

    @Test
    fun `when inserting multiple fallbacks insertion order should presist`() {
        val e = FailFastRepositoriesExtension(ProjectBuilder.builder().build())
        e.fallback(Collections.singletonList(Repo("http://repo1.com")))
        e.fallback(Collections.singletonList(Repo("http://repo2.com")))
        assertEquals(e.repos.size, 2)
        assertEquals(e.repos.poll().invokationCount(), 0)
        assertEquals(e.repos.poll().invokationCount(), 1)
    }

    @Test
    fun `when inserting multiple items insertion order and weight distribution should presist - failfast first`() {
        val e = FailFastRepositoriesExtension(ProjectBuilder.builder().build())
        e.failFast(Repo("http://repo1.com"))
        e.failFast(Repo("http://repo2.com"))
        e.fallback(Collections.singletonList(Repo("http://repo1.com")))
        e.fallback(Collections.singletonList(Repo("http://repo2.com")))
        assertEquals(e.repos.size, 4)
        assertAll("failfast 1", {
            val that = e.repos.poll()
            assertEquals(that.weight().ordinal, 0)
            assertEquals(that.invokationCount(), 0)
        })
        assertAll("failfast 2", {
            val that = e.repos.poll()
            assertEquals(that.weight().ordinal, 0)
            assertEquals(that.invokationCount(), 1)
        })
        assertAll("fallback 1", {
            val that = e.repos.poll()
            assertEquals(that.weight().ordinal, 1)
            assertEquals(that.invokationCount(), 2)
        })
        assertAll("fallback 1", {
            val that = e.repos.poll()
            assertEquals(that.weight().ordinal, 1)
            assertEquals(that.invokationCount(), 3)
        })
    }

    @Test
    fun `when inserting multiple items insertion order and weight distribution should presist - fallback first`() {
        val e = FailFastRepositoriesExtension(ProjectBuilder.builder().build())
        e.fallback(Collections.singletonList(Repo("http://repo1.com")))
        e.fallback(Collections.singletonList(Repo("http://repo2.com")))
        e.failFast(Repo("http://repo1.com"))
        e.failFast(Repo("http://repo2.com"))
        assertEquals(e.repos.size, 4)
        assertAll("failfast 1", {
            val that = e.repos.poll()
            assertEquals(that.weight().ordinal, 0)
            assertEquals(that.invokationCount(), 2)
        })
        assertAll("failfast 2", {
            val that = e.repos.poll()
            assertEquals(that.weight().ordinal, 0)
            assertEquals(that.invokationCount(), 3)
        })
        assertAll("fallback 1", {
            val that = e.repos.poll()
            assertEquals(that.weight().ordinal, 1)
            assertEquals(that.invokationCount(), 0)
        })
        assertAll("fallback 1", {
            val that = e.repos.poll()
            assertEquals(that.weight().ordinal, 1)
            assertEquals(that.invokationCount(), 1)
        })
    }

}