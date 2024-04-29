import kotlinx.coroutines.*
import no.dossier.libraries.functional.Failure
import no.dossier.libraries.functional.Outcome
import no.dossier.libraries.functional.resolve
import kotlin.coroutines.CoroutineContext

/**
 * This is a context object used as a receiver of the [composeOutcome] function. It carries a
 * CoroutineContext instance so that we can launch child coroutines in that block, and also it carries a reference
 * to [failureSetter] so that the latest partial result can be captured in case of [Failure].
 */
class AccumulatedResultContext<E, T>(
    override val coroutineContext: CoroutineContext,
    private val failureSetter: (failure: Failure<E>) -> Unit
) : CoroutineScope {
    suspend operator fun <F: E, T> Outcome<F, T>.component1(): T = this.bind()
    suspend operator fun <F: E, T> Outcome<F, T>.not(): T = this.bind()
    suspend fun <F: E, T> Outcome<F, T>.bind(): T = this.resolve {
        failureSetter(it)
        coroutineScope { cancel() }
        awaitCancellation()
    }
}

/**
 * Attempts to compose an instance of [Outcome] by evaluating a set of partial results.
 * This emulates monad comprehensions by controlling flow via coroutines.
 *
 * Expressions inside the *block* lambda returning Outcome<E, T> are supposed to be bound via *!* operator function
 * so that they either return the Success value of T, or in case of Failure the whole block is interrupted
 * and that specific partial Failure result is returned from the whole [composeOutcome] block.
 *
 * This a simple coroutine-based implementation of Result/Outcome/Either monad comprehension inspired by Arrow library.
 *
 * @param E Error type
 * @param T Success type
 */
fun <E, T> composeOutcome(
    block: suspend AccumulatedResultContext<E, T>.() -> Outcome<E, T>
): Outcome<E, T> = runBlocking {
    lateinit var outcome: Outcome<E, T>
    val failureSetter: (failure: Failure<E>) -> Unit = { outcome = it }

    coroutineScope {
        launch {
            outcome = block(AccumulatedResultContext(coroutineContext, failureSetter))
        }
    }

    outcome
}

/**
 * @see composeOutcome
 * @param E Error type
 * @param T Success type
 */
@Deprecated("This function was renamed", ReplaceWith("composeOutcome(block)"))
fun <E, T> attemptBuildResult(
    block: suspend AccumulatedResultContext<E, T>.() -> Outcome<E, T>
): Outcome<E, T> = composeOutcome(block)