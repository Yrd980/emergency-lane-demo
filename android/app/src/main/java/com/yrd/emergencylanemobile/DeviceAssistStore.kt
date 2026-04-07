package com.yrd.emergencylanemobile

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yrd.emergencylanemobile.model.MobileLocalDraft
import com.yrd.emergencylanemobile.model.MobileSyncSnapshot

private const val DEVICE_ASSIST_PREFS = "emergency_lane_device_assist"
private const val SNAPSHOT_KEY = "sync_snapshot"
private const val DRAFTS_KEY = "case_local_drafts"

class DeviceAssistStore(context: Context) {
    private val preferences = context.getSharedPreferences(DEVICE_ASSIST_PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val draftsType = object : TypeToken<Map<String, MobileLocalDraft>>() {}.type

    fun loadSnapshot(): MobileSyncSnapshot? {
        val raw = preferences.getString(SNAPSHOT_KEY, null) ?: return null
        return runCatching { gson.fromJson(raw, MobileSyncSnapshot::class.java) }.getOrNull()
    }

    fun saveSnapshot(snapshot: MobileSyncSnapshot) {
        preferences.edit().putString(SNAPSHOT_KEY, gson.toJson(snapshot)).apply()
    }

    fun loadDraft(caseId: String): MobileLocalDraft? = loadDraftMap()[caseId]

    fun saveDraft(draft: MobileLocalDraft) {
        val next = loadDraftMap().toMutableMap()
        next[draft.caseId] = draft
        preferences.edit().putString(DRAFTS_KEY, gson.toJson(next, draftsType)).apply()
    }

    fun clearDraft(caseId: String) {
        val next = loadDraftMap().toMutableMap()
        next.remove(caseId)
        preferences.edit().putString(DRAFTS_KEY, gson.toJson(next, draftsType)).apply()
    }

    private fun loadDraftMap(): Map<String, MobileLocalDraft> {
        val raw = preferences.getString(DRAFTS_KEY, null) ?: return emptyMap()
        return runCatching { gson.fromJson<Map<String, MobileLocalDraft>>(raw, draftsType) }.getOrDefault(emptyMap())
    }
}
