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
import java.nio.file.Files
import java.nio.file.Path

interface DataAccess {
    fun getAll(): MutableMap<String, Webhook>
    fun get(topic: String): Webhook?
    fun add(webhook: Webhook)
    fun remove(webhook: Webhook)
    fun save()
}

open class MemoryDataAccess : DataAccess {
    protected open var webhooks: MutableMap<String, Webhook> = mutableMapOf()

    override fun getAll(): MutableMap<String, Webhook> {
        return webhooks
    }

    override fun get(topic: String): Webhook? {
        return webhooks[topic]
    }

    override fun add(webhook: Webhook) {
        webhooks[webhook.topic] = webhook
        save()
    }

    override fun remove(webhook: Webhook) {
        webhooks.remove(webhook.topic)
        save()
    }

    override fun save() {

    }
}

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


    override fun save() {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(webhooks)
        Files.write(file, json.toByteArray())
    }
}