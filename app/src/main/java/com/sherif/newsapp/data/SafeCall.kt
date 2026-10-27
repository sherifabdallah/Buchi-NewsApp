package com.sherif.newsapp.data

import com.sherif.newsapp.domain.model.DataError
import com.sherif.newsapp.domain.model.DataResult
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

// CancellationException has to be rethrown, otherwise a cancelled refresh would show up as an error.
inline fun <T> safeCall(block: () -> T): DataResult<T> =
    try {
        DataResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DataResult.Failure(e.toDataError())
    }

fun Throwable.toDataError(): DataError = when (this) {
    is SocketTimeoutException -> DataError.Timeout
    is IOException -> DataError.NoConnection
    is HttpException -> when (code()) {
        401 -> DataError.Unauthorized
        429 -> DataError.RateLimited
        in 500..599 -> DataError.Server
        else -> DataError.Unknown
    }
    else -> DataError.Unknown
}
