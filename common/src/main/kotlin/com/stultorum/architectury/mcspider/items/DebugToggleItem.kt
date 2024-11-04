package com.stultorum.architectury.mcspider.items

import com.stultorum.architectury.mcspider.McSpiderState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

class DebugToggleItem(settings: Settings): Item(settings) {
    override fun use(world: World, player: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        McSpiderState.showDebugVisuals = !McSpiderState.showDebugVisuals
        McSpiderState.chainVisualizer?.detailed = McSpiderState.showDebugVisuals

        val pitch = if (McSpiderState.showDebugVisuals) 2f else 1.5f
        world.playSound(player, player.blockPos, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 1f, pitch)
        return TypedActionResult.success(player.getStackInHand(hand), true)
    }
}