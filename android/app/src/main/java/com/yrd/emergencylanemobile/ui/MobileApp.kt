package com.yrd.emergencylanemobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yrd.emergencylanemobile.ui.components.InfoCard
import com.yrd.emergencylanemobile.ui.screens.CaseDetailSection
import com.yrd.emergencylanemobile.ui.screens.CaseRow
import com.yrd.emergencylanemobile.ui.screens.ConnectionSection
import com.yrd.emergencylanemobile.ui.screens.FilterSection
import com.yrd.emergencylanemobile.ui.screens.HeaderSection
import com.yrd.emergencylanemobile.ui.screens.RunSummarySection
import com.yrd.emergencylanemobile.ui.screens.SystemRoleSection
import com.yrd.emergencylanemobile.viewmodel.MainViewModel
import com.yrd.emergencylanemobile.viewmodel.MainViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileApp() {
    val context = LocalContext.current.applicationContext
    val factory = remember(context) { MainViewModelFactory(context) }
    val viewModel: MainViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("应急车道移动协同端") }) }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF08101F))
                .padding(innerPadding),
        ) {
            val showFullScreenLoading = state.loading && state.overview == null && state.cases.isEmpty()
            if (showFullScreenLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        ConnectionSection(
                            state = state,
                            onRefresh = { viewModel.refresh() },
                            onApplyBaseUrl = { viewModel.applyDebugApiBaseUrl(it) },
                            onClearOverride = { viewModel.clearDebugApiBaseUrlOverride() },
                        )
                    }
                    item { HeaderSection(state = state) }
                    item { RunSummarySection(state.runs, state.overview?.latestRun) }
                    item { FilterSection(state.selectedStatusFilter, onSelect = { viewModel.selectStatusFilter(it) }) }
                    item { SystemRoleSection(state.overview?.system) }
                    item {
                        Text(
                            text = "案件列表",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                    }
                    items(state.cases, key = { it.id }) { item ->
                        CaseRow(
                            item = item,
                            selected = item.id == state.selectedCaseId,
                            onClick = { viewModel.selectCase(item.id) },
                        )
                    }
                    item {
                        CaseDetailSection(
                            detail = state.selectedCase,
                            localDraft = state.localDraft,
                            busy = state.busy,
                            onSave = { caseId, correctedPlate, note, reviewStatus ->
                                viewModel.updateCaseReview(caseId, correctedPlate, note, reviewStatus)
                            },
                            onSaveLocalDraft = { caseId, localPlate, sceneNote, reviewStatus ->
                                viewModel.saveLocalDraft(caseId, localPlate, sceneNote, reviewStatus)
                            },
                            onClearLocalDraft = { caseId -> viewModel.clearLocalDraft(caseId) },
                            onReport = { caseId -> viewModel.reportCase(caseId) },
                        )
                    }
                    state.error?.let { message ->
                        item { InfoCard(title = "错误", body = message, accent = Color(0xFFFCA5A5)) }
                    }
                    state.toast?.let { message ->
                        item { InfoCard(title = "状态", body = message) }
                    }
                }
            }

            if (state.busy || state.refreshing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.TopEnd).padding(20.dp))
            }
        }
    }
}
