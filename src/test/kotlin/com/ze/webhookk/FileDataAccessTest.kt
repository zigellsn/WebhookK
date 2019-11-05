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

import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files


class FileDataAccessTest {

    @Rule
    @JvmField
    val rule = FileSystemRule()

    @Test
    fun testSave() = runBlocking {
        val fileSystem = rule.fileSystem
        val p = fileSystem.getPath("filetest")
        val a = FileDataAccess(p)
        a.add("test1", Url("a"))
        a.add("test2", Url("b"))

        val nP = fileSystem.getPath("filetest")
        val access = FileDataAccess(nP)
        assertEquals(a.get("test1"), access.get("test1"))
        assertEquals(a.get("test2"), access.get("test2"))
    }
}