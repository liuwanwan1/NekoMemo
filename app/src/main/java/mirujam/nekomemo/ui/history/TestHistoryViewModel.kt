package mirujam.nekomemo.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import mirujam.nekomemo.data.local.dao.BankStatistics
import mirujam.nekomemo.data.local.dao.TestSessionWithBankTitle
import mirujam.nekomemo.data.repository.TestHistoryRepository
import javax.inject.Inject

@HiltViewModel
class TestHistoryViewModel @Inject constructor(
    testHistoryRepository: TestHistoryRepository
) : ViewModel() {

    val sessions: StateFlow<List<TestSessionWithBankTitle>> =
        testHistoryRepository.getAllSessionsWithBankTitle()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statistics: StateFlow<BankStatistics?> =
        testHistoryRepository.getOverallStatistics()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
