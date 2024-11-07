package com.stultorum.architectury.mcspider.spider

import com.stultorum.architectury.mcspider.utilities.lookingAtPoint
import com.stultorum.architectury.mcspider.utilities.port.AngledPosition
import com.stultorum.architectury.mcspider.utilities.port.eyeAngledPosition
import net.minecraft.entity.player.PlayerEntity

// TODO port

class PointDetector(val spider: Spider) : SpiderComponent {
    var selectedLeg: Leg? = null
    var player: PlayerEntity? = null

    override fun update() {
        val player = player
        selectedLeg = if (player != null) getLeg(player.eyeAngledPosition()) else null
    }

    private fun getLeg(location: AngledPosition): Leg? {
        val locationAsVector = location.toVec3d()
        val direction = location.toDirection()
        for (leg in spider.body.legs) {
            val lookingAt = lookingAtPoint(locationAsVector, direction, leg.endEffector, spider.gait.bodyHeight * .15)
            if (lookingAt) return leg
        }
        return null
    }
}