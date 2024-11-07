package com.stultorum.architectury.mcspider.spider

import com.stultorum.architectury.mcspider.utilities.horizontalLength
import com.stultorum.architectury.mcspider.utilities.moveTowards
import com.stultorum.architectury.mcspider.utilities.port.AngledPosition
import com.stultorum.architectury.mcspider.utilities.port.setY
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.io.Closeable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

interface SpiderComponent : Closeable {
    fun update() {}
    fun render() {}
    override fun close() {}
}

class SpiderDebugOptions {
    var scanBars = true
    var triggerZones = true
    var endEffectors = true
    var targetPositions = true
    var legPolygons = true
    var centreOfMass = true
    var normalForce = true
    var direction = true
}

class Gait(
    walkSpeed: Double,
    gallop: Boolean,
) {
    companion object {
        fun defaultWalk(): Gait {
            return Gait(.15, false)
        }

        fun defaultGallop(): Gait {
            return Gait(.4, true).apply {
                legWalkCooldown = 1
                legMoveSpeed = .6
                rotateSpeed = .25
                uncomfortableSpeedMultiplier = .6
            }
        }
    }

    fun scale(scale: Double) {
        walkSpeed *= scale
        walkAcceleration *= scale
        legMoveSpeed *= scale
        legLiftHeight *= scale
        legDropDistance *= scale
        legStationaryTriggerDistance *= scale
        legWalkingTriggerDistance *= scale
        legDiscomfortDistance *= scale
        legVerticalTriggerDistance *= scale
        legVerticalDiscomfortDistance *= scale
        bodyHeight *= scale
        legScanHeightBias *= scale
    }

    var gallop = gallop

    var walkSpeed = walkSpeed
    var walkAcceleration = .0375

    var rotateSpeed = .15; private set

    var legMoveSpeed = walkSpeed * 3

    var legLiftHeight = .35
    var legDropDistance = legLiftHeight

    var legStationaryTriggerDistance = .25
    var legWalkingTriggerDistance = .8
    var legDiscomfortDistance = 1.2

    var legVerticalTriggerDistance = 1.5
    var legVerticalDiscomfortDistance = 1.6

    var gravityAcceleration = .08
    var airDragCoefficient = .02
    var bounceFactor = .5

    var bodyHeight = 1.1

    var bodyHeightCorrectionAcceleration = gravityAcceleration * 4
    var bodyHeightCorrectionFactor = .25

    var legScanAlternativeGround = true
    var legScanHeightBias = .5

    var tridentKnockBack = .3
    var legLookAheadFraction = .6
    var groundDragCoefficient = .2

    var legWalkCooldown = 2
    var legGallopHorizontalCooldown = 1
    var legGallopVerticalCooldown = 4

    var useLegacyNormalForce = false
    var polygonLeeway = .0
    var stabilizationFactor = 0.7

    var uncomfortableSpeedMultiplier = 0.0
}


class Spider(val world: World, var location: AngledPosition, val bodyPlan: BodyPlan): Closeable {
    var gallop = false
    var showDebugVisuals = false

    var debugOptions = SpiderDebugOptions()

    var walkGait = Gait.defaultWalk()
    var gallopGait = Gait.defaultGallop()

    val gait get() = if (gallop) gallopGait else walkGait

    var isWalking = false; private set
    var isRotatingYaw = false; private set
    var isRotatingPitch = false; private set
    var rotateVelocity = 0.0; private set

    var velocity: Vec3d = Vec3d.ZERO

    val body = SpiderBody(this)

//    val cloak = Cloak(this)
    val sound = SoundEffects(this)
    val mount = Mountable(this)
    val pointDetector = PointDetector(this)

    var behaviour: Behaviour = StayStillBehaviour(this)

    var renderer: SpiderComponent = SpiderRenderer(this)
        set (value) {
            field.close()
            field = value
        }


    override fun close() {
        getComponents().forEach { it.close() }
    }

    fun teleport(newLocation: AngledPosition) {
        val diff = newLocation.toVec3d().subtract(location.toVec3d())

        location = newLocation
        for (leg in body.legs) leg.endEffector.add(diff)
        // todo this doesn't seem to rotate according to the new position, in their code or ours. Should that be changed?
    }

    fun getComponents() = iterator<SpiderComponent> {
//        yield(cloak)
        yield(body)
        yield(sound)
        yield(mount)
        yield(pointDetector)
        yield(renderer)
    }

    fun update() {
        // update behaviour
        behaviour.update()
        walkAt(behaviour.targetVelocity)
        rotateTowards(behaviour.targetDirection)


        for (component in getComponents()) component.update()
        for (component in getComponents()) component.render()
    }

    fun relativePosition(point: Vec3d, pitch: Float = location.pitch): Vec3d {
        return location.toVec3d().add(relativeVector(point, pitch))
    }

    fun relativeVector(point: Vec3d, pitch: Float = location.pitch): Vec3d {
        return point
            .rotateX(Math.toRadians(pitch.toDouble()).toFloat())
            .rotateY(-Math.toRadians(location.yaw.toDouble()).toFloat())
    }

    private fun rotateTowards(targetDirection: Vec3d) {
        // pitch
        val targetPitch = -Math.toDegrees(atan2(targetDirection.y, horizontalLength(targetDirection))).coerceIn(-30.0, 30.0)
        val oldPitch = location.pitch
        location.pitch = oldPitch.toDouble().moveTowards(targetPitch, Math.toDegrees(gait.rotateSpeed)).toFloat()//.coerceIn(minPitch, maxPitch)
        isRotatingPitch = abs(targetPitch - oldPitch) > 0.0001

        // yaw
        val maxSpeed = gait.rotateSpeed * body.legs.filter { it.isGrounded() }.size / body.legs.size
        location.yaw %= 360
        val oldYaw = Math.toRadians(location.yaw.toDouble())
        val targetYaw = atan2(-targetDirection.x, targetDirection.z)

        val optimizedTargetYaw = if (abs(targetYaw - oldYaw) > PI) {
            if (targetYaw > oldYaw) targetYaw - PI * 2 else targetYaw + PI * 2
        } else {
            targetYaw
        }

        isRotatingYaw = abs(optimizedTargetYaw - oldYaw) > 0.0001

        rotateVelocity = 0.0
        if (!isRotatingYaw || body.legs.any { it.isUncomfortable && !it.isMoving }) return

        val newYaw = oldYaw.moveTowards(optimizedTargetYaw, maxSpeed)
        location.yaw = Math.toDegrees(newYaw).toFloat()

        rotateVelocity = -(newYaw - oldYaw)
    }

    private fun walkAt(targetVelocity: Vec3d) {
        val acceleration = gait.walkAcceleration// * body.legs.filter { it.isGrounded() }.size / body.legs.size
        val target = targetVelocity

        isWalking = true

        if (body.legs.any { it.isUncomfortable && !it.isMoving }) { //  && !it.targetOutsideComfortZone
            val scaled = target.setY(velocity.y).multiply(gait.uncomfortableSpeedMultiplier)
            velocity = velocity.moveTowards(scaled, acceleration)
        } else {
            velocity = velocity.moveTowards(target.setY(velocity.y), acceleration)
            isWalking = velocity.x != 0.0 && velocity.z != 0.0
        }
    }
}

