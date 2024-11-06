package com.stultorum.architectury.mcspider.quilt

import com.stultorum.architectury.mcspider.fabriclike.McSpiderFabriclike
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer

object McSpiderQuilt: ModInitializer {
    override fun onInitialize(mod: ModContainer) {
        McSpiderFabriclike.init()
    }
}