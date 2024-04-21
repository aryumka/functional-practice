import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

typealias Callback<T, Throwable> = (Result<T, Throwable>) -> Unit

interface Scheduler {
    fun execute(command: () -> Unit)
    fun shutdown()
}

object SchedulerIO: Scheduler {
    private var executorService = Executors.newFixedThreadPool(1)
    override fun execute(command: () -> Unit) {
        if (executorService.isShutdown) {
            executorService = Executors.newFixedThreadPool(1)
        }
        executorService.execute(command)
    }

    override fun shutdown() {
        if (!executorService.isShutdown) {
            executorService.shutdown()
        }
    }
}

object SchedulerMain: Scheduler {
    override fun execute(command: () -> Unit) {
        Thread(command).run()
    }
    override fun shutdown() = Unit
}

class Future<T, Throwable>(private var scheduler: Scheduler = SchedulerIO) {
    private var subscribers: MutableList<Callback<T, Throwable>> = mutableListOf()
    private var cache: Result<T, Throwable>? = null
    private var semaphore = Semaphore(1)

    private var callback: Callback<T, Throwable> = { value ->
        semaphore.acquire()
        cache = value
        while (subscribers.size > 0) {
            val subscriber = subscribers.last()
            subscribers = subscribers.dropLast(1).toMutableList()
            scheduler.execute {
                subscriber.invoke(value)
            }
        }
        semaphore.release()
    }

    fun create(f: (Callback<T, Throwable>) -> Unit): Future<T, Throwable> {
        scheduler.execute {
            f(callback)
        }
        return this
    }

    fun subscribe(cb: Callback<T, Throwable>): Disposable {
        semaphore.acquire()
        if (cache == null){
            subscribers.add(cb)
            semaphore.release()
        } else {
            semaphore.release()
            cb.invoke(cache!!)
        }

        return Disposable()
    }

    inner class Disposable {
        fun dispose() {
            scheduler.shutdown()
        }
    }
}

fun <T, P> Future<T, Throwable>.map(f: (value: T) -> P): Future<P, Throwable> {
    return this.flatMap { value ->
        Future<P, Throwable>().create { callback ->
            callback(Result.Success(f(value)))
        }
    }
}

fun <T, P> Future<T, Throwable>.flatMap(f: (value: T) -> Future<P, Throwable>): Future<P, Throwable> {
    return Future<P, Throwable>().create { callback ->
        this.subscribe { value ->
            when (value) {
                is Result.Success -> {
                    f(value.value).subscribe(callback)
                }
                is Result.Failure -> {
                    callback(Result.Failure(value.error))
                }
            }
        }
    }
}
