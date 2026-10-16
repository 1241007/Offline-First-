package com.offline_First.ui.screens.ai.tools

data class DailyTask(
    val id: String,
    val title: String,
    val timeSlot: String, // e.g. "07:00 AM • 45 mins"
    val taskType: String, // "Theory", "Practice", "Quiz", "Revision"
    val isDone: Boolean = false
)

data class DayPlan(
    val dayNumber: Int,
    val dayLabel: String, // e.g. "Day 1: Foundations & Core Laws"
    val focusArea: String,
    val tasks: List<DailyTask>
)

data class StudyPlanData(
    val goalTitle: String,
    val targetExam: String,
    val totalDays: Int,
    val dailyHours: Float,
    val completionPercentage: Int,
    val motivationalQuote: String,
    val days: List<DayPlan>
)

object DemoStudyPlannerProvider {
    val sampleGoals = listOf(
        "CBSE Class 10 Board Science (7-Day Sprint)",
        "Python Full-Stack & System Design (5-Day Crash)",
        "Physics Mechanics Mastery (3-Day Revision)",
        "Calculus & Mathematics Finals (5-Day Plan)"
    )

    fun getPlanForGoal(goalQuery: String): StudyPlanData {
        val query = goalQuery.trim().lowercase()
        return when {
            query.contains("python") || query.contains("code") || query.contains("stack") -> pythonPlan
            query.contains("physics") || query.contains("mech") -> physicsSprintPlan
            query.contains("math") || query.contains("calc") -> mathPlan
            else -> cbseSciencePlan
        }
    }

    private val cbseSciencePlan = StudyPlanData(
        goalTitle = "CBSE Class 10 Science Board Exam",
        targetExam = "Class 10 CBSE Board • 7-Day Sprint",
        totalDays = 7,
        dailyHours = 2.5f,
        completionPercentage = 30,
        motivationalQuote = "Consistency beats intensity. 2 focused hours today is worth 10 hours the night before the exam.",
        days = listOf(
            DayPlan(
                dayNumber = 1,
                dayLabel = "Day 1: Chemical Reactions & Equations",
                focusArea = "Balancing equations, Redox reactions, Types of reactions",
                tasks = listOf(
                    DailyTask("1-1", "Revise Combination, Decomposition & Displacement reactions", "Morning • 45m", "Theory", true),
                    DailyTask("1-2", "Solve 10 equation balancing problems (NCERT Ex 1.1)", "Afternoon • 30m", "Practice", true),
                    DailyTask("1-3", "Take EduNova AI Chemical Bonding Quiz (4 questions)", "Evening • 15m", "Quiz", false),
                    DailyTask("1-4", "Quick summary flashcards on corrosion and rancidity", "Night • 20m", "Revision", false)
                )
            ),
            DayPlan(
                dayNumber = 2,
                dayLabel = "Day 2: Electricity & Circuits",
                focusArea = "Ohm's Law, Series vs Parallel circuits, Joule's heating",
                tasks = listOf(
                    DailyTask("2-1", "Master V=IR, resistivity derivation, and resistor networks", "Morning • 50m", "Theory", false),
                    DailyTask("2-2", "Solve 5 numericals on equivalent resistance and power dissipation", "Afternoon • 45m", "Practice", false),
                    DailyTask("2-3", "Review CBSE past 5-year PYQs for Electricity", "Evening • 30m", "PYQ", false)
                )
            ),
            DayPlan(
                dayNumber = 3,
                dayLabel = "Day 3: Life Processes (Biology)",
                focusArea = "Nutrition, Photosynthesis light/dark cycles, Heart circulation",
                tasks = listOf(
                    DailyTask("3-1", "Review photosynthesis photolysis & stomata guard cells", "Morning • 45m", "Theory", false),
                    DailyTask("3-2", "Practice double circulation diagram and nephron structure", "Afternoon • 40m", "Practice", false),
                    DailyTask("3-3", "Biology Active Recall Flashcard Deck", "Evening • 20m", "Quiz", false)
                )
            ),
            DayPlan(
                dayNumber = 4,
                dayLabel = "Day 4: Light & Optics (Reflection & Refraction)",
                focusArea = "Ray diagrams for concave/convex mirrors, Snell's law",
                tasks = listOf(
                    DailyTask("4-1", "Draw all 6 concave mirror ray diagrams", "Morning • 45m", "Practice", false),
                    DailyTask("4-2", "Lens formula & magnification sign convention numericals", "Afternoon • 45m", "Practice", false),
                    DailyTask("4-3", "Attempt 15-minute optics timed mock quiz", "Night • 15m", "Quiz", false)
                )
            ),
            DayPlan(
                dayNumber = 5,
                dayLabel = "Day 5: Full Mock Paper & Weak Area Review",
                focusArea = "Complete 3-hour sample paper under exam conditions",
                tasks = listOf(
                    DailyTask("5-1", "Solve CBSE Science Sample Question Paper 2026", "Morning • 120m", "Practice", false),
                    DailyTask("5-2", "Self-assess against marking scheme using EduNova AI Report", "Evening • 45m", "Revision", false)
                )
            )
        )
    )

    private val pythonPlan = StudyPlanData(
        goalTitle = "Python Full-Stack & System Design Crash",
        targetExam = "Technical Interview & Backend Architecture",
        totalDays = 5,
        dailyHours = 3.0f,
        completionPercentage = 20,
        motivationalQuote = "First make it work, then make it right, then make it fast. — Kent Beck",
        days = listOf(
            DayPlan(
                dayNumber = 1,
                dayLabel = "Day 1: Async Python & Event Loops",
                focusArea = "asyncio, non-blocking I/O, Task orchestration",
                tasks = listOf(
                    DailyTask("p1-1", "Understand asyncio event loop, coroutines & tasks", "Morning • 60m", "Theory", true),
                    DailyTask("p1-2", "Refactor synchronous scraper to httpx async concurrent gather", "Afternoon • 60m", "Practice", false),
                    DailyTask("p1-3", "Test Python Data Structures Quiz on EduNova", "Evening • 20m", "Quiz", false)
                )
            ),
            DayPlan(
                dayNumber = 2,
                dayLabel = "Day 2: FastAPI & High Performance ASGI",
                focusArea = "Pydantic V2 validation, Dependency Injection, Middleware",
                tasks = listOf(
                    DailyTask("p2-1", "Build CRUD endpoints with Pydantic serialization models", "Morning • 60m", "Practice", false),
                    DailyTask("p2-2", "Configure CORS, background tasks & exception handlers", "Afternoon • 60m", "Practice", false)
                )
            ),
            DayPlan(
                dayNumber = 3,
                dayLabel = "Day 3: SQLAlchemy 2.0 Async & PostgreSQL",
                focusArea = "Asyncpg sessions, connection pools, indexes and relationships",
                tasks = listOf(
                    DailyTask("p3-1", "Setup AsyncEngine, async_sessionmaker & Alembic migrations", "Morning • 75m", "Practice", false),
                    DailyTask("p3-2", "Eliminate N+1 queries using selectinload & joinedload", "Evening • 60m", "Practice", false)
                )
            )
        )
    )

    private val physicsSprintPlan = StudyPlanData(
        goalTitle = "Physics Mechanics 3-Day Sprint",
        targetExam = "Mechanics & Newton's Laws Test",
        totalDays = 3,
        dailyHours = 2.0f,
        completionPercentage = 50,
        motivationalQuote = "Look up at the stars and not down at your feet. — Stephen Hawking",
        days = listOf(
            DayPlan(
                dayNumber = 1,
                dayLabel = "Day 1: Newton's Laws & Free-Body Diagrams",
                focusArea = "Inertia, F = ma along axes, Pulleys & Inclines",
                tasks = listOf(
                    DailyTask("m1-1", "Draw FBD for inclined planes with friction", "Morning • 45m", "Practice", true),
                    DailyTask("m1-2", "Solve 5 pulley-mass acceleration problems", "Afternoon • 45m", "Practice", true),
                    DailyTask("m1-3", "Attempt Newton's Laws AI Quiz", "Night • 15m", "Quiz", false)
                )
            ),
            DayPlan(
                dayNumber = 2,
                dayLabel = "Day 2: Work, Energy & Power",
                focusArea = "Work-energy theorem, Kinetic vs Potential energy, Springs",
                tasks = listOf(
                    DailyTask("m2-1", "Revise Work-Energy Theorem for variable forces", "Morning • 45m", "Theory", false),
                    DailyTask("m2-2", "Conservation of mechanical energy derivations", "Afternoon • 45m", "Practice", false)
                )
            )
        )
    )

    private val mathPlan = StudyPlanData(
        goalTitle = "Calculus & Trigonometry Finals",
        targetExam = "Class 12 / College Engineering Math",
        totalDays = 5,
        dailyHours = 3.0f,
        completionPercentage = 25,
        motivationalQuote = "Mathematics is the language with which God has written the universe.",
        days = listOf(
            DayPlan(
                dayNumber = 1,
                dayLabel = "Day 1: Limits & Continuity",
                focusArea = "L'Hôpital's Rule, standard trigonometric limits",
                tasks = listOf(
                    DailyTask("c1-1", "Revise indeterminate forms 0/0 and ∞/∞", "Morning • 60m", "Theory", true),
                    DailyTask("c1-2", "Solve 10 problems applying L'Hôpital's rule", "Afternoon • 60m", "Practice", false)
                )
            )
        )
    )
}
