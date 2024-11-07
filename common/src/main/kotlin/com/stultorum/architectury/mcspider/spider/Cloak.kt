package com.stultorum.architectury.mcspider.spider

import com.stultorum.architectury.mcspider.utilities.*
import com.stultorum.architectury.mcspider.utilities.port.AngledPosition
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.Vec2f
import net.minecraft.world.World
import java.util.*
import kotlin.math.abs

@Deprecated("Doesn't support multiple players")
class Cloak(var  spider: Spider) : SpiderComponent {
    var active = false
    val onCloakDamage = EventEmitter()
    val onToggle = EventEmitter()

    private var cloakMaterial = WeakHashMap<Pair<Int, Int>, Block>()
    private var cloakGlitching = false

    init {
        spider.body.onGetHitByTrident.listen {
            if (active) onCloakDamage.emit()
            active = false
        }

        onCloakDamage.listen {
            breakCloak()
        }
    }

    override fun update() {
        for ((legIndex, leg) in spider.body.legs.withIndex()) {
            val chain = leg.chain
            for ((segmentIndex, _) in chain.segments.withIndex()) {
                val segment = legIndex to segmentIndex

                val location = (chain.segments.getOrNull(segmentIndex - 1)?.position ?: chain.root)
                val vector = chain.segments[segmentIndex].position.subtract(location)

                if (!cloakGlitching) {
                    val otherSegments = (0 until chain.segments.size).map { Pair(legIndex, it) }.filter { it != segment }

                    val centre = location.add(vector.multiply(0.5))
                    applyCloak(segment, AngledPosition(centre, Vec2f.ZERO), otherSegments)
                }
            }
        }
    }

//    fun toggleCloak() {
//        active = !active
//        onToggle.emit()
//    }

    fun getSegment(segment: Pair<Int, Int>): BlockState? {
        return cloakMaterial[segment]?.defaultState
    }


    private fun breakCloak() {
        cloakGlitching = true

        val originalMaterials = cloakMaterial.toList()

        var maxTime = 0
        for ((segment, originalBlock) in originalMaterials) {
            val scheduler = SeriesScheduler()

            fun randomSleep(min: Int, max: Int) {
                scheduler.sleep((min + Math.random() * (max - min)).toLong())
            }

            val glitchBlocks = listOf(
                Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,
//                Blocks.BLUE_GLAZED_TERRACOTTA,
                Blocks.CYAN_GLAZED_TERRACOTTA,
                Blocks.WHITE_GLAZED_TERRACOTTA,
                Blocks.GRAY_GLAZED_TERRACOTTA,
                null,
                originalBlock
            )

            randomSleep(0, 3)
            for (i in 0 until (Math.random() * 4).toInt()) {
                val transitionBlock = glitchBlocks[(Math.random() * glitchBlocks.size).toInt()]

                scheduler.run {
                    cloakMaterial[segment] = transitionBlock
                    if (Math.random() < .5) {
                        val (legIndex, segmentIndex) = segment
                        val chain = spider.body.legs[legIndex].chain
                        val location = (chain.segments.getOrNull(segmentIndex - 1)?.position ?: chain.root)
                        spider.world.spawnParticle(ParticleTypes.FISHING, location)
//                      spawnParticle(Particle.FISHING, location, (1 * Math.random()).toInt(), .3, .3, .3, 0.0)
                    }
                }

                scheduler.sleep(2L)
            }

            scheduler.run { cloakMaterial[segment] = null }

            if (Math.random() < 1.0 / 6) continue

            randomSleep(0, 3)

            for (i in 0 until  (Math.random() * 3).toInt()) {
                scheduler.run {
                    val randomBlock = originalMaterials[(Math.random() * originalMaterials.size).toInt()].second
                    cloakMaterial[segment] = randomBlock
                }

                randomSleep(5, 15)

                scheduler.run { cloakMaterial[segment] = null }
                scheduler.sleep(2L)
            }

            if (scheduler.time > maxTime) maxTime = scheduler.time.toInt()
        }

        runLater(maxTime.toLong()) {
            cloakGlitching = false
        }
    }

    private fun applyCloak(segment: Pair<Int, Int>, centre: AngledPosition, otherSegments: List<Pair<Int, Int>>) {
        val currentMaterial = cloakMaterial[segment]

//        if (!spider.cloak.active) {
//            if (currentMaterial != null) { // Was !== but that seemed strange; changed to !=
//                transitionSegmentBlock(
//                    segment,
//                    (Math.random() * 3).toInt(),
//                    (Math.random() * 3).toInt(),
//                    null
//                )
//            }
//            return
//        }

        fun groundCast(): BlockHitResult? {
            return spider.world.raycastGround(centre, DOWN_VECTOR, 5.0)
        }
        fun cast(): BlockHitResult? {
            val targetPlayer = spider.world.firstPlayer()
            if (targetPlayer != null) {
                val direction = centre.toVec3d().subtract(targetPlayer.eyePos)
                val rayCast = spider.world.raycastGround(centre, direction, 30.0)
                if (rayCast != null) return rayCast
            }
            return groundCast()
        }

        val hit = cast()
        if (hit != null) {
            val palette = getCloakPalette(spider.world.getBlockState(hit.blockPos).block)
            if (palette.isNotEmpty()) {
                val hash = abs(centre.x.toInt() + centre.z.toInt())
                val choice = palette[hash % palette.size]

                if (currentMaterial != choice) { //
                    val alreadyInPalette = palette.contains(currentMaterial)
                    val doGlitch = Math.random() < 1.0 / 2 || currentMaterial == null

                    val waitTime = if (alreadyInPalette || !doGlitch) 0 else (Math.random() * 3).toInt()
                    val glitchTime = if (alreadyInPalette || !doGlitch) 0 else (Math.random() * 3).toInt()

                    transitionSegmentBlock(segment, waitTime, glitchTime, choice)
                }
            } else {
                // take block from another segment
                val other = otherSegments
                    .firstOrNull { cloakMaterial[it] != null }
                    ?: otherSegments.firstOrNull()

                val otherMaterial = cloakMaterial[other]

                if (other != null && currentMaterial != otherMaterial) {
                    transitionSegmentBlock(
                        segment,
                        (Math.random() * 3).toInt(),
                        (Math.random() * 3).toInt(),
                        otherMaterial
                    )
                }
            }
        }
    }


    val transitioningSegments = ArrayList<Pair<Int, Int>>()
    fun transitionSegmentBlock(segment: Pair<Int, Int>, waitTime: Int, glitchTime: Int, newBlock: Block?) {
        if (transitioningSegments.contains(segment)) return
        transitioningSegments.add(segment)

        val scheduler = SeriesScheduler()
        scheduler.sleep(waitTime.toLong())
        scheduler.run {
            cloakMaterial[segment] = Blocks.GRAY_GLAZED_TERRACOTTA
        }

        scheduler.sleep(glitchTime.toLong())
        scheduler.run {
            cloakMaterial[segment] = newBlock
            transitioningSegments.remove(segment)
        }
    }
}


// TODO this should really be refactored to use tags...
fun getCloakPalette(material: Block): List<Block> {

    fun weighted(vararg pairs: Pair<Block, Int>): List<Block> {
        val list = mutableListOf<Block>()
        for ((option, weight) in pairs) {
            for (i in 0 until weight) list.add(option)
        }
        return list
    }

    val mossLike = listOf(
        Blocks.GRASS_BLOCK,
        Blocks.OAK_LEAVES,
        Blocks.AZALEA_LEAVES,
        Blocks.MOSS_BLOCK,
        Blocks.MOSS_CARPET
    )

    if (mossLike.contains(material)) {
        return weighted(Blocks.MOSS_BLOCK to 4, Blocks.GREEN_SHULKER_BOX to 1)
    }
    if (material == Blocks.DIRT || material == Blocks.COARSE_DIRT || material == Blocks.ROOTED_DIRT) {
        return listOf(Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT)
    }

    if (material == Blocks.STONE_BRICKS || material == Blocks.STONE_BRICK_SLAB || material == Blocks.STONE_BRICK_STAIRS) {
        return weighted(Blocks.STONE_BRICKS to 3, Blocks.CRACKED_STONE_BRICKS to 1, Blocks.LIGHT_GRAY_SHULKER_BOX to 1)
    }

    if (material == Blocks.OAK_LOG) {
        return listOf(Blocks.OAK_WOOD)
    }

    val spruceLike = listOf(
        Blocks.SPRUCE_LOG,
        Blocks.SPRUCE_WOOD,
        Blocks.STRIPPED_SPRUCE_LOG,
        Blocks.STRIPPED_SPRUCE_WOOD,
        Blocks.SPRUCE_PLANKS,
        Blocks.SPRUCE_SLAB,
        Blocks.SPRUCE_STAIRS,
        // Blocks.SPRUCE_FENCE,
        // Blocks.SPRUCE_FENCE_GATE,
        Blocks.SPRUCE_TRAPDOOR,
        Blocks.SPRUCE_DOOR,
        Blocks.COMPOSTER,
        Blocks.BARREL
    )

    if (spruceLike.contains(material)) {
        return weighted(Blocks.SPRUCE_PLANKS to 3, Blocks.STRIPPED_SPRUCE_WOOD to 1)
    }

    if (material == Blocks.DIRT_PATH) { // Was === but that seemed strange; changed to ==
        return listOf(Blocks.STRIPPED_OAK_WOOD)
    }

    if (material == Blocks.DEEPSLATE_TILES || material == Blocks.DEEPSLATE_BRICKS || material == Blocks.DEEPSLATE || material == Blocks.POLISHED_DEEPSLATE_SLAB) {
        return listOf(Blocks.DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES)
    }

    if (material == Blocks.SAND) { // Was === but that seemed strange; changed to ==
        return weighted(Blocks.SAND to 4, Blocks.SANDSTONE to 1)
    }

    val deepSlateLike = listOf(
        Blocks.POLISHED_DEEPSLATE,
        Blocks.POLISHED_DEEPSLATE_SLAB,
        Blocks.POLISHED_DEEPSLATE_STAIRS,
        // Blocks.POLISHED_DEEPSLATE_WALL,
        Blocks.DEEPSLATE_TILES,
        Blocks.DEEPSLATE_TILE_SLAB,
        Blocks.DEEPSLATE_TILE_STAIRS,
        // Blocks.DEEPSLATE_TILE_WALL,
        Blocks.DEEPSLATE_BRICKS,
        Blocks.DEEPSLATE_BRICK_SLAB,
        Blocks.DEEPSLATE_BRICK_STAIRS,
        // Blocks.DEEPSLATE_BRICK_WALL,
        Blocks.ANVIL,
        Blocks.POLISHED_BASALT,
        Blocks.BASALT
    )

    if (deepSlateLike.contains(material)) {
        return weighted(Blocks.POLISHED_DEEPSLATE to 3, Blocks.DEEPSLATE_TILES to 1)
    }

    val stoneLike = listOf(
        Blocks.ANDESITE,
        Blocks.ANDESITE_SLAB,
        Blocks.ANDESITE_STAIRS,
        // Blocks.ANDESITE_WALL,
        Blocks.POLISHED_ANDESITE,
        Blocks.POLISHED_ANDESITE_SLAB,
        Blocks.POLISHED_ANDESITE_STAIRS,
        Blocks.COBBLESTONE,
        Blocks.COBBLESTONE_SLAB,
        Blocks.COBBLESTONE_STAIRS,
        // Blocks.COBBLESTONE_WALL,
        Blocks.STONE,
        Blocks.STONE_SLAB,
        Blocks.STONE_STAIRS,
        Blocks.GRAVEL,
    )

    if (stoneLike.contains(material)) {
        return listOf(Blocks.ANDESITE, Blocks.ANDESITE, Blocks.GRAVEL)
    }

    if (material == Blocks.NETHERITE_BLOCK) {
        return listOf(Blocks.NETHERITE_BLOCK)
    }

    if (material == Blocks.DARK_PRISMARINE || material == Blocks.DARK_PRISMARINE_SLAB || material == Blocks.DARK_PRISMARINE_STAIRS) {
        return listOf(Blocks.DARK_PRISMARINE)
    }

    if (material == Blocks.MUD_BRICKS || material == Blocks.MUD_BRICK_SLAB || material == Blocks.MUD_BRICK_STAIRS/* || material == Blocks.MUD_BRICK_WALL*/) {
        return listOf(Blocks.MUD_BRICKS)
    }

    if (material == Blocks.RED_CONCRETE) {
        return listOf(material)
    }

    val copper = listOf(
        Blocks.WAXED_EXPOSED_COPPER,
        Blocks.WAXED_EXPOSED_CUT_COPPER,
        Blocks.WAXED_EXPOSED_COPPER_BULB,
        Blocks.WAXED_EXPOSED_CHISELED_COPPER,
        Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB,
        Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS,
        Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR,
        Blocks.LIGHTNING_ROD
    )

    if (copper.contains(material)) {
        return listOf(Blocks.WAXED_EXPOSED_COPPER, Blocks.WAXED_EXPOSED_CUT_COPPER)
    }

    if (material == Blocks.YELLOW_CONCRETE || material == Blocks.YELLOW_TERRACOTTA) {
        return listOf(Blocks.YELLOW_TERRACOTTA, Blocks.YELLOW_CONCRETE, Blocks.YELLOW_SHULKER_BOX)
    }

    val tuffLike = listOf(
        Blocks.TUFF,
        Blocks.TUFF_SLAB,
        Blocks.TUFF_STAIRS,
        Blocks.CHISELED_TUFF,
        Blocks.TUFF_BRICKS,
        Blocks.TUFF_BRICK_SLAB,
        Blocks.TUFF_BRICK_STAIRS,
        Blocks.POLISHED_TUFF,
        Blocks.POLISHED_TUFF_SLAB,
        Blocks.POLISHED_TUFF_STAIRS
    )

    if (tuffLike.contains(material)) {
        return weighted(Blocks.POLISHED_TUFF to 2, Blocks.GRAY_SHULKER_BOX to 1)
    }

    return listOf()
}


fun getCloakSkyBlock(world: World): Block {
    val time = world.time

    if (time in 13188..22812) {
        return Blocks.BLACK_CONCRETE
    }

    if (time in 12542..23460) {
        return Blocks.CYAN_CONCRETE
    }

    return Blocks.LIGHT_BLUE_CONCRETE
}