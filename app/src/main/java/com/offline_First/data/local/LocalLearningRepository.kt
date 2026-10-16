package com.offline_First.data.local

import com.offline_First.data.repository.LearningRepository
import com.offline_First.domain.model.ContinueLearningItem
import com.offline_First.domain.model.LearningCourse
import com.offline_First.domain.model.StudyFocusItem
import com.offline_First.domain.model.Subject
import com.offline_First.domain.model.UpcomingExam

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

    private val schoolSubjects = listOf(
        Subject(id = "sub-math", name = "Mathematics", icon = "🧮", progress = 0.80f, totalTopics = 12),
        Subject(id = "sub-phy", name = "Physics", icon = "⚛", progress = 0.65f, totalTopics = 10),
        Subject(id = "sub-chem", name = "Chemistry", icon = "🧪", progress = 0.45f, totalTopics = 8),
        Subject(id = "sub-bio", name = "Biology", icon = "🧬", progress = 0.90f, totalTopics = 14)
    )

    private val continueItem = ContinueLearningItem(
        subjectName = "Mathematics",
        topicName = "Quadratic Equations",
        lessonInfo = "Lesson 8 of 12",
        progress = 0.80f
    )

    private val upcomingExams = listOf(
        UpcomingExam("exam-1", "Mathematics Board Exam", 12, "Class 10 CBSE"),
        UpcomingExam("exam-2", "Physics Pre-Board", 15, "Class 10 CBSE"),
        UpcomingExam("exam-3", "Chemistry Board Exam", 19, "Class 10 CBSE")
    )

    private val studyFocus = StudyFocusItem(
        subjectName = "Mathematics",
        actionTitle = "Practice / Continue Quadratic Equations",
        lessonInfo = "Lesson 8 of 12"
    )

    override suspend fun getInProgressCourses(): Result<List<LearningCourse>> {
        return Result.success(inProgressCourses)
    }

    override suspend fun getCompletedCourses(): Result<List<LearningCourse>> {
        return Result.success(completedCourses)
    }

    override suspend fun getSubjects(): Result<List<Subject>> {
        return Result.success(schoolSubjects)
    }

    override suspend fun getContinueLearning(): Result<ContinueLearningItem?> {
        return Result.success(continueItem)
    }

    override suspend fun getUpcomingExams(): Result<List<UpcomingExam>> {
        return Result.success(upcomingExams)
    }

    override suspend fun getStudyFocus(): Result<StudyFocusItem?> {
        return Result.success(studyFocus)
    }
}
