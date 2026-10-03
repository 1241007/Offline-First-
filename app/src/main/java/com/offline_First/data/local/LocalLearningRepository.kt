package com.offline_First.data.local

import com.offline_First.data.repository.LearningRepository
import com.offline_First.domain.model.LearningCourse

class LocalLearningRepository : LearningRepository {

    private val inProgressCourses = listOf(
        LearningCourse("learn-1", "Kotlin Fundamentals", "Lesson 8 of 12", 0.67f),
        LearningCourse("learn-2", "UI Design with Compose", "Lesson 4 of 10", 0.4f),
        LearningCourse("learn-3", "Python for Beginners", "Lesson 6 of 16", 0.38f)
    )

    private val completedCourses = listOf(
        LearningCourse("learn-4", "Programming Basics", "Completed 12 lessons", 1f),
        LearningCourse("learn-5", "Git and GitHub Essentials", "Completed 8 lessons", 1f)
    )

    override suspend fun getInProgressCourses(): Result<List<LearningCourse>> {
        return Result.success(inProgressCourses)
    }

    override suspend fun getCompletedCourses(): Result<List<LearningCourse>> {
        return Result.success(completedCourses)
    }
}
