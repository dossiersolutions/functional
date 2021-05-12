package no.dossier.libraries.functional

import kotlinx.coroutines.*

class AccumulatedResultContext<U, E> {
    var lastPartialResult: Result<U, E>? = null
    var continuation: CancellableContinuation<*>? = null

    suspend operator fun <T, F: E> Result<T, F>.not(): T = when(this) {
        is Success -> this.value
        is Failure -> suspendCancellableCoroutine {
            lastPartialResult = this
            continuation = it
        }
    }
}

/**
 * Attempts to build an instance of [Result] by evaluating a set of partial results.
 * This emulates monad comprehensions by controlling flow via coroutines.
 *
 * The statements inside of the *block* lambda passed in are supposed to be bound via *!* operator function
 * so that they either return the actual Success value of T or in case of Failure the whole block is interrupted
 * and that particular partial result is returned
 *
 * @param T Success type
 * @param E Error type
 */
fun <T, E> attemptBuildResult(
    block: suspend AccumulatedResultContext<T, E>.() -> Result<T, E>
): Result<T, E> =
    runBlocking {
        val context = AccumulatedResultContext<T, E>()
        launch(start = CoroutineStart.UNDISPATCHED) { context.lastPartialResult = block(context) }
        context.continuation?.cancel()
        context.lastPartialResult!!
    }