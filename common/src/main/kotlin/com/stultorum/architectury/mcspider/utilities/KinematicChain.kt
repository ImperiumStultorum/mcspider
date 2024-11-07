package com.stultorum.architectury.mcspider.utilities

import com.stultorum.architectury.mcspider.utilities.port.distance
import net.minecraft.util.math.Vec3d

class KinematicChain(
        var root: Vec3d,
        val segments: MutableList<ChainSegment>
) {
    fun fabrik(target: Vec3d) {
        val tolerance = 0.01

        for (i in 0 until 10) {
            fabrikForward(target)
            fabrikBackward()

            if (getEndEffector().distance(target) < tolerance) {
                break
            }
        }
    }

    fun straightenDirection(direction: Vec3d) {
        val normalized = direction.normalize()
        //val direction = target.clone().subtract(root).normalize()
        var position = root
        for (segment in segments) {
            position = position.add(normalized.multiply(segment.length))
            segment.position = position
        }
    }

    fun fabrikForward(newPosition: Vec3d) {
        val lastSegment = segments.last()
        lastSegment.position = newPosition

        for (i in segments.size - 1 downTo 1) {
            val previousSegment = segments[i]
            val segment = segments[i - 1]

            segment.position = moveSegment(segment.position, previousSegment.position, previousSegment.length)
        }
    }

    fun fabrikBackward() {
        val firstSegment = segments.first()
        moveSegment(firstSegment.position, root, firstSegment.length)

        for (i in 1 until segments.size) {
            val previousSegment = segments[i - 1]
            val segment = segments[i]

            segment.position = moveSegment(segment.position, previousSegment.position, segment.length)
        }
    }

    fun moveSegment(point: Vec3d, pullTowards: Vec3d, segment: Double): Vec3d {
        val direction = pullTowards.subtract(point).normalize()
        return pullTowards.subtract(direction.multiply(segment))
    }

    fun getEndEffector(): Vec3d {
        return segments.last().position
    }
}

class ChainSegment(
        var position: Vec3d,
        var length: Double
)