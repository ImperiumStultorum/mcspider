package com.stultorum.architectury.mcspider.utilities

import com.stultorum.architectury.mcspider.utilities.port.AngledPosition
import com.stultorum.architectury.mcspider.utilities.port.refreshPositionAndAngles
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.joml.Matrix4f
import java.io.Closeable

class ModelPart <T : Entity> (
    val clazz : Class<T>,
    val type: EntityType<T>,
    val world: World,
    val location : AngledPosition,
    val init : (T) -> Unit = {},
    val update : (T) -> Unit = {}
) {
    fun spawn(): T {
        // todo make this less cursed at some point; probably after port finished
        val entity = clazz.getConstructor(EntityType::class.java, World::class.java).newInstance(type, world)
        entity.setPosition(location.toVec3d())
        entity.setAngles(location.yaw, location.pitch)
        init(entity)
        return entity
    }
}

class Model {
    val parts = mutableMapOf<Any, ModelPart<out Entity>>()

    fun add(id: Any, part: ModelPart<out Entity>) {
        parts[id] = part
    }


    fun add(id: Any, model: Model) {
        for ((subId, part) in model.parts) {
            parts[id to subId] = part
        }
    }
}

fun blockModel(
    world: World,
    location: AngledPosition,
    init: (BlockDisplayEntity) -> Unit = {},
    update: (BlockDisplayEntity) -> Unit = {}
) = ModelPart(
    clazz = BlockDisplayEntity::class.java,
    type = EntityType.BLOCK_DISPLAY,
    world = world,
    location = location,
    init = init,
    update = update
)

fun lineModel(
    world: World,
    location: AngledPosition,
    vector: Vec3d,
    upVector: Vec3d = if (vector.x + vector.z != 0.0) UP_VECTOR else Vec3d(0.0, 0.0, 1.0),
    thickness: Float = .1f,
    interpolation: Int = 1,
    init: (BlockDisplayEntity) -> Unit = {},
    update: (BlockDisplayEntity) -> Unit = {}
) = blockModel(
    world = world,
    location = location,
    init = {
        it.setTeleportDuration(interpolation)
        it.setInterpolationDuration(interpolation)
        init(it)
    },
    update = {
        val matrix = Matrix4f().rotateTowards(vector.toVector3f(), upVector.toVector3f())
            .translate(-thickness / 2, -thickness / 2, 0f)
            .scale(thickness, thickness, vector.length().toFloat())

        applyTransformationWithInterpolation(it, matrix)
        update(it)
    }
)

fun textModel(
    world: World,
    location: AngledPosition,
    text: String,
    interpolation: Int,
    init: (TextDisplayEntity) -> Unit = {},
    update: (TextDisplayEntity) -> Unit = {},
) = ModelPart(
    clazz = TextDisplayEntity::class.java,
    type = EntityType.TEXT_DISPLAY,
    world = world,
    location = location,
    init = {
        it.setTeleportDuration(interpolation)
        it.setBillboardMode(DisplayEntity.BillboardMode.CENTER)
        init(it)
    },
    update = {
        it.setText(Text.of(text))
        update(it)
    }
)

class ModelPartRenderer<T : Entity>: Closeable {
    var entity: T? = null

    fun render(part: ModelPart<T>) {
        if (entity == null) entity = part.spawn()
        entity = entity!!.apply {
            this.refreshPositionAndAngles(part.location)
            part.update(this)
        }
    }

    fun renderIf(predicate: Boolean, template: ModelPart<T>) {
        if (predicate) render(template) else close()
    }

    override fun close() {
        entity?.remove(Entity.RemovalReason.DISCARDED)
        entity = null
    }
}

class ModelRenderer: Closeable {
    val rendered = mutableMapOf<Any, Entity>()

    private val used = mutableSetOf<Any>()

    override fun close() {
        for (entity in rendered.values) {
            entity.remove(Entity.RemovalReason.DISCARDED)
        }
        rendered.clear()
        used.clear()
    }

    fun render(model: Model) {
        for ((id, template) in model.parts) {
            renderPart(id, template)
        }

        val toRemove = rendered.keys - used
        for (key in toRemove) {
            val entity = rendered[key]!!
            entity.remove(Entity.RemovalReason.DISCARDED)
            rendered.remove(key)
        }
        used.clear()
    }

    fun <T: Entity>render(part: ModelPart<T>) {
        val model = Model().apply { add(0, part) }
        render(model)
    }

    private fun <T: Entity>renderPart(id: Any, template: ModelPart<T>) {
        used.add(id)

        val oldEntity = rendered[id]
        if (oldEntity != null) {
            // check if the entity is of the same type
            if (oldEntity.type == template.type) {
                oldEntity.refreshPositionAndAngles(template.location)
                @Suppress("UNCHECKED_CAST")
                template.update(oldEntity as T)
                return
            }

            oldEntity.remove(Entity.RemovalReason.DISCARDED)
            rendered.remove(id)
        }

        val entity = template.spawn()
        template.update(entity)
        rendered[id] = entity
    }
}


class MultiModelRenderer: Closeable {
    private val renderer = ModelRenderer()
    private var model = Model()

    fun render(id: Any, model: Model) {
        this.model.add(id, model)
    }

    fun render(id: Any, model: ModelPart<out Entity>) {
        this.model.add(id, model)
    }

    fun flush() {
        renderer.render(model)
        model = Model()
    }

    override fun close() {
        renderer.close()
    }
}