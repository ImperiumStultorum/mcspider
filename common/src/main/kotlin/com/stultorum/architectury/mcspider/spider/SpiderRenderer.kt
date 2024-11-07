package com.stultorum.architectury.mcspider.spider

import com.stultorum.architectury.mcspider.utilities.MultiModelRenderer
import com.stultorum.architectury.mcspider.utilities.port.distance
import com.stultorum.architectury.mcspider.utilities.spawnParticle
import net.minecraft.particle.DustParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

// todo port?

class SpiderRenderer(val spider: Spider): SpiderComponent {
    private val renderer = MultiModelRenderer()

    override fun render() {
        renderer.render("spider", spiderModel(spider))
        if (spider.showDebugVisuals) renderer.render("debug", spiderDebugModel(spider))
        renderer.flush()
    }

    override fun close() {
        renderer.close()
    }
}

class SpiderParticleRenderer(val spider: Spider): SpiderComponent {
    override fun render() {
        renderSpider(spider)
    }

    companion object {
        fun renderTarget(world: World, location: Vec3d) {
            world.spawnParticle(DustParticleEffect(DustParticleEffect.RED, 1f), location)
        }

        fun renderSpider(spider: Spider) {
            for (leg in spider.body.legs) {
                val world = leg.spider.world
                val chain = leg.chain
                var current = chain.root

                for ((i, segment) in chain.segments.withIndex()) {
                    val thickness = (chain.segments.size - i - 1) * 0.025
                    renderLine(world, current, segment.position, thickness)
                    current = segment.position
                }
            }
        }

        fun renderLine(world: World, point1: Vec3d, point2: Vec3d, thickness: Double) {
            // todo reimpl thickness
            val gap = .05

            val amount = point1.distance(point2) / gap
            val step = point2.subtract(point1).multiply(1 / amount)

            var current = point1

            for (i in 0..amount.toInt()) {
                world.spawnParticle(ParticleTypes.BUBBLE, current)
                current = current.add(step)
            }
        }
    }
}