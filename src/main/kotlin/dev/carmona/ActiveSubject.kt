package dev.carmona

import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.Subject

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
                if (!active) {
                    active = true
                    try {
                        onActive()
                    } finally {
                        // NOP
                    }
                }
            }
        }.doFinally {
            synchronized(this) {
                if (active && !actual.hasObservers()) {
                    try {
                        onInactive()
                    } finally {
                        // NOP
                    }
                }
            }
        }.subscribe(observer)
    }

    override fun onNext(t: T) = actual.onNext(t)

    override fun hasComplete(): Boolean = actual.hasComplete()

    protected abstract fun onActive()

    protected abstract fun onInactive()
}