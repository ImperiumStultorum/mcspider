package com.stultorum.architectury.mcspider.items

import com.stultorum.architectury.mcspider.McSpiderState
import com.stultorum.architectury.mcspider.spider.TargetBehaviour
import com.stultorum.architectury.mcspider.utilities.port.eyeAngledPosition
import net.minecraft.entity.Entity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class ComeHereItem(settings: Settings): Item(settings) {
    override fun inventoryTick(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean) {
        if (!selected) return
        McSpiderState.spider?.let {
            val distance = if (it.bodyPlan.straightenLegs) 2.0 else it.gait.bodyHeight * 5
            it.behaviour = TargetBehaviour(it, entity.eyeAngledPosition(), distance)
        }
    }
}