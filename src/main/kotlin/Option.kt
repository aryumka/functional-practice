sealed class Option<T> {
    data class Some<T>(val value: T): Option<T>()
    class None<T>: Option<T>()

    companion object {
        fun <T> of(value: T): Option<T> =
            when (value) {
                null -> None()
                else -> Some(value)
            }
    }

    override fun toString(): String =
        when (this) {
            is None -> "None"
            is Some -> "Some($value)"
        }
}

fun <T> Option<T>.getOrElse(default: T): T =
    when (this) {
        is Option.None -> default
        is Option.Some -> value
    }

fun <T> Option<T>.isEmpty(): Boolean =
    when (this) {
        is Option.None -> true
        is Option.Some -> false
    }

fun <T> Option<T>.map(f: (T) -> T): Option<T> =
    when (this) {
        is Option.None -> Option.None()
        is Option.Some -> Option.Some(f(value))
    }

fun <T> Option<T>.flatMap(f: (T) -> Option<T>): Option<T> =
    when (this) {
        is Option.None -> Option.None()
        is Option.Some -> f(value)
    }
