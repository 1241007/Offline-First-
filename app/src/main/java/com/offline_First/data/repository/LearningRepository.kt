package com.offline_First.data.repository

import com.offline_First.domain.model.LearningCourse

/**
 * Repository interface for student's personal learning progress.
 */
interface LearningRepository {
    suspend fun getInProgressCourses(): Result<List<LearningCourse>>
    suspend fun getCompletedCourses(): Result<List<LearningCourse>>
}
