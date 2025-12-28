/*
 * Copyright 2019-2026 Simon Zigelli
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList


/**
 * Adds a collection of URIs to a webhook
 *
 * @param topic Name of the webhook
 * @param urls Collection of URIs
 */
public fun Topics.addAll(topic: String, urls: Collection<Url>) {
    this.computeIfAbsent(topic) { CopyOnWriteArrayList() }.addAllAbsent(urls)
}

/**
 * Adds a URI to a webhook
 *
 * @param topic Name of the webhook
 * @param url URI
 */
public fun Topics.add(topic: String, url: Url) {
    this.computeIfAbsent(topic) { CopyOnWriteArrayList() }.addIfAbsent(url)
}

/**
 * Removes a URI from a webhook
 *
 * @param topic Name of the webhook
 */
public fun Topics.removeUrl(topic: String, url: Url) {
    if (this.containsKey(topic)) {
        this[topic]?.remove(url)
    }
}

/**
 * Removes a collection of URIs from a webhook
 *
 * @param topic Name of the webhook
 */
public fun Topics.removeAllUrl(topic: String, urls: Collection<Url>) {
    if (this.containsKey(topic)) {
        this[topic]?.removeAll(urls.toSet())
    }
}

/**
 * Removes a webhook
 *
 * @param topic Name of the webhook
 */
public fun Topics.removeTopic(topic: String) {
    this.remove(topic)
}

/**
 * Removes a collection of webhooks
 *
 * @param topics Collection of webhooks
 */
public fun Topics.removeAllTopic(topics: Collection<String>) {
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
    public val webhooks: Topics

    /**
     * Persist Webhooks if possible
     */
    public suspend fun persist()
}

/**
 * 'MemoryDataAccess' stores all webhook data in memory
 */
public class MemoryDataAccess : DataAccess {
    override val webhooks: Topics = ConcurrentHashMap()
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
private data class DB(val topics: Map<String, List<@Serializable(with = UrlAsStringSerializer::class) Url>>)

/**
 * 'FileDataAccess' stores all webhook data in a file
 */
public class FileDataAccess(private val file: Path, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    DataAccess {

    public override val webhooks: Topics = ConcurrentHashMap()

    override suspend fun persist(): Unit = withContext(ioDispatcher) {
        val json = Json.encodeToString(DB(webhooks.toMap()))
        val directory = file.parent ?: file.toAbsolutePath().parent
        val tmpFile = Files.createTempFile(directory, "webhook_tmp", null)
        try {
            Files.write(tmpFile, json.toByteArray())
            Files.move(tmpFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            Files.deleteIfExists(tmpFile)
            throw e
        }
    }

    public fun init() {
        if (Files.exists(file)) {
            val bytes = Files.newBufferedReader(file).readText()
            val topics = Json.decodeFromString<DB>(bytes).topics
            for ((topic, urls) in topics) {
                webhooks.addAll(topic, urls)
            }
        }
    }
}