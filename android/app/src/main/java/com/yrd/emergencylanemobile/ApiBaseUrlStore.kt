package com.yrd.emergencylanemobile

import android.content.Context
import android.net.Uri

private const val API_BASE_URL_PREFS = "emergency_lane_debug"
private const val API_BASE_URL_OVERRIDE_KEY = "api_base_url_override"

data class ResolvedApiBaseUrl(
    val activeBaseUrl: String,
    val activeSource: String,
    val buildDefaultBaseUrl: String,
    val buildDefaultSource: String,
    val emulatorFallbackBaseUrl: String,
    val lanHintBaseUrl: String,
    val hasRuntimeOverride: Boolean,
)

class ApiBaseUrlStore(context: Context) {
    private val preferences = context.getSharedPreferences(API_BASE_URL_PREFS, Context.MODE_PRIVATE)

    fun getRuntimeOverride(): String? = preferences.getString(API_BASE_URL_OVERRIDE_KEY, null)

    fun saveRuntimeOverride(value: String) {
        preferences.edit().putString(API_BASE_URL_OVERRIDE_KEY, value).apply()
    }

    fun clearRuntimeOverride() {
        preferences.edit().remove(API_BASE_URL_OVERRIDE_KEY).apply()
    }
}

fun normalizeApiBaseUrl(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isBlank()) return null

    val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "http://$trimmed"
    }

    val uri = Uri.parse(withScheme)
    if (uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) return null

    val normalizedPath = when (val currentPath = uri.path.orEmpty()) {
        "", "/" -> "/api/"
        "/api" -> "/api/"
        else -> if (currentPath.endsWith('/')) currentPath else "$currentPath/"
    }

    return uri.buildUpon()
        .path(normalizedPath)
        .encodedQuery(null)
        .fragment(null)
        .build()
        .toString()
}

fun resolveApiBaseUrl(store: ApiBaseUrlStore): ResolvedApiBaseUrl {
    val buildDefaultBaseUrl = normalizeApiBaseUrl(BuildConfig.API_BASE_URL) ?: BuildConfig.API_BASE_URL
    val runtimeOverride = normalizeApiBaseUrl(store.getRuntimeOverride())
    val emulatorFallbackBaseUrl = normalizeApiBaseUrl(BuildConfig.API_BASE_URL_EMULATOR) ?: BuildConfig.API_BASE_URL_EMULATOR

    return ResolvedApiBaseUrl(
        activeBaseUrl = runtimeOverride ?: buildDefaultBaseUrl,
        activeSource = if (runtimeOverride != null) "debug session override" else BuildConfig.API_BASE_URL_SOURCE,
        buildDefaultBaseUrl = buildDefaultBaseUrl,
        buildDefaultSource = BuildConfig.API_BASE_URL_SOURCE,
        emulatorFallbackBaseUrl = emulatorFallbackBaseUrl,
        lanHintBaseUrl = BuildConfig.API_BASE_URL_LAN_HINT,
        hasRuntimeOverride = runtimeOverride != null,
    )
}
