/*
 * Copyright 2019-2020 Simon Zigelli
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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.Url
import java.nio.file.Files
import java.nio.file.Path


/**
 * Adds a collection of URIs to a webhook
 *
 * @param topic Name of the webhook
 * @param urls Collection of URIs
 */
fun MutableMap<String, MutableList<Url>>.addAll(topic: String, urls: Collection<Url>) {
    if (!this.containsKey(topic)) {
        this[topic] = urls.toMutableList()
    } else {
        this[topic]?.addAll(urls)
    }
}

/**
 * Adds a URI to a webhook
 *
 * @param topic Name of the webhook
 * @param url URI
 */
fun MutableMap<String, MutableList<Url>>.add(topic: String, url: Url) {
    if (!this.containsKey(topic)) {
        this[topic] = mutableListOf(url)
    } else {
        if (!(this[topic]?.contains(url) ?: return))
            this[topic]?.add(url)
    }
}

/**
 * Removes a URI from a webhook
 *
 * @param topic Name of the webhook
 */
fun MutableMap<String, MutableList<Url>>.removeUrl(topic: String, url: Url) {
    if (this.containsKey(topic)) {
        this[topic]?.remove(url)
    } else {
        throw AssertionError()
    }
}

/**
 * Removes a collection of URIs from a webhook
 *
 * @param topic Name of the webhook
 */
fun MutableMap<String, MutableList<Url>>.removeAllUrl(topic: String, urls: Collection<Url>) {
    if (this.containsKey(topic)) {
        this[topic]?.removeAll(urls)
    } else {
        throw AssertionError()
    }
}

/**
 * Removes a webhook
 *
 * @param topic Name of the webhook
 */
fun MutableMap<String, MutableList<Url>>.removeTopic(topic: String) {
    this.remove(topic)
}

/**
 * Removes a collection of webhooks
 *
 * @param topics Collection of webhooks
 */
fun MutableMap<String, MutableList<Url>>.removeAllTopic(topics: Collection<String>) {
    for (topic in topics) {
        this.remove(topic)
    }
}

/**
 * Interface for DataAccess
 */
interface DataAccess {
    /**
     * Collection of Webhooks
     */
    val webhooks: MutableMap<String, MutableList<Url>>
    /**
     * Persist Webhooks if possible
     */
    fun persist()
}

/**
 * 'MemoryDataAccess' stores all webhook data in memory
 */
class MemoryDataAccess : DataAccess {
    override val webhooks: MutableMap<String, MutableList<Url>> = mutableMapOf()
    override fun persist() {
    }
}

/**
 * 'FileDataAccess' stores all webhook data in a file
 */
class FileDataAccess(private val file: Path) : DataAccess {

    inner class UrlSerializer : JsonSerializer<Url>() {
        override fun serialize(value: Url?, gen: JsonGenerator?, serializers: SerializerProvider?) {
            gen?.writeString(value.toString())
        }
    }

    inner class UrlDeserializer : JsonDeserializer<Url>() {
        override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Url {
            val url = ctxt?.readValue(p, String::class.java) ?: ""
            return Url(url)
        }
    }

    private val mapper = jacksonObjectMapper()

    init {
        val urlModule = SimpleModule("UrlModule")
        urlModule.addSerializer(Url::class.java, UrlSerializer())
        urlModule.addDeserializer(Url::class.java, UrlDeserializer())
        mapper.registerModule(urlModule)
    }

    override val webhooks: MutableMap<String, MutableList<Url>> =
        if (Files.exists(file)) {
            val bytes = Files.newBufferedReader(file)
            mapper.readValue(bytes)
        } else {
            Files.createFile(file)
            mutableMapOf()
        }

    override fun persist() {
        val json = mapper.writeValueAsString(webhooks)
        Files.write(file, json.toByteArray())
    }
}