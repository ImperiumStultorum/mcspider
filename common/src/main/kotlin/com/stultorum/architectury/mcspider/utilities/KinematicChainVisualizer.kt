package com.stultorum.architectury.mcspider.utilities

import com.stultorum.architectury.mcspider.McSpider
import com.stultorum.architectury.mcspider.utilities.port.AngledPosition
import com.stultorum.architectury.mcspider.utilities.port.setY
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.decoration.Brightness
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import java.io.Closeable


class KinematicChainVisualizer(
    val root: AngledPosition,
    val segments: List<ChainSegment>
): Closeable {
    enum class Stage {
        Backwards,
        Forwards
    }

    val interruptions = mutableListOf<() -> Unit>()
    var iterator = 0
    var previous: Triple<Stage, Int, List<ChainSegment>>? = null
    var stage = Stage.Forwards
    var target: AngledPosition? = null

    var detailed = false
    set(value) {
        field = value
        interruptions.clear()
        render()
    }

    val renderer = ModelRenderer()
    override fun close() {
        renderer.close()
    }

    init {
        reset()
        render()
    }

    companion object {
        fun create(segments: Int, length: Double, root: AngledPosition): KinematicChainVisualizer {
            val segmentList = (0 until segments).map { ChainSegment(root.toVec3d(), length) }
            return KinematicChainVisualizer(root, segmentList)
        }
    }

    fun resetIterator() {
        interruptions.clear()
        iterator = segments.size - 1
        previous = null
        stage = Stage.Forwards
    }

    fun reset() {
        resetIterator()

        target = null

        var direction = Vec3d(0.0, 1.0, 0.0)
        val rotAxis = Vec3d(1.0, 0.0, -1.0)
        val rotation = -0.2
        var pos = root.toVec3d()
        for (segment in segments) {
            direction = direction.rotateAroundAxis(rotAxis, rotation)
            pos = pos.add(direction.multiply(segment.length))
            segment.position = pos
        }

        render()
    }

    fun step() {
        if (interruptions.isNotEmpty()) {
            interruptions.removeAt(0)()
            return
        }

        val target = target?.toVec3d() ?: return

        previous = Triple(stage, iterator, segments.map { ChainSegment(it.position, it.length) })

        if (stage == Stage.Forwards) {
            val segment = segments[iterator]
            val nextSegment = segments.getOrNull(iterator + 1)

            if (nextSegment == null) {
                segment.position = target
            } else {
                segment.position = fabrik_moveSegment(segment.position, nextSegment.position, nextSegment.length)
            }

            if (iterator == 0) stage = Stage.Backwards
            else iterator--
        } else {
            val segment = segments[iterator]
            val prevPosition = segments.getOrNull(iterator - 1)?.position ?: root.toVec3d()

            segment.position = fabrik_moveSegment(segment.position, prevPosition, segment.length)

            if (iterator == segments.size - 1) stage = Stage.Forwards
            else iterator++
        }

        render()
    }

    fun straighten(target: Vec3d) {
        resetIterator()

        val direction = target.subtract(root.toVec3d()).normalize().setY(0.5).normalize()

        var position = root.toVec3d()
        for (segment in segments) {
            position = position.add(direction.multiply(segment.length))
            segment.position = position
        }

        render()
    }

    fun fabrik_moveSegment(point: Vec3d, pullTowards: Vec3d, segment: Double): Vec3d {
        val direction = pullTowards.subtract(point).normalize()
        return pullTowards.subtract(direction.multiply(segment))
    }

    fun render() {
        if (detailed) {
            renderer.render(renderDetailed())
        } else {
            renderer.render(renderNormal())
        }

    }

    private fun renderNormal(): Model {
        val model = Model()

        val previous = previous
        for (i in segments.indices) {
            val thickness = (segments.size - i) * 1.5f/16f

            val list = if (previous == null || i == previous.second) segments else previous.third

            val segment = list[i]
            val prev = list.getOrNull(i - 1)?.position ?: root.toVec3d()
            var vector = segment.position.subtract(prev)
            if (vector != Vec3d.ZERO) vector = vector.normalize().multiply(segment.length)
            val location = AngledPosition(segment.position.subtract(vector), Vec2f.ZERO)

            model.add(i, lineModel(
                location = location,
                vector = vector,
                thickness = thickness,
                interpolation = 3,
                update = {
                    it.setBlockState(McSpider.renderedBlockState)
                    it.setBrightness(Brightness(0, 15))
                }
            ))
        }

        return model
    }

    private fun renderDetailed(subStage: Int = 0): Model {
        val model = Model()

        val previous = previous

        var renderedSegments = segments

        if (previous != null) run arrow@{
            val (stage, iterator, segments) = previous

            val arrowStart = if (stage == Stage.Forwards)
                segments.getOrNull(iterator + 1)?.position else
                segments.getOrNull(iterator - 1)?.position ?: root.toVec3d()

            if (arrowStart == null) return@arrow
            renderedSegments = segments

            if (subStage == 0) {
                interruptions += { renderDetailed(1) }
                interruptions += { renderDetailed(2) }
                interruptions += { renderDetailed(3) }
                interruptions += { renderDetailed(4) }
            }

            // stage 0: subtract vector
            var arrow = segments[iterator].position.subtract(arrowStart)

            // stage 1: normalise vector
            if (subStage >= 1) arrow = arrow.normalize()

            // stage 2: multiply by length
            if (subStage >= 2) arrow = arrow.multiply(segments[iterator].length)

            // stage 3: move segment
            if (subStage >= 3) renderedSegments = this.segments

            // stage 4: hide arrow
            if (subStage >= 4) return@arrow


            val crossProduct = if (arrow == UP_VECTOR) Vec3d(0.0, 0.0, 1.0) else
                arrow.crossProduct(UP_VECTOR).normalize()

            val arrowCenter = arrowStart
                .add(arrow.multiply(0.5))
                .add(crossProduct.rotateAroundAxis(arrow, Math.toRadians(-90.0)).multiply(.5))

            model.add("arrow_length", textModel(
                location = AngledPosition(arrowCenter, Vec2f.ZERO),
                text = String.format("%.2f", arrow.length()),
                interpolation = 3,
            ))

            model.add("arrow", arrowTemplate(
                location = AngledPosition(arrowStart, Vec2f.ZERO),
                vector = arrow,
                thickness = .101f,
                interpolation = 3,
            ))
        }

        model.add("root", pointTemplate(root, Blocks.DIAMOND_BLOCK))

        for (i in renderedSegments.indices) {
            val segment = renderedSegments[i]
            model.add("p$i", pointTemplate(AngledPosition(segment.position, Vec2f.ZERO), Blocks.EMERALD_BLOCK))

            val prev = renderedSegments.getOrNull(i - 1)?.position ?: root.toVec3d()

            val (a,b) = prev to segment.position

            model.add(i, lineModel(
                location = AngledPosition(a, Vec2f.ZERO),
                vector = b.subtract(a),
                thickness = .1f,
                interpolation = 3,
                update = {
                    it.setBlockState(Blocks.BLACK_STAINED_GLASS.defaultState)
                    it.setBrightness(Brightness(0, 15))
                }
            ))
        }

        return model
    }
}

fun pointTemplate(location: AngledPosition, block: Block) = blockModel(
    location = location,
    init = {
        it.setBlockState(block.defaultState)
        it.setTeleportDuration(3)
        it.setBrightness(Brightness(15, 15))
        it.setTransformation(centredTransform(.26f, .26f, .26f))
    }
)

fun arrowTemplate(
    location: AngledPosition,
    vector: Vec3d,
    thickness: Float,
    interpolation: Int
): Model {
    val line = lineModel(
        location = location,
        vector = vector,
        thickness = thickness,
        interpolation = interpolation,
        init = {
            it.setBlockState(Blocks.GOLD_BLOCK.defaultState)
            it.setBrightness(Brightness(0, 15))
        },
    )

    val tipLength = 0.5
    val tip = location.add(vector)
    val crossProduct = if (vector == UP_VECTOR) Vec3d(0.0, 0.0, 1.0) else
        vector.crossProduct(UP_VECTOR).normalize().multiply(tipLength)

    val tipDirection = vector.normalize().multiply(-tipLength)
    val tipRotation = 30.0


    val top = lineModel(
        location = tip,
        vector = tipDirection.rotateAroundAxis(crossProduct, Math.toRadians(tipRotation)),
        thickness = thickness,
        interpolation = interpolation,
        init = {
            it.setBlockState(Blocks.GOLD_BLOCK.defaultState)
            it.setBrightness(Brightness(0, 15))
        },
    )

    val bottom = lineModel(
        location = tip,
        vector = tipDirection.rotateAroundAxis(crossProduct, Math.toRadians(-tipRotation)),
        thickness = thickness,
        interpolation = interpolation,
        init = {
            it.setBlockState(Blocks.GOLD_BLOCK.defaultState)
            it.setBrightness(Brightness(0, 15))
        },
    )

    return Model().apply {
        add("line", line)
        add("top", top)
        add("bottom", bottom)
    }
}
