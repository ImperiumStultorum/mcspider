package com.stultorum.architectury.mcspider.utilities

import com.heledron.spideranimation.SpiderAnimationPlugin
import com.stultorum.architectury.mcspider.utilities.port.AngledPosition
import com.stultorum.architectury.mcspider.utilities.port.distance
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.AffineTransformation
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.bukkit.ChatColor
import org.bukkit.FluidCollisionMode
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.joml.*
import java.io.Closeable


fun runLater(delay: Long, task: () -> Unit): Closeable {
    val plugin = SpiderAnimationPlugin.instance
    val handler = plugin.server.scheduler.runTaskLater(plugin, task, delay)
    return Closeable {
        handler.cancel()
    }
}

fun interval(delay: Long, period: Long, task: () -> Unit): Closeable {
    val plugin = SpiderAnimationPlugin.instance
    val handler = plugin.server.scheduler.runTaskTimer(plugin, task, delay, period)
    return Closeable {
        handler.cancel()
    }
}

fun addEventListener(listener: Listener): Closeable {
    val plugin = SpiderAnimationPlugin.instance
    plugin.server.pluginManager.registerEvents(listener, plugin)
    return Closeable {
        org.bukkit.event.HandlerList.unregisterAll(listener)
    }
}

fun onInteractEntity(listener: (Player, Entity, EquipmentSlot) -> Unit): Closeable {
    return addEventListener(object : Listener {
        @org.bukkit.event.EventHandler
        fun onInteract(event: org.bukkit.event.player.PlayerInteractEntityEvent) {
            listener(event.player, event.rightClicked, event.hand)
        }
    })
}

fun onSpawnEntity(listener: (Entity, World) -> Unit): Closeable {
    return addEventListener(object : Listener {
        @org.bukkit.event.EventHandler
        fun onSpawn(event: org.bukkit.event.entity.EntitySpawnEvent) {
            listener(event.entity, event.entity.world)
        }
    })
}

fun onGestureUseItem(listener: (Player, ItemStack) -> Unit): Closeable {
    return addEventListener(object : Listener {
        @org.bukkit.event.EventHandler
        fun onPlayerInteract(event: org.bukkit.event.player.PlayerInteractEvent) {
            if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
            if (event.action == Action.RIGHT_CLICK_BLOCK && !(event.clickedBlock?.type?.isInteractable == false || event.player.isSneaking)) return
            listener(event.player, event.item ?: return)
        }
    })
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


fun createNamedItem(material: org.bukkit.Material, name: String): ItemStack {
    val item = ItemStack(material)
    val itemMeta = item.itemMeta ?: throw Exception("ItemMeta is null")
    itemMeta.setItemName(ChatColor.RESET.toString() + name)
    item.itemMeta = itemMeta
    return item
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
    // TODO fix gooey insides
    return location.world!!.rayTraceBlocks(location, direction, maxDistance, FluidCollisionMode.NEVER, true)
}

fun World.isOnGround(location: AngledPosition): Boolean {
    return raycastGround(location, DOWN_VECTOR, 0.001) != null
}

data class CollisionResult(val position: Vec3d, val offset: Vec3d)

fun World.resolveCollision(location: AngledPosition, direction: Vec3d): CollisionResult? {
    // TODO fix gooey insides
    val ray = location.world!!.rayTraceBlocks(location.clone().subtract(direction), direction, direction.length(), FluidCollisionMode.NEVER, true)
    if (ray != null) {
        val newLocation = ray.hitPosition.toLocation(location.world!!)
        return CollisionResult(newLocation.toVector(), ray.hitPosition.subtract(location.toVector()))
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
        entity.setStartInterpolation(0);
    }
}

fun applyTransformationWithInterpolation(entity: BlockDisplayEntity, matrix: Matrix4f) {
    applyTransformationWithInterpolation(entity, transformFromMatrix(matrix))
}