package com.sherif.newsapp.domain.model

sealed interface DataError {
    data object NoConnection : DataError
    data object Timeout : DataError
    data object Unauthorized : DataError
    data object RateLimited : DataError
    data object Server : DataError
    data object Unknown : DataError
}
