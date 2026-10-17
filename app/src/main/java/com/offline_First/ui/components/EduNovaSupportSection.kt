package com.offline_First.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaBorder
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaPrimaryContainer
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSurface
import com.offline_First.ui.theme.EduNovaTextPrimary
import com.offline_First.ui.theme.EduNovaTextSecondary

enum class SupportIcon(val containerColor: Color) {
    Help(EduNovaPrimaryContainer),
    Contact(EduNovaPrimaryContainer.copy(alpha = 0.5f)),
    About(EduNovaPrimaryContainer.copy(alpha = 0.3f))
}

@Composable
fun SupportIcon(icon: SupportIcon) {
    Canvas(Modifier.size(22.dp)) {
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (icon) {
            SupportIcon.Help -> {
                val questionMark = Path().apply {
                    moveTo(size.width * .3f, size.height * .36f)
                    cubicTo(
                        size.width * .32f, size.height * .12f,
                        size.width * .7f, size.height * .12f,
                        size.width * .72f, size.height * .36f
                    )
                    cubicTo(
                        size.width * .73f, size.height * .56f,
                        size.width * .52f, size.height * .58f,
                        size.width * .5f, size.height * .75f
                    )
                }
                drawPath(questionMark, EduNovaPrimary, style = stroke)
                drawCircle(EduNovaPrimary, 1.2.dp.toPx(), center.copy(y = size.height * .9f))
            }
            SupportIcon.Contact -> {
                drawRoundRect(
                    color = EduNovaSecondary,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * .12f, size.height * .24f),
                    size = androidx.compose.ui.geometry.Size(size.width * .76f, size.height * .52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    style = stroke
                )
                drawLine(
                    EduNovaSecondary,
                    androidx.compose.ui.geometry.Offset(size.width * .16f, size.height * .3f),
                    androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .56f),
                    stroke.width
                )
                drawLine(
                    EduNovaSecondary,
                    androidx.compose.ui.geometry.Offset(size.width * .84f, size.height * .3f),
                    androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .56f),
                    stroke.width
                )
            }
            SupportIcon.About -> {
                drawCircle(EduNovaPrimary, size.minDimension * .36f, style = stroke)
                drawCircle(EduNovaPrimary, 1.2.dp.toPx(), center.copy(y = size.height * .36f))
                drawLine(
                    EduNovaPrimary,
                    center.copy(y = size.height * .49f),
                    center.copy(y = size.height * .7f),
                    stroke.width
                )
            }
        }
    }
}

/**
 * Responsive Help & Support section designed for landing and home dashboards.
 */
@Composable
fun SupportSection(
    onSupportAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Help & Support",
            color = ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp
        )
        Text(
            text = "Need a hand? We're here to help.",
            color = mutedInk,
            fontSize = 13.5.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(2.dp))
        SupportRow(
            icon = SupportIcon.Help,
            title = "Help Center",
            description = "Find answers to common learning questions.",
            onClick = { onSupportAction("Help resources are being prepared.") }
        )
        SupportRow(
            icon = SupportIcon.Contact,
            title = "Contact Support",
            description = "Need help? Get in touch with our support team.",
            onClick = { onSupportAction("Support contact will be available soon.") }
        )
        SupportRow(
            icon = SupportIcon.About,
            title = "About EduNova",
            description = "Learn more about EduNova and its learning experience.",
            onClick = { onSupportAction("More about EduNova will be available soon.") }
        )
    }
}

@Composable
fun SupportRow(
    icon: SupportIcon,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Open $title" },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, EduNovaBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(icon.containerColor),
                contentAlignment = Alignment.Center
            ) {
                SupportIcon(icon)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    color = ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = mutedInk,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = EduNovaPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
