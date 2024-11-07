package com.stultorum.architectury.mcspider

import com.stultorum.architectury.mcspider.items.*
import com.stultorum.architectury.mcspider.platform.tickTimers
import com.stultorum.architectury.mcspider.spider.*
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.event.events.common.TickEvent
import dev.architectury.registry.registries.RegistrarManager
import net.minecraft.block.Blocks
import net.minecraft.item.Item
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object McSpider {
    const val MOD_ID = "mcspider"

    private val manager: RegistrarManager by lazy { RegistrarManager.get(MOD_ID) }

    lateinit var instance: McSpiderInstance

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

        CommandRegistrationEvent.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(spiderCommand)
        }

        TickEvent.SERVER_POST.register { it.tickTimers() }

        val itemRegistry = manager.get(RegistryKeys.ITEM)
        itemRegistry.register(Identifier.of(MOD_ID, "come_here")) { ComeHereItem(Item.Settings().maxCount(1)) }
        itemRegistry.register(Identifier.of(MOD_ID, "debug_toggle")) { DebugToggleItem(Item.Settings().maxCount(1)) }
        itemRegistry.register(Identifier.of(MOD_ID, "gait_control")) { GaitControlItem(Item.Settings().maxCount(1)) }
        itemRegistry.register(Identifier.of(MOD_ID, "laser_pointer")) { LaserPointerItem(Item.Settings().maxCount(1)) }
        itemRegistry.register(Identifier.of(MOD_ID, "leg_toggle")) { LegToggleItem(Item.Settings().maxCount(1)) }
        itemRegistry.register(Identifier.of(MOD_ID, "renderer_control")) { RendererControlItem(Item.Settings().maxCount(1)) }
        itemRegistry.register(Identifier.of(MOD_ID, "summon_spider")) { SummonSpiderItem(Item.Settings().maxCount(1)) }

        instance.enable()
    }
}