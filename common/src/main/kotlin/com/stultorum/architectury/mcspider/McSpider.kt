package com.stultorum.architectury.mcspider

import com.stultorum.architectury.mcspider.spider.*
import net.minecraft.block.Blocks
import org.slf4j.LoggerFactory

object McSpider {
    const val MOD_ID = "mcspider"

    public lateinit var instance: McSpiderInstance

    internal val renderedBlockState = Blocks.NETHERITE_BLOCK.defaultState
    internal val options: MutableMap<String, Any> = mutableMapOf(
        "gaitWalk"   to McSpiderState.gaitWalk,
        "gaitGallop" to McSpiderState.gaitGallop,
        "debug"      to McSpiderState.debugOptions,
        "body"       to McSpiderState.bodyPlan,
        "misc"       to McSpiderState.miscOptions,
    )
    internal val defaultOptions: Map<String, () -> Any> = mapOf(
        "gaitWalk"   to { Gait.defaultWalk().apply { scale(McSpiderState.bodyPlan.storedScale) } },
        "gaitGallop" to { Gait.defaultGallop().apply { scale(McSpiderState.bodyPlan.storedScale) } },
        "debug"      to { SpiderDebugOptions() },
        "body"       to { quadrupedBodyPlan(segmentCount = 3, segmentLength = 1.0) },
        "misc"       to { MiscellaneousOptions() },
    )
    internal val bodyPlans: Map<String, (Int, Double) -> BodyPlan> = mapOf(
        "biped" to ::bipedBodyPlan,
        "quadruped" to ::quadrupedBodyPlan,
        "hexapod" to ::hexapodBodyPlan,
        "octopod" to ::octopodBodyPlan,
    )

    fun init() {
        val logger = LoggerFactory.getLogger(MOD_ID)
        instance = McSpiderInstance(logger)
    }
}