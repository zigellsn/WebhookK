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

class WebhookK(private val client: HttpClient, private val dataAccess: DataAccess = MemoryDataAccess()) {

    fun add(topic: String, uri: URI) {
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

    fun remove(topic: String, uri: URI) {
        val webhook = dataAccess.get(topic)
        webhook?.uris?.remove(uri)
        if (webhook?.uris?.count() == 0) {
            dataAccess.remove(webhook)
        }
    }

    suspend fun trigger(
        topic: String,
        callBody: Any,
        callHeader: List<Pair<String, List<String>>>,
        client: HttpClient = this.client
    ) = flow {
        val webhook = dataAccess.get(topic)
        webhook?.uris?.forEach {
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