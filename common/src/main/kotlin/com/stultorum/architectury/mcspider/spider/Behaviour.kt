package com.stultorum.architectury.mcspider.spider

import com.stultorum.architectury.mcspider.utilities.horizontalDistance
import com.stultorum.architectury.mcspider.utilities.port.AngledPosition
import com.stultorum.architectury.mcspider.utilities.port.setY
import net.minecraft.util.math.Vec3d

interface Behaviour {
    val targetVelocity: Vec3d
    val targetDirection: Vec3d
    fun update()
}

class StayStillBehaviour(val spider: Spider) : Behaviour {
    override var targetVelocity: Vec3d = Vec3d.ZERO
    override var targetDirection: Vec3d = Vec3d.ZERO

    override fun update() {
        targetVelocity = Vec3d.ZERO
        targetDirection = spider.location.toDirection().setY(0.0)
    }
}

class TargetBehaviour(val spider: Spider, val target: AngledPosition, val distance: Double) : Behaviour {
    override var targetVelocity: Vec3d = Vec3d.ZERO
    override var targetDirection: Vec3d = Vec3d.ZERO

    override fun update() {
        targetDirection = target.toVec3d().subtract(spider.location.toVec3d()).normalize()

        val currentSpeed = spider.velocity.length()

        val decelerateDistance = (currentSpeed * currentSpeed) / (2 * spider.gait.walkAcceleration)

        val currentDistance = horizontalDistance(spider.location.toVec3d(), target.toVec3d())

        targetVelocity = if (currentDistance > distance + decelerateDistance) {
            targetDirection.multiply(spider.gait.walkSpeed)
        } else {
            Vec3d.ZERO
        }
    }
}

class DirectionBehaviour(val spider: Spider, override val targetDirection: Vec3d, val walkDirection: Vec3d) :
    Behaviour {
    override var targetVelocity: Vec3d = Vec3d.ZERO

    override fun update() {
        targetVelocity = walkDirection.multiply(spider.gait.walkSpeed)
    }
}