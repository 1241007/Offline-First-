package com.offline_First.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CourseDto(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    @SerialName("accentColor") val accentColor: String = "primary",
    val category: String = "",
    @SerialName("categorySlug") val categorySlug: String? = null,
    val slug: String? = null,
    val level: String = "Beginner",
    @SerialName("durationHours") val durationHours: Int? = null,
    @SerialName("isFeatured") val isFeatured: Boolean = false,
    @SerialName("totalLessons") val totalLessons: Int = 0,
    @SerialName("modulesCount") val modulesCount: Int = 0
)

@Serializable
data class LessonDto(
    val id: String,
    val title: String,
    @SerialName("contentType") val contentType: String = "video",
    @SerialName("durationMinutes") val durationMinutes: Int? = null,
    val description: String? = null,
    @SerialName("displayOrder") val displayOrder: Int = 0,
    @SerialName("resourceUrl") val resourceUrl: String? = null,
    @SerialName("resourceProvider") val resourceProvider: String? = null,
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("videoProvider") val videoProvider: String? = null
)

@Serializable
data class ModuleDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("displayOrder") val displayOrder: Int = 0,
    val lessons: List<LessonDto> = emptyList()
)

@Serializable
data class CourseDetailDto(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    @SerialName("accentColor") val accentColor: String = "primary",
    val category: String = "",
    @SerialName("categorySlug") val categorySlug: String? = null,
    val slug: String? = null,
    val level: String = "Beginner",
    @SerialName("durationHours") val durationHours: Int? = null,
    @SerialName("isFeatured") val isFeatured: Boolean = false,
    @SerialName("totalLessons") val totalLessons: Int = 0,
    @SerialName("modulesCount") val modulesCount: Int = 0,
    val prerequisites: List<String> = emptyList(),
    val modules: List<ModuleDto> = emptyList()
)
