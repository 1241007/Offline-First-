package com.offline_First.data.remote

import com.offline_First.domain.model.EducationMode
import com.offline_First.domain.model.UserProfile
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RAGContextPropagationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun testUserProfileCurriculumFields() {
        val profile = UserProfile(
            fullName = "Ananya Sharma",
            email = "ananya@example.com",
            mobile = "9876543210",
            interests = "Science and Math",
            level = "Intermediate",
            educationMode = EducationMode.SCHOOL,
            board = "CBSE",
            classGrade = "10",
            subject = "Science",
            preferredLanguage = "en"
        )

        assertEquals("CBSE", profile.board)
        assertEquals("10", profile.classGrade)
        assertEquals("Science", profile.subject)
        assertEquals("en", profile.preferredLanguage)
        assertEquals(EducationMode.SCHOOL, profile.educationMode)
    }

    @Test
    fun testSendMessageRequestDtoSerialization() {
        val dto = SendMessageRequestDto(
            content = "Explain life processes",
            explanationMode = "teacher",
            clientMessageId = "msg-123",
            parentId = null,
            appMode = "student",
            board = "CBSE",
            classGrade = "10",
            subject = "Science",
            language = "en",
            selectedChapter = "Chapter 6 Life Processes"
        )

        val serialized = json.encodeToString(SendMessageRequestDto.serializer(), dto)
        assertTrue(serialized.contains("\"app_mode\":\"student\""))
        assertTrue(serialized.contains("\"board\":\"CBSE\""))
        assertTrue(serialized.contains("\"class_grade\":\"10\""))
        assertTrue(serialized.contains("\"subject\":\"Science\""))
        assertTrue(serialized.contains("\"selected_chapter\":\"Chapter 6 Life Processes\""))

        val deserialized = json.decodeFromString(SendMessageRequestDto.serializer(), serialized)
        assertEquals("student", deserialized.appMode)
        assertEquals("CBSE", deserialized.board)
        assertEquals("10", deserialized.classGrade)
        assertEquals("Science", deserialized.subject)
        assertEquals("Chapter 6 Life Processes", deserialized.selectedChapter)
    }

    @Test
    fun testProfileDtoCurriculumSerialization() {
        val rawJson = """
            {
                "fullName": "Rohan Verma",
                "email": "rohan@example.com",
                "mobile": "9998887776",
                "interests": "Coding",
                "level": "Advanced",
                "educationMode": "school",
                "board": "ICSE",
                "classGrade": "11",
                "subject": "Computer Science",
                "preferredLanguage": "en"
            }
        """.trimIndent()

        val dto = json.decodeFromString(ProfileResponseDto.serializer(), rawJson)
        assertEquals("ICSE", dto.board)
        assertEquals("11", dto.classGrade)
        assertEquals("Computer Science", dto.subject)
        assertEquals("en", dto.preferredLanguage)
        assertEquals("school", dto.educationMode)
    }
}
