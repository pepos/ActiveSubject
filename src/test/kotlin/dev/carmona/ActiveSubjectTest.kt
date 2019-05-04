package dev.carmona

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

object ActiveSubjectTest : Spek({

    val listenerMock = mockk<ActiveInactive>(relaxed = true)
    val numOfObservers = 10

    fun publishSubject() = object : ActiveSubject<String>(PublishSubject.create()) {
        override fun onActive() {
            listenerMock.onActive()
        }

        override fun onInactive() {
            listenerMock.onInactive()
        }
    }

    fun behaviorSubject() = object : ActiveSubject<String>(BehaviorSubject.create()) {
        override fun onActive() {
            listenerMock.onActive()
        }

        override fun onInactive() {
            listenerMock.onInactive()
        }
    }

    describe("A ActiveSubject which notifies when active/inactive events") {
        lateinit var subject: Subject<String>
        beforeEach {
            clearMocks(listenerMock, answers = false)
            subject = publishSubject()
        }

        describe("subscribing first observer") {
            lateinit var firstObserver: TestObserver<String>

            beforeEach { firstObserver = subject.test() }

            it("should invoke onActive") { verify(exactly = 1) { listenerMock.onActive() } }

            it("should not invoke onInactive") { verify(exactly = 0) { listenerMock.onInactive() } }

            val subscribedObservers = Stack<TestObserver<String>>()

            (2..numOfObservers).forEach { index ->
                describe("subscribing observer #$index") {
                    beforeEach {
                        clearMocks(listenerMock, answers = false)
                        subscribedObservers.push(subject.test())
                    }

                    it("should not invoke onActive") { verify(exactly = 0) { listenerMock.onActive() } }

                    it("should not invoke onInactive") { verify(exactly = 0) { listenerMock.onInactive() } }
                }
            }

            (2..numOfObservers).forEach { index ->
                describe("disposing observer #$index") {
                    beforeEach {
                        clearMocks(listenerMock, answers = false)
                        subscribedObservers.pop().dispose()
                    }

                    it("should not invoke onActive") { verify(exactly = 0) { listenerMock.onActive() } }

                    it("should not invoke onInactive") { verify(exactly = 0) { listenerMock.onInactive() } }
                }
            }

            describe("disposing first observer") {
                beforeEach {
                    clearMocks(listenerMock, answers = false)
                    firstObserver.dispose()
                }

                it("should not invoke onActive") { verify(exactly = 0) { listenerMock.onActive() } }

                it("should invoke onInactive") { verify(exactly = 1) { listenerMock.onInactive() } }
            }
        }
    }


    describe("An ActiveSubject wrapping PublishSubject") {

        lateinit var subject: Subject<String>

        beforeEach { subject = publishSubject() }

        describe("emitting one item") {
            val item1 = "Item 1"
            beforeEach { subject.onNext(item1) }

            it("should not deliver the previous event to a any new observer") {
                (1..numOfObservers).forEach { _ -> subject.test().assertEmpty() }
            }
        }

        describe("subscribing first observer") {

            lateinit var firstObserver: TestObserver<String>

            beforeEach { firstObserver = subject.test() }

            it("should not deliver any event") { firstObserver.assertEmpty() }

            describe("emitting one item") {

                val item1 = "Item 1"

                beforeEach { subject.onNext(item1) }

                it("should deliver the item to the current observer") { firstObserver.assertValue(item1) }

                describe("subscribing second observer") {

                    lateinit var secondObserver: TestObserver<String>

                    beforeEach { secondObserver = subject.test() }

                    it("should not deliver the item to second observer") { secondObserver.assertEmpty() }

                    describe("emitting second item") {

                        val item2 = "Item 2"

                        beforeEach { subject.onNext(item2) }

                        it("first observer should get the second item") { firstObserver.assertValues(item1, item2) }
                        it("second observer should get the second item") { secondObserver.assertValue(item2) }
                    }
                }
            }
        }
    }

    describe("An ActiveSubject wrapping a BehaviorSubject") {

        lateinit var subject: Subject<String>

        beforeEach { subject = behaviorSubject() }

        describe("emitting one item") {
            val item1 = "Item 1"
            beforeEach { subject.onNext(item1) }

            it("should deliver the previous event to a any new observer") {
                (1..numOfObservers).forEach { _ -> subject.test().assertValue(item1) }
            }
        }

        describe("subscribing first observer") {

            lateinit var firstObserver: TestObserver<String>

            beforeEach { firstObserver = subject.test() }

            it("should not deliver any event") { firstObserver.assertEmpty() }

            describe("emitting one item") {

                val item1 = "Item 1"

                beforeEach { subject.onNext(item1) }

                it("should deliver the item to the current observer") { firstObserver.assertValue(item1) }

                describe("subscribing second observer") {

                    lateinit var secondObserver: TestObserver<String>

                    beforeEach { secondObserver = subject.test() }

                    it("should deliver the item to second observer") { secondObserver.assertValue(item1) }

                    describe("emitting second item") {

                        val item2 = "Item 2"

                        beforeEach { subject.onNext(item2) }

                        it("first observer should get the second item") { firstObserver.assertValues(item1, item2) }
                        it("second observer should get the second item") { secondObserver.assertValues(item1, item2) }
                    }
                }
            }
        }
    }
})

private interface ActiveInactive {
    fun onActive()
    fun onInactive()
}
