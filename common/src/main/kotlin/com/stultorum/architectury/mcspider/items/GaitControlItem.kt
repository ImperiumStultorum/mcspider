package com.stultorum.architectury.mcspider.items

import com.stultorum.architectury.mcspider.McSpiderState
import com.stultorum.architectury.mcspider.utilities.sendActionBar
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

class GaitControlItem(settings: Settings): Item(settings) {
    override fun use(world: World, player: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        world.playSound(player, player.blockPos, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 1f, 2f)
        McSpiderState.gallop = !McSpiderState.gallop
        sendActionBar(player, if (!McSpiderState.gallop) "Walk mode" else "Gallop mode")
        return TypedActionResult.success(player.getStackInHand(hand), true)
    }
}