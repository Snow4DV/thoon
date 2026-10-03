package com.mvlog.agent.impl.koog

import ai.koog.http.client.KoogHttpClient
import ai.koog.http.client.KoogHttpClientException
import ai.koog.http.client.ktor.KtorKoogHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.readLine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

/**
 * KtorKoogHttpClient.lines emits from inside Ktor's execute, which switches dispatcher on iOS and
 * violates the flow invariant: https://github.com/JetBrains/koog/issues/2245. Delete when fixed.
 */
internal class ChannelLinesKoogHttpClient(
    private val delegate: KtorKoogHttpClient,
) : KoogHttpClient by delegate {

    override fun <T : Any> lines(
        path: String,
        requestBody: T,
        requestBodyType: KClass<T>,
        parameters: Map<String, String>,
        headers: Map<String, String>,
    ): Flow<String> = channelFlow {
        try {
            delegate.ktorClient.preparePost(path) {
                parameters.forEach { (key, value) -> parameter(key, value) }
                applyRequestHeaders(headers)
                if (requestBodyType == String::class) {
                    setBody(requestBody as String)
                } else {
                    setBody(requestBody, TypeInfo(requestBodyType))
                }
            }.execute { response: HttpResponse ->
                if (!response.status.isSuccess()) {
                    throw KoogHttpClientException(
                        clientName = clientName,
                        statusCode = response.status.value,
                        errorBody = response.bodyAsText(),
                    )
                }
                val channel = response.bodyAsChannel()
                while (true) {
                    val line = channel.readLine() ?: break
                    if (line.isNotBlank()) send(line)
                }
            }
        } catch (e: KoogHttpClientException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw KoogHttpClientException(
                clientName = clientName,
                message = "Exception during streaming: ${e.message}",
                cause = e,
            )
        }
    }

    private fun HttpRequestBuilder.applyRequestHeaders(headers: Map<String, String>) {
        headers.forEach { (name, value) ->
            if (name.equals(HttpHeaders.ContentType, ignoreCase = true)) {
                contentType(ContentType.parse(value))
            } else {
                this.headers.remove(name)
                header(name, value)
            }
        }
    }

    class Factory(baseClient: HttpClient) : KoogHttpClient.Factory {

        private val delegate = KtorKoogHttpClient.Factory(baseClient = baseClient)

        override fun create(
            clientName: String,
            baseUrl: String,
            headers: Map<String, String>,
            queryParameters: Map<String, String>,
            requestTimeoutMillis: Long,
            connectTimeoutMillis: Long,
            socketTimeoutMillis: Long,
            json: Json,
        ): KoogHttpClient = ChannelLinesKoogHttpClient(
            delegate.create(
                clientName = clientName,
                baseUrl = baseUrl,
                headers = headers,
                queryParameters = queryParameters,
                requestTimeoutMillis = requestTimeoutMillis,
                connectTimeoutMillis = connectTimeoutMillis,
                socketTimeoutMillis = socketTimeoutMillis,
                json = json,
            ),
        )
    }
}
