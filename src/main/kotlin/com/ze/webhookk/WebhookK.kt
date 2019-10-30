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
import kotlinx.coroutines.flow.flow
import java.net.URI
import java.util.*


/**
 * 'Webhook' is defined by a topic name and a list of URIs
 * @param topic Name of the webhook
 * @param uris List of URIs
 */
data class Webhook(val topic: String, val uris: MutableList<URI> = mutableListOf())

/**
 * 'WebhookK' is the central entry point for webhook processing
 *
 * @param client HttpClient
 * @param dataAccess DataAccess object
 */
class WebhookK(private val client: HttpClient, private val dataAccess: DataAccess = MemoryDataAccess()) {

    /**
     * Adds a receiving URI to a webhook. If the webhook does not exist it is created.
     *
     * @param topic Name of the Webhook
     * @param uri URI to be removed
     */
    suspend fun add(topic: String, uri: URI) {
        val webhook = dataAccess.get(topic)
        if (webhook == null) {
            val newWebhook = Webhook(topic)
            newWebhook.uris.add(uri)
            dataAccess.add(newWebhook)
        } else {
            if (!webhook.uris.contains(uri))
                webhook.uris.add(uri)
        }
    }

    /**
     * Removes a receiving URI from a webhook. If the webhook is empty it is removed, too.
     *
     * @param topic Name of the webhook
     * @param uri URI to be removed
     */
    suspend fun remove(topic: String, uri: URI) {
        //TODO: Exception
        val webhook = dataAccess.get(topic) ?: throw Exception()
        webhook.uris.remove(uri)
        if (webhook.uris.count() == 0) {
            dataAccess.remove(webhook)
        }
    }

    /**
     * Gets all URIs from a webhook.
     *
     * @param topic Name of the webhook
     * @throws Exception
     */
    suspend fun getUris(topic: String): List<URI> {
        //TODO: Exception
        val webhook = dataAccess.get(topic) ?: throw Exception()
        return Collections.unmodifiableList(webhook.uris)
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
    ) = flow {
        //TODO: Exception
        val webhook = dataAccess.get(topic) ?: throw Exception()
        webhook.uris.forEach {
            emit(client.post<HttpResponse> {
                url(it.toString())
                for (h in callHeader) {
                    headers.appendAll(h.first, h.second)
                }
                body = callBody
            })
        }
    }
}