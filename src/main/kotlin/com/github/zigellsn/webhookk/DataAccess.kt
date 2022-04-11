/*
 * Copyright 2019-2022 Simon Zigelli
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

import io.ktor.http.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path


/**
 * Adds a collection of URIs to a webhook
 *
 * @param topic Name of the webhook
 * @param urls Collection of URIs
 */
public fun MutableMap<String, MutableList<Url>>.addAll(topic: String, urls: Collection<Url>) {
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
public fun MutableMap<String, MutableList<Url>>.add(topic: String, url: Url) {
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
public fun MutableMap<String, MutableList<Url>>.removeUrl(topic: String, url: Url) {
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
public fun MutableMap<String, MutableList<Url>>.removeAllUrl(topic: String, urls: Collection<Url>) {
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
public fun MutableMap<String, MutableList<Url>>.removeTopic(topic: String) {
    this.remove(topic)
}

/**
 * Removes a collection of webhooks
 *
 * @param topics Collection of webhooks
 */
public fun MutableMap<String, MutableList<Url>>.removeAllTopic(topics: Collection<String>) {
    for (topic in topics) {
        this.remove(topic)
    }
}

/**
 * Interface for DataAccess
 */
public interface DataAccess {
    /**
     * Collection of Webhooks
     */
    public val webhooks: MutableMap<String, MutableList<Url>>

    /**
     * Persist Webhooks if possible
     */
    public suspend fun persist()
}

/**
 * 'MemoryDataAccess' stores all webhook data in memory
 */
public class MemoryDataAccess : DataAccess {
    override val webhooks: MutableMap<String, MutableList<Url>> = mutableMapOf()
    override suspend fun persist() {}
}

private object UrlAsStringSerializer : KSerializer<Url> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Url", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Url) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Url =
        Url(decoder.decodeString())
}

@Serializable
private data class DB(val topics: MutableMap<String, MutableList<@Serializable(with = UrlAsStringSerializer::class) Url>>)

/**
 * 'FileDataAccess' stores all webhook data in a file
 */
public class FileDataAccess(private val file: Path, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    DataAccess {

    public override val webhooks: MutableMap<String, MutableList<Url>> = if (Files.exists(file)) {
        val bytes = Files.newBufferedReader(file).readText()
        Json.decodeFromString<DB>(bytes).topics
    } else {
        mutableMapOf()
    }

    override suspend fun persist(): Unit = withContext(ioDispatcher) {
        val json = Json.encodeToString(DB(webhooks))
        launch(Dispatchers.IO) {
            Files.write(file, json.toByteArray())
        }
    }
}