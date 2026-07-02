package com.abbas57.stockframe.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    /**
     * ConnectivityObserver.observe() emits true = "online". This screen-facing
     * property is named from OfflineBanner's perspective instead — inverted
     * here, once, so every call site reads naturally (`if (isOffline)`)
     * rather than every caller having to remember to negate a "isOnline" flag.
     */
    val isOffline: StateFlow<Boolean> = connectivityObserver.observe()
        .map { isOnline -> !isOnline }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // assume online until the first real reading arrives — avoids a flash of the banner on cold start before connectivity is known
        )
}