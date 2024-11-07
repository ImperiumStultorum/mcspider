package com.stultorum.architectury.mcspider.spider

import com.stultorum.architectury.mcspider.utilities.*
import com.stultorum.architectury.mcspider.utilities.port.AngledPosition
import com.stultorum.architectury.mcspider.utilities.port.isZero
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.decoration.Brightness
import net.minecraft.registry.Registries
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import kotlin.jvm.optionals.getOrNull

// todo Port

fun targetModel(
    world: World,
    location: AngledPosition
) = blockModel(
    world = world,
    location = location,
    init = {
        it.setBlockState(Blocks.REDSTONE_BLOCK.defaultState)
        it.setTeleportDuration(1)
        it.setBrightness(Brightness(15, 15))
        it.setTransformation(centredTransform(.25f, .25f, .25f))
    }
)

//private val DEFAULT_MATERIAL = Material.NETHERITE_BLOCK

fun spiderModel(spider: Spider): Model {
    val scale = spider.bodyPlan.storedScale

    val model = Model()

    for ((legIndex, leg) in spider.body.legs.withIndex()) {
        val chain = leg.chain
        val maxThickness = 1.5/16 * 4 * scale * spider.bodyPlan.renderSegmentThickness
        val minThickness = 1.5/16 * 1 * scale * spider.bodyPlan.renderSegmentThickness

        // up vector is the cross product of the y-axis and the end-effector direction
        fun segmentUpVector(): Vec3d {
            val direction = chain.getEndEffector().subtract(chain.root)
            return direction.crossProduct(Vec3d(0.0, 1.0, 0.0))
        }

        val segmentUpVector = segmentUpVector()

        // Render leg segment

        // get material from namespacedID

        val defaultMaterial: Block = Registries.BLOCK.getOrEmpty(spider.bodyPlan.material).getOrNull() ?: Blocks.NETHERITE_BLOCK
        for ((segmentIndex, segment) in chain.segments.withIndex()) {
            val parent = chain.segments.getOrNull(segmentIndex - 1)?.position ?: chain.root
            val vector = segment.position.subtract(parent).normalize().multiply(segment.length)

            val thickness = (chain.segments.size - segmentIndex - 1) * (maxThickness - minThickness) / chain.segments.size + minThickness

            model.add(Pair(legIndex, segmentIndex), lineModel(
                world = spider.world,
                location = AngledPosition(parent, Vec2f.ZERO),
                vector = vector,
                thickness = thickness.toFloat(),
                upVector = segmentUpVector,
                update = {
//                    val cloak = spider.cloak.getSegment(legIndex to segmentIndex)
//                    it.block =  cloak ?: defaultMaterial.createBlockData()
                    it.setBlockState(defaultMaterial.defaultState)
                }
            ))
        }
    }

    return model
}


fun spiderDebugModel(spider: Spider): Model {
    val model = Model()

    val scale = spider.bodyPlan.storedScale.toFloat()

    for ((legIndex, leg) in spider.body.legs.withIndex()) {
        // Render scan bars
        if (spider.debugOptions.scanBars) model.add(Pair("scanBar", legIndex), lineModel(
            world = spider.world,
            location = AngledPosition(leg.scanStartPosition, Vec2f.ZERO),
            vector = leg.scanVector,
            thickness = .05f * scale,
            init = {
                it.setBrightness(Brightness(15, 15))
            },
            update = {
                val material = if (leg.isPrimary) Blocks.GOLD_BLOCK else Blocks.IRON_BLOCK
                it.setBlockState(material.defaultState)
            }
        ))

        // Render trigger zone
        val vector = Vec3d(0.0,1.0,0.0).multiply(leg.triggerZone.vertical)
        if (spider.debugOptions.triggerZones) model.add(Pair("triggerZoneVertical", legIndex), lineModel(
            world = spider.world,
            location = AngledPosition(leg.restPosition.subtract(vector.multiply(.5)), Vec2f.ZERO),
            vector = vector,
            thickness = .07f * scale,
            init = { it.setBrightness(Brightness(15, 15)) },
            update = {
                val material = if (leg.isUncomfortable) Blocks.RED_STAINED_GLASS else Blocks.CYAN_STAINED_GLASS
                it.setBlockState(material.defaultState)
            }
        ))

        // Render trigger zone
        if (spider.debugOptions.triggerZones) model.add(Pair("triggerZoneHorizontal", legIndex), blockModel(
            world = spider.world,
            location = run {
                val location = AngledPosition(leg.restPosition, Vec2f.ZERO)
                location.y = leg.target.position.y.coerceIn(location.y - leg.triggerZone.vertical, location.y + leg.triggerZone.vertical)
                location
            },
            init = {
                it.setTeleportDuration(1)
                it.setInterpolationDuration(1)
                it.setBrightness(Brightness(15, 15))
            },
            update = {
                val material = if (leg.isUncomfortable) Blocks.RED_STAINED_GLASS else Blocks.CYAN_STAINED_GLASS
                it.setBlockState(material.defaultState)

                val size = 2 * leg.triggerZone.horizontal.toFloat()
                val ySize = 0.02f
                it.setTransformation(centredTransform(size, ySize, size))
            }
        ))

        // Render end effector
        if (spider.debugOptions.endEffectors) model.add(Pair("endEffector", legIndex), blockModel(
            world = spider.world,
            location = AngledPosition(leg.endEffector, Vec2f.ZERO),
            init = {
                it.setTeleportDuration(1)
                it.setBrightness(Brightness(15, 15))
            },
            update = {
                val size = (if (leg == spider.pointDetector.selectedLeg) .2f else .15f) * scale
                it.setTransformation(centredTransform(size, size, size))
                it.setBlockState(when {
                    leg.isDisabled -> Blocks.BLACK_CONCRETE.defaultState
                    leg.isGrounded() -> Blocks.DIAMOND_BLOCK.defaultState
                    leg.touchingGround -> Blocks.LAPIS_BLOCK.defaultState
                    else -> Blocks.REDSTONE_BLOCK.defaultState
                })
            }
        ))

        // Render target position
        if (spider.debugOptions.targetPositions) model.add(Pair("targetPosition", legIndex), blockModel(
            world = spider.world,
            location = AngledPosition(leg.target.position, Vec2f.ZERO),
            init = {
                it.setTeleportDuration(1)
                it.setBrightness(Brightness(15, 15))

                val size = 0.2f * scale
                it.setTransformation(centredTransform(size, size, size))
            },
            update = {
                val material = if (leg.target.isGrounded) Blocks.LIME_STAINED_GLASS else Blocks.RED_STAINED_GLASS
                it.setBlockState(material.defaultState)
            }
        ))
    }

    // Render spider direction
    if (spider.debugOptions.direction) model.add("direction", blockModel(
        world = spider.world,
        location = spider.location.add(spider.location.toDirection().multiply(scale.toDouble())),
        init = {
            it.setTeleportDuration(1)
            it.setBrightness(Brightness(15, 15))

            val size = 0.1f * scale
            it.setTransformation(centredTransform(size, size, size))
        },
        update = {
            it.setBlockState(if (spider.gait.gallop) Blocks.REDSTONE_BLOCK.defaultState else Blocks.EMERALD_BLOCK.defaultState)
        }
    ))


    val normal = spider.body.normal ?: return model
    if (spider.debugOptions.legPolygons && normal.contactPolygon != null) {
        val points = normal.contactPolygon.map { AngledPosition(it, Vec2f.ZERO) }
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]

            model.add(Pair("polygon",i), lineModel(
                world = spider.world,
                location = a,
                vector = b.toVec3d().subtract(a.toVec3d()),
                thickness = .05f * scale,
                interpolation = 0,
                init = { it.setBrightness(Brightness(15, 15)) },
                update = { it.setBlockState(Blocks.EMERALD_BLOCK.defaultState) }
            ))
        }
    }

    if (spider.debugOptions.centreOfMass && normal.centreOfMass != null) model.add("centreOfMass", blockModel(
        world = spider.world,
        location = AngledPosition(normal.centreOfMass!!, Vec2f.ZERO),
        init = {
            it.setTeleportDuration(1)
            it.setBrightness(Brightness(15, 15))

            val size = 0.1f * scale
            it.setTransformation(centredTransform(size, size, size))
        },
        update = {
            val material = if (horizontalLength(normal.normal) == .0) Blocks.LAPIS_BLOCK else Blocks.REDSTONE_BLOCK
            it.setBlockState(material.defaultState)
        }
    ))


    if (spider.debugOptions.normalForce && normal.centreOfMass != null && normal.origin !== null) model.add("acceleration", lineModel(
        world = spider.world,
        location = AngledPosition(normal.origin!!, Vec2f.ZERO),
        vector = normal.centreOfMass!!.subtract(normal.origin),
        thickness = .02f * scale,
        interpolation = 1,
        init = { it.setBrightness(Brightness(15, 15)) },
        update = {
            val material = if (spider.body.normalAcceleration.isZero()) Blocks.BLACK_CONCRETE else Blocks.WHITE_CONCRETE
            it.setBlockState(material.defaultState)
        }
    ))

    return model
}