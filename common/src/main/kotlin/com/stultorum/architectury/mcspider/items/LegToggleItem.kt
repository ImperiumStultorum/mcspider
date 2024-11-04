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

class LegToggleItem(settings: Settings): Item(settings) {
    override fun use(world: World, player: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val handstack = player.getStackInHand(hand)
        val leg = McSpiderState.spider?.pointDetector?.selectedLeg
        if (leg == null) {
            world.playSound(player, player.blockPos, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 1f, 2f)
            return TypedActionResult.fail(handstack)
        }

        leg.isDisabled = !leg.isDisabled
        world.playSound(player, player.blockPos, SoundEvents.BLOCK_LANTERN_PLACE, SoundCategory.PLAYERS, 1f, 1f)
        return TypedActionResult.success(handstack, true)
    }
}