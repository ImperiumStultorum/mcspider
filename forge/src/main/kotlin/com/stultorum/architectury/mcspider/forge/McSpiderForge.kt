package com.stultorum.architectury.mcspider.forge

import dev.architectury.platform.forge.EventBuses
import net.examplemod.ExampleMod
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.forge.MOD_CONTEXT

@Mod(McSpider.MOD_ID)
object McSpiderForge {
    init {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(McSpider.MOD_ID, MOD_BUS)
        McSpider.init()
    }
}