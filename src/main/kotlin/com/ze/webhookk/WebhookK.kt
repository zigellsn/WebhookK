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

package com.ze.webhookk

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.response.HttpResponse
import io.ktor.http.Url
import kotlinx.coroutines.flow.flow


/**
 * 'WebhookK' is the central entry point for webhook processing
 *
 * @param client HttpClient
 * @param dataAccess DataAccess object
 */
class WebhookK(private val client: HttpClient, private val dataAccess: DataAccess = MemoryDataAccess()) {

    /**
     * Adds a receiving Url to a webhook. If the webhook does not exist it is created.
     *
     * @param topic Name of the Webhook
     * @param uri Url to be added
     */
    suspend fun add(topic: String, uri: Url) {
        dataAccess.add(topic, uri)
    }

    /**
     * Adds a receiving Url to a webhook. If the webhook does not exist it is created.
     *
     * @param topic Name of the Webhook
     * @param uris Collection of Urls to be added
     */
    suspend fun addAll(topic: String, uris: Collection<Url>) {
        dataAccess.addAll(topic, uris)
    }

    /**
     * Removes a webhook
     *
     * @param topic Name of the webhook to be removed
     */
    suspend fun remove(topic: String) {
        dataAccess.removeTopic(topic)
    }

    /**
     * Removes a receiving Url from a webhook. If the webhook is empty it is removed, too.
     *
     * @param topic Name of the webhook
     * @param uri Url to be removed
     */
    suspend fun remove(topic: String, uri: Url) {
        dataAccess.removeUrl(topic, uri)
    }

    /**
     * Removes a collection of receiving Url from a webhook. If the webhook is empty it is removed, too.
     *
     * @param topic Name of the webhook
     * @param uris Collection of Urls to be removed
     */
    suspend fun removeAll(topic: String, uris: Collection<Url>) {
        dataAccess.removeAllUrl(topic, uris)
    }

    /**
     * Removes a collection of webhook.
     *
     * @param topic Name of the webhook
     */
    suspend fun removeAll(topic: Collection<String>) {
        dataAccess.removeAllTopic(topic)
    }

    /**
     * Gets all Urls from a webhook.
     *
     * @param topic Name of the webhook
     * @throws Exception
     */
    suspend fun getUrls(topic: String): List<Url> {
        return dataAccess.get(topic)
    }

    /**
     * Triggers the webhooks
     *
     * @param topic Name of the webhook
     * @param callBody Request body content
     * @param callHeader Request header content
     * @param client HttpClient
     */
    suspend fun trigger(
        topic: String,
        callBody: Any,
        callHeader: List<Pair<String, List<String>>>,
        client: HttpClient = this.client
    ) = flow<HttpResponse> {
        //TODO: Exception
        val webhook = dataAccess.get(topic)
        webhook.forEach {
            emit(client.post {
                url(it)
                for (h in callHeader) {
                    headers.appendAll(h.first, h.second)
                }
                body = callBody
            })
        }
    }
}