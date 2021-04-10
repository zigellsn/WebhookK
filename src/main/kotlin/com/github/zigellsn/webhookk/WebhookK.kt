/*
 * Copyright 2019-2021 Simon Zigelli
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
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

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
public class WebhookK(private val client: HttpClient, private val dataAccess: DataAccess = MemoryDataAccess()) {

    public val topics: MutableMap<String, MutableList<Url>> = dataAccess.webhooks

    @ExperimentalCoroutinesApi
    private val responses: BroadcastChannel<WebhookResponse> = BroadcastChannel(Channel.Factory.BUFFERED)

    /**
     * Triggers the webhooks
     *
     * @param topic Name of the webhook
     * @param dispatcher Coroutine dispatcher
     * @param post Post-Method
     */
    @Synchronized
    @ExperimentalCoroutinesApi
    public suspend fun trigger(
        topic: String,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        post: suspend (url: Url) -> HttpResponse
    ): Job = webhookScope.launch(dispatcher) {
        topics[topic]?.forEach {
            val a = post(it)
            responses.offer(WebhookResponse(topic, a))
        }
    }

    /**
     * Receive the responses as Flow
     *
     * @return Flow of HttpResponses
     */
    @FlowPreview
    @ExperimentalCoroutinesApi
    @Synchronized
    public fun responses(): Flow<WebhookResponse> {
        return responses.asFlow()
    }

    /**
     * Triggers the webhooks
     *
     * @param callBody Request body content
     * @param callHeader Request header content
     * @param client HttpClient
     */
    public suspend fun post(
        url: Url,
        callBody: Any,
        callHeader: List<Pair<String, List<String>>> = emptyList(),
        client: HttpClient = this.client
    ): HttpStatement {
        return client.post {
            url(url)
            for (h in callHeader) {
                headers.appendAll(h.first, h.second)
            }
            body = callBody
        }
    }

    /**
     * Closes the Webhook
     */
    public fun close() {
        webhookJob.cancel()
    }
}

private val webhookJob = SupervisorJob()
private val webhookScope = CoroutineScope(webhookJob)