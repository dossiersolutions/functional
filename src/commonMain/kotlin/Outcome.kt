package no.dossier.libraries.functional

import kotlin.js.JsExport

@JsExport
sealed class Outcome<out E, out T>
class Success<out T>(val value: T) : Outcome<Nothing, T>()
class Failure<out E>(val error: E) : Outcome<E, Nothing>()

@JsExport
inline fun <E, T, U> Outcome<E, T>.map(transform: (T) -> U): Outcome<E, U> =
    when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
@JsExport
inline fun <E, T, F> Outcome<E, T>.mapError(transform: (E) -> F): Outcome<F, T> =
    when (this) {
        is Success -> this
        is Failure -> Failure(transform(error))
    }
@JsExport
inline fun <E, T, U> Outcome<E, T>.andThen(transform: (T) -> Outcome<E, U>): Outcome<E, U> =
    when (this) {
        is Success -> transform(value)
        is Failure -> this
    }
@JsExport
inline fun <E, T> Outcome<E, T>.failUnless(
    conditionBuilder: (T) -> Boolean,
    errorBuilder: (T) -> E
): Outcome<E, T> =
    when (this) {
        is Success -> if (conditionBuilder(value)) this else Failure(errorBuilder(value))
        is Failure -> this
    }
@JsExport
inline fun <E, T> Outcome<E, T>.getOrElse(
    onFailure: (Failure<E>) -> Nothing
): T =
    when(this) {
        is Failure -> onFailure(this)
        is Success -> value
    }

@Deprecated("Method renamed to unwrap", replaceWith = ReplaceWith("unwrap"))
fun <E, T> Outcome<E, T>.forceGet(): T = unwrap()
fun <E, T> Outcome<E, T>.unwrap(): T =
    getOrElse { throw RuntimeException(it.error.toString()) }

@JsExport
inline fun <E, T> Outcome<E, T>.resolve(
    onFailure: (Failure<E>) -> T
): T =
    when(this) {
        is Failure -> onFailure(this)
        is Success -> value
    }

@JsExport
fun <E, T> Iterable<Outcome<E, T>>.partition(): Pair<List<Success<T>>, List<Failure<E>>> =
    Pair(this.filterIsInstance<Success<T>>(), this.filterIsInstance<Failure<E>>())


@JsExport
fun <E, T, U> Iterable<T>.traverseToOutcome(f: (T) -> Outcome<E, U>): Outcome<E, List<U>> {
    tailrec fun go(iter: Iterator<T>, values: MutableList<U>): Outcome<E, List<U>> =
        if (iter.hasNext()) {
            when (val elemResult = f(iter.next())) {
                is Success -> {
                    values.add(elemResult.value)
                    go(iter, values)
                }
                is Failure -> elemResult
            }
        } else {
            Success(values)
        }

    return go(iterator(), mutableListOf())
}

@JsExport
fun <E, T> Iterable<Outcome<E, T>>.sequenceToOutcome(): Outcome<E, List<T>> = traverseToOutcome { it }

@JsExport
inline fun <E, T> runCatching(errorBuilder: (exception: Exception) -> E, block: () -> T): Outcome<E, T> = try {
    Success(block())
}
catch(e: Exception) {
    Failure(errorBuilder(e))
}

@JsExport
fun <E, T> T?.notNull(errorBuilder: () -> E) = this?.let { Success(it) } ?: Failure(errorBuilder())