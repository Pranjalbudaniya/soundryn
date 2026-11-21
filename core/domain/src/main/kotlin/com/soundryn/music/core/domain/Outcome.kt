package com.soundryn.music.core.domain

sealed class Outcome<out T> {
    data class Success<out T>(val value: T) : Outcome<T>()
    data class Failure(val error: DomainError) : Outcome<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    inline fun <R> map(transform: (T) -> R): Outcome<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
}

sealed class DomainError {
    data object Network : DomainError()
    data object Unauthorized : DomainError()
    data object NotFound : DomainError()
    data class RateLimited(val retryAfterMs: Long? = null) : DomainError()
    data class Parse(val detail: String) : DomainError()
    data object Storage : DomainError()
    data object Unsupported : DomainError()
    data class Unknown(val detail: String) : DomainError()
}
