package coolio.zoewong.traverse.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

/**
 * Blocking coroutines from continuing until a resource is ready/loaded or the
 * coroutine is cancelled.
 */
interface WaitFor<T> {

    /**
     * Waits until the resource is ready, returning it.
     */
    suspend operator fun invoke(): T

}

/**
 * Blocking coroutines from continuing until a resource is ready/loaded or the
 * coroutine is cancelled.
 */
class MutableWaitFor<T> : WaitFor<T> {
    private val _signaler = MutableSharedFlow<T?>(
        replay = 1,
    )

    override suspend operator fun invoke(): T {
        return _signaler.first()!!
    }

    /**
     * Notifies all waiting coroutines, allowing them to proceed.
     */
    fun done(value: T) {
        _signaler.tryEmit(value)
    }
}
