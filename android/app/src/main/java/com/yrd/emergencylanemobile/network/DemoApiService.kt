package com.yrd.emergencylanemobile.network

import com.yrd.emergencylanemobile.model.MobileCaseDetail
import com.yrd.emergencylanemobile.model.MobileCaseSummary
import com.yrd.emergencylanemobile.model.MobileCaseUpdateRequest
import com.yrd.emergencylanemobile.model.MobileOverview
import com.yrd.emergencylanemobile.model.MobileReportResponse
import com.yrd.emergencylanemobile.model.MobileRunSummary
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DemoApiService {
    @GET("overview")
    suspend fun getOverview(): MobileOverview

    @GET("runs")
    suspend fun getRuns(): List<MobileRunSummary>

    @GET("cases")
    suspend fun getCases(@Query("status") status: String? = null): List<MobileCaseSummary>

    @GET("cases/{caseId}")
    suspend fun getCase(@Path("caseId") caseId: String): MobileCaseDetail

    @PATCH("cases/{caseId}")
    suspend fun updateCase(@Path("caseId") caseId: String, @Body payload: MobileCaseUpdateRequest): MobileCaseDetail

    @POST("cases/{caseId}/report")
    suspend fun reportCase(@Path("caseId") caseId: String): MobileReportResponse
}
