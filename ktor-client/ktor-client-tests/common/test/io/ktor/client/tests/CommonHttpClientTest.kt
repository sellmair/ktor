/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.client.tests

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.*
import io.ktor.util.*
import io.ktor.utils.io.concurrent.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlin.test.*

class CommonHttpClientTest {
    @Test
    fun testHttpClientWithCustomEngineLifecycle() {
        val engine = MockEngine { respondOk() }
        val client = HttpClient(engine)
        client.close()

        // When the engine is provided by a user it should not be closed together with the client.
        assertTrue { engine.isActive }
    }

    @Test
    fun testHttpClientWithFactoryEngineLifecycle() {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { respondOk() }
            }
        }
        val engine = client.engine
        client.close()

        // When the engine is provided by Ktor factory is should be closed together with the client.
        assertFalse { engine.isActive }
    }

    @Test
    fun testHttpClientClosesInstalledFeatures() {
        val client = HttpClient(MockEngine) {
            engine { addHandler { respond("") } }
            install(TestFeature)
        }
        client.close()
        assertTrue(client.feature(TestFeature)!!.closed)
    }
    class TestFeature : Closeable {
        var closed by shared(false)
        override fun close() {
            closed = true
        }

        companion object : HttpClientFeature<Unit, TestFeature> {
            override val key: AttributeKey<TestFeature> = AttributeKey("TestFeature")
            override fun install(feature: TestFeature, scope: HttpClient) = Unit
            override fun prepare(block: Unit.() -> Unit): TestFeature = TestFeature()
        }
    }
}
