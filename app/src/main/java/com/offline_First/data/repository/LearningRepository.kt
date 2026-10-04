package com.offline_First.data.repository

import com.offline_First.domain.model.LearningCourse
import com.offline_First.domain.model.ContinueLearningItem
import com.offline_First.domain.model.StudyFocusItem
import com.offline_First.domain.model.Subject
import com.offline_First.domain.model.UpcomingExam

/**
 * Repository interface for student's personal learning progress.
 */
interface LearningRepository {
    suspend fun getInProgressCourses(): Result<List<LearningCourse>>
    suspend fun getCompletedCourses(): Result<List<LearningCourse>>
    suspend fun getSubjects(): Result<List<Subject>>
    suspend fun getContinueLearning(): Result<ContinueLearningItem?>
    suspend fun getUpcomingExams(): Result<List<UpcomingExam>>
    suspend fun getStudyFocus(): Result<StudyFocusItem?>
}
