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

import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test


class FileDataAccessTest {

    @Rule
    @JvmField
    val rule = FileSystemRule()

    @ExperimentalCoroutinesApi
    @Test
    fun testSave(): Unit = runTest {
        val fileSystem = rule.fileSystem
        val p = fileSystem.getPath("filetest")
        val a = FileDataAccess(p, UnconfinedTestDispatcher())
        a.webhooks.add("test1", Url("a"))
        a.webhooks.addAll("test2", listOf(Url("b"), Url("c")))
        a.persist()
        val nP = fileSystem.getPath("filetest")
        val access = FileDataAccess(nP)
        assertEquals(a.webhooks["test1"], access.webhooks["test1"])
        assertEquals(a.webhooks["test2"], access.webhooks["test2"])
    }
}