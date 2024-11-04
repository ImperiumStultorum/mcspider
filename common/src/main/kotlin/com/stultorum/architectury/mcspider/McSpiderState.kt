package com.stultorum.architectury.mcspider

import com.stultorum.architectury.mcspider.spider.Gait
import com.stultorum.architectury.mcspider.spider.Spider
import com.stultorum.architectury.mcspider.spider.SpiderDebugOptions
import com.stultorum.architectury.mcspider.spider.quadrupedBodyPlan
import com.stultorum.architectury.mcspider.utilities.KinematicChainVisualizer
import com.stultorum.architectury.mcspider.utilities.port.AngledPosition

object McSpiderState {
    var showDebugVisuals = true
    var gallop = false

    var spider: Spider? = null
    set (value) {
        field?.close()
        field = value
    }

    var target: AngledPosition? = null

    var chainVisualizer: KinematicChainVisualizer? = null
    set (value) {
        field?.close()
        field = value
    }

    var gaitWalk = Gait.defaultWalk()
    var gaitGallop = Gait.defaultGallop()
    var debugOptions = SpiderDebugOptions()
    var miscOptions = MiscellaneousOptions()

    var bodyPlan = quadrupedBodyPlan(segmentCount = 3, segmentLength = 1.0)

    fun createSpider(location: AngledPosition): Spider {
        location.y += gaitWalk.bodyHeight
        return Spider(location, bodyPlan)
    }

    fun update() {
        spider?.gallopGait = gaitGallop
        spider?.walkGait = gaitWalk
        spider?.debugOptions = debugOptions
        spider?.showDebugVisuals = showDebugVisuals
        spider?.gallop = gallop
    }
}

class MiscellaneousOptions {
    var showLaser = true
}