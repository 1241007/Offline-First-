package com.offline_First.data.remote

import com.offline_First.data.repository.CourseRepository
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseAccent

class OnlineCourseRepository : CourseRepository {

    private fun mapAccent(color: String): CourseAccent {
        return when (color.lowercase()) {
            "secondary", "orange", "yellow", "green", "cyan", "pink", "gray", "indigo" -> CourseAccent.SECONDARY
            "accent", "red", "purple" -> CourseAccent.ACCENT
            else -> CourseAccent.PRIMARY
        }
    }

    private fun CourseDto.toDomain(): Course {
        return Course(
            id = id,
            name = name,
            description = description,
            icon = icon,
            accent = mapAccent(accentColor)
        )
    }

    override suspend fun getCourses(limit: Int?, offset: Int): Result<List<Course>> {
        return ChatApiClient.getCourses(featured = null, limit = limit, offset = offset)
            .map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun getFeaturedCourses(): Result<List<Course>> {
        return ChatApiClient.getCourses(featured = true)
            .map { dtos -> dtos.map { it.toDomain() } }
    }
}
