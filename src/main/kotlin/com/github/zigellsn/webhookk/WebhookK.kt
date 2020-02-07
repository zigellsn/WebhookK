/*
 * Copyright 2019 Simon Zigelli
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

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.Url
import kotlinx.coroutines.flow.flow


/**
 * 'WebhookK' is the central entry point for webhook processing
 *
 * @param client HttpClient
 * @param dataAccess DataAccess object
 */
class WebhookK(private val client: HttpClient, private val dataAccess: DataAccess = MemoryDataAccess()) {

    val topics = dataAccess.webhooks

    /**
     * Triggers the webhooks
     *
     * @param topic Name of the webhook
     * @param post Post-Method
     */
    suspend fun trigger(
        topic: String,
        post: suspend (url: Url) -> HttpResponse
    ) = flow {
        topics[topic]?.forEach {
            emit(post(it))
        }
    }

    /**
     * Triggers the webhooks
     *
     * @param callBody Request body content
     * @param callHeader Request header content
     * @param client HttpClient
     */
    suspend fun post(
        url: Url,
        callBody: Any,
        callHeader: List<Pair<String, List<String>>>,
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
}