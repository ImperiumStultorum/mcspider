package com.stultorum.architectury.mcspider.neoforge;

import net.neoforged.fml.common.Mod;

import com.stultorum.architectury.mcspider.McSpider;

@Mod(ExampleMod.MOD_ID)
public final class McSpiderNeoforge {
    public McSpiderNeoforge() {
        // Run our common setup.
        ExampleMod.init();
    }
}
