package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdUnits
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppConfig
import com.example.model.DEFAULT_ADMOB_BANNER_UNIT_ID
import com.example.model.DEFAULT_ADMOB_REWARDED_UNIT_ID
import com.example.model.isValidAdMobUnitId
import com.example.model.normalizeAdMobUnitId
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline

@Composable
fun AdminAdMobTab(
    appConfig: AppConfig,
    isBusy: Boolean,
    onSaveAppConfig: (AppConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Status Banner
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCard),
            border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AdUnits,
                    contentDescription = "Google AdMob Live Config",
                    tint = CyanSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Live Google AdMob Unit Manager",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Change, add, or remove Rewarded and Banner Ad Unit IDs live across all user devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Rewarded Ad Unit ID Card
        AdMobUnitSlotCard(
            title = "Rewarded Ad Unit ID",
            subtitle = "Used for Match rewards, Daily Streak, Tap Tap, Math Quiz & Watch 3 Ads (+15 Coins).",
            icon = Icons.Filled.PlayCircle,
            accent = EmeraldPrimary,
            activeUnitId = appConfig.rewardedAdUnitId,
            savedPool = appConfig.savedRewardedAdUnitIds,
            defaultTestId = DEFAULT_ADMOB_REWARDED_UNIT_ID,
            isBusy = isBusy,
            tagPrefix = "rewarded",
            onApplyUpdatedSlot = { newActive, newPool ->
                onSaveAppConfig(
                    appConfig.copy(
                        rewardedAdUnitId = newActive,
                        savedRewardedAdUnitIds = newPool
                    )
                )
            }
        )

        // Banner Ad Unit ID Card
        AdMobUnitSlotCard(
            title = "Banner Ad Unit ID",
            subtitle = "Displayed on Home, History, Wallet, Ads Lab, Tap Tap & Math Quiz screens.",
            icon = Icons.Filled.ViewAgenda,
            accent = CyanSecondary,
            activeUnitId = appConfig.bannerAdUnitId,
            savedPool = appConfig.savedBannerAdUnitIds,
            defaultTestId = DEFAULT_ADMOB_BANNER_UNIT_ID,
            isBusy = isBusy,
            tagPrefix = "banner",
            onApplyUpdatedSlot = { newActive, newPool ->
                onSaveAppConfig(
                    appConfig.copy(
                        bannerAdUnitId = newActive,
                        savedBannerAdUnitIds = newPool
                    )
                )
            }
        )
    }
}

@Composable
private fun AdMobUnitSlotCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    activeUnitId: String,
    savedPool: List<String>,
    defaultTestId: String,
    isBusy: Boolean,
    tagPrefix: String,
    onApplyUpdatedSlot: (newActive: String, newPool: List<String>) -> Unit
) {
    var activeInput by remember { mutableStateOf(activeUnitId) }
    var newPoolInput by remember { mutableStateOf("") }
    var activeError by remember { mutableStateOf<String?>(null) }
    var poolError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeUnitId) {
        activeInput = activeUnitId
        activeError = null
    }

    val isEnabled = activeUnitId.isNotBlank()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard),
        border = BorderStroke(1.dp, if (isEnabled) accent.copy(alpha = 0.55f) else CoralError.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_admob_${tagPrefix}_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = if (isEnabled) accent else CoralError)
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = (if (isEnabled) EmeraldPrimary else CoralError).copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isEnabled) "ACTIVE" else "REMOVED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isEnabled) EmeraldPrimary else CoralError,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Current Active Ad Unit ID Editor
            OutlinedTextField(
                value = activeInput,
                onValueChange = {
                    activeInput = it
                    activeError = null
                },
                label = { Text("Active AdMob Unit ID (ca-app-pub-.../...)") },
                placeholder = { Text(defaultTestId) },
                isError = activeError != null,
                supportingText = activeError?.let { msg -> { Text(msg, color = CoralError) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_${tagPrefix}_active_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val cleaned = normalizeAdMobUnitId(activeInput)
                        if (!isValidAdMobUnitId(cleaned)) {
                            activeError = "Format: ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
                            return@Button
                        }
                        val nextPool = (savedPool + cleaned).distinct()
                        onApplyUpdatedSlot(cleaned, nextPool)
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = if (accent == EmeraldPrimary) EmeraldOnPrimary else Color(0xFF082F49)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("admin_${tagPrefix}_save_button")
                ) {
                    Text("Change / Save", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        activeInput = ""
                        activeError = null
                        onApplyUpdatedSlot("", savedPool)
                    },
                    enabled = !isBusy && (isEnabled || activeInput.isNotBlank()),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CoralError.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("admin_${tagPrefix}_remove_active_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.RemoveCircle,
                        contentDescription = "Remove Active Ad Unit ID",
                        tint = CoralError,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Remove", color = CoralError)
                }
            }

            OutlinedButton(
                onClick = {
                    activeInput = defaultTestId
                    activeError = null
                    val nextPool = (listOf(defaultTestId) + savedPool).distinct()
                    onApplyUpdatedSlot(defaultTestId, nextPool)
                },
                enabled = !isBusy && activeUnitId != defaultTestId,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MidnightOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Use Google Test Unit ID ($defaultTestId)", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Saved Pool + Add New ID
            Surface(
                color = MidnightCardHigh,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MidnightOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Saved Ad Unit IDs (${savedPool.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    if (savedPool.isEmpty()) {
                        Text(
                            text = "No saved IDs yet. Add a Google AdMob Unit ID below.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        savedPool.forEach { unitId ->
                            val isCurrentlyActive = unitId == activeUnitId
                            Surface(
                                color = if (isCurrentlyActive) accent.copy(alpha = 0.14f) else MidnightCard,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrentlyActive) accent else MidnightOutline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = unitId,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCurrentlyActive) accent else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isCurrentlyActive) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                append(if (isCurrentlyActive) "Active Live" else "Saved")
                                                if (unitId == defaultTestId) append(" • Google Test ID")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCurrentlyActive) EmeraldPrimary else AmberGold
                                        )
                                    }

                                    if (!isCurrentlyActive) {
                                        OutlinedButton(
                                            onClick = { onApplyUpdatedSlot(unitId, savedPool) },
                                            enabled = !isBusy,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = "Set active", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.size(4.dp))
                                            Text("Use", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            val updatedPool = savedPool.filterNot { it == unitId }
                                            val updatedActive = if (unitId == activeUnitId) "" else activeUnitId
                                            onApplyUpdatedSlot(updatedActive, updatedPool)
                                        },
                                        enabled = !isBusy,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Remove Ad Unit ID",
                                            tint = CoralError,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add New Unit ID Row
                    OutlinedTextField(
                        value = newPoolInput,
                        onValueChange = {
                            newPoolInput = it
                            poolError = null
                        },
                        label = { Text("Add New Ad Unit ID") },
                        placeholder = { Text("ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY") },
                        isError = poolError != null,
                        supportingText = poolError?.let { msg -> { Text(msg, color = CoralError) } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_${tagPrefix}_add_pool_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cleaned = normalizeAdMobUnitId(newPoolInput)
                                if (!isValidAdMobUnitId(cleaned)) {
                                    poolError = "Invalid format (expected ca-app-pub-digits/digits)"
                                    return@OutlinedButton
                                }
                                val nextPool = (savedPool + cleaned).distinct()
                                newPoolInput = ""
                                onApplyUpdatedSlot(activeUnitId, nextPool)
                            },
                            enabled = !isBusy && newPoolInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Add Only")
                        }

                        Button(
                            onClick = {
                                val cleaned = normalizeAdMobUnitId(newPoolInput)
                                if (!isValidAdMobUnitId(cleaned)) {
                                    poolError = "Invalid format (expected ca-app-pub-digits/digits)"
                                    return@Button
                                }
                                val nextPool = (savedPool + cleaned).distinct()
                                newPoolInput = ""
                                onApplyUpdatedSlot(cleaned, nextPool)
                            },
                            enabled = !isBusy && newPoolInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = EmeraldOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_${tagPrefix}_add_activate_button")
                        ) {
                            Text("Add & Use Live", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
