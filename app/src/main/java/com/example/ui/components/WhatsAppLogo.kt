package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline

val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppDeepGreen = Color(0xFF128C7E)

/**
 * Vector-drawn WhatsApp logo (speech bubble with tail + phone handset glyph).
 */
@Composable
fun WhatsAppLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.52f
        val cy = h * 0.48f
        val r = w * 0.42f

        // Outer green badge circle + classic bottom-left tail
        val bubblePath = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    left = cx - r,
                    top = cy - r,
                    right = cx + r,
                    bottom = cy + r
                )
            )
        }
        val tailPath = Path().apply {
            moveTo(cx - r * 0.72f, cy + r * 0.48f)
            lineTo(w * 0.08f, h * 0.92f)
            lineTo(cx - r * 0.16f, cy + r * 0.88f)
            close()
        }

        drawPath(
            path = tailPath,
            color = WhatsAppGreen
        )
        drawPath(
            path = bubblePath,
            brush = Brush.linearGradient(
                colors = listOf(WhatsAppGreen, WhatsAppDeepGreen),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Subtle crisp white inner ring
        drawCircle(
            color = Color.White,
            radius = r * 0.82f,
            center = Offset(cx, cy),
            style = Stroke(width = w * 0.075f)
        )

        // Classic curved phone receiver inside
        val phonePath = Path().apply {
            moveTo(cx - r * 0.30f, cy - r * 0.36f)
            quadraticTo(cx - r * 0.46f, cy - r * 0.14f, cx - r * 0.24f, cy + r * 0.14f)
            quadraticTo(cx - r * 0.02f, cy + r * 0.42f, cx + r * 0.32f, cy + r * 0.32f)
            lineTo(cx + r * 0.40f, cy + r * 0.16f)
            lineTo(cx + r * 0.20f, cy + r * 0.06f)
            lineTo(cx + r * 0.10f, cy + r * 0.14f)
            quadraticTo(cx - r * 0.06f, cy + r * 0.04f, cx - r * 0.16f, cy - r * 0.12f)
            lineTo(cx - r * 0.08f, cy - r * 0.22f)
            lineTo(cx - r * 0.18f, cy - r * 0.38f)
            close()
        }
        drawPath(
            path = phonePath,
            color = Color.White
        )
        drawPath(
            path = phonePath,
            color = Color.White,
            style = Stroke(width = w * 0.02f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun LiveAnnouncementBannerCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    if (title.isBlank() && body.isBlank()) return
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard),
        border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_announcement_banner_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            CyanSecondary.copy(alpha = 0.14f),
                            EmeraldPrimary.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = CyanSecondary.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Campaign,
                        contentDescription = "Official Announcement",
                        tint = CyanSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = CyanSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (body.isNotBlank()) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun LiveCustomAlertCard(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCardHigh),
        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_custom_alert_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = "New message from NexaEarn",
                tint = AmberGold,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AmberGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                color = MidnightCard,
                border = BorderStroke(1.dp, MidnightOutline)
            ) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun MaintenanceLockScreen(
    message: String,
    supportHours: String,
    onContactSupport: () -> Unit,
    onStealthTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBg)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCard),
            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = AmberGold.copy(alpha = 0.16f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Build,
                            contentDescription = "Scheduled Maintenance",
                            tint = AmberGold,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Text(
                    text = "App Update & Maintenance",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message.ifBlank {
                        "NexaEarn is currently updating live. Please try again shortly or reach Customer Support."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onContactSupport,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    WhatsAppLogoIcon(size = 22.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "WhatsApp Customer Support",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (supportHours.isNotBlank()) {
                    Text(
                        text = "Support Hours: $supportHours",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onStealthTap
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun BannedAccountLockScreen(
    email: String?,
    banReason: String?,
    supportHours: String,
    onContactSupport: () -> Unit,
    onSignOut: () -> Unit,
    onStealthTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBg)
            .safeDrawingPadding()
            .padding(24.dp)
            .testTag("banned_account_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCard),
            border = BorderStroke(1.dp, CoralError.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = CoralError.copy(alpha = 0.16f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.GppBad,
                            contentDescription = "Account Suspended / Blacklisted",
                            tint = CoralError,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Text(
                    text = "Account Blacklisted",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (!email.isNullOrBlank()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                Surface(
                    color = CoralError.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CoralError.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = banReason?.ifBlank { null }
                            ?: "Your NexaEarn account has been suspended by an Administrator. Please contact Customer Support via WhatsApp if you believe this is a mistake.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Button(
                    onClick = onContactSupport,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    WhatsAppLogoIcon(size = 22.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Appeal on WhatsApp Support",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MidnightOutline),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MidnightCardHigh),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Sign Out")
                }
                if (supportHours.isNotBlank()) {
                    Text(
                        text = "Support Hours: $supportHours",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onStealthTap
                        )
                    )
                }
            }
        }
    }
}
