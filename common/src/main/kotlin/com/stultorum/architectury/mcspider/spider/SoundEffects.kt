package com.stultorum.architectury.mcspider.spider

import com.stultorum.architectury.mcspider.utilities.port.playSound
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import java.io.Closeable

private val soundCategory = SoundCategory.NEUTRAL

class SoundEffects(val spider: Spider) : SpiderComponent {
    var closeables = mutableListOf<Closeable>()

    override fun close() {
        closeables.forEach { it.close() }
        closeables = mutableListOf()
    }

    init {
        closeables += spider.body.onHitGround.listen {
            spider.world.playSound(spider.location.toVec3d(), SoundEvents.BLOCK_NETHERITE_BLOCK_FALL, soundCategory, 1f, .8f, true)
        }

        closeables += spider.body.onGetHitByTrident.listen {
            spider.world.playSound(spider.location.toVec3d(), SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, soundCategory, .5f, 1f, true)
        }

//        closeables += spider.cloak.onToggle.listen {
//            spider.world.playSound(spider.location.toVec3d(), SoundEvents.BLOCK_LODESTONE_PLACE, soundCategory, 1f, 0f, true)
//        }
//
//        closeables += spider.cloak.onCloakDamage.listen {
//            spider.world.playSound(spider.location.toVec3d(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), soundCategory, .5f, 1.5f, true)
//            spider.world.playSound(spider.location.toVec3d(), SoundEvents.BLOCK_LODESTONE_PLACE, soundCategory, .1f, 0f, true)
//            spider.world.playSound(spider.location.toVec3d(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, soundCategory, .02f, 1.5f, true)
//        }

        for (leg in spider.body.legs) {
            // Step sound effect
            closeables += leg.onStep.listen {
                val location = leg.endEffector
                val volume = .3f
                val pitch = 1.0f + Math.random().toFloat() * 0.1f
                spider.world.playSound(location, SoundEvents.BLOCK_NETHERITE_BLOCK_STEP, soundCategory, volume, pitch, true)
            }
        }
    }
}