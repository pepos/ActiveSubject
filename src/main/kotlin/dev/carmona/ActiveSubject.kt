package dev.carmona

import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.Subject

/**
 * Wraps a {@link Subject} and provides callbacks on active or inactive state change events.
 * ActiveSubject becomes active when observers count go from 0 to 1. Symmetrically, it becomes
 * inactive when observers count go from 1 to 0.
 *
 * <p>
 * This class only keep tracks of the observers that subscribe directly to the wrapper, it won't
 * work for those observers that subscribe directly to the actual subject.
 *
 * <p>
 * onActive and onInactive rely on doOnSubscribe and doFinally respectively. onActive runs in the
 * same thread that the first observers subscribe on and onInactive runs in the thread where the
 * last observer is disposed. It is not guarantee the callbacks will run in the same thread but,
 * since both are synchronized, they won't be executed concurrently.
 *
 * <p>
 * Any error thrown by onActive or onInactive callbacks is propagated to the wrapped subject.
 *
 * @param <T> the item value type
 */
abstract class ActiveSubject<T>(private val actual: Subject<T>) : Subject<T>() {

    private var active: Boolean = false

    override fun hasThrowable(): Boolean = actual.hasThrowable()

    override fun hasObservers(): Boolean = actual.hasObservers()

    override fun onComplete() = actual.onComplete()

    override fun onSubscribe(d: Disposable) = actual.onSubscribe(d)

    override fun onError(e: Throwable) = actual.onError(e)

    override fun getThrowable(): Throwable? = actual.throwable

    override fun subscribeActual(observer: Observer<in T>) {
        actual.doOnSubscribe {
            synchronized(this) {
                if (!active && !it.isDisposed) {
                    active = true
                    try {
                        onActive()
                    } catch (e: Exception) {
                        actual.onError(e)
                    }
                }
            }
        }.doFinally {
            synchronized(this) {
                if (active && !actual.hasObservers()) {
                    try {
                        onInactive()
                    } catch(e: Exception) {
                        actual.onError(e)
                    }
                }
            }
        }.subscribe(observer)
    }

    override fun onNext(t: T) = actual.onNext(t)

    override fun hasComplete(): Boolean = actual.hasComplete()

    /**
     * Invoked when observers count go from 0 to 1.
     */
    protected abstract fun onActive()

    /**
     * Invoked when observers count go from 1 to 0.
     */
    protected abstract fun onInactive()
}