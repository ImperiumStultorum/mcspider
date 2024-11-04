package com.stultorum.architectury.mcspider.utilities.port

import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec2f

fun Entity.angle(): Vec2f {
    return Vec2f(pitch, yaw)
}
fun Entity.angledPosition(): AngledPosition {
    return AngledPosition(pos, angle())
}
fun Entity.eyeAngledPosition(): AngledPosition {
    return AngledPosition(eyePos, angle())
}