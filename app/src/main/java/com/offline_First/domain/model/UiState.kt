package com.offline_First.domain.model

/**
 * Standard UI state encapsulation for data-driven screens.
 * Supports offline-first presentation, errors, empty results, and ongoing loading.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}
