package com.offline_First.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.ui.navigation.AppDestination
import com.offline_First.ui.theme.EduNovaBorder
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaPrimaryContainer

enum class EduNovaBottomNavItem(
    val title: String,
    val icon: ImageVector,
    val destination: AppDestination
) {
    HOME("Home", Icons.Default.Home, AppDestination.LANDING),
    SUBJECTS("Subjects", Icons.AutoMirrored.Filled.MenuBook, AppDestination.MY_LEARNING),
    PRACTICE("Practice", Icons.AutoMirrored.Filled.Assignment, AppDestination.ROADMAP),
    COURSES("Courses", Icons.AutoMirrored.Filled.MenuBook, AppDestination.MY_LEARNING),
    ROADMAP("Roadmap", Icons.Default.AccountTree, AppDestination.ROADMAP),
    CHAT("AI Chat", Icons.Default.ChatBubbleOutline, AppDestination.AI_WORKSPACE),
    TOOLS("Tools", Icons.Default.Build, AppDestination.AI_WORKSPACE)
}

@Composable
fun EduNovaBottomNavigation(
    selectedItem: EduNovaBottomNavItem = EduNovaBottomNavItem.HOME,
    isSchoolMode: Boolean = false,
    onNavigate: (EduNovaBottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = if (isSchoolMode) {
        listOf(
            EduNovaBottomNavItem.HOME,
            EduNovaBottomNavItem.SUBJECTS,
            EduNovaBottomNavItem.PRACTICE,
            EduNovaBottomNavItem.CHAT,
            EduNovaBottomNavItem.TOOLS
        )
    } else {
        listOf(
            EduNovaBottomNavItem.HOME,
            EduNovaBottomNavItem.COURSES,
            EduNovaBottomNavItem.ROADMAP,
            EduNovaBottomNavItem.CHAT,
            EduNovaBottomNavItem.TOOLS
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, EduNovaBorder),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onNavigate(item) }
                        )
                        .padding(vertical = 4.dp)
                        .semantics { contentDescription = "Navigate to ${item.title}" }
                ) {
                    Box(
                        modifier = Modifier
                            .size(height = 28.dp, width = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) EduNovaPrimaryContainer else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (isSelected) EduNovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) EduNovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
