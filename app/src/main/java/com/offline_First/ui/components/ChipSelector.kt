package com.offline_First.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EduNovaFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = Color.White,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) selectedContainerColor else unselectedContainerColor)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = if (selected) selectedContentColor else unselectedContentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}

@Composable
fun EduNovaBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
