package no.dossier.libraries.functional

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Wrapper that holds an instance of the latest partial result of the [attemptBuildResult]
 */
class ResultHolder<E, T> {
    lateinit var lastPartialOutcome: Outcome<E, T>
}

/**
 * This is a context object used as a receiver of the [attemptBuildResult] function. It carries a
 * CoroutineContext instance so that we can launch child coroutines in that block, and also it carries a reference
 * to [ResultHolder] so that the latest partial result can be set in case of [Failure].
 */
class AccumulatedResultContext<E, T>(
    override val coroutineContext: CoroutineContext,
    private val resultHolder: ResultHolder<E, T>
) : CoroutineScope {
    suspend operator fun <F: E, T> Outcome<F, T>.component1(): T = this.bind()
    suspend operator fun <F: E, T> Outcome<F, T>.not(): T = this.bind()
    suspend fun <F: E, T> Outcome<F, T>.bind(): T = when(this) {
        is Success -> this.value
        is Failure ->  {
            resultHolder.lastPartialOutcome = this
            coroutineScope { cancel() }
            awaitCancellation()
        }
    }
}

/**
 * Attempts to build an instance of [Outcome] by evaluating a set of partial results.
 * This emulates monad comprehensions by controlling flow via coroutines.
 *
 * Expressions inside the *block* lambda returning Result<T, E> are supposed to be bound via *!* operator function
 * so that they either return the Success value of T, or in case of Failure the whole block is interrupted
 * and that specific partial Failure result is returned from the whole [attemptBuildResult] block.
 *
 * This a simple coroutine-based implementation of Result monad comprehension inspired by Arrow library.
 *
 * @param T Success type
 * @param E Error type
 */
fun <E, T> attemptBuildResult(
    block: suspend AccumulatedResultContext<E, T>.() -> Outcome<E, T>
): Outcome<E, T> = runBlocking {
    val resultHolder = ResultHolder<E, T>()

    coroutineScope {
        launch {
            resultHolder.lastPartialOutcome = block(AccumulatedResultContext(coroutineContext, resultHolder))
        }
    }

    resultHolder.lastPartialOutcome
}