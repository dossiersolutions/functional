package no.dossier.libraries.functional

sealed class Result<out T, out E>
class Success<out T>(val value: T) : Result<T, Nothing>()
class Failure<out E>(val error: E) : Result<Nothing, E>()

fun <U, T, E> Result<T, E>.map(transform: (T) -> U): Result<U, E> =
    when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

fun <U, T, E> Result<T, E>.mapError(transform: (E) -> U): Result<T, U> =
    when (this) {
        is Success -> this
        is Failure -> Failure(transform(error))
    }

fun <U, T, E> Result<T, E>.andThen(transform: (T) -> Result<U, E>): Result<U, E> =
    when (this) {
        is Success -> transform(value)
        is Failure -> this
    }

fun <T, E> Result<T, E>.failUnless(
    conditionBuilder: (T) -> Boolean,
    errorBuilder: (T) -> E
): Result<T, E> =
    when (this) {
        is Success -> if (conditionBuilder(value)) this else Failure(errorBuilder(value))
        is Failure -> this
    }

fun <T, E> Iterable<Result<T, E>>.partition(): Pair<List<Success<T>>, List<Failure<E>>> =
    Pair(this.filterIsInstance<Success<T>>(), this.filterIsInstance<Failure<E>>())

inline fun <T, E> Result<T, E>.getOrElse(
    onFailure: (Failure<E>) -> Nothing
): T =
    when(this) {
        is Failure -> onFailure(this)
        is Success -> value
    }