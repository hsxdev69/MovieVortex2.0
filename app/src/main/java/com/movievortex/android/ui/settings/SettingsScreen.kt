package com.movievortex.android.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movievortex.android.controller.SettingsController
import com.movievortex.android.ui.components.VortexIcons
import com.movievortex.android.ui.theme.VortexColors

@Composable
fun SettingsScreen(
    settingsController: SettingsController,
    onOpenDnsSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by settingsController.uiState.collectAsState()
    val context = LocalContext.current

    var adultModeEnabled by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var animeSkipEnabled by remember { mutableStateOf(true) }
    var skipDuration by remember { mutableStateOf("10s") }
    var showSkipDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VortexColors.PureBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Top Header (Centered "Settings" as in Screenshot_20261014_180812.jpg)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. "Make MovieVortex yours" Account Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF141414))
                .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Make MovieVortex yours",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sync your list and watching history",
                    color = Color(0xFF888888),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "Sign in with Vortex Account", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 48.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Sign in",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. "My List" Action Card
        SettingsActionCard(
            icon = VortexIcons.Bookmark,
            title = "My List",
            subtitle = "Your saved movies, series and anime",
            onClick = {
                Toast.makeText(context, "Opening My List...", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. "Join Our Telegram Channel" Action Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF141414))
                .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
                .clickable {
                    try {
                        val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/MovieVortex"))
                        context.startActivity(telegramIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Telegram channel: @MovieVortex", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Telegram circular blue icon
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2CA5E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = VortexIcons.Share,
                        contentDescription = "Telegram",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Join Our Telegram Channel",
                    color = Color(0xFF2CA5E0),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. "Connect to TV" Action Card
        SettingsActionCard(
            icon = VortexIcons.Tv,
            title = "Connect to TV",
            subtitle = "Scan the QR shown by VortexTV",
            onClick = {
                Toast.makeText(context, "Opening VortexTV Scanner...", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Preferences Section Header
        Text(
            text = "Preferences",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preferences Group Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141414))
                .border(1.dp, Color(0xFF222222), RoundedCornerShape(14.dp))
        ) {
            // Subtitle Settings Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        Toast.makeText(context, "Subtitle customization", Toast.LENGTH_SHORT).show()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = VortexIcons.Subtitles,
                    contentDescription = "Subtitles",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Subtitle Settings",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Customize subtitle size, color, outline and position",
                        color = Color(0xFF888888),
                        fontSize = 11.sp
                    )
                }
                Icon(
                    imageVector = VortexIcons.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(18.dp)
                )
            }

            SettingsDivider()

            // Adult Mode Toggle Row (with [18] badge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF888888), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "18",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Adult Mode",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Show 18+ content in homepage and search results",
                        color = Color(0xFF888888),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = adultModeEnabled,
                    onCheckedChange = { adultModeEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF222222),
                        uncheckedThumbColor = Color(0xFFAAAAAA),
                        uncheckedTrackColor = Color(0xFF1A1A1A)
                    )
                )
            }

            SettingsDivider()

            // Notifications Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = VortexIcons.Security,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifications",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "New releases and important updates are enabled",
                        color = Color(0xFF888888),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF222222),
                        uncheckedThumbColor = Color(0xFFAAAAAA),
                        uncheckedTrackColor = Color(0xFF1A1A1A)
                    )
                )
            }

            SettingsDivider()

            // Double tap to skip Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSkipDropdown = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = VortexIcons.Forward10,
                    contentDescription = "Skip",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Double tap to skip",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Choose how far the player seeks",
                        color = Color(0xFF888888),
                        fontSize = 11.sp
                    )
                }

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showSkipDropdown = true }
                    ) {
                        Text(
                            text = skipDuration,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = VortexIcons.ChevronDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSkipDropdown,
                        onDismissRequest = { showSkipDropdown = false }
                    ) {
                        listOf("5s", "10s", "15s", "30s").forEach { sec ->
                            DropdownMenuItem(
                                text = { Text(sec) },
                                onClick = {
                                    skipDuration = sec
                                    showSkipDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            SettingsDivider()

            // Anime skip button Row (Show Skip 90s)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = VortexIcons.SkipNext,
                    contentDescription = "Anime skip",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Anime skip button",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Show Skip 90s during anime playback",
                        color = Color(0xFF888888),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = animeSkipEnabled,
                    onCheckedChange = { animeSkipEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF222222),
                        uncheckedThumbColor = Color(0xFFAAAAAA),
                        uncheckedTrackColor = Color(0xFF1A1A1A)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SettingsActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141414))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = VortexIcons.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF888888),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.6.dp)
            .background(Color(0xFF222222))
    )
}
