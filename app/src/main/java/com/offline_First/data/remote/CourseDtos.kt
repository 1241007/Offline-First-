package com.offline_First.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CourseDto(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    @SerialName("accentColor") val accentColor: String
)

@Serializable
data class LessonDto(
    val id: String,
    val title: String,
    @SerialName("contentType") val contentType: String,
    @SerialName("durationMinutes") val durationMinutes: Int?
)

@Serializable
data class ModuleDto(
    val id: String,
    val title: String,
    val description: String?,
    val lessons: List<LessonDto>
)

@Serializable
data class CourseDetailDto(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    @SerialName("accentColor") val accentColor: String,
    val level: String,
    @SerialName("durationHours") val durationHours: Int?,
    val modules: List<ModuleDto>
)
