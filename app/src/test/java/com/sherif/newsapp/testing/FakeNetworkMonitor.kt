package com.sherif.newsapp.testing

import com.sherif.newsapp.domain.connectivity.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    override val isOnline = MutableStateFlow(online)
}
