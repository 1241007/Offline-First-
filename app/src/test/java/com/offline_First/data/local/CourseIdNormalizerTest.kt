package com.offline_First.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CourseIdNormalizerTest {

    @Before
    fun setUp() {
        CourseIdNormalizer.resetForTesting()
    }

    @Test
    fun preConfiguredAliasesResolveBidirectionally() {
        assertTrue(CourseIdNormalizer.isSameCourse("c-cpp", "cpp-programming"))
        assertTrue(CourseIdNormalizer.isSameCourse("cpp-programming", "c-cpp"))
        assertTrue(CourseIdNormalizer.isSameCourse("c-cpp", "cpp"))

        assertEquals("c-cpp", CourseIdNormalizer.resolveToLocalId("cpp-programming"))
        assertEquals("c-cpp", CourseIdNormalizer.resolveToLocalId("c-cpp"))
    }

    @Test
    fun dynamicRemoteRegistrationBridgesBackendUuidToLocalId() {
        val actualBackendUuid = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
        
        // Simulating actual API record received from backend
        CourseIdNormalizer.registerRemoteCourse(
            id = actualBackendUuid,
            slug = "cpp-programming",
            name = "C++ Programming"
        )

        // Now actual backend UUID is linked to cpp-programming and c-cpp
        assertTrue(CourseIdNormalizer.isSameCourse(actualBackendUuid, "c-cpp"))
        assertTrue(CourseIdNormalizer.isSameCourse(actualBackendUuid, "cpp-programming"))
        assertEquals("c-cpp", CourseIdNormalizer.resolveToLocalId(actualBackendUuid))

        val aliases = CourseIdNormalizer.getAllAliases(actualBackendUuid)
        assertTrue(aliases.contains("c-cpp"))
        assertTrue(aliases.contains("cpp-programming"))
        assertTrue(aliases.contains(actualBackendUuid))
    }

    @Test
    fun toSlugHandlesSpecialCharactersAndSpaces() {
        assertEquals("cpp-programming", CourseIdNormalizer.toSlug("C++ Programming"))
        assertEquals("data-structures-and-algorithms", CourseIdNormalizer.toSlug("Data Structures & Algorithms"))
        assertEquals("full-stack-web-development", CourseIdNormalizer.toSlug("Full Stack Web Development"))
    }
}
