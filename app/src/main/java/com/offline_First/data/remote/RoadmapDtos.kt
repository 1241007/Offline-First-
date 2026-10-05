package com.offline_First.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoadmapDto(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val skills: List<String>,
    val level: String,
    val duration: String,
    val stages: Int,
    val icon: String,
    @SerialName("accentTheme") val accentTheme: String
)

@Serializable
data class RoadmapItemDto(
    val id: String,
    val title: String,
    val description: String?,
    @SerialName("courseId") val courseId: String?,
    val skills: List<String>?,
    val duration: String?
)

@Serializable
data class RoadmapDetailDto(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val level: String,
    val duration: String,
    val stages: Int,
    val icon: String,
    @SerialName("accentTheme") val accentTheme: String,
    val items: List<RoadmapItemDto>
)
