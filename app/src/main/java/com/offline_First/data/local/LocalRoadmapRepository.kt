package com.offline_First.data.local

import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.GeneratedRoadmapPreview
import com.offline_First.domain.model.RoadmapAccentTheme
import com.offline_First.domain.model.RoadmapOption
import kotlinx.coroutines.delay

class LocalRoadmapRepository : RoadmapRepository {

    private val categories = listOf(
        "All",
        "Development",
        "Data & AI",
        "Mobile",
        "Cloud & DevOps",
        "Security"
    )

    private val roadmaps = listOf(
        RoadmapOption(
            id = "fe-dev",
            title = "Frontend Developer",
            category = "Development",
            description = "Build polished interfaces and production-ready web apps.",
            skills = listOf("HTML", "CSS", "JavaScript", "React"),
            level = "Beginner",
            duration = "8–10 weeks",
            stages = 6,
            icon = "FE",
            accentTheme = RoadmapAccentTheme.PRIMARY
        ),
        RoadmapOption(
            id = "be-dev",
            title = "Backend Developer",
            category = "Development",
            description = "Create APIs, databases, and backend systems students can ship.",
            skills = listOf("Node.js", "APIs", "Databases", "Security"),
            level = "Intermediate",
            duration = "10–12 weeks",
            stages = 7,
            icon = "BE",
            accentTheme = RoadmapAccentTheme.SECONDARY
        ),
        RoadmapOption(
            id = "fs-dev",
            title = "Full Stack Developer",
            category = "Development",
            description = "Move from UI work to complete product development workflows.",
            skills = listOf("React", "Node", "Databases", "DevOps"),
            level = "Intermediate",
            duration = "12–14 weeks",
            stages = 8,
            icon = "FS",
            accentTheme = RoadmapAccentTheme.SUCCESS
        ),
        RoadmapOption(
            id = "ds-sci",
            title = "Data Scientist",
            category = "Data & AI",
            description = "Learn data analysis, modeling, and decision-making with real datasets.",
            skills = listOf("Python", "SQL", "Statistics", "ML"),
            level = "Beginner",
            duration = "10–12 weeks",
            stages = 7,
            icon = "DS",
            accentTheme = RoadmapAccentTheme.SECONDARY
        ),
        RoadmapOption(
            id = "ml-eng",
            title = "Machine Learning Engineer",
            category = "Data & AI",
            description = "Master model training, evaluation, and deployment for AI products.",
            skills = listOf("Python", "ML", "PyTorch", "Math"),
            level = "Intermediate",
            duration = "12–16 weeks",
            stages = 9,
            icon = "ML",
            accentTheme = RoadmapAccentTheme.ACCENT
        ),
        RoadmapOption(
            id = "android-dev",
            title = "Android Developer",
            category = "Mobile",
            description = "Build user-friendly mobile apps with Kotlin and Jetpack Compose.",
            skills = listOf("Kotlin", "Compose", "UI", "Testing"),
            level = "Beginner",
            duration = "8–10 weeks",
            stages = 6,
            icon = "AD",
            accentTheme = RoadmapAccentTheme.SECONDARY
        ),
        RoadmapOption(
            id = "devops-eng",
            title = "DevOps Engineer",
            category = "Cloud & DevOps",
            description = "Learn automation, deployment flow, and cloud-first engineering habits.",
            skills = listOf("Linux", "CI/CD", "Cloud", "Containers"),
            level = "Intermediate",
            duration = "10–12 weeks",
            stages = 7,
            icon = "DO",
            accentTheme = RoadmapAccentTheme.ACCENT
        ),
        RoadmapOption(
            id = "data-analyst",
            title = "Data Analyst",
            category = "Data & AI",
            description = "Turn messy numbers into trends, dashboards, and smart business decisions.",
            skills = listOf("Excel", "SQL", "Tableau", "Insights"),
            level = "Beginner",
            duration = "6–8 weeks",
            stages = 5,
            icon = "DA",
            accentTheme = RoadmapAccentTheme.SECONDARY
        ),
        RoadmapOption(
            id = "cybersec",
            title = "Cybersecurity",
            category = "Security",
            description = "Build the fundamentals of secure systems, networks, and threat awareness.",
            skills = listOf("Networking", "Security", "Ethical Hacking", "Monitoring"),
            level = "Intermediate",
            duration = "10–12 weeks",
            stages = 7,
            icon = "CY",
            accentTheme = RoadmapAccentTheme.PRIMARY
        )
    )

    override suspend fun getRoadmaps(): Result<List<RoadmapOption>> {
        return Result.success(roadmaps)
    }

    override suspend fun getCategories(): List<String> {
        return categories
    }

    override suspend fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ): Result<GeneratedRoadmapPreview> {
        delay(1200L) // Simulate non-blocking async generation
        return Result.success(
            GeneratedRoadmapPreview(
                goal = goal,
                level = level,
                studyTime = studyTime,
                duration = "10–12 weeks",
                stages = listOf(
                    "Python Foundations",
                    "Math for ML",
                    "Data Processing",
                    "Machine Learning",
                    "Deep Learning",
                    "Real-world Projects"
                )
            )
        )
    }
}
