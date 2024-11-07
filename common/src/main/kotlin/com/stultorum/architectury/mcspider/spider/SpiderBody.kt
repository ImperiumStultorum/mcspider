package com.stultorum.architectury.mcspider.spider

import com.stultorum.architectury.mcspider.utilities.*
import com.stultorum.architectury.mcspider.utilities.port.*
import net.minecraft.entity.projectile.TridentEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min


class LegTarget(
    var position: Vec3d,
    val isGrounded: Boolean,
    val id: Int,
)


class NormalInfo(
    var normal: Vec3d,
    var origin: Vec3d? = null,
    val contactPolygon: List<Vec3d>? = null,
    var centreOfMass: Vec3d? = null
)

class Leg(
    val spider: Spider,
    var legPlan: LegPlan
) {
    var triggerZone = triggerZone(); private set
    var comfortZone = comfortZone(); private set
    var restPosition = restPosition(); private set
    var lookAheadPosition = lookAheadPosition(); private set
    var scanStartPosition = scanStartPosition(); private set
    var attachmentPosition = attachmentPosition(); private set
    var scanVector = scanVector(); private set
    var target = locateGround() ?: strandedTarget(); private set
    var isOutsideTriggerZone = false; private set
    var isUncomfortable = false; private set
    var targetOutsideComfortZone = false; private set
    var touchingGround = true; private set
    var isMoving = false; private set
    var timeSinceBeginMove = 0; private set
    var chain = KinematicChain(Vec3d.ZERO, mutableListOf())

    val onStep = EventEmitter()
    var isDisabled = false
    var endEffector = target.position
    var isPrimary = false
    var canMove = false

    private fun triggerDistance(): Double {
        val maxSpeed = spider.gait.walkSpeed
        val walkFraction = min(spider.velocity.length() / maxSpeed, 1.0)
        val fraction = if (spider.isRotatingYaw || spider.isRotatingPitch) 1.0 else walkFraction

        val diff = spider.gait.legWalkingTriggerDistance - spider.gait.legStationaryTriggerDistance
        return spider.gait.legStationaryTriggerDistance + diff * fraction
    }

    fun isGrounded(): Boolean {
        return touchingGround && !isMoving && !isDisabled
    }

    fun updateMemo() {
        triggerZone = triggerZone()
        comfortZone = comfortZone()
        restPosition = restPosition()
        lookAheadPosition = lookAheadPosition()
        attachmentPosition = attachmentPosition()
        touchingGround = touchingGround()
        scanStartPosition = scanStartPosition()
        scanVector = scanVector()

        isOutsideTriggerZone = !triggerZone.contains(restPosition, endEffector)
        targetOutsideComfortZone = !comfortZone.contains(restPosition, target.position)
        isUncomfortable = !comfortZone.contains(restPosition, endEffector)

        legPlan = spider.bodyPlan.legs.getOrNull(spider.body.legs.indexOf(this)) ?: legPlan
    }

    fun update() {
        updateMovement()
        chain = chain()
    }

    private fun attachmentPosition(): Vec3d {
        return spider.relativePosition(legPlan.attachmentPosition)
    }

    private fun updateMovement() {
        val gait = spider.gait
        var didStep = false

        timeSinceBeginMove += 1

        // update target
        if (isDisabled) {
            target = disabledTarget()
        } else {
            val ground = locateGround()
            if (ground != null) target = ground

            if (!target.isGrounded || !comfortZone.contains(restPosition, target.position)) {
                target = strandedTarget()
            }
        }

        // inherit parent velocity
        if (!isGrounded()) {
            endEffector = rotateAroundY(endEffector.add(spider.velocity), spider.rotateVelocity.toFloat(), spider.location.toVec3d())
        }

        // resolve ground collision
        if (!touchingGround) {
            val collision = spider.world.resolveCollision(AngledPosition(endEffector, Vec2f.ZERO), DOWN_VECTOR)
            if (collision != null) {
                didStep = true
                touchingGround = true
                endEffector = endEffector.setY(collision.position.y)
            }
        }

        if (isMoving) {
            val legMoveSpeed = gait.legMoveSpeed

            endEffector = endEffector.moveTowards(target.position, legMoveSpeed)

            val targetY = target.position.y + gait.legLiftHeight
            val hDistance = horizontalDistance(endEffector, target.position)
            if (hDistance > gait.legDropDistance) {
                endEffector = endEffector.setY(endEffector.y.moveTowards(targetY, legMoveSpeed))
            }

            if (endEffector.distance(target.position) < 0.0001) {
                isMoving = false

                touchingGround = touchingGround()
                didStep = touchingGround
            }

        } else {
//            canMove = spider.bodyPlan.canMoveLeg(spider, this)
            canMove = if (spider.gait.gallop) GallopGaitType.canMoveLeg(this) else WalkGaitType.canMoveLeg(this)

            if (canMove) {
                isMoving = true
                timeSinceBeginMove = 0
            }
        }

        if (didStep) this.onStep.emit()
    }

    private fun chain(): KinematicChain {
        if (chain.segments.size != legPlan.segments.size) {
            var stride = 0.0
            chain = KinematicChain(attachmentPosition, legPlan.segments.map {
                stride += it.length
                val position = spider.location.toVec3d().add(legPlan.restPosition.normalize().multiply(stride))
                ChainSegment(position, it.length)
            }.toMutableList())
        }

        chain.root = attachmentPosition

        if (spider.bodyPlan.straightenLegs) {
            var direction = endEffector.subtract(attachmentPosition).setY(0.0)

            val crossAxis = Vec3d(0.0, 1.0, 0.0).crossProduct(direction).normalize()

            direction = direction.rotateAroundAxis(crossAxis, Math.toRadians(spider.bodyPlan.legStraightenRotation))

            chain.straightenDirection(direction)
        }

        chain.fabrik(endEffector)

        return chain
    }

    private fun triggerZone(): SplitDistance {
        return SplitDistance(triggerDistance(), spider.gait.legVerticalTriggerDistance)
    }

    private fun comfortZone(): SplitDistance {
        return SplitDistance(spider.gait.legDiscomfortDistance, spider.gait.legVerticalDiscomfortDistance)
    }

    private fun touchingGround(): Boolean {
        return spider.world.isOnGround(AngledPosition(endEffector, Vec2f.ZERO))
    }

    private fun restPosition(): Vec3d {
        val pos = legPlan.restPosition.subY(spider.gait.bodyHeight)
        return spider.relativePosition(pos, pitch = 0f)
    }

    private fun lookAheadPosition(): Vec3d {
        if (!spider.isWalking || spider.velocity.isZero() && spider.rotateVelocity == 0.0) return restPosition

        val fraction = min(spider.velocity.length() / spider.gait.walkSpeed, 1.0)
        val mag = fraction * spider.gait.legWalkingTriggerDistance * spider.gait.legLookAheadFraction

        val direction = if (spider.velocity.isZero()) spider.location.toDirection() else spider.velocity.normalize()

        val lookAhead = direction.normalize().multiply(mag).add(restPosition)
        return rotateAroundY(lookAhead, spider.rotateVelocity.toFloat(), spider.location.toVec3d())
    }

    private fun scanStartPosition(): Vec3d {
        val vector = spider.relativeVector(Vec3d(.0, spider.gait.bodyHeight * 1.6, .0), pitch = 0f)
        return lookAheadPosition.add(vector)
    }

    private fun scanVector(): Vec3d {
        return spider.relativeVector(Vec3d(.0, -spider.gait.bodyHeight * 3.5, .0), pitch = 0f)
    }

    private fun locateGround(): LegTarget? {
        var lookAhead = lookAheadPosition
        val scanLength = scanVector.length()

        fun candidateAllowed(id: Int): Boolean {
            return true
//            if (!target.isGrounded) return true
//            if (!isMoving) return true
//            return id == target.id
        }

        var id = 0
        fun rayCast(x: Double, z: Double): LegTarget? {
            id += 1

            if (!candidateAllowed(id)) return null

            val start = AngledPosition(Vec3d(x, scanStartPosition.y, z), Vec2f.ZERO)
            val hit = spider.world.raycastGround(start, scanVector, scanLength) ?: return null

            return LegTarget(position = hit.pos, isGrounded = true, id = id)
        }

        val x = lookAhead.x
        val z = lookAhead.z

        val mainCandidate = rayCast(x, z)

        if (!spider.gait.legScanAlternativeGround) return mainCandidate

        if (mainCandidate != null) {
            if (mainCandidate.position.y in lookAhead.y - .24 .. lookAhead.y + 1.5) {
                return mainCandidate
            }
        }

        val margin = 2 / 16.0
        val nx = floor(x) - margin
        val nz = floor(z) - margin
        val pz = ceil(z) + margin
        val px = ceil(x) + margin

        val candidates = listOf(
            rayCast(nx, nz), rayCast(nx, z), rayCast(nx, pz),
            rayCast(x, nz),  mainCandidate,  rayCast(x, pz),
            rayCast(px, nz), rayCast(px, z), rayCast(px, pz),
        )
        val frontBlockPos = BlockPos.ofFloored(lookAhead.add(spider.location.toDirection().multiply(1.0)))
        val frontBlock = spider.world.getBlockState(frontBlockPos)
        if (!frontBlock.isSolidBlock(spider.world, frontBlockPos)) lookAhead = lookAhead.addY(spider.gait.legScanHeightBias)

        val best = candidates
            .filterNotNull()
            .minByOrNull { it.position.squaredDistanceTo(lookAhead) }

        if (best != null && !comfortZone.contains(restPosition, best.position)) {
            return null
        }

        return best
    }

    private fun strandedTarget(): LegTarget {
        return LegTarget(position = lookAheadPosition, isGrounded = false, id = -1)
    }

    private fun disabledTarget(): LegTarget {
        val target = strandedTarget()
        target.position = target.position.addY(spider.gait.bodyHeight / 2)

        val groundPosition = spider.world.raycastGround(AngledPosition(endEffector.add(0.0, .5, 0.0), Vec2f.ZERO), DOWN_VECTOR, 2.0)?.pos
        if (groundPosition != null && groundPosition.y > target.position.y) target.position = target.position.addY(spider.gait.bodyHeight * .3)

        return target
    }
}


class SpiderBody(val spider: Spider): SpiderComponent {
    val onHitGround = EventEmitter()
    var onGround = false; private set
    var legs = spider.bodyPlan.legs.map { Leg(spider, it) }
    var normal: NormalInfo? = null; private set
    var normalAcceleration: Vec3d = Vec3d.ZERO; private set
    val onGetHitByTrident = EventEmitter()

    override fun update() {
        val groundedLegs = legs.filter { it.isGrounded() }
        val fractionOfLegsGrounded = groundedLegs.size.toDouble() / spider.body.legs.size

        // apply gravity and air resistance
        spider.velocity = spider.velocity.subY(spider.gait.gravityAcceleration).mulY(1 - spider.gait.airDragCoefficient)

        // apply ground drag
        if (!spider.isWalking) {
            val drag = spider.gait.groundDragCoefficient * fractionOfLegsGrounded
            spider.velocity = spider.velocity.mulX(drag).mulZ(drag)
        }

        if (onGround) {
            spider.velocity = spider.velocity.mulX(.5).mulZ(.5)
        }


        normal = getNormal(spider)

        normalAcceleration = Vec3d.ZERO
        normal?.let {
            val preferredY = getPreferredY()
            val preferredYAcceleration = (preferredY - spider.location.y - spider.velocity.y).coerceAtLeast(0.0)
            val capableAcceleration = spider.gait.bodyHeightCorrectionAcceleration * fractionOfLegsGrounded
            val accelerationMagnitude = min(preferredYAcceleration, capableAcceleration)

            normalAcceleration = it.normal.multiply(accelerationMagnitude)

            // if the horizontal acceleration is too high,
            // there's no point accelerating as the spider will fall over anyway
            if (horizontalLength(normalAcceleration) > normalAcceleration.y) normalAcceleration = normalAcceleration.multiply(0.0)

            spider.velocity = spider.velocity.add(normalAcceleration)
        }

        // apply velocity
        spider.location = spider.location.add(spider.velocity)

        // resolve collision
        val collision = spider.world.resolveCollision(spider.location, Vec3d(0.0, min(-1.0, -abs(spider.velocity.y)), 0.0))
        if (collision != null) {
            onGround = true

            val didHit = collision.offset.length() > (spider.gait.gravityAcceleration * 2) * (1 - spider.gait.airDragCoefficient)
            if (didHit) onHitGround.emit()

            spider.location.y = collision.position.y
            if (spider.velocity.y < 0) spider.velocity = spider.velocity.mulY(-spider.gait.bounceFactor)
            if (spider.velocity.y < spider.gait.gravityAcceleration) spider.velocity = spider.velocity.setY(0.0)
        } else {
            onGround = spider.world.isOnGround(spider.location)
        }

        val updateOrder = getLegsInUpdateOrder(spider)
        for (leg in updateOrder) leg.updateMemo()
        for (leg in updateOrder) leg.update()

        val tridents = spider.world.getOtherEntities(null, Box.of(spider.location.toVec3d(), 1.5, 1.5, 1.5)) {
            it is TridentEntity && it.owner != spider.mount.getRider()
        }
        for (trident in tridents) {
            if (trident != null && trident.velocity.length() > 2.0) {
                val tridentDirection = trident.velocity.normalize()

                trident.velocity = tridentDirection.multiply(-.3)
                onGetHitByTrident.emit()

                spider.velocity = spider.velocity.add(tridentDirection.multiply(spider.gait.tridentKnockBack))
            }
        }
    }

    private fun getPreferredY(): Double {
    //        val groundY = getGround(spider.location) + .3
        val averageY = spider.body.legs.map { it.target.position.y }.average() + spider.gait.bodyHeight
        val targetY = averageY //max(averageY, groundY)
        val stabilizedY = spider.location.y.lerp(targetY, spider.gait.bodyHeightCorrectionFactor)
        return stabilizedY
    }

    private fun legsInPolygonalOrder(): List<Int> {
        val lefts = legs.indices.filter { LegLookUp.isLeftLeg(it) }
        val rights = legs.indices.filter { LegLookUp.isRightLeg(it) }
        return lefts + rights.reversed()
    }

    private fun getLegsInUpdateOrder(spider: Spider): List<Leg> {
        val diagonal1 = legs.indices.filter { LegLookUp.isDiagonal1(it) }
        val diagonal2 = legs.indices.filter { LegLookUp.isDiagonal2(it) }
        val indices = diagonal1 + diagonal2
        return indices.map { spider.body.legs[it] }
    }

    private fun getNormal(spider: Spider): NormalInfo? {
        if (spider.gait.useLegacyNormalForce) {
            val pairs = LegLookUp.diagonalPairs(legs.indices.toList())
            if (pairs.any { pair -> pair.mapNotNull { spider.body.legs.getOrNull(it) }.all { it.isGrounded() } }) {
                return NormalInfo(normal = Vec3d(0.0, 1.0, 0.0))
            }

            return null
        }

        var centreOfMass = averageVector(spider.body.legs.map { it.endEffector })
        centreOfMass = centreOfMass.lerp(spider.location.toVec3d(), 0.5)
        centreOfMass = centreOfMass.addY(0.01)

        val groundedLegs = legsInPolygonalOrder().map { spider.body.legs[it] }.filter { it.isGrounded() }
        if (groundedLegs.isEmpty()) return null

        fun applyStabilization(normal: NormalInfo) {
            if (normal.origin == null) return
            if (normal.centreOfMass == null) return

            if (horizontalDistance(normal.origin!!, normal.centreOfMass!!) < spider.gait.polygonLeeway) {
                normal.origin = normal.origin!!.setX(normal.centreOfMass!!.x).setZ(normal.centreOfMass!!.z)
            }

            val stabilizationTarget = normal.origin!!.setY(normal.centreOfMass!!.y)
            normal.centreOfMass = normal.centreOfMass!!.lerp(stabilizationTarget, spider.gait.stabilizationFactor)

            normal.normal = normal.centreOfMass!!.subtract(normal.origin).normalize()
        }

        val legsPolygon = groundedLegs.map { it.endEffector }
        val polygonCenterY = legsPolygon.map { it.y }.average()

        if (legsPolygon.size > 1) {
            val polygon2D = legsPolygon.map { Vec2f(it.x.toFloat(), it.z.toFloat()) }

            if (pointInPolygon(Vec2f(centreOfMass.x.toFloat(), centreOfMass.z.toFloat()), polygon2D)) {
                // inside polygon. accelerate upwards towards centre of mass
                return NormalInfo(
                    normal = Vec3d(0.0, 1.0, 0.0),
                    origin = Vec3d(centreOfMass.x, polygonCenterY, centreOfMass.z),
                    centreOfMass = centreOfMass,
                    contactPolygon = legsPolygon
                )
            } else {
                // outside polygon, accelerate at an angle from within the polygon
                val point = nearestPointInPolygon(Vec2f(centreOfMass.x.toFloat(), centreOfMass.z.toFloat()), polygon2D)
                val origin = Vec3d(point.x.toDouble(), polygonCenterY, point.y.toDouble())
                return NormalInfo(
                    normal = centreOfMass.subtract(origin).normalize(),
                    origin = origin,
                    centreOfMass = centreOfMass,
                    contactPolygon = legsPolygon
                ).apply { applyStabilization(this )}
            }
        } else {
            // only 1 leg on ground
            val origin = groundedLegs.first().endEffector
            return NormalInfo(
                normal = centreOfMass.subtract(origin).normalize(),
                origin = origin,
                centreOfMass = centreOfMass,
                contactPolygon = legsPolygon
            ).apply { applyStabilization(this )}
        }
    }
}