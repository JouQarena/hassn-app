package com.hassn.app.service

import kotlinx.coroutines.CompletableDeferred
import kotlin.jvm.Volatile

enum class ChallengeResult {
    SUCCESS,
    FAILURE,
    TIMEOUT
}

/**
 * Bridges the challenge overlay UI (which completes a challenge)
 * and the ResponseExecutor (which waits for the outcome).
 */
object ChallengeResultBus {
    private val lock = Any()
    @Volatile
    private var pending: CompletableDeferred<ChallengeResult> = CompletableDeferred()

    /** Start a new challenge round; returns the deferred to await. */
    fun begin(): CompletableDeferred<ChallengeResult> = synchronized(lock) {
        pending = CompletableDeferred()
        pending
    }

    fun complete(result: ChallengeResult) {
        synchronized(lock) {
            if (!pending.isCompleted) pending.complete(result)
        }
    }
}
