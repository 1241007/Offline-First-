package com.offline_First.data.repository

import com.offline_First.domain.model.Course

/**
 * Repository interface for course catalog.
 */
interface CourseRepository {
    suspend fun getCourses(): Result<List<Course>>
    suspend fun getFeaturedCourses(): Result<List<Course>>
}
