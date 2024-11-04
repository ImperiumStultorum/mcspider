package com.stultorum.architectury.mcspider.utilities

import com.stultorum.architectury.mcspider.utilities.port.distance
import com.stultorum.architectury.mcspider.utilities.port.plus
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import kotlin.math.*

fun Double.lerp(target: Double, factor: Double): Double {
    return this + (target - this) * factor
}

fun Double.moveTowards(target: Double, speed: Double): Double {
    val distance = target - this
    return if (abs(distance) < speed) target else this + speed * distance.sign
}

fun Vec3d.moveTowards(target: Vec3d, constant: Double): Vec3d {
    val diff = target.subtract(this)
    val distance = diff.length()
    if (distance <= constant) {
        return target
    } else {
        return this.add(diff.multiply(constant / distance))
    }
}

fun Vec3d.lerp(target: Vec3d, factor: Double): Vec3d {
    return this.add(target.subtract(this).multiply(factor))
}

// https://stackoverflow.com/a/31276717
fun Vec3d.rotateAroundAxis(axis: Vec3d, theta: Double): Vec3d {
    val x = this.x
    val y = this.y
    val z = this.z
    val u = axis.x
    val v = axis.y
    val w = axis.z

    // avoid repeating operations
    val multiplied = u*x + v*y + w*z
    val cosTheta = cos(theta)
    val sinTheta = sin(theta)

    return Vec3d(
        u * multiplied * (1.0 - cosTheta) + x*cosTheta + (-w*y + v*z)*sinTheta,
        v * multiplied * (1.0 - cosTheta) + y*cosTheta + ( w*x - u*z)*sinTheta,
        w * multiplied * (1.0 - cosTheta) + z*cosTheta + (-v*x + u*y)*sinTheta
    )
}

fun verticalDistance(a: Vec3d, b: Vec3d): Double {
    return abs(a.y - b.y)
}

fun horizontalDistance(a: Vec3d, b: Vec3d): Double {
    val x = a.x - b.x
    val z = a.z - b.z
    return sqrt(x * x + z * z)
}

fun horizontalLength(vector: Vec3d): Double {
    return sqrt(vector.x * vector.x + vector.z * vector.z)
}

fun rotateAroundY(out: Vec3d, angle: Float, origin: Vec3d): Vec3d {
    return out.subtract(origin).rotateY(angle).add(origin)
}

fun averageVector(vectors: List<Vec3d>): Vec3d {
    var out = Vec3d.ZERO
    for (vector in vectors) out += vector
    return out.multiply(1.0 / vectors.size)
}



//class SplitDistance(val horizontal: Double, val vertical: Double) {
//    fun contains(origin: Vec3d, point: Vec3d): Boolean {
//        return horizontalDistance(origin, point) <= horizontal && verticalDistance(origin, point) <= vertical
//    }
//}

val DOWN_VECTOR = Vec3d(0.0, -1.0, 0.0)
val UP_VECTOR = Vec3d(0.0, 1.0, 0.0)



fun pointInPolygon(point: Vec2f, polygon: List<Vec2f>): Boolean {
    // count intersections
    var count = 0
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[(i + 1) % polygon.size]

        if (a.y <= point.y && b.y > point.y || b.y <= point.y && a.y > point.y) {
            val slope = (b.x - a.x) / (b.y - a.y)
            val intersect = a.x + (point.y - a.y) * slope
            if (intersect < point.x) count++
        }
    }

    return count % 2 == 1
}

fun nearestPointInPolygon(point: Vec2f, polygon: List<Vec2f>): Vec2f {
    var closest = polygon[0]
    var closestDistance = point.distance(closest)

    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[(i + 1) % polygon.size]

        val closestOnLine = nearestPointOnClampedLine(point, a, b) ?: continue
        val distance = point.distance(closestOnLine)

        if (distance < closestDistance) {
            closest = closestOnLine
            closestDistance = distance
        }
    }

    return closest
}

fun nearestPointOnClampedLine(point: Vec2f, a: Vec2f, b: Vec2f): Vec2f {
    val ap = Vec2f(point.x - a.x, point.y - a.y)
    val ab = Vec2f(b.x - a.x, b.y - a.y)

    val dotProduct = ap.dot(ab)
    val lengthAB = a.distance(b)

    val t = dotProduct / (lengthAB * lengthAB)

    // Ensure the nearest point lies within the line segment
    val tClamped = t.coerceIn(0f, 1f)

    val nearestX = a.x + tClamped * ab.x
    val nearestY = a.y + tClamped * ab.y

    return Vec2f(nearestX, nearestY)
}