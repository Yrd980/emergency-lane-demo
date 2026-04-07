package com.yrd.emergencylanemobile.model

import com.google.gson.annotations.SerializedName

data class MobileOverview(
    val summary: MobileSummary,
    @SerializedName("latest_run") val latestRun: MobileRunSummary?,
    val runs: List<MobileRunSummary> = emptyList(),
    val source: MobileSource,
    val system: MobileSystem,
)

data class MobileSummary(
    @SerializedName("total_events") val totalEvents: Int,
    @SerializedName("total_cases") val totalCases: Int,
    @SerializedName("reported_cases") val reportedCases: Int,
    @SerializedName("pending_review_cases") val pendingReviewCases: Int,
    @SerializedName("avg_confidence") val avgConfidence: Double,
)

data class MobileSource(
    val name: String,
    val title: String,
    @SerializedName("preview_url") val previewUrl: String?,
    @SerializedName("duration_seconds") val durationSeconds: Double,
)

data class MobileSystem(
    @SerializedName("web_role") val webRole: String,
    @SerializedName("android_role") val androidRole: String,
    @SerializedName("reference_basis") val referenceBasis: String,
)

data class MobileRunSummary(
    val id: String,
    @SerializedName("source_name") val sourceName: String,
    val status: String,
    val message: String,
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("finished_at") val finishedAt: String?,
    @SerializedName("progress_percent") val progressPercent: Int,
    @SerializedName("event_count") val eventCount: Int,
    @SerializedName("case_count") val caseCount: Int,
)

data class MobileEventSummary(
    val id: String,
    @SerializedName("plate_number") val plateNumber: String,
    val status: String,
    @SerializedName("first_seen") val firstSeen: String,
)

data class MobileEvidence(
    val label: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("captured_at") val capturedAt: String,
)

data class MobileTimelineItem(
    @SerializedName("timestamp_seconds") val timestampSeconds: Double,
    val description: String,
)

data class MobileRawAnalysis(
    val timeline: List<MobileTimelineItem> = emptyList(),
)

data class MobileCaseSummary(
    val id: String,
    @SerializedName("run_id") val runId: String,
    @SerializedName("source_name") val sourceName: String,
    @SerializedName("plate_number") val plateNumber: String,
    @SerializedName("corrected_plate_number") val correctedPlateNumber: String?,
    @SerializedName("review_status") val reviewStatus: String,
    @SerializedName("operator_note") val operatorNote: String,
    val status: String,
    val location: String,
    val confidence: Double,
    val summary: String,
    @SerializedName("event_count") val eventCount: Int,
    @SerializedName("evidence_count") val evidenceCount: Int,
    @SerializedName("clip_url") val clipUrl: String?,
)

data class MobileCaseDetail(
    val id: String,
    @SerializedName("run_id") val runId: String,
    @SerializedName("source_name") val sourceName: String,
    @SerializedName("plate_number") val plateNumber: String,
    @SerializedName("corrected_plate_number") val correctedPlateNumber: String?,
    @SerializedName("review_status") val reviewStatus: String,
    @SerializedName("operator_note") val operatorNote: String,
    val status: String,
    val location: String,
    val confidence: Double,
    val summary: String,
    @SerializedName("clip_url") val clipUrl: String?,
    @SerializedName("report_content") val reportContent: String?,
    @SerializedName("report_file_url") val reportFileUrl: String?,
    @SerializedName("reported_at") val reportedAt: String?,
    val evidence: List<MobileEvidence>,
    val events: List<MobileEventSummary>,
    @SerializedName("raw_analysis") val rawAnalysis: MobileRawAnalysis,
)

data class MobileCaseUpdateRequest(
    @SerializedName("corrected_plate_number") val correctedPlateNumber: String? = null,
    @SerializedName("operator_note") val operatorNote: String? = null,
    @SerializedName("review_status") val reviewStatus: String? = null,
)

data class MobileReportResponse(
    val message: String,
)

data class MobileLocalDraft(
    val caseId: String,
    val localPlateCandidate: String = "",
    val sceneNote: String = "",
    val reviewStatus: String = "待复核",
    val inputMode: String = "manual-device-cache",
    val savedAt: String? = null,
)

data class MobileSyncSnapshot(
    val cachedAt: String,
    val overview: MobileOverview? = null,
    val runs: List<MobileRunSummary> = emptyList(),
    val cases: List<MobileCaseSummary> = emptyList(),
    val selectedCaseId: String? = null,
    val selectedCase: MobileCaseDetail? = null,
    val selectedStatusFilter: String? = null,
)

data class MobileConnectionState(
    val activeBaseUrl: String = "",
    val activeSource: String = "",
    val buildDefaultBaseUrl: String = "",
    val buildDefaultSource: String = "",
    val emulatorFallbackBaseUrl: String = "",
    val lanHintBaseUrl: String = "",
    val hasRuntimeOverride: Boolean = false,
    val cacheAvailable: Boolean = false,
    val cacheStatus: String? = null,
    val cacheRunCount: Int = 0,
    val cacheCaseCount: Int = 0,
    val usingCachedData: Boolean = false,
    val lastOverviewSummary: String? = null,
    val lastSuccessAt: String? = null,
    val lastError: String? = null,
)

data class MobileUiState(
    val overview: MobileOverview? = null,
    val runs: List<MobileRunSummary> = emptyList(),
    val cases: List<MobileCaseSummary> = emptyList(),
    val selectedCaseId: String? = null,
    val selectedCase: MobileCaseDetail? = null,
    val selectedStatusFilter: String? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val busy: Boolean = false,
    val toast: String? = null,
    val error: String? = null,
    val connection: MobileConnectionState = MobileConnectionState(),
    val localDraft: MobileLocalDraft? = null,
)
