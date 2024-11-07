package com.stultorum.architectury.mcspider.utilities

import com.stultorum.architectury.mcspider.platform.repeatTask
import com.stultorum.architectury.mcspider.platform.runTaskLater
import com.stultorum.architectury.mcspider.utilities.port.AngledPosition
import com.stultorum.architectury.mcspider.utilities.port.distance
import dev.architectury.event.CompoundEventResult
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.EntityEvent
import dev.architectury.event.events.common.InteractionEvent
import net.minecraft.block.ShapeContext
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.AffineTransformation
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import org.joml.AxisAngle4f
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.io.Closeable


fun runLater(delay: Long, task: () -> Unit): Closeable {
    return runTaskLater(delay) { task.invoke() }
}

fun interval(delay: Long, period: Long, task: () -> Unit): Closeable {
    return repeatTask(delay, period) { task.invoke() }
}


fun onInteractEntity(listener: (PlayerEntity, Entity, Hand) -> Unit): Closeable {
    val actual = InteractionEvent.InteractEntity { player, entity, hand ->
        listener.invoke(player, entity, hand)
        EventResult.pass()
    }
    InteractionEvent.INTERACT_ENTITY.register(actual)
    return Closeable { InteractionEvent.INTERACT_ENTITY.unregister(actual) }
}

fun onSpawnEntity(listener: (Entity, World) -> Unit): Closeable {
    val actual = EntityEvent.Add { entity, world ->
        listener.invoke(entity, world)
        EventResult.pass()
    }
    EntityEvent.ADD.register(actual)
    return Closeable { EntityEvent.ADD.unregister(actual) }
}

fun onGestureUseItem(listener: (PlayerEntity, ItemStack) -> Unit): Closeable {
    val actual = InteractionEvent.RightClickItem { player, hand ->
        // todo [after MVP] do these `if`s need to be reimplemented?
        // if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        // if (event.action == Action.RIGHT_CLICK_BLOCK && !(event.clickedBlock?.type?.isInteractable == false || event.player.isSneaking)) return
        val item = player.getStackInHand(hand)
        // todo [after MVP] is the isEmpty check needed?
        if (!item.isEmpty) listener.invoke(player, item)
        CompoundEventResult.pass()
    }
    InteractionEvent.RIGHT_CLICK_ITEM.register(actual)
    return Closeable { InteractionEvent.RIGHT_CLICK_ITEM.unregister(actual) }
}


class SeriesScheduler {
    var time = 0L

    fun sleep(time: Long) {
        this.time += time
    }

    fun run(task: () -> Unit) {
        runLater(time, task)
    }
}

class EventEmitter {
    val listeners = mutableListOf<() -> Unit>()
    fun listen(listener: () -> Unit): Closeable {
        listeners.add(listener)
        return Closeable { listeners.remove(listener) }
    }

    fun emit() {
        for (listener in listeners) listener()
    }
}

fun World.firstPlayer(): PlayerEntity? {
    return this.server!!.playerManager.playerList.firstOrNull()!!
}

fun World.sendDebugMessage(message: String) {
    sendActionBar(firstPlayer() ?: return, message)
}

fun sendActionBar(player: PlayerEntity, message: String) {
    player.sendMessage(Text.of(message), true)
}

fun World.raycastGround(location: AngledPosition, direction: Vec3d, maxDistance: Double): BlockHitResult? {
    // TODO test. This is one of the things I'm least sure about.
    val endPos = location.toVec3d().add(direction.normalize().multiply(maxDistance))
    return this.raycast(RaycastContext(location.toVec3d(), endPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, ShapeContext.absent()))
}

fun World.isOnGround(location: AngledPosition): Boolean {
    return raycastGround(location, DOWN_VECTOR, 0.001) != null
}

data class CollisionResult(val position: Vec3d, val offset: Vec3d)

fun World.resolveCollision(location: AngledPosition, direction: Vec3d): CollisionResult? {
    // TODO test. This is one of the things I'm least sure about
    val newStart = location.toVec3d().subtract(direction) // while I'm not sure why we do this, it's what the original code seems to do
    val endPos = newStart.add(direction) // The original code has the maxDistance set to direction.length, implying we don't need to .normalize().multiply()
    val hit = this.raycast(RaycastContext(newStart, endPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, ShapeContext.absent()))
    if (hit != null) {
        return CollisionResult(hit.pos, hit.pos.subtract(location.toVec3d()))
    }
    return null
}

fun lookingAtPoint(eye: Vec3d, direction: Vec3d, point: Vec3d, tolerance: Double): Boolean {
    val pointDistance = eye.distance(point)
    val lookingAtPoint = eye.add(direction.multiply(pointDistance))
    return lookingAtPoint.distance(point) < tolerance
}

fun centredTransform(xSize: Float, ySize: Float, zSize: Float): AffineTransformation {
    return AffineTransformation(
        Vector3f(-xSize / 2, -ySize / 2, -zSize / 2),
        Quaternionf(AxisAngle4f(0f, 0f, 0f, 1f)),
        Vector3f(xSize, ySize, zSize),
        Quaternionf(AxisAngle4f(0f, 0f, 0f, 1f))
    )
}

fun transformFromMatrix(matrix: Matrix4f): AffineTransformation {
    val translation = matrix.getTranslation(Vector3f())
    val rotation = matrix.getUnnormalizedRotation(Quaternionf())
    val scale = matrix.getScale(Vector3f())

    return AffineTransformation(translation, rotation, scale, Quaternionf())
}

fun applyTransformationWithInterpolation(entity: BlockDisplayEntity, transformation: AffineTransformation) {
    if (DisplayEntity.getTransformation(entity.dataTracker) != transformation) {
        entity.setTransformation(transformation)
        entity.setStartInterpolation(0)
    }
}

fun applyTransformationWithInterpolation(entity: BlockDisplayEntity, matrix: Matrix4f) {
    applyTransformationWithInterpolation(entity, transformFromMatrix(matrix))
}