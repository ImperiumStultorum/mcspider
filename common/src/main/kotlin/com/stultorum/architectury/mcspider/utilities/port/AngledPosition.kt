package com.stultorum.architectury.mcspider.utilities.port

import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin

data class AngledPosition(private var position: Vec3d, private var rotation: Vec2f) {
    var x
        get() = position.x
        set(new) { position = position.setX(new) }
    var y
        get() = position.y
        set(new) { position = position.setY(new) }
    var z
        get() = position.z
        set(new) { position = position.setZ(new) }
    var pitch
        get() = rotation.x
        set(new) { rotation = rotation.setX(new) }
    var yaw
        get() = rotation.y
        set(new) { rotation = rotation.setY(new) }

    fun add(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0, pitch: Float = 0f, yaw: Float = 0f): AngledPosition {
        return AngledPosition(Vec3d(this.x + x, this.y + y, this.z + z), Vec2f(this.pitch + pitch, this.yaw + yaw))
    }
    fun add(position: Vec3d = Vec3d.ZERO, rotation: Vec2f = Vec2f.ZERO): AngledPosition {
        return AngledPosition(this.position.add(position), this.rotation.add(rotation))
    }

    fun toVec3d(): Vec3d = position
    fun toVec2f(): Vec2f = rotation
    fun toDirection(): Vec3d {
        val xz = cos(Math.toRadians(this.pitch.toDouble()))
        return Vec3d(
            -xz * sin(Math.toRadians(this.pitch.toDouble())),
            -sin(Math.toRadians(this.pitch.toDouble())),
            xz * cos(Math.toRadians(this.yaw.toDouble()))
        )
    }
}
