package com.developer_rahul.meetmind_ai.feature.representative.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.representative.domain.model.RepresentativeReport
import com.developer_rahul.meetmind_ai.feature.representative.domain.repository.LiveRepresentativeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RepresentativeReportViewModel(
    private val meetingId: Long,
    private val representativeId: Long,
    private val representativeRepository: LiveRepresentativeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepresentativeReportUiState())
    val uiState: StateFlow<RepresentativeReportUiState> = _uiState.asStateFlow()

    init {
        loadReport()
    }

    private fun loadReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = representativeRepository.getReport(meetingId, representativeId)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, report = result.data) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}

data class RepresentativeReportUiState(
    val report: RepresentativeReport? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
