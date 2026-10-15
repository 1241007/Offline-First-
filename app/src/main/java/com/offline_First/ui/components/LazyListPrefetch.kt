package com.offline_First.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Reusable Compose side-effect for viewport prefetch and infinite scrolling.
 * Fires [onPrefetch] when user scrolls within [prefetchThreshold] items from the list end.
 */
@Composable
fun LazyListPrefetchEffect(
    listState: LazyListState,
    prefetchThreshold: Int = 3,
    enabled: Boolean = true,
    onPrefetch: () -> Unit
) {
    if (!enabled) return

    val shouldPrefetch = remember(listState, prefetchThreshold) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= (totalItems - prefetchThreshold)
        }
    }

    LaunchedEffect(shouldPrefetch) {
        snapshotFlow { shouldPrefetch.value }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                onPrefetch()
            }
    }
}
