package com.offline_First.data.local

import com.offline_First.data.repository.CourseRepository
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseAccent

class LocalCourseRepository : CourseRepository {

    private val allCourses = listOf(
        Course("c-py", "Python", "Build a strong programming foundation.", "Py", CourseAccent.PRIMARY),
        Course("c-java", "Java", "Learn practical object-oriented programming.", "J", CourseAccent.SECONDARY),
        Course("c-cpp", "C++", "Strengthen logic with powerful fundamentals.", "C+", CourseAccent.PRIMARY),
        Course("c-ds", "Data Science", "Turn data into useful insights.", "DS", CourseAccent.SECONDARY),
        Course("c-dsa", "DSA", "Master problem solving and algorithms.", "⌘", CourseAccent.ACCENT),
        Course("c-fs", "Full Stack", "Create complete modern web experiences.", "</>", CourseAccent.PRIMARY)
    )

    override suspend fun getCourses(): Result<List<Course>> {
        return Result.success(allCourses)
    }

    override suspend fun getFeaturedCourses(): Result<List<Course>> {
        // Featured subset: Python, DSA, Full Stack
        return Result.success(listOf(allCourses[0], allCourses[4], allCourses[5]))
    }
}
