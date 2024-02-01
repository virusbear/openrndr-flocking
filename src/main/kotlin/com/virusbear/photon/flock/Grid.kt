package com.virusbear.photon.flock

import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.Array
import kotlin.math.ceil
import kotlin.math.pow


class Grid(
    size: Rectangle,
    cellSize: Double
) {
    private val rows = ceil(size.height / cellSize).toInt()
    private val columns = ceil(size.width / cellSize).toInt()
    private val pitch = Vector2(cellSize, cellSize)

    private val cells = Array(columns * rows) {
        Cell(
            grid = this,
            area = Rectangle(
                x = (it % columns).toDouble(),
                y = (it / columns).toDouble(),
                width = pitch.x,
                height = pitch.y
            )
        )
    }

    private val pool = ArrayPool<Boid>()

    private fun cellIdx(pos: IntVector2): Int =
        pos.y * columns + pos.x

    fun initialize(boids: Collection<Boid>) {
        for(boid in boids) {
            this += boid
        }
    }

    operator fun plusAssign(boid: Boid) {
        cells[cellIdx((boid.pos / pitch).toInt())] += boid
    }

    operator fun minusAssign(boid: Boid) {
        boid.cell?.minusAssign(boid)
    }

    fun neighbors(boid: Boid, radius: Double): Borrowed<List<Boid>> {
        val topleft = ((boid.pos - Vector2(radius)) / pitch).toInt().let {
            it.copy(it.x.coerceIn(0..<columns), it.y.coerceIn(0..<rows))
        }
        val bottomright = ((boid.pos + Vector2(radius)) / pitch).toInt().let {
            it.copy(it.x.coerceIn(0..<columns), it.y.coerceIn(0..<rows))
        }

        var possibleNeighborCount = 0

        for(y in topleft.y..bottomright.y) {
            for(x in topleft.x..bottomright.x) {
                possibleNeighborCount += cells[cellIdx(IntVector2(x, y))].size
            }
        }

        val neighbors = pool.borrow(possibleNeighborCount)

        var neighborCount = 0
        val view = neighbors.unsafe {
            val view = it as ArrayViewList<Boid>

            for(y in topleft.y..bottomright.y) {
                for(x in topleft.x..bottomright.x) {
                    val cell = cells[cellIdx(IntVector2(x, y))]

                    for(i in cell.boids.indices) {
                        val neighbor = cell.boids[i]

                        val inRange = boid.pos.squaredDistanceTo(neighbor.pos) <= radius * radius

                        if(inRange) {
                            view[neighborCount++] = neighbor
                        }
                    }
                }
            }

            view.subList(0, neighborCount - 1)
        }

        return neighbors.wrap(view)
    }

    fun dispose() {
        for(cell in cells) {
            cell.dispose()
        }
    }

    class Cell(
        val grid: Grid,
        private val area: Rectangle
    ) {
        private val _boids = ArrayList<Boid>()

        val boids: List<Boid>
            get() = _boids

        val size: Int
            get() = _boids.size

        operator fun plusAssign(boid: Boid) {
            if(boid.cell != null) {
                return
            }

            boid.cell = this
            synchronized(_boids) {
                _boids += boid
            }
        }

        operator fun minusAssign(boid: Boid) {
            if(boid.cell != this) {
                return
            }

            synchronized(_boids) {
                _boids -= boid
            }

            boid.cell = null
        }

        operator fun contains(boid: Boid): Boolean =
            boid.pos in area

        fun dispose() {
            for(boid in _boids) {
                if(boid.cell == this) {
                    boid.cell = null
                }
            }

            _boids.clear()
        }
    }
}