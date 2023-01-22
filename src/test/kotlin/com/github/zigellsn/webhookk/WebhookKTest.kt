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
import io.ktor.client.engine.mock.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WebhookKTest {

    @ExperimentalCoroutinesApi
    @Test
    fun testWebhook() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    when (it.url.toString()) {
                        "https://www.anonym.de/" -> {
                            assertEquals("d", it.headers["c"])
                            respond(it.body.toString())
                        }
                        else -> error("error")
                    }
                }
            }
        }

        val webhook = WebhookK(client)
        webhook.topics.add("topic", Url("https://www.anonym.de/"))
        webhook.trigger(
            "topic"
        ) {
            post(
                it,
                TextContent("success", ContentType.Text.Plain),
                listOf("c" to listOf("d", "e")),
            )
        }
        webhook.responses().take(1).collect { (topic, response) ->
            val s = response.bodyAsText()
            assertEquals("TextContent[text/plain] \"success\"", s)
            assertEquals("topic", topic)
        }
        webhook.close()
        client.close()
        assertEquals(1, webhook.topics["topic"]?.count())
        webhook.topics.removeUrl("topic", Url("https://www.anonym.de/"))
        try {
            webhook.topics["topic"]?.count()
        } catch (e: Exception) {
            assert(true)
        }
        assert(true)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun testTopicOperations() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    when (it.url.toString()) {
                        "https://www.anonym.de/" -> {
                            assertEquals("d", it.headers["c"])
                            respond(it.body.toString())
                        }
                        else -> error("error")
                    }
                }
            }
        }

        val webhook = WebhookK(client)
        webhook.topics.add("topic", Url("https://www.anonym.de/"))
        assertEquals(1, webhook.topics["topic"]?.count())
        assertEquals(1, webhook.topics.count())
        webhook.topics.addAll(
            "topic2",
            listOf(Url("https://www.anonym.de/"), Url("https://www.anonym2.de/"), Url("https://www.anonym3.de/"))
        )
        assertEquals(3, webhook.topics["topic2"]?.count())
        assertEquals(2, webhook.topics.count())
        webhook.topics.addAll("topic3", listOf(Url("https://www.anonym.de/"), Url("https://www.anonym2.de/")))
        assertEquals(2, webhook.topics["topic3"]?.count())
        assertEquals(3, webhook.topics.count())
        webhook.topics.removeUrl("topic2", Url("https://www.anonym.de/"))
        assertEquals(2, webhook.topics["topic2"]?.count())
        assertEquals(3, webhook.topics.count())
        webhook.topics.removeAllUrl("topic2", listOf(Url("https://www.anonym.de/"), Url("https://www.anonym3.de/")))
        assertEquals(1, webhook.topics["topic2"]?.count())
        assertEquals(3, webhook.topics.count())
        webhook.topics.removeTopic("topic2")
        assertEquals(2, webhook.topics.count())
        webhook.topics.removeAllTopic(listOf("topic", "topic2"))
        assertEquals(1, webhook.topics.count())
    }
}
