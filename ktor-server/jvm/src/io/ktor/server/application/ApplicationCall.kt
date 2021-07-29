/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.server.application

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.*

/**
 * Represents a single act of communication between client and server.
 */
public interface ApplicationCall {
    /**
     * Application being called
     */
    public val application: Application

    /**
     * Client request.
     */
    public val request: ApplicationRequest

    /**
     * Server response.
     */
    public val response: ApplicationResponse

    /**
     * Attributes attached to this instance.
     */
    public val attributes: Attributes

    /**
     * Parameters associated with this call.
     */
    public val parameters: Parameters

    /**
     * Callback that will be executed after the call finishes its execution.
     *
     * @param exception optional exception that happened before the call has finished
     * */
    public fun afterFinish(handler: (Throwable?) -> Unit)
}

public interface ApplicationCallWithContext : ApplicationCall, CoroutineScope {
    override fun afterFinish(handler: (Throwable?) -> Unit) {
        coroutineContext.job.invokeOnCompletion(handler)
    }
}
