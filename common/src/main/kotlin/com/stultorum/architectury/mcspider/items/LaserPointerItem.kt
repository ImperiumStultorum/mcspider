package com.stultorum.architectury.mcspider.items

import com.stultorum.architectury.mcspider.McSpiderState
import com.stultorum.architectury.mcspider.spider.DirectionBehaviour
import com.stultorum.architectury.mcspider.spider.TargetBehaviour
import com.stultorum.architectury.mcspider.utilities.port.AngledPosition
import com.stultorum.architectury.mcspider.utilities.port.angle
import com.stultorum.architectury.mcspider.utilities.port.eyeAngledPosition
import com.stultorum.architectury.mcspider.utilities.port.setY
import com.stultorum.architectury.mcspider.utilities.raycastGround
import net.minecraft.entity.Entity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class LaserPointerItem(settings: Settings): Item(settings) {
    override fun inventoryTick(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean) {
        if (!selected) return
        val location = entity.eyeAngledPosition()
        val hit = world.raycastGround(location, location.toDirection(), 100.0)

        if (hit == null) {
            val direction = location.toDirection().setY(0.0).normalize()
            McSpiderState.spider?.let { it.behaviour = DirectionBehaviour(it, direction, direction) }

            McSpiderState.chainVisualizer?.let {
                it.target = null
                it.resetIterator()
            }
        } else {
            val target = AngledPosition(hit.pos, entity.angle())
            McSpiderState.target = target

            McSpiderState.chainVisualizer?.let {
                it.target = target
                it.resetIterator()
            }

            McSpiderState.spider?.let { it.behaviour = TargetBehaviour(it, target, it.gait.bodyHeight) }
        }
    }
}