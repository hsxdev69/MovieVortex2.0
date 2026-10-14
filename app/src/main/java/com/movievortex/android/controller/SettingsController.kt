package com.movievortex.android.controller

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.utils.DataStoreHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DnsOption(
    val id: Int,
    val name: String,
    val provider: String,
    val ips: String,
    val description: String
)

data class SettingsUiState(
    val selectedDnsId: Int = 2, // Cloudflare 1.1.1.1 default
    val autoPlayNext: Boolean = true,
    val defaultQuality: String = "1080p FHD",
    val downloadWifiOnly: Boolean = true,
    val cacheSizeMb: Long = 18L
)

class SettingsController(private val context: Context) : ViewModel() {

    private val settingsManager = PreferenceManager.getDefaultSharedPreferences(context)
    private val dnsKey = context.getString(R.string.dns_key)

    val dnsOptions = listOf(
        DnsOption(2, "Cloudflare 1.1.1.1", "Cloudflare", "1.1.1.1 / 1.0.0.1", "Fastest worldwide DoH resolver with privacy protection"),
        DnsOption(1, "Google Public DNS", "Google", "8.8.8.8 / 8.8.4.4", "Reliable and high-capacity global DNS infrastructure"),
        DnsOption(4, "AdGuard DNS", "AdGuard", "94.140.14.140", "Blocks annoying trackers and malicious ads automatically"),
        DnsOption(6, "Quad9 DNS", "Quad9", "9.9.9.9", "Enhanced security with automated threat blocking"),
        DnsOption(0, "System Default", "ISP / Network", "Auto", "Uses standard system network resolver")
    )

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            selectedDnsId = settingsManager.getInt(dnsKey, 2)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setDns(dnsId: Int) {
        settingsManager.edit().putInt(dnsKey, dnsId).apply()
        _uiState.value = _uiState.value.copy(selectedDnsId = dnsId)
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoPlayNext = enabled)
    }

    fun setDownloadWifiOnly(wifiOnly: Boolean) {
        _uiState.value = _uiState.value.copy(downloadWifiOnly = wifiOnly)
    }

    fun clearCache() {
        context.cacheDir.deleteRecursively()
        _uiState.value = _uiState.value.copy(cacheSizeMb = 0L)
    }

    fun clearWatchHistory() {
        DataStoreHelper.deleteAllResumeStateIds()
    }
}
