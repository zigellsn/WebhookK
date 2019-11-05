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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.nio.file.Files
import java.nio.file.Path


/**
 * Interface for DataAccess
 */
interface DataAccess {

    /**
     * @return All registered webhooks
     */
    suspend fun getAll(): Map<String, List<Url>>

    /**
     * @param topic Name of the webhook
     * @return Collection of URIs
     */
    suspend fun get(topic: String): List<Url>

    /**
     * Adds a URI to a webhook
     *
     * @param topic Name of the webhook
     * @param url URI
     */
    suspend fun add(topic: String, url: Url)

    /**
     * Adds a collection of URIs to a webhook
     *
     * @param topic Name of the webhook
     * @param urls Collection of URIs
     */
    suspend fun addAll(topic: String, urls: Collection<Url>)

    /**
     * Removes a URI from a webhook
     *
     * @param topic Name of the webhook
     */
    suspend fun removeUrl(topic: String, url: Url)

    /**
     * Removes a collection of URIs from a webhook
     *
     * @param topic Name of the webhook
     */
    suspend fun removeAllUrl(topic: String, urls: Collection<Url>)

    /**
     * Removes a webhook
     *
     * @param topic Name of the webhook
     */
    suspend fun removeTopic(topic: String)

    /**
     * Removes a collection of webhooks
     *
     * @param topics Collection of webhooks
     */
    suspend fun removeAllTopic(topics: Collection<String>)
}

/**
 * 'MemoryDataAccess' stores all webhook data in memory
 */
open class MemoryDataAccess : DataAccess {

    protected open var webhooks: MutableMap<String, MutableList<Url>> = mutableMapOf()

    override suspend fun getAll(): Map<String, List<Url>> {
        return webhooks
    }

    override suspend fun get(topic: String): List<Url> {
        return webhooks[topic] ?: throw AssertionError("")
    }

    override suspend fun addAll(topic: String, urls: Collection<Url>) {
        if (!webhooks.containsKey(topic)) {
            webhooks[topic] = urls.toMutableList()
        } else {
            webhooks[topic]?.addAll(urls)
        }
    }

    override suspend fun add(topic: String, url: Url) {
        if (!webhooks.containsKey(topic)) {
            webhooks[topic] = mutableListOf(url)
        } else {
            webhooks[topic]?.add(url)
        }
    }

    override suspend fun removeUrl(topic: String, url: Url) {
        if (webhooks.containsKey(topic)) {
            webhooks[topic]?.remove(url)
        } else {
            throw AssertionError()
        }
    }

    override suspend fun removeAllUrl(topic: String, urls: Collection<Url>) {
        if (webhooks.containsKey(topic)) {
            webhooks[topic]?.removeAll(urls)
        } else {
            throw AssertionError()
        }
    }

    override suspend fun removeTopic(topic: String) {
        webhooks.remove(topic)
    }

    override suspend fun removeAllTopic(topics: Collection<String>) {
        for (topic in topics) {
            webhooks.remove(topic)
        }
    }
}

/**
 * 'FileDataAccess' stores all webhook data in a file
 */
class FileDataAccess(private val file: Path) : MemoryDataAccess() {

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

    override var webhooks =
        if (Files.exists(file)) {
            val bytes = Files.newBufferedReader(file)
            mapper.readValue(bytes)
        } else {
            Files.createFile(file)
            mutableMapOf<String, MutableList<Url>>()
        }

    override suspend fun add(topic: String, url: Url) {
        super.add(topic, url)
        save()
    }

    override suspend fun addAll(topic: String, urls: Collection<Url>) {
        super.addAll(topic, urls)
        save()
    }

    override suspend fun removeAllUrl(topic: String, urls: Collection<Url>) {
        super.removeAllUrl(topic, urls)
        save()
    }

    override suspend fun removeUrl(topic: String, url: Url) {
        super.removeUrl(topic, url)
        save()
    }

    override suspend fun removeTopic(topic: String) {
        super.removeTopic(topic)
        save()
    }

    override suspend fun removeAllTopic(topics: Collection<String>) {
        super.removeAllTopic(topics)
        save()
    }

    private suspend fun save() = coroutineScope {
        val json = async { getJson() }
        async { writeFile(json.await().toByteArray()) }
    }.await()

    private fun writeFile(byteArray: ByteArray) = Files.write(file, byteArray)
    private fun getJson(): String = mapper.writeValueAsString(webhooks)
}