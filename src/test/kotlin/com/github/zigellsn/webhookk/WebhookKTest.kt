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
import io.ktor.client.engine.mock.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WebhookKTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @FlowPreview
    @ExperimentalCoroutinesApi
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
            webhook.post(
                it,
                TextContent("success", ContentType.Text.Plain),
                listOf("c" to listOf("d", "e")),
                client = client
            ).execute()
        }
        webhook.responses().take(1).collect { (topic, response) ->
            val s = response.readText()
            assertEquals("TextContent[text/plain] \"success\"", s)
            assertEquals("topic", topic)
        }
        webhook.close()
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

    @ExperimentalCoroutinesApi
    @Test
    fun testTopicOperations() = mainCoroutineRule.runBlockingTest {
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
        assertEquals(1, webhook.topics["topic"]?.count())
        assertEquals(1, webhook.topics.count())
        webhook.topics.addAll(
            "topic2",
            listOf(Url("http://www.anonym.de/"), Url("http://www.anonym2.de/"), Url("http://www.anonym3.de/"))
        )
        assertEquals(3, webhook.topics["topic2"]?.count())
        assertEquals(2, webhook.topics.count())
        webhook.topics.addAll("topic3", listOf(Url("http://www.anonym.de/"), Url("http://www.anonym2.de/")))
        assertEquals(2, webhook.topics["topic3"]?.count())
        assertEquals(3, webhook.topics.count())
        webhook.topics.removeUrl("topic2", Url("http://www.anonym.de/"))
        assertEquals(2, webhook.topics["topic2"]?.count())
        assertEquals(3, webhook.topics.count())
        webhook.topics.removeAllUrl("topic2", listOf(Url("http://www.anonym.de/"), Url("http://www.anonym3.de/")))
        assertEquals(1, webhook.topics["topic2"]?.count())
        assertEquals(3, webhook.topics.count())
        webhook.topics.removeTopic("topic2")
        assertEquals(2, webhook.topics.count())
        webhook.topics.removeAllTopic(listOf("topic", "topic2"))
        assertEquals(1, webhook.topics.count())
    }
}
