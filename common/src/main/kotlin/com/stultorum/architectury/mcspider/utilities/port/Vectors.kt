package com.stultorum.architectury.mcspider.utilities.port

import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import kotlin.math.sqrt

operator fun Vec3d.plus(other: Vec3d): Vec3d = this.add(other)
operator fun Vec3d.times(other: Vec3d): Vec3d = this.multiply(other)
operator fun Vec3d.times(value: Double): Vec3d = this.multiply(value)

fun Vec3d.isZero(): Boolean = this == Vec3d.ZERO
fun Vec3d.setX(new: Double): Vec3d {
    return Vec3d(new, this.y, this.z)
}
fun Vec3d.addX(new: Double): Vec3d = setX(x + new)
fun Vec3d.mulX(new: Double): Vec3d = setX(x * new)
fun Vec3d.subX(new: Double): Vec3d = setX(x - new)
fun Vec3d.setY(new: Double): Vec3d {
    return Vec3d(this.x, new, this.z)
}
fun Vec3d.addY(new: Double): Vec3d = setY(x + new)
fun Vec3d.mulY(new: Double): Vec3d = setY(x * new)
fun Vec3d.subY(new: Double): Vec3d = setY(x - new)
fun Vec3d.setZ(new: Double): Vec3d {
    return Vec3d(this.x, this.y, new)
}
fun Vec3d.addZ(new: Double): Vec3d = setZ(x + new)
fun Vec3d.mulZ(new: Double): Vec3d = setZ(x * new)
fun Vec3d.subZ(new: Double): Vec3d = setZ(x - new)
fun Vec3d.distance(other: Vec3d): Double {
    return sqrt(this.squaredDistanceTo(other))
}
fun Vec2f.setX(new: Float): Vec2f {
    return Vec2f(new, this.y)
}
fun Vec2f.setY(new: Float): Vec2f {
    return Vec2f(this.x, new)
}
fun Vec2f.subtract(other: Vec2f): Vec2f {
    return Vec2f(this.x - other.x, this.y - other.y)
}
fun Vec2f.distance(other: Vec2f): Float {
    return sqrt(this.distanceSquared(other))
}