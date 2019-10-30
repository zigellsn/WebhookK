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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
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
    suspend fun getAll(): MutableMap<String, Webhook>

    /**
     * @param topic Name of the webhook
     * @return The webhook
     */
    suspend fun get(topic: String): Webhook?

    /**
     * Adds a webhook
     *
     * @param webhook Webhook
     */
    suspend fun add(webhook: Webhook)

    /**
     * Removes a webhook
     *
     * @param webhook Webhook
     */
    suspend fun remove(webhook: Webhook)
}

/**
 * 'MemoryDataAccess' stores all webhook data in memory
 */
open class MemoryDataAccess : DataAccess {
    protected open var webhooks: MutableMap<String, Webhook> = mutableMapOf()

    override suspend fun getAll(): MutableMap<String, Webhook> {
        return webhooks
    }

    override suspend fun get(topic: String): Webhook? {
        return webhooks[topic]
    }

    override suspend fun add(webhook: Webhook) {
        webhooks[webhook.topic] = webhook
    }

    override suspend fun remove(webhook: Webhook) {
        webhooks.remove(webhook.topic)
    }
}

/**
 * 'FileDataAccess' stores all webhook data in a file
 */
class FileDataAccess(private val file: Path) : MemoryDataAccess() {

    override var webhooks =
        if (Files.exists(file)) {
            val bytes = Files.newBufferedReader(file)
            val sType = object : TypeToken<MutableMap<String, Webhook>>() {}.type
            Gson().fromJson(bytes, sType)
        } else {
            Files.createFile(file)
            mutableMapOf<String, Webhook>()
        }

    override suspend fun add(webhook: Webhook) {
        super.add(webhook)
        save()
    }

    override suspend fun remove(webhook: Webhook) {
        super.remove(webhook)
        save()
    }

    private suspend fun save() = coroutineScope {
        val json = async { getJson() }
        async { writeFile(json.await().toByteArray()) }
    }.await()

    private fun writeFile(byteArray: ByteArray) = Files.write(file, byteArray)
    private fun getJson(): String = GsonBuilder().setPrettyPrinting().create().toJson(webhooks)
}