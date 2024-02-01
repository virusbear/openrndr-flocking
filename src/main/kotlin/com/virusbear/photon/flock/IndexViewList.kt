package com.virusbear.photon.flock

class IndexViewList<T>(
    private val viewed: List<T>,
    private val indices: List<Int>
): List<T> {
    override val size: Int
        get() = indices.size

    override fun get(index: Int): T =
        viewed[indices[index]]

    override fun isEmpty(): Boolean =
        size == 0

    override fun iterator(): Iterator<T> = object: Iterator<T> {
        private var index = 0

        override fun hasNext(): Boolean =
            index < indices.size

        override fun next(): T =
            viewed[indices[index++]]
    }

    override fun listIterator(): ListIterator<T> =
        listIterator(0)

    override fun listIterator(index: Int): ListIterator<T> = object: ListIterator<T> {
        var idx = index

        override fun hasNext(): Boolean =
            idx < indices.size

        override fun hasPrevious(): Boolean =
            idx > 0

        override fun next(): T =
            viewed[indices[idx++]]

        override fun nextIndex(): Int =
            idx + 1

        override fun previous(): T =
            viewed[indices[--idx]]

        override fun previousIndex(): Int =
            idx - 1
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        require(fromIndex in indices.indices)
        require(toIndex in indices.indices)
        return IndexViewList(viewed, indices.slice(IntRange(fromIndex, toIndex)))
    }

    override fun lastIndexOf(element: T): Int {
        for(i in indices.indices.reversed()) {
            if(element == viewed[indices[i]]) {
                return i
            }
        }

        return -1
    }

    override fun indexOf(element: T): Int {
        for(i in indices.indices) {
            if(element == viewed[indices[i]]) {
                return i
            }
        }

        return -1
    }

    override fun containsAll(elements: Collection<T>): Boolean =
        elements.all { it in this }

    override fun contains(element: T): Boolean {
        for(i in indices.indices) {
            if(element == viewed[indices[i]]) {
                return true
            }
        }

        return false
    }

}