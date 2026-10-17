package com.kejing.gridspace.ui.editor.components

import androidx.compose.ui.graphics.Color
import com.kejing.gridspace.domain.model.Vec3
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Component
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.EntityQueryCondition
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.Material
import com.pico.spatial.core.ecs.resource.MeshInstancesResource
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Transform
import com.pico.spatial.core.math.Vector3

/** Small factories for the entities the editor scene is made of. Every entity owns its own mesh. */
internal object SceneParts {
    fun vector(v: Vec3): Vector3 = Vector3(v.x, v.y, v.z)

    fun color4(argb: Int, alpha: Float = 1f): Color4 =
        Color4(((argb shr 16) and 0xFF) / 255f, ((argb shr 8) and 0xFF) / 255f, (argb and 0xFF) / 255f, alpha)

    fun color4(color: Color, alpha: Float = 1f): Color4 = Color4(color.red, color.green, color.blue, alpha)

    /** Matte shaded cube material: no metal, high roughness, as the spec asks for user cubes. */
    fun cubeMaterial(argb: Int): Material =
        PhysicallyBasedMaterial.create().apply {
            setBaseColor(color4(argb))
            setRoughness(0.9f)
            setMetallic(0f)
        }

    fun unlit(color: Color4, transparent: Boolean): Material =
        (if (transparent) UnlitMaterial.create(BlendingMode.TRANSPARENT) else UnlitMaterial.create()).apply { setBaseColor(color) }

    /**
     * A unit cube at [position] scaled to [size]. The mesh is shared: generating box geometry is a
     * blocking round trip to the spatial server, and one call per cube stalled the main thread into
     * an ANR on the emulator.
     */
    fun box(mesh: MeshResource, position: Vec3, size: Vec3, material: Material): ModelEntity =
        ModelEntity(mesh, material).also {
            it.components[TransformComponent::class.java]?.apply {
                setPosition(vector(position))
                setScaleVector(vector(size))
            }
        }

    /** Makes an entity hit-testable for the spatial gestures with a box collider of [size]. */
    fun interactable(entity: Entity, size: Vec3) = interactable(entity, ShapeResource.createBox(vector(size)))

    /** Same with a pre-built (shared) collider shape; the entity's transform scales it. */
    fun interactable(entity: Entity, shape: ShapeResource) {
        entity.components.set(InteractableComponent())
        entity.components.set(CollisionComponent(listOf(shape), PhysicsMaterialResource()))
    }

    /**
     * Twelve thin boxes drawn as instances of one mesh: a wireframe box of [size] centred at
     * [position], with edges [width] thick. Used for occupied, erase and selection feedback.
     */
    fun wireBox(mesh: MeshResource, position: Vec3, size: Vec3, width: Float, material: Material): ModelEntity {
        val entity = ModelEntity(mesh, material)
        entity.components[TransformComponent::class.java]?.setPosition(vector(position))
        entity.components[ModelComponent::class.java]?.meshInstances = MeshInstancesResource.create("wire", edges(size, width))
        return entity
    }

    fun setWireSize(entity: ModelEntity, size: Vec3, width: Float) {
        entity.components[ModelComponent::class.java]?.meshInstances = MeshInstancesResource.create("wire", edges(size, width))
    }

    private fun edges(size: Vec3, w: Float): List<MeshInstancesResource.Instance> {
        val hx = size.x / 2f
        val hy = size.y / 2f
        val hz = size.z / 2f
        val identity = Quat.identity()
        val list = ArrayList<MeshInstancesResource.Instance>(12)
        var i = 0
        fun add(p: Vector3, s: Vector3) {
            list.add(MeshInstancesResource.Instance("e${i++}", Transform(p, identity, s)))
        }
        for (sy in listOf(-hy, hy)) for (sz in listOf(-hz, hz)) add(Vector3(0f, sy, sz), Vector3(size.x + w, w, w))
        for (sx in listOf(-hx, hx)) for (sz in listOf(-hz, hz)) add(Vector3(sx, 0f, sz), Vector3(w, size.y + w, w))
        for (sx in listOf(-hx, hx)) for (sy in listOf(-hy, hy)) add(Vector3(sx, sy, 0f), Vector3(w, w, size.z + w))
        return list
    }

    /** Grid lines on the plate top as one instanced entity: (n + 1) lines along X and along Z. */
    fun gridLines(mesh: MeshResource, size: Int, width: Float, material: Material): ModelEntity {
        val entity = ModelEntity(mesh, material)
        val half = size / 2f
        val identity = Quat.identity()
        val instances = ArrayList<MeshInstancesResource.Instance>((size + 1) * 2)
        for (i in 0..size) {
            val offset = i - half
            instances.add(MeshInstancesResource.Instance("gx$i", Transform(Vector3(0f, width / 2f, offset), identity, Vector3(size.toFloat(), width, width))))
            instances.add(MeshInstancesResource.Instance("gz$i", Transform(Vector3(offset, width / 2f, 0f), identity, Vector3(width, width, size.toFloat()))))
        }
        entity.components[ModelComponent::class.java]?.meshInstances = MeshInstancesResource.create("grid", instances)
        return entity
    }
}

/** Marks a freshly placed cube; [PopSystem] grows it from 0.8 to 1.0 over 150 ms. */
class PopComponent(val startNanos: Long = java.lang.System.nanoTime()) : Component() {
    override fun clone(): Component = PopComponent(startNanos)
}

/** Per-frame scale animation for new cubes; runs in the ECS, never in recomposition. */
class PopSystem : System() {
    override fun update(context: SceneUpdateContext) {
        super.update(context)
        val now = java.lang.System.nanoTime()
        val entities = context.scene.queryEntity(EntityQueryCondition.hasComponent(PopComponent::class.java))
        for (entity in entities) {
            val pop = entity.components[PopComponent::class.java] ?: continue
            val t = ((now - pop.startNanos) / 1_000_000f) / POP_MS
            val s = if (t >= 1f) 1f else 0.8f + 0.2f * CameraMath.easeOut(t)
            entity.components[TransformComponent::class.java]?.setScaleVector(Vector3(s))
            if (t >= 1f) entity.components.remove(PopComponent::class.java)
        }
    }

    companion object {
        const val POP_MS = 150f
    }
}
