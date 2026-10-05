package com.offline_First.ui.screens.ai.tools

data class SubjectMastery(
    val subject: String,
    val scorePercentage: Int,
    val grade: String,
    val strongArea: String,
    val focusArea: String
)

data class LearningMetric(
    val label: String,
    val value: String,
    val change: String,
    val isPositive: Boolean
)

data class StudentLearningReport(
    val studentName: String,
    val academicPeriod: String,
    val overallScore: Int,
    val studyHoursTotal: Float,
    val quizzesCompleted: Int,
    val flashcardsMastered: Int,
    val metrics: List<LearningMetric>,
    val subjectMastery: List<SubjectMastery>,
    val aiRecommendations: List<String>
)

object DemoReportsProvider {
    fun getStudentReport(studentName: String = "Student"): StudentLearningReport {
        return StudentLearningReport(
            studentName = if (studentName.isNotBlank() && studentName != "Student") studentName else "Krish Patil",
            academicPeriod = "Past 30 Days (Demo Period)",
            overallScore = 86,
            studyHoursTotal = 42.5f,
            quizzesCompleted = 18,
            flashcardsMastered = 142,
            metrics = listOf(
                LearningMetric("Accuracy Rate", "88.4%", "+5.2% this week", true),
                LearningMetric("Study Streak", "12 Days", "Personal best 🔥", true),
                LearningMetric("Concept Retention", "92%", "+3.8%", true),
                LearningMetric("Avg. Time / Quiz", "4m 12s", "-45s faster", true)
            ),
            subjectMastery = listOf(
                SubjectMastery(
                    subject = "Physics (Mechanics & Waves)",
                    scorePercentage = 92,
                    grade = "A+",
                    strongArea = "Newton's Laws & Inertia",
                    focusArea = "Work-Energy Theorem & Friction"
                ),
                SubjectMastery(
                    subject = "Mathematics (Calculus)",
                    scorePercentage = 88,
                    grade = "A",
                    strongArea = "Derivatives & Product Rule",
                    focusArea = "Definite Integrals by Substitution"
                ),
                SubjectMastery(
                    subject = "Computer Science (Python)",
                    scorePercentage = 95,
                    grade = "A+",
                    strongArea = "Data Structures & Time Complexity",
                    focusArea = "Memory Profiling & Generators"
                ),
                SubjectMastery(
                    subject = "Chemistry (Bonding)",
                    scorePercentage = 76,
                    grade = "B+",
                    strongArea = "Ionic Bonds & Stoichiometry",
                    focusArea = "Hybridization & Le Chatelier's Principle"
                )
            ),
            aiRecommendations = listOf(
                "Revise Chemistry chemical equilibrium using the AI Flashcard Deck (focus on temperature shift effects).",
                "Great consistency on Physics! Try CBSE Previous Year Questions (PYQs) in Exam Mode for 5-mark derivations.",
                "Maintain your 12-day streak to unlock the 'Active Recall Champion' badge."
            )
        )
    }
}
