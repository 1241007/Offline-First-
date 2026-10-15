package com.offline_First.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    @SerialName("contact") val contact: String,
    @SerialName("password") val password: String
)

@Serializable
data class RegisterRequestDto(
    @SerialName("fullName") val fullName: String,
    @SerialName("email") val email: String,
    @SerialName("mobile") val mobile: String? = null,
    @SerialName("password") val password: String
)

@Serializable
data class RefreshTokenRequestDto(
    @SerialName("refreshToken") val refreshToken: String
)

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("mobile") val mobile: String? = null,
    @SerialName("fullName") val fullName: String
)

@Serializable
data class AuthTokensResponseDto(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("tokenType") val tokenType: String,
    @SerialName("expiresIn") val expiresIn: Int,
    @SerialName("user") val user: UserDto
)

@Serializable
data class ForgotPasswordRequestDto(
    @SerialName("contact") val contact: String
)

@Serializable
data class ResetPasswordRequestDto(
    @SerialName("token") val token: String,
    @SerialName("newPassword") val newPassword: String
)

@Serializable
data class AuthMessageResponseDto(
    @SerialName("message") val message: String
)

@Serializable
data class ForgotPasswordResponseDto(
    @SerialName("message") val message: String,
    @SerialName("devResetToken") val devResetToken: String? = null
)

@Serializable
data class LogoutRequestDto(
    @SerialName("refreshToken") val refreshToken: String? = null
)

@Serializable
data class ApiErrorDto(
    @SerialName("code") val code: String,
    @SerialName("message") val message: String
)
