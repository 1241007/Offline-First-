package com.offline_First.data.remote

import com.offline_First.data.repository.LearningRepository
import com.offline_First.domain.model.ContinueLearningItem
import com.offline_First.domain.model.LearningCourse
import com.offline_First.domain.model.StudyFocusItem
import com.offline_First.domain.model.Subject
import com.offline_First.domain.model.UpcomingExam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.IOException

class RemoteLearningRepository(
    private val authenticatedApiClient: AuthenticatedApiClient,
    private val baseUrl: String = ChatApiConfig.BASE_URL
) : LearningRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun getInProgressCourses(): Result<List<LearningCourse>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/learning/courses/in-progress")
                .get()
                .build()

            authenticatedApiClient.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: Failed to fetch in-progress courses")
                }
                val body = response.body?.string() ?: "[]"
                val dtos = json.decodeFromString<List<LearningCourseResponseDto>>(body)
                dtos.map {
                    LearningCourse(
                        id = it.id,
                        name = it.name,
                        lesson = it.lesson,
                        progress = it.progress
                    )
                }
            }
        }
    }

    override suspend fun getCompletedCourses(): Result<List<LearningCourse>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/learning/courses/completed")
                .get()
                .build()

            authenticatedApiClient.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: Failed to fetch completed courses")
                }
                val body = response.body?.string() ?: "[]"
                val dtos = json.decodeFromString<List<LearningCourseResponseDto>>(body)
                dtos.map {
                    LearningCourse(
                        id = it.id,
                        name = it.name,
                        lesson = it.lesson,
                        progress = it.progress
                    )
                }
            }
        }
    }

    override suspend fun getSubjects(): Result<List<Subject>> = withContext(Dispatchers.IO) {
        Result.success(
            listOf(
                Subject(
                    id = "subj-math",
                    name = "Mathematics",
                    icon = "calculate",
                    progress = 0.65f,
                    totalTopics = 15,
                    gradeLevel = "Class 10 • CBSE",
                    completedChapters = 9,
                    totalChapters = 14
                ),
                Subject(
                    id = "subj-phys",
                    name = "Physics",
                    icon = "science",
                    progress = 0.50f,
                    totalTopics = 12,
                    gradeLevel = "Class 10 • CBSE",
                    completedChapters = 6,
                    totalChapters = 12
                ),
                Subject(
                    id = "subj-chem",
                    name = "Chemistry",
                    icon = "science",
                    progress = 0.40f,
                    totalTopics = 10,
                    gradeLevel = "Class 10 • CBSE",
                    completedChapters = 4,
                    totalChapters = 10
                ),
                Subject(
                    id = "subj-bio",
                    name = "Biology",
                    icon = "eco",
                    progress = 0.75f,
                    totalTopics = 14,
                    gradeLevel = "Class 10 • CBSE",
                    completedChapters = 9,
                    totalChapters = 12
                )
            )
        )
    }

    override suspend fun getContinueLearning(): Result<ContinueLearningItem?> = withContext(Dispatchers.IO) {
        Result.success(
            ContinueLearningItem(
                subjectName = "Mathematics",
                topicName = "Quadratic Equations",
                lessonInfo = "Lesson 3: Finding Roots by Factorisation",
                progress = 0.65f,
                estimatedMinutes = 12,
                practiceQuestionsCount = 5
            )
        )
    }

    override suspend fun getUpcomingExams(): Result<List<UpcomingExam>> = withContext(Dispatchers.IO) {
        Result.success(
            listOf(
                UpcomingExam(
                    id = "exam-math-term1",
                    title = "Mid-Term Mathematics",
                    daysRemaining = 8,
                    className = "Class 10"
                ),
                UpcomingExam(
                    id = "exam-sci-term1",
                    title = "Science Periodic Test",
                    daysRemaining = 14,
                    className = "Class 10"
                )
            )
        )
    }

    override suspend fun getStudyFocus(): Result<StudyFocusItem?> = withContext(Dispatchers.IO) {
        Result.success(
            StudyFocusItem(
                subjectName = "Physics",
                actionTitle = "Revise Light: Reflection & Refraction",
                lessonInfo = "Practice Ray Diagrams and Lens Formula"
            )
        )
    }
}
