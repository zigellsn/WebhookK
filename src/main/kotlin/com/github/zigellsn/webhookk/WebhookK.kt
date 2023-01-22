/*
 * Copyright 2019-2023 Simon Zigelli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.zigellsn.webhookk

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * WebhookResponse represents the response of a webhook trigger
 *
 * @param topic Name of the webhook
 * @param response Content of the response
 */
public data class WebhookResponse(val topic: String, val response: HttpResponse)

/**
 * 'WebhookK' is the central entry point for webhook processing
 *
 * @param client HttpClient
 * @param dataAccess DataAccess object
 */
public class WebhookK(public val client: HttpClient, private val dataAccess: DataAccess = MemoryDataAccess()) {

    public val topics: MutableMap<String, MutableList<Url>> = dataAccess.webhooks

    private val responses: MutableSharedFlow<WebhookResponse> = MutableSharedFlow()


    /**
     * WebhookPost represents a post request
     *
     * @param client HttpClient
     */
    public open class WebhookPost(public val client: HttpClient) {
        /**
         * Triggers the webhooks
         *
         * @param callBody Request body content
         * @param callHeader Request header content
         */
        public suspend fun post(
            url: Url,
            callBody: Any,
            callHeader: List<Pair<String, List<String>>> = emptyList(),
        ): HttpResponse {
            return client.post {
                url(url)
                for (h in callHeader) {
                    headers.appendAll(h.first, h.second)
                }
                setBody(callBody)
            }
        }
    }

    /**
     * Triggers the webhooks
     *
     * @param topic Name of the webhook
     * @param dispatcher Coroutine dispatcher
     * @param post Post-Method
     */
    public suspend fun trigger(
        topic: String,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        post: suspend WebhookPost.(url: Url) -> HttpResponse
    ): Job = webhookScope.launch(dispatcher) {
        topics[topic]?.forEach {
            val postInst = WebhookPost(this@WebhookK.client)
            val httpResponse = postInst.post(it)
            responses.emit(WebhookResponse(topic, httpResponse))
        }
    }

    /**
     * Receive the responses as Flow
     *
     * @return Flow of HttpResponses
     */
    @Synchronized
    public fun responses(): Flow<WebhookResponse> = responses

    /**
     * Closes the Webhook
     */
    public fun close() {
        webhookJob.cancel()
    }
}

private val webhookJob = SupervisorJob()
private val webhookScope = CoroutineScope(webhookJob)