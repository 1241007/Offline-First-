package com.offline_First.domain.model

enum class RoadmapAccentTheme {
    PRIMARY,
    SECONDARY,
    ACCENT,
    SUCCESS
}

data class RoadmapOption(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val skills: List<String>,
    val level: String,
    val duration: String,
    val stages: Int,
    val icon: String,
    val accentTheme: RoadmapAccentTheme = RoadmapAccentTheme.PRIMARY
)

data class GeneratedRoadmapPreview(
    val goal: String,
    val level: String,
    val studyTime: String,
    val duration: String,
    val stages: List<String>
)
