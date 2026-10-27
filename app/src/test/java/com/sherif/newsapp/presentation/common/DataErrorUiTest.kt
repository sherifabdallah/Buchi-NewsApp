package com.sherif.newsapp.presentation.common

import com.google.common.truth.Truth.assertThat
import com.sherif.newsapp.domain.model.DataError
import org.junit.Test

class DataErrorUiTest {

    private val errors = listOf(
        DataError.NoConnection,
        DataError.Timeout,
        DataError.Unauthorized,
        DataError.RateLimited,
        DataError.Server,
        DataError.Unknown,
    )

    @Test
    fun `every error has its own title and message`() {
        assertThat(errors.map { it.titleRes }.distinct()).hasSize(errors.size)
        assertThat(errors.map { it.messageRes }.distinct()).hasSize(errors.size)
    }

    @Test
    fun `server and unknown errors share an icon, the rest have their own`() {
        assertThat(errors.map { it.icon }.distinct()).hasSize(errors.size - 1)
        assertThat(DataError.Server.icon).isEqualTo(DataError.Unknown.icon)
    }
}
