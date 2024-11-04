package com.stultorum.architectury.mcspider

import com.heledron.spideranimation.spider.*
import com.heledron.spideranimation.utilities.*
import com.stultorum.architectury.mcspider.spider.StayStillBehaviour
import com.stultorum.architectury.mcspider.spider.targetModel
import com.stultorum.architectury.mcspider.utilities.*
import org.slf4j.Logger
import java.io.Closeable

@Suppress("unused")
class McSpiderInstance(private val logger: Logger) {
    private val closables = mutableListOf<Closeable>()
    var enabled = false
        private set

    fun toggle(): Boolean {
        if (enabled) { disable() } else { enable() }
        return enabled
    }

    fun disable() {
        if (!enabled) throw IllegalStateException()
        logger.info("Disabling Spider")
        closables.forEach { it.close() }
        enabled = false
    }

    fun enable() {
        if (enabled) throw IllegalStateException()
        enabled = true
        val renderer = MultiModelRenderer()

        closables += Closeable {
            McSpiderState.spider?.close()
            McSpiderState.chainVisualizer?.close()
            renderer.close()
        }

        logger.info("Enabling Spider")

        registerItems()

        interval(0, 1) {
            McSpiderState.update()

            McSpiderState.spider?.let { spider ->
                spider.update()
                if (spider.mount.getRider() == null) spider.behaviour = StayStillBehaviour(spider)
            }

            (if (McSpiderState.miscOptions.showLaser) McSpiderState.target else null ?: McSpiderState.chainVisualizer?.target)?.let { target ->
                renderer.render("target", targetModel(target))
            }


            renderer.flush()
            McSpiderState.target = null
        }

        // /summon minecraft:area_effect_cloud -26 -11 26 {Tags:["spider.chain_visualizer"]}
        // todo after finishing util
        closables += onSpawnEntity { entity, _ ->
            if (!entity.scoreboardTags.contains("spider.chain_visualizer")) return@onSpawnEntity
            val location = entity.location
            McSpiderState.chainVisualizer = if (McSpiderState.chainVisualizer != null) null else KinematicChainVisualizer.create(3, 1.5, location)
            entity.remove()
        }

        // TODO config (ono)
        config.getConfigurationSection("body_plan")?.getValues(true)?.let { println(it) }

        config.getConfigurationSection("walk_gait")?.getValues(true)?.let { AppState.walkGait = Serializer.fromMap(it, Gait::class.java) }
        config.getConfigurationSection("gallop_gait")?.getValues(true)?.let { AppState.gallopGait = Serializer.fromMap(it, Gait::class.java) }
        config.getConfigurationSection("options")?.getValues(true)?.let { AppState.debugOptions = Serializer.fromMap(it, SpiderDebugOptions::class.java) }
        config.getConfigurationSection("body_plan")?.getValues(true)?.let { AppState.bodyPlan = Serializer.fromMap(it, BodyPlan::class.java) }
    }
}