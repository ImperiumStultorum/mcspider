package com.stultorum.architectury.mcspider.fabric

import com.stultorum.architectury.mcspider.fabriclike.McSpiderFabriclike
import net.fabricmc.api.ModInitializer


object McSpiderFabric: ModInitializer {
    override fun onInitialize() {
        McSpiderFabriclike.init()
    }
}
