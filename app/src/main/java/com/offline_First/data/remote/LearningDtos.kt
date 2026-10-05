package com.offline_First.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LearningCourseResponseDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("lesson") val lesson: String,
    @SerialName("progress") val progress: Float
)
