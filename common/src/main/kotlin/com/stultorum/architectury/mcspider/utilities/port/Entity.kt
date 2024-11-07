package com.stultorum.architectury.mcspider.utilities.port

import net.minecraft.entity.Entity
import net.minecraft.network.packet.s2c.play.PositionFlag
import net.minecraft.server.world.ServerWorld
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

/**
 * You should probably be using refreshPositionAndAngles, not this
 * @see refreshPositionAndAngles
 */
fun Entity.teleport(world: ServerWorld, position: AngledPosition, flags: Set<PositionFlag>) {
    teleport(world, position.x, position.y, position.z, flags, position.yaw, position.pitch)
}
fun Entity.requestTeleport(position: AngledPosition) {
    requestTeleport(position.x, position.y, position.z)
}
fun Entity.refreshPositionAndAngles(position: AngledPosition) {
    refreshPositionAndAngles(position.x, position.y, position.z, position.yaw, position.pitch)
}