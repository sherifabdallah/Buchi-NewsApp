package com.sherif.newsapp.presentation.common

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.sherif.newsapp.R
import com.sherif.newsapp.domain.model.DataError

@get:StringRes
val DataError.titleRes: Int
    get() = when (this) {
        DataError.NoConnection -> R.string.error_no_connection_title
        DataError.Timeout -> R.string.error_timeout_title
        DataError.Unauthorized -> R.string.error_unauthorized_title
        DataError.RateLimited -> R.string.error_rate_limited_title
        DataError.Server -> R.string.error_server_title
        DataError.Unknown -> R.string.error_unknown_title
    }

@get:StringRes
val DataError.messageRes: Int
    get() = when (this) {
        DataError.NoConnection -> R.string.error_no_connection_message
        DataError.Timeout -> R.string.error_timeout_message
        DataError.Unauthorized -> R.string.error_unauthorized_message
        DataError.RateLimited -> R.string.error_rate_limited_message
        DataError.Server -> R.string.error_server_message
        DataError.Unknown -> R.string.error_unknown_message
    }

val DataError.icon: ImageVector
    get() = when (this) {
        DataError.NoConnection -> Icons.Rounded.CloudOff
        DataError.Timeout -> Icons.Rounded.HourglassBottom
        DataError.Unauthorized -> Icons.Rounded.Lock
        DataError.RateLimited -> Icons.Rounded.Timer
        DataError.Server, DataError.Unknown -> Icons.Rounded.ErrorOutline
    }
