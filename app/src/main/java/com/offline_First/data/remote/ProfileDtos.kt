package com.offline_First.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponseDto(
    @SerialName("fullName") val fullName: String,
    @SerialName("email") val email: String,
    @SerialName("mobile") val mobile: String? = null,
    @SerialName("interests") val interests: String? = null,
    @SerialName("level") val level: String,
    @SerialName("educationMode") val educationMode: String
)

@Serializable
data class ProfileUpdateRequestDto(
    @SerialName("fullName") val fullName: String,
    @SerialName("email") val email: String,
    @SerialName("mobile") val mobile: String? = null,
    @SerialName("interests") val interests: String? = null,
    @SerialName("level") val level: String,
    @SerialName("educationMode") val educationMode: String
)
