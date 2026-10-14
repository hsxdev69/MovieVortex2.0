package com.movievortex.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.movievortex.android.ui.components.VortexIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movievortex.android.controller.SettingsController
import com.movievortex.android.ui.components.MovieVortexMark
import com.movievortex.android.ui.theme.VortexColors

@Composable
fun SettingsScreen(
    settingsController: SettingsController,
    onOpenDnsSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by settingsController.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VortexColors.PureBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Settings",
            color = VortexColors.PrimaryText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Profile Summary Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(VortexColors.CardSurface)
                .border(1.dp, VortexColors.BorderSubtle, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x33E50914)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = VortexIcons.Person,
                        contentDescription = "Profile",
                        tint = VortexColors.CinematicRed,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "MovieVortex VIP",
                        color = VortexColors.PrimaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ultra HD 4K Streaming Active",
                        color = VortexColors.SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Playback Preferences Group
        SettingsSection(title = "PLAYBACK") {
            SettingsToggleRow(
                icon = VortexIcons.HighQuality,
                title = "Auto-play Next Episode",
                subtitle = "Seamlessly play the next episode when current ends",
                checked = state.autoPlayNext,
                onCheckedChange = { settingsController.setAutoPlayNext(it) }
            )

            SettingsDivider()

            SettingsActionRow(
                icon = VortexIcons.HighQuality,
                title = "Default Streaming Quality",
                value = state.defaultQuality
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Network & DNS Group
        SettingsSection(title = "NETWORK & SECURITY") {
            SettingsClickableRow(
                icon = VortexIcons.Dns,
                title = "Secure DNS (DoH)",
                subtitle = "Encrypted streaming resolution & anti-throttling",
                value = settingsController.dnsOptions.firstOrNull { it.id == state.selectedDnsId }?.name ?: "Cloudflare",
                onClick = onOpenDnsSetup
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Downloads Group
        SettingsSection(title = "DOWNLOADS") {
            SettingsToggleRow(
                icon = VortexIcons.Wifi,
                title = "Download over Wi-Fi Only",
                subtitle = "Save mobile cellular data allowance",
                checked = state.downloadWifiOnly,
                onCheckedChange = { settingsController.setDownloadWifiOnly(it) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Library & Storage Group
        SettingsSection(title = "DATA & STORAGE") {
            SettingsClickableRow(
                icon = VortexIcons.Storage,
                title = "Clear App Cache",
                subtitle = "Free temporary cache storage",
                value = "${state.cacheSizeMb} MB",
                onClick = { settingsController.clearCache() }
            )

            SettingsDivider()

            SettingsClickableRow(
                icon = VortexIcons.DeleteSweep,
                title = "Clear Watch History",
                subtitle = "Reset all Continue Watching entries",
                value = "Clear",
                onClick = { settingsController.clearWatchHistory() }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 6. About MovieVortex
        SettingsSection(title = "ABOUT") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MovieVortexMark(size = 32.dp, showGlow = false)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "MovieVortex",
                        color = VortexColors.PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0.0 (Production Release)",
                        color = VortexColors.MutedText,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            color = VortexColors.MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(VortexColors.CardSurface)
                .border(1.dp, VortexColors.BorderSubtle, RoundedCornerShape(16.dp))
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.6.dp)
            .background(VortexColors.Divider)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VortexColors.SecondaryText,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = VortexColors.PrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = VortexColors.MutedText,
                fontSize = 11.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VortexColors.CinematicRed,
                uncheckedThumbColor = VortexColors.MutedText,
                uncheckedTrackColor = VortexColors.DarkGraphite
            )
        )
    }
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VortexColors.SecondaryText,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = VortexColors.PrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = VortexColors.MutedText,
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .background(Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = value,
                color = VortexColors.SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VortexColors.SecondaryText,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            color = VortexColors.PrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .background(Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = value,
                color = VortexColors.SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
