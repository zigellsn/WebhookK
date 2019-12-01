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
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.response.readText
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.Url
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WebhookKTest {
    @Test
    fun testWebhook() = runBlocking {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    when (it.url.toString()) {
                        "http://www.anonym.de/" -> {
                            assertEquals("d", it.headers["c"])
                            respond(it.body.toString())
                        }
                        else -> error("error")
                    }
                }
            }
        }

        val webhook = WebhookK(client)
        webhook.topics.add("topic", Url("http://www.anonym.de/"))
        webhook.trigger(
            "topic"
        ) {
            runBlocking {
                webhook.post(
                    it,
                    TextContent("success", ContentType.Text.Plain),
                    listOf("c" to listOf("d", "e")),
                    client = client
                )
            }
        }
            .collect {
                val s = it.readText()
                assertEquals("TextContent[text/plain] \"success\"", s)
            }
        client.close()
        assertEquals(1, webhook.topics["topic"]?.count())
        webhook.topics.removeUrl("topic", Url("http://www.anonym.de/"))
        try {
            webhook.topics["topic"]?.count()
        } catch (e: Exception) {
            assert(true)
        }
        assert(true)
    }
}