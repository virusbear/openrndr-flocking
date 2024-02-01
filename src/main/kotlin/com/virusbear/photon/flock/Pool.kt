package com.virusbear.photon.flock

import java.util.LinkedList

open class Borrowed<T>(
    val pool: Pool<T>,
    private val value: T
) {
    fun <R> unsafe(block: (T) -> R): R =
        block(value)

    fun <R> use(block: (T) -> R): R =
        try {
            block(value)
        } finally {
            release()
        }

    open fun release() {
        pool.release(this)
    }

    fun wrap(value: T): Borrowed<T> =
        WrappedBorrowed(this, value)
}

class WrappedBorrowed<T>(
    private val wrapped: Borrowed<T>,
    value: T
): Borrowed<T>(wrapped.pool, value) {
    override fun release() {
        wrapped.release()
    }
}

interface Pool<T> {
    fun borrow(): Borrowed<T>
    fun release(value: Borrowed<T>)
}

inline fun <reified T> ArrayPool(): ArrayPool<T> =
    ArrayPool(T::class.java)

class ArrayPool<T>(
    private val clazz: Class<T>
): Pool<List<T>> {
    private val bins = HashMap<Int, LinkedList<Array<T>>>()

    fun borrow(count: Int): Borrowed<List<T>> {
        val size = (count.takeHighestOneBit() shl 1)

        val array = synchronized(bins) {
            val bin = bins.computeIfAbsent(size) { LinkedList() }
            if(bin.isEmpty()) {
                java.lang.reflect.Array.newInstance(clazz, size) as Array<T>
            } else {
                bin.removeFirst()
            }
        }

        return Borrowed(this, ArrayViewList(array, IntRange(0, count)))
    }

    override fun borrow(): Borrowed<List<T>> =
        borrow(0)

    override fun release(value: Borrowed<List<T>>) {
        if(value.pool != this) {
            return
        }

        value.unsafe {
            if(it is ArrayViewList<T>) {
                synchronized(bins) {
                    bins.computeIfAbsent(it.data.size) { LinkedList<Array<T>>() } += it.data
                }
            }
        }
    }
}

class ArrayViewList<T>(
    val data: Array<T>,
    private val view: IntRange
): List<T> {
    override val size: Int = view.last - view.first + 1

    override fun contains(element: T): Boolean {
        for(i in view) {
            if(data[i] == element) {
                return true
            }
        }

        return false
    }

    override fun containsAll(elements: Collection<T>): Boolean =
        elements.all { it in this }

    override fun get(index: Int): T {
        require(index - view.first < size)
        require(index >= 0)

        return data[view.first + index]
    }

    operator fun set(index: Int, value: T) {
        require(index - view.first < size)
        require(index >= 0)

        data[view.first + index] = value
    }

    override fun indexOf(element: T): Int {
        for(i in view) {
            if(data[i] == element) {
                i - view.first
            }
        }

        return -1
    }

    override fun isEmpty(): Boolean =
        size == 0

    override fun iterator(): Iterator<T> = object: Iterator<T> {
        private var idx = 0

        override fun hasNext(): Boolean =
            idx < size

        override fun next(): T =
            get(idx++)
    }

    override fun lastIndexOf(element: T): Int {
        for(i in view.reversed()) {
            if(data[i] == element) {
                i - view.first
            }
        }

        return -1
    }

    override fun listIterator(): ListIterator<T> =
        listIterator(0)

    override fun listIterator(index: Int): ListIterator<T> = object: ListIterator<T> {
        private var idx = 0

        override fun hasNext(): Boolean =
            idx < size

        override fun hasPrevious(): Boolean =
            idx > 0

        override fun next(): T =
            get(idx++)

        override fun nextIndex(): Int =
            idx + 1

        override fun previous(): T =
            get(--idx)

        override fun previousIndex(): Int =
            idx - 1

    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> =
        ArrayViewList(data, IntRange(view.first + fromIndex, view.first + toIndex))
}