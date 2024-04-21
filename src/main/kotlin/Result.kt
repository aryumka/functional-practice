sealed class Result<out V, out E> {
    abstract operator fun component1(): V?
    abstract operator fun component2(): E?

    companion object {
        inline fun <V> of(f: () -> V): Result<V, Throwable> {
            return try {
                Success(f())
            } catch (e: Throwable) {
                Failure(e)
            }
        }
    }

    class Success<out V>(val value: V): Result<V, Nothing>() {
        override fun component1(): V = value
        override fun component2(): Nothing? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class || this.value != (other as Success<*>).value) return false

            return true
        }

        override fun hashCode(): Int = this.value.hashCode()
        override fun toString(): String = "Result.Success($value)"
    }

    class Failure<out E>(val error: E): Result<Nothing, E>() {
        override fun component1(): Nothing? = null
        override fun component2(): E = error

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class || this.error != (other as Failure<*>).error) return false

            return true
        }

        override fun hashCode(): Int = this.error.hashCode()
        override fun toString(): String = "Result.Failure($error)"
    }
}

inline infix fun <V, E, R> Result<V, E>.map(transform: (V) -> R): Result<R, E> =
    when (this) {
        is Result.Success -> Result.Success(transform(value))
        is Result.Failure -> this
    }

inline infix fun <V, E, R> Result<V, E>.flatMap(transform: (V) -> Result<R, E>): Result<R, E> =
    when (this) {
        is Result.Success -> transform(value)
        is Result.Failure -> this
    }

inline infix fun <V, E, F> Result<V, E>.mapError(transform: (E) -> F): Result<V, F> =
    when (this) {
        is Result.Success -> this
        is Result.Failure -> Result.Failure(transform(this.error))
    }

inline infix fun <V, E> Result<V, E>.recover(transform: (E) -> V): Result.Success<V> {
    return when (this) {
        is Result.Success -> this
        is Result.Failure -> Result.Success(transform(error))
    }
}

inline infix fun <V, E> Result<V, E>.flatRecover(transform: (E) -> Result<V, E>): Result<V, E> {
    return when (this) {
        is Result.Success -> this
        is Result.Failure -> transform(error)
    }
}