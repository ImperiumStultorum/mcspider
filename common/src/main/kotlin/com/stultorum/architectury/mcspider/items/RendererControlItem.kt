package com.stultorum.architectury.mcspider.items

import com.stultorum.architectury.mcspider.McSpiderState
import com.stultorum.architectury.mcspider.spider.SpiderParticleRenderer
import com.stultorum.architectury.mcspider.spider.SpiderRenderer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

class RendererControlItem(settings: Settings): Item(settings) {
    override fun use(world: World, player: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val handstack = player.getStackInHand(hand)
        val spider = McSpiderState.spider ?: return TypedActionResult.fail(handstack)

        spider.renderer = if (spider.renderer is SpiderRenderer) {
            world.playSound(player, player.blockPos, SoundEvents.ENTITY_AXOLOTL_ATTACK, SoundCategory.PLAYERS, 1f, 1f)
            SpiderParticleRenderer(spider)
        } else {
            world.playSound(player, player.blockPos, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(), SoundCategory.PLAYERS, 1f, 1f)
            SpiderRenderer(spider)
        }

        return TypedActionResult.success(handstack, true)
    }
}