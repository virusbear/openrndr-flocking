package com.virusbear.photon.flock

import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.sqrt

class Flock {
    private val boids: MutableList<Boid> = mutableListOf()

    val size: Int
        get() = boids.size

    val parameters = Parameters()

    private var grid = Grid(Rectangle.EMPTY, 0.0)

    private val threads = Array(8) { FlockUpdateThread(boids, this).also { it.start() } }

    fun resize(pointCount: Int, bounds: Rectangle) {
        when {
            pointCount < size -> {
                while(size > pointCount) {
                    val idx = Random.int(max = size)
                    boids.removeAt(idx)
                }
            }
            pointCount > size -> {
                for(i in size ..< pointCount) {
                    boids += Boid(
                        initialPos = Vector2.uniform(bounds),
                        initialVel = Vector2.uniform(min = -Vector2.ONE),
                        bounds = bounds,
                        vision = parameters.vision
                    )
                }
            }
        }

        grid.dispose()
        grid = Grid(bounds, bounds.width / sqrt(pointCount.toDouble()))

        grid.initialize(boids)
    }

    fun clear() {
        boids.clear()
        grid.dispose()
    }

    fun update() {
        val ranges = threads.indices.map {
            val batchSize = boids.size / threads.size
            val start = batchSize * it
            val end = batchSize * (it + 1) - 1

            IntRange(start, end)
        }

        threads.zip(ranges).forEach { (t, r) ->
            t.runFlock(r)
        }
        threads.forEach { t ->
            t.await()
        }
        threads.zip(ranges).forEach { (t, r) ->
            t.runUpdate(r)
        }
        threads.forEach { t ->
            t.await()
        }
    }

    fun neighbors(boid: Boid, radius: Double): Borrowed<List<Boid>> =
        grid.neighbors(boid, radius)

    fun onEach(block: Boid.() -> Unit) {
        for(i in boids.indices) {
            boids[i].block()
        }
    }

    inner class Parameters {
        var vision: Double = 0.0
            set(value) {
                if(value != field) {
                    field = value

                    onEach {
                        vision = value
                    }
                }
            }

        var alignment: Double = 1.0
        var cohesion: Double = 0.1
        var separation: Double = 1.0
    }
}

class FlockUpdateThread(
    private val boids: List<Boid>,
    private val flock: Flock
): Thread() {
    private val lock = Semaphore(1).also { it.acquire() }

    private var range = IntRange(0, 0)
    private var mode = Mode.Flock

    override fun run() {
        while(true) {
            lock.acquire()

            when(mode) {
                Mode.Flock -> flock()
                Mode.Update -> update()
                else -> {}
            }

            mode = Mode.Idle

            lock.release()
        }
    }

    fun await() {
        lock.acquire()
    }

    private fun flock() {
        for(i in range) {
            boids[i].flock(flock)
        }
    }

    private fun update() {
        for(i in range) {
            boids[i].update()
        }
    }

    fun runFlock(range: IntRange) {
        this.range = range
        mode = Mode.Flock
        lock.release()
    }

    fun runUpdate(range: IntRange) {
        this.range = range
        mode = Mode.Update
        lock.release()
    }

    enum class Mode {
        Flock,
        Update,
        Idle
    }
}