package com.stultorum.architectury.mcspider.spider

import com.stultorum.architectury.mcspider.utilities.lookingAtPoint
import org.bukkit.Location
import org.bukkit.entity.Player

// TODO port

class PointDetector(val spider: Spider) : SpiderComponent {
    var selectedLeg: Leg? = null
    var player: Player? = null

    override fun update() {
        val player = player
        selectedLeg = if (player !== null) getLeg(player.eyeLocation) else null
    }

    private fun getLeg(location: Location): Leg? {
        if (spider.location.world != location.world) return null

        val locationAsVector = location.toVector()
        val direction = location.direction
        for (leg in spider.body.legs) {
            val lookingAt = lookingAtPoint(locationAsVector, direction, leg.endEffector, spider.gait.bodyHeight * .15)
            if (lookingAt) return leg
        }
        return null
    }
}