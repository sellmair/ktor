/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine.curl

import io.ktor.client.engine.curl.internal.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.native.concurrent.*

/**
 * Only set in curl worker thread
 */
@ThreadLocal
private lateinit var curlApi: CurlMultiApiHandler

internal class CurlProcessor(
    override val coroutineContext: CoroutineContext
) : CoroutineScope {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val curlDispatcher: SingleThreadDispatcher = newSingleThreadContext("curl-dispatcher")
    private val curlScope = CoroutineScope(coroutineContext + curlDispatcher)

    init {
        curlScope.launch {
            curlApi = CurlMultiApiHandler()
        }
    }

    public suspend fun executeRequest(request: CurlRequestData): CurlSuccess {
        val deferred = CompletableDeferred<CurlSuccess>()

        request.freeze()
        val easyHandle = withContext(curlScope.coroutineContext) { curlApi.scheduleRequest(request, deferred) }

        val requestCleaner = request.executionContext.invokeOnCompletion { cause ->
            if (cause == null) return@invokeOnCompletion
            cancelRequest(easyHandle, cause)
        }

        try {
            curlPerform()
            return deferred.await()
        } finally {
            requestCleaner.dispose()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    public fun close() {
        runBlocking {
            curlScope.launch { curlApi.close() }.join()
        }
        curlScope.cancel()
        curlDispatcher.close()
    }

    private fun curlPerform() = curlScope.launch { curlApi.perform(100).freeze() }

    private fun cancelRequest(easyHandle: EasyHandle, cause: Throwable) {
        curlScope.launch {
            curlApi.cancelRequest(easyHandle, cause)
        }
    }
}
