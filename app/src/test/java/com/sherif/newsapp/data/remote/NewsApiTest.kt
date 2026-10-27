package com.sherif.newsapp.data.remote

import com.google.common.truth.Truth.assertThat
import com.sherif.newsapp.data.mapper.toEntities
import com.sherif.newsapp.data.safeCall
import com.sherif.newsapp.di.NetworkModule
import com.sherif.newsapp.domain.model.DataError
import com.sherif.newsapp.domain.model.DataResult
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit

class NewsApiTest {

    private val server = MockWebServer()
    private lateinit var api: NewsApi

    @Before
    fun setUp() {
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/v2/"))
            .client(OkHttpClient.Builder().addInterceptor(ApiKeyInterceptor("test-key")).build())
            .addConverterFactory(NetworkModule.provideJson().asConverterFactory("application/json".toMediaType()))
            .build()
            .create()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `sends the api key header and the expected query`() = runTest {
        server.enqueue(MockResponse(body = fixture("top_headlines.json")))

        api.getTopHeadlines(country = "us", page = 2)

        val request = server.takeRequest()
        assertThat(request.headers["x-api-key"]).isEqualTo("test-key")
        assertThat(request.url.encodedPath).isEqualTo("/v2/top-headlines")
        assertThat(request.url.queryParameter("country")).isEqualTo("us")
        assertThat(request.url.queryParameter("page")).isEqualTo("2")
    }

    @Test
    fun `parses a real looking response, nulls and removed articles included`() = runTest {
        server.enqueue(MockResponse(body = fixture("top_headlines.json")))

        val response = api.getTopHeadlines(country = "us", page = 1)
        val entities = response.articles.toEntities(savedAt = 0)

        assertThat(response.articles).hasSize(3)
        assertThat(entities.map { it.title })
            .containsExactly("City council approves new bike lanes", "Local team wins the championship")
            .inOrder()
        assertThat(entities[1].imageUrl).isNull()
        assertThat(entities[1].author).isNull()
    }

    @Test
    fun `copes with nulls where lists and numbers are expected`() = runTest {
        server.enqueue(MockResponse(body = """{"status":"ok","totalResults":null,"articles":null}"""))

        val response = api.getTopHeadlines(country = "us", page = 1)

        assertThat(response.articles).isEmpty()
        assertThat(response.totalResults).isEqualTo(0)
    }

    @Test
    fun `maps a rejected api key to Unauthorized`() = runTest {
        server.enqueue(
            MockResponse(
                code = 401,
                body = """{"status":"error","code":"apiKeyInvalid","message":"Your API key is invalid or incorrect."}""",
            ),
        )

        val result = safeCall { api.getTopHeadlines(country = "us", page = 1) }

        assertThat(result).isEqualTo(DataResult.Failure(DataError.Unauthorized))
    }

    @Test
    fun `maps the developer plan rate limit to RateLimited`() = runTest {
        server.enqueue(
            MockResponse(
                code = 429,
                body = """{"status":"error","code":"rateLimited","message":"You have made too many requests recently."}""",
            ),
        )

        val result = safeCall { api.searchTopHeadlines(country = "us", query = "apple") }

        assertThat(result).isEqualTo(DataResult.Failure(DataError.RateLimited))
    }

    @Test
    fun `maps a server that takes too long to Timeout`() = runTest {
        val impatientApi = Retrofit.Builder()
            .baseUrl(server.url("/v2/"))
            .client(OkHttpClient.Builder().readTimeout(100, TimeUnit.MILLISECONDS).build())
            .addConverterFactory(NetworkModule.provideJson().asConverterFactory("application/json".toMediaType()))
            .build()
            .create<NewsApi>()
        server.enqueue(
            MockResponse.Builder()
                .body(fixture("top_headlines.json"))
                .headersDelay(1, TimeUnit.SECONDS)
                .build(),
        )

        val result = safeCall { impatientApi.getTopHeadlines(country = "us", page = 1) }

        assertThat(result).isEqualTo(DataResult.Failure(DataError.Timeout))
    }

    @Test
    fun `maps a refused connection to NoConnection`() = runTest {
        server.close()

        val result = safeCall { api.getTopHeadlines(country = "us", page = 1) }

        assertThat(result).isEqualTo(DataResult.Failure(DataError.NoConnection))
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource(name)) { "Missing test resource $name" }.readText()
}
