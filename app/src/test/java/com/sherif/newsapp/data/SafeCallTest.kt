package com.sherif.newsapp.data

import com.google.common.truth.Truth.assertThat
import com.sherif.newsapp.domain.model.DataError
import com.sherif.newsapp.domain.model.DataResult
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

// Timeouts, refused connections, 401 and 429 are covered against a real server in NewsApiTest.
class SafeCallTest {

    @Test
    fun `wraps the value when nothing goes wrong`() {
        assertThat(safeCall { 42 }).isEqualTo(DataResult.Success(42))
    }

    @Test
    fun `maps server errors to Server`() {
        assertThat(safeCall { throw httpException(503) }).isEqualTo(DataResult.Failure(DataError.Server))
    }

    @Test
    fun `maps other http errors to Unknown`() {
        // NewsAPI answers 426 when a free key asks for more than 100 results.
        assertThat(safeCall { throw httpException(426) }).isEqualTo(DataResult.Failure(DataError.Unknown))
    }

    @Test
    fun `maps a malformed response to Unknown`() {
        assertThat(safeCall { throw SerializationException("unexpected json") })
            .isEqualTo(DataResult.Failure(DataError.Unknown))
    }

    @Test
    fun `lets cancellation through instead of turning it into an error`() {
        assertThrows(CancellationException::class.java) {
            safeCall { throw CancellationException("screen closed") }
        }
    }

    private fun httpException(code: Int) =
        HttpException(Response.error<Unit>(code, "{}".toResponseBody("application/json".toMediaType())))
}
