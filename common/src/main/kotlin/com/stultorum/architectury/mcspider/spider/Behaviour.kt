package com.stultorum.architectury.mcspider.spider

import com.stultorum.architectury.mcspider.utilities.horizontalDistance

// TODO port

interface Behaviour {
    val targetVelocity: Vector
    val targetDirection: Vector
    fun update()
}

class StayStillBehaviour(val spider: Spider) : Behaviour {
    override var targetVelocity = Vector(0.0, 0.0, 0.0)
    override var targetDirection = Vector(0.0, 0.0, 0.0)

    override fun update() {
        targetVelocity = Vector(0.0, 0.0, 0.0)
        targetDirection = spider.location.direction.clone().setY(0.0)
    }
}

class TargetBehaviour(val spider: Spider, val target: Location, val distance: Double) : Behaviour {
    override var targetVelocity = Vector(0.0, 0.0, 0.0)
    override var targetDirection = Vector(0.0, 0.0, 0.0)

    override fun update() {
        targetDirection = target.toVector().clone().subtract(spider.location.toVector()).normalize()

        val currentSpeed = spider.velocity.length()

        val decelerateDistance = (currentSpeed * currentSpeed) / (2 * spider.gait.walkAcceleration)

        val currentDistance = horizontalDistance(spider.location.toVector(), target.toVector())

        targetVelocity = if (currentDistance > distance + decelerateDistance) {
            targetDirection.clone().multiply(spider.gait.walkSpeed)
        } else {
            Vector(0.0, 0.0, 0.0)
        }
    }
}

class DirectionBehaviour(val spider: Spider, override val targetDirection: Vector, val walkDirection: Vector) :
    Behaviour {
    override var targetVelocity = Vector(0.0, 0.0, 0.0)

    override fun update() {
        targetVelocity = walkDirection.clone().multiply(spider.gait.walkSpeed)
    }
}