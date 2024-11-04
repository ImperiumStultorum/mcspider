package com.stultorum.architectury.mcspider.items

import com.stultorum.architectury.mcspider.McSpiderState
import com.stultorum.architectury.mcspider.utilities.port.AngledPosition
import com.stultorum.architectury.mcspider.utilities.sendActionBar
import net.minecraft.item.Item
import net.minecraft.item.ItemUsageContext
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.ActionResult
import net.minecraft.util.math.Vec2f

class SummonSpiderItem(settings: Settings): Item(settings) {
    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val player = context.player!!
        if (McSpiderState.spider == null) {
            McSpiderState.spider = McSpiderState.createSpider(AngledPosition(context.hitPos, Vec2f(player.pitch, player.yaw)))
            context.world.playSound(player, context.blockPos, SoundEvents.BLOCK_NETHERITE_BLOCK_PLACE, SoundCategory.NEUTRAL, 1f, 1f)
            sendActionBar(player, "Spider created")
        } else {
            McSpiderState.spider = null
            sendActionBar(player, "Spider removed")
        }
        return ActionResult.SUCCESS_NO_ITEM_USED
    }
}