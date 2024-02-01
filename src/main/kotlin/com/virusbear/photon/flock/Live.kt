package com.virusbear.photon.flock

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.isolated
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noclear.NoClear
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.videoprofiles.h265
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle

internal val Background = ColorRGBa.fromHex("EE7862")
internal val Foreground = ColorRGBa.fromHex("FCD8A2")

fun main() = application {
    configure {
        width = 1200
        height = 1200
        multisample = WindowMultisample.Disabled
        windowResizable = false
    }

    program {
        val Record = false

        val Settings = object {
            @IntParameter("Point Count", low = 0, high = 100_000)
            var pointCount: Int = 50

            @IntParameter("Diameter", low = 1, high = 32)
            var radius: Int = 16

            @DoubleParameter("Vision", low = 0.0, high = 200.0)
            var vision: Double = 50.0

            @DoubleParameter("Alignment", low = 0.0, high = 1.0)
            var alignment: Double = 1.0

            @DoubleParameter("Cohesion", low = 0.0, high = 0.1)
            var cohesion: Double = 0.1

            @DoubleParameter("Separation", low = 0.0, high = 1.0)
            var separation: Double = 1.0
        }

        val gui = GUI()

        gui.add(Settings, "Settings")
        extend(gui) {
            visible = false
        }

        if (Record) {
            extend(ScreenRecorder()) {
                h265 {
                    constantRateFactor = 20
                }
                frameRate = 30
                quitAfterMaximum = true
                maximumDuration = 60.0
                frameSkip = 25
                multisample = BufferMultisample.SampleCount(8)
            }
        }

        val flock = Flock()

        keyboard.keyUp.listen {
            when(it.key) {
                KEY_F1 -> gui.visible = !gui.visible
                KEY_F12 -> flock.clear()
            }
        }

        extend {
            with(flock.parameters) {
                vision = Settings.vision
                alignment = Settings.alignment
                cohesion = Settings.cohesion
                separation = Settings.separation
            }
        }

        extend(NoClear())

        extend {
            if(flock.size != Settings.pointCount) {
                flock.resize(Settings.pointCount, bounds)
            }
        }

        extend {
            drawer.isolated {
                stroke = null
                fill = Background.opacify(0.1)
                rectangle(Vector2.ZERO, width.toDouble(), height.toDouble())
            }
        }

        extend {
            flock.update()
            println(1 / (program as ProgramImplementation).deltaTime)
        }

        extend {
            drawer.isolated {
                fill = null
                stroke = Foreground
                strokeWeight = Settings.radius / 8.0

                val b: List<LineSegment> = buildList {
                    flock.onEach {
                        add(LineSegment(prev, pos))
                    }
                }

                lineSegments(b)
            }
        }
    }
}

val Program.bounds: Rectangle
    get() = Rectangle(
        Vector2.ZERO,
        width.toDouble(),
        height.toDouble()
    )