package com.virusbear.photon.flock

import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.math.average
import org.openrndr.math.sum
import org.openrndr.shape.Rectangle

class Boid(
    initialPos: Vector2,
    initialVel: Vector2,
    private val bounds: Rectangle,
    var vision: Double
) {
    var cell: Grid.Cell? = null

    var pos = initialPos
        private set
    var prev = initialPos
        private set
    var vel = initialVel
        private set
    var acc = Vector2.ZERO
        private set

    fun alignment(neighbors: List<Boid>): Vector2 {
        if(neighbors.isEmpty()) {
            return Vector2.ZERO
        }

        val desired = neighbors.map { it.vel }.average()

        return desired - vel
    }

    fun cohesion(neighbors: List<Boid>): Vector2 {
        if(neighbors.isEmpty()) {
            return Vector2.ZERO
        }

        val desired = neighbors.map { it.pos }.average()

        return (desired - pos) - vel
    }

    fun separation(neighbors: List<Boid>): Vector2 {
        if(neighbors.isEmpty()) {
            return Vector2.ZERO
        }

        val desired = neighbors.map { pos - it.pos }.average()

        return desired - vel
    }

    fun flock(flock: Flock) {
        flock.neighbors(this, vision).use { neighbors ->
            val fAlign = alignment(neighbors) * flock.parameters.alignment
            val fCohesion = cohesion(neighbors) * flock.parameters.cohesion
            val fSeparation = separation(neighbors) * flock.parameters.separation

            acc += fAlign
            acc += fCohesion
            acc += fSeparation
        }
    }

    fun update() {
        vel += acc
        vel = vel.normalized * 1.0
        prev = pos
        pos += vel
        acc = Vector2.ZERO
        mirror()

        cell?.let {
            if(this !in it) {
                it.grid -= this
                it.grid += this
            }
        }
    }

    private fun mirror() {
        vel = vel.copy(
            x = if(pos.x <= 0 || pos.x >= bounds.width) {
                -vel.x
            } else {
                vel.x
            },
            y = if(pos.y <= 0 || pos.y >= bounds.height) {
                -vel.y
            } else {
                vel.y
            }
        )
        pos = pos.copy(
            x = when {
                pos.x < 0 -> 0.0
                pos.x > bounds.width -> bounds.width
                else -> pos.x
            },
            y = when {
                pos.y < 0 -> 0.0
                pos.y > bounds.height -> bounds.height
                else -> pos.y
            }
        )
    }
}