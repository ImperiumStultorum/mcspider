package com.stultorum.architectury.mcspider.spider

import com.stultorum.architectury.mcspider.utilities.ModelPart
import com.stultorum.architectury.mcspider.utilities.ModelPartRenderer
import com.stultorum.architectury.mcspider.utilities.onInteractEntity
import com.stultorum.architectury.mcspider.utilities.port.requestTeleport
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.InteractionEvent
import net.minecraft.entity.EntityType
import net.minecraft.entity.MarkerEntity
import net.minecraft.entity.passive.PigEntity
import net.minecraft.item.Items
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Vec3d
import java.io.Closeable


class Mountable(val spider: Spider): SpiderComponent {
    val pig = ModelPartRenderer<PigEntity>()
    var marker = ModelPartRenderer<MarkerEntity>()

    var closable = mutableListOf<Closeable>()

    fun getRider() = marker.entity?.passengerList?.firstOrNull()

    init {
        closable += pig
        closable += marker

        closable += onInteractEntity { player, entity, hand ->
            val pigEntity = pig.entity
            if (entity != pigEntity) return@onInteractEntity

            // if right click with saddle, add saddle (automatic)
            if (player.getStackInHand(hand).item == Items.SADDLE && !pigEntity.isSaddled) {
                spider.world.playSound(pigEntity, pigEntity.blockPos, SoundEvents.ENTITY_PIG_SADDLE, SoundCategory.PLAYERS, 1f, 1f)
            }

            // if right click with empty hand, remove saddle
            if (player.getStackInHand(hand).isEmpty && getRider() == null) {
                if (player.isSneaking) {
                    pigEntity.saddledComponent.isSaddled = false
                }
            }
        }



        // when player mounts the pig, switch them to the marker entity
        // TODO test
        val callback = InteractionEvent.InteractEntity event@{ player, entity, hand ->
            val pigEntity = pig.entity
            // if statements seperated by entity for readability
            if (pigEntity == null || pigEntity != entity || !pigEntity.isSaddled) return@event EventResult.pass()
            if (player.isSneaking || !player.getStackInHand(hand).isEmpty) return@event EventResult.pass()
            if (marker.entity == null) return@event EventResult.pass()
            marker.entity!!.passengerList.add(player)
            return@event EventResult.interruptTrue()
        }
        InteractionEvent.INTERACT_ENTITY.register(callback)
        closable += Closeable { InteractionEvent.INTERACT_ENTITY.unregister(callback) }
    }

    override fun render() {
        val location = spider.location.add(spider.velocity)

        val pigLocation = location.add(Vec3d(.0, -.4, .0))
        val markerLocation = location.add(Vec3d(.0, .5, .0))

        pig.render(ModelPart(
            world = spider.world,
            clazz = PigEntity::class.java,
            type = EntityType.PIG,
            location = pigLocation,
            init = {
                it.setNoGravity(true)
                it.isAiDisabled = true
                it.isInvisible = true
                it.isInvulnerable = true
                it.isSilent = true
                // it.isCollidable = false // TODO there doesn't seem to be an easy equivalent... can we omit this or do we do some mixin chicanery?
            }
        ))

        marker.render(ModelPart(
            world = spider.world,
            clazz = MarkerEntity::class.java,
            type = EntityType.MARKER,
            location = markerLocation,
            init = {
                it.setNoGravity(true)
                it.isInvisible = true
                it.isInvulnerable = true
                it.isSilent = true
            },
            update = update@{
                if (getRider() == null) return@update
                it.requestTeleport(markerLocation) // also teleports passengers
            }
        ))
    }

    override fun close() {
        closable.forEach { it.close() }
    }
}