package com.kejing.gridspace.ui.editor.components

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.ColorMath
import com.kejing.gridspace.domain.model.Vec3
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.ui.editor.Preview
import com.kejing.gridspace.ui.theme.GridColors
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.HoverEffectComponent
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.dsl.registerComponent
import com.pico.spatial.ui.foundation.dsl.registerSystem

/**
 * Owns the entity tree of the editor Volume.
 *
 * [root] never rotates; it holds [model] (rotated, zoomed, scaled to one cell = cellMeters) and the
 * invisible orbit catcher behind the model. Children of [model] are in cell units with the origin at
 * the centre of the plate's top face. Only cubes with an exposed face get an entity (spec L.2).
 *
 * All boxes share one unit mesh and one unit collider: generating geometry blocks the main thread
 * (one call per cube produced an ANR), and a hidden keeper entity holds the shared mesh so that
 * destroying cubes never releases it (ElementSpace's released-mesh crash).
 */
class VoxelScene {
    val root: Entity = Entity()
    val model: Entity = Entity()

    /** Set by the view once the SpatialView content exists; used for coordinate conversion. */
    var content: SpatialViewContent? = null

    private val voxelEntities = HashMap<Cell, ModelEntity>()
    private val entityCells = HashMap<Entity, Cell>()
    private var voxelColors = HashMap<Cell, Int>()
    private var grid: VoxelGrid? = null
    private var pendingAdds: ArrayDeque<Cell> = ArrayDeque()
    private var popCells: Set<Cell> = emptySet()

    private var plate: ModelEntity? = null
    private var lines: ModelEntity? = null
    private val catcher: Entity = Entity()
    private var ghost: ModelEntity? = null
    private var wire: ModelEntity? = null
    private var occupiedFill: ModelEntity? = null
    private var paintOverlay: ModelEntity? = null
    /** Base-plate footprint highlight and its vertical guide (spec R4 O.1). */
    private var footprint: ModelEntity? = null
    private var guide: ModelEntity? = null
    /** Everything a selection drag-move preview draws (spec R4 O.2.3). */
    private var moveHolder: Entity? = null
    private var selectionHolder = Entity()
    private var selectionShown: Set<Cell> = emptySet()
    private var previewShown: Preview? = null
    private var previewColor: Int = 0
    private var gridSize = 0
    private var wireWidth = 0.03f
    private var feedbackWireWidth = 0.07f
    /** Footprint outset in cells: 0.06 cell, but never less than ~3 dp on fine grids. */
    private var footprintOutset = 0.06f
    private var unitMesh: MeshResource? = null
    private var unitShape: ShapeResource? = null
    private var keeper: ModelEntity? = null

    init {
        ensureRegistered()
        root.addChild(model)
        root.addChild(catcher)
        model.addChild(selectionHolder)
        catcher.setName(CATCHER_NAME)
    }

    /** The shared unit box; the keeper entity under [root] is never destroyed while the scene lives. */
    private fun mesh(): MeshResource =
        unitMesh
            ?: MeshResource.createBox(Vector3(1f, 1f, 1f)).also { created ->
                unitMesh = created
                keeper = ModelEntity(created, SceneParts.unlit(Color4(0f, 0f, 0f, 0f), transparent = true)).also { k ->
                    k.components[TransformComponent::class.java]?.setScaleVector(Vector3(HIDDEN_SCALE))
                    root.addChild(k)
                }
            }

    private fun shape(): ShapeResource = unitShape ?: ShapeResource.createBox(Vector3(1f, 1f, 1f)).also { unitShape = it }

    /** Drops the shared resources so the next use recreates them (after the SDK released them). */
    private fun resetShared() {
        keeper?.destroy(true)
        keeper = null
        unitMesh = null
        unitShape = null
    }

    // ---- queries used by the gesture view --------------------------------------------------------

    fun cellOf(entity: Entity?): Cell? = entity?.let { entityCells[it] }

    fun isPlate(entity: Entity?): Boolean = entity != null && entity === plate

    fun isCatcher(entity: Entity?): Boolean = entity != null && entity === catcher

    // ---- pose ------------------------------------------------------------------------------------

    /** Places the model root and sizes the catcher; all lengths in metres. */
    fun setPose(cellMeters: Float, plateDropMeters: Float, yaw: Float, pitch: Float, zoom: Float, catcherSize: Vec3, catcherZ: Float) {
        model.components[TransformComponent::class.java]?.apply {
            setPosition(Vector3(0f, -plateDropMeters, 0f))
            setScaleVector(Vector3(cellMeters * zoom))
            setQuaternion(Quat(Vector3(1f, 0f, 0f), pitch) * Quat(Vector3(0f, 1f, 0f), yaw))
        }
        catcher.components[TransformComponent::class.java]?.setPosition(Vector3(0f, 0f, catcherZ))
        if (!catcher.components.has(com.pico.spatial.core.ecs.CollisionComponent::class.java)) {
            SceneParts.interactable(catcher, catcherSize)
        }
    }

    // ---- grid ------------------------------------------------------------------------------------

    /**
     * Brings the entities in line with [next]. Additions are spread over several calls (at most
     * [CHUNK] cubes each); returns true while more remain so the view can request another frame.
     */
    fun setGrid(next: VoxelGrid, popped: Set<Cell>): Boolean {
        if (next.size != gridSize) rebuildPlate(next.size)
        if (next !== grid) {
            grid = next
            popCells = popped
            val visible = next.visibleCells().toHashSet()
            // Remove cubes that vanished or became fully enclosed; recolour the ones that changed.
            val iterator = voxelEntities.entries.iterator()
            while (iterator.hasNext()) {
                val (cell, entity) = iterator.next()
                val color = next[cell]
                if (color == null || cell !in visible) {
                    entity.destroy(true)
                    entityCells.remove(entity)
                    iterator.remove()
                } else if (voxelColors[cell] != color) {
                    entity.destroy(true)
                    entityCells.remove(entity)
                    iterator.remove()
                    pendingAdds.addLast(cell)
                }
            }
            for (cell in visible) if (cell !in voxelEntities) pendingAdds.addLast(cell)
            voxelColors = HashMap<Cell, Int>().also { map -> for (cell in visible) map[cell] = next.getValue(cell) }
        }
        var budget = CHUNK
        val current = grid ?: return false
        while (budget > 0 && pendingAdds.isNotEmpty()) {
            val cell = pendingAdds.removeFirst()
            val color = current[cell] ?: continue
            if (cell in voxelEntities) continue
            addVoxel(cell, color, pop = cell in popCells)
            budget--
        }
        return pendingAdds.isNotEmpty()
    }

    private fun VoxelGrid.getValue(cell: Cell): Int = this[cell] ?: 0

    private fun addVoxel(cell: Cell, color: Int, pop: Boolean) {
        val entity =
            try {
                SceneParts.box(mesh(), CameraMath.cellCenter(cell, gridSize), Vec3(1f, 1f, 1f), SceneParts.cubeMaterial(color))
            } catch (e: IllegalArgumentException) {
                // The shared mesh was released underneath us: rebuild it once and retry.
                resetShared()
                SceneParts.box(mesh(), CameraMath.cellCenter(cell, gridSize), Vec3(1f, 1f, 1f), SceneParts.cubeMaterial(color))
            }
        SceneParts.interactable(entity, shape())
        entity.components.set(HoverEffectComponent())
        if (pop) {
            entity.components[TransformComponent::class.java]?.setScaleVector(Vector3(0.8f))
            entity.components.set(PopComponent())
        }
        model.addChild(entity)
        voxelEntities[cell] = entity
        entityCells[entity] = cell
    }

    private fun rebuildPlate(size: Int) {
        gridSize = size
        // Wire and line widths are given in dp and converted to cells: thinner cells get thicker
        // fractions. Selection outlines stay at the spec's 2 dp; the coral occupied / erase wires
        // use 5 dp because 2 dp was about 1.5 px at Volume distance on the emulator (QA D-04).
        val cellDp = CameraMath.MODEL_REGION_DP / size
        wireWidth = SELECTION_WIRE_DP / cellDp
        feedbackWireWidth = FEEDBACK_WIRE_DP / cellDp
        footprintOutset = maxOf(FOOTPRINT_OUTSET_CELLS, FOOTPRINT_MIN_DP / cellDp)
        plate?.destroy(true)
        lines?.destroy(true)
        voxelEntities.values.forEach { it.destroy(true) }
        voxelEntities.clear()
        entityCells.clear()
        voxelColors.clear()
        pendingAdds.clear()
        grid = null
        val n = size.toFloat()
        plate =
            SceneParts.box(
                mesh(),
                Vec3(0f, -PLATE_THICKNESS / 2f, 0f),
                Vec3(n, PLATE_THICKNESS, n),
                SceneParts.unlit(SceneParts.color4(GridColors.BaseInk, 0.7f), transparent = true),
            ).also {
                // A thin but non-zero collider: a zero-thickness box lets rays through.
                SceneParts.interactable(it, Vec3(n, PLATE_THICKNESS, n))
                model.addChild(it)
            }
        lines =
            SceneParts.gridLines(mesh(), size, 1.5f / cellDp, SceneParts.unlit(SceneParts.color4(GridColors.GridLine, 0.4f), transparent = true)).also {
                model.addChild(it)
            }
        ghost?.destroy(true)
        wire?.destroy(true)
        occupiedFill?.destroy(true)
        paintOverlay?.destroy(true)
        footprint?.destroy(true)
        guide?.destroy(true)
        moveHolder?.destroy(true)
        ghost = null
        wire = null
        occupiedFill = null
        paintOverlay = null
        footprint = null
        guide = null
        moveHolder = null
        previewShown = null
        selectionShown = emptySet()
    }

    // ---- feedback --------------------------------------------------------------------------------

    fun setPreview(preview: Preview?, activeColor: Int) {
        if (preview == previewShown && activeColor == previewColor) return
        previewShown = preview
        previewColor = activeColor
        hideFeedback()
        when (preview) {
            is Preview.Ghost ->
                if (preview.occupied) {
                    showOccupied(CameraMath.cellCenter(preview.cell, gridSize))
                    showFootprint(preview.cell, preview.cell, GridColors.Coral, FOOTPRINT_ALPHA)
                } else {
                    showGhost(CameraMath.cellCenter(preview.cell, gridSize), Vec3(1f, 1f, 1f))
                    showFootprint(preview.cell, preview.cell, GridColors.Mint, FOOTPRINT_ALPHA)
                }
            is Preview.EraseCell -> showWire(CameraMath.cellCenter(preview.cell, gridSize), Vec3(1.04f, 1.04f, 1.04f), GridColors.Coral, feedbackWireWidth)
            is Preview.SelectCell -> showWire(CameraMath.cellCenter(preview.cell, gridSize), Vec3(1.04f, 1.04f, 1.04f), GridColors.SelectionWhite, wireWidth)
            is Preview.PaintCell -> showPaint(preview.cell, preview.color)
            is Preview.BlockAdd ->
                if (preview.blocked) {
                    showWire(regionCenter(preview.region.min, preview.region.max), regionSize(preview.region.min, preview.region.max) + Vec3(0.04f, 0.04f, 0.04f), GridColors.Coral, feedbackWireWidth)
                    showFootprint(preview.region.min, preview.region.max, GridColors.Coral, FOOTPRINT_ALPHA)
                } else {
                    showGhost(regionCenter(preview.region.min, preview.region.max), regionSize(preview.region.min, preview.region.max))
                    showFootprint(preview.region.min, preview.region.max, GridColors.Mint, FOOTPRINT_ALPHA)
                }
            is Preview.BlockErase ->
                showWire(regionCenter(preview.region.min, preview.region.max), regionSize(preview.region.min, preview.region.max) + Vec3(0.04f, 0.04f, 0.04f), GridColors.Coral, feedbackWireWidth)
            is Preview.MoveSelection -> showMove(preview)
            is Preview.Outside, null -> Unit
        }
    }

    /**
     * Footprint highlight (spec R4 O.1): a thin plate on top of the base under the target cells,
     * outset so a rim shows around the cubes' own projection, plus a vertical guide column from the
     * plate to the underside of the lowest target cell when it floats (y > 0). Both are scaled
     * instances of the shared unit box; no collider, so they never catch gestures.
     */
    private fun showFootprint(min: Cell, max: Cell, color: androidx.compose.ui.graphics.Color, alpha: Float) {
        val a = CameraMath.cellCenter(min, gridSize)
        val b = CameraMath.cellCenter(max, gridSize)
        val cx = (a.x + b.x) / 2f
        val cz = (a.z + b.z) / 2f
        val w = (max.x - min.x + 1) + 2f * footprintOutset
        val d = (max.z - min.z + 1) + 2f * footprintOutset
        footprint =
            SceneParts.box(mesh(), Vec3(cx, FOOTPRINT_LIFT + FOOTPRINT_THICKNESS / 2f, cz), Vec3(w, FOOTPRINT_THICKNESS, d), SceneParts.unlit(SceneParts.color4(color, alpha), transparent = true))
                .also { model.addChild(it) }
        if (min.y > 0) {
            val h = min.y.toFloat()
            guide =
                SceneParts.box(mesh(), Vec3(cx, h / 2f, cz), Vec3(GUIDE_SIDE, h, GUIDE_SIDE), SceneParts.unlit(SceneParts.color4(color, GUIDE_ALPHA), transparent = true))
                    .also { model.addChild(it) }
        }
    }

    /**
     * Drag-move preview (spec R4 O.2.3): the originals keep their white outline and are dimmed by
     * an opaque overlay blended 50 % towards BaseInk (the paint-preview mechanism); each target
     * cell gets a mint ghost with a white outline; a white footprint covers the targets' XZ
     * extent; cubes that would be overwritten get a coral outline. Delta 0 shows only the dimming.
     */
    private fun showMove(preview: Preview.MoveSelection) {
        val holder = Entity().also { model.addChild(it) }
        moveHolder = holder
        val ghostMaterial = SceneParts.unlit(SceneParts.color4(GridColors.Mint, 0.45f), transparent = true)
        val whiteWire = SceneParts.unlit(SceneParts.color4(GridColors.SelectionWhite), transparent = false)
        val coralWire = SceneParts.unlit(SceneParts.color4(GridColors.Coral), transparent = false)
        for (cell in preview.origin) {
            val current = voxelColors[cell] ?: continue
            val dimmed = ColorMath.mix(current, BASE_INK_ARGB, MOVE_DIM_WEIGHT)
            holder.addChild(SceneParts.box(mesh(), CameraMath.cellCenter(cell, gridSize), Vec3(1.02f, 1.02f, 1.02f), SceneParts.cubeMaterial(dimmed)))
        }
        if (!preview.moving) return
        for (cell in preview.targets) {
            val center = CameraMath.cellCenter(cell, gridSize)
            holder.addChild(SceneParts.box(mesh(), center, Vec3(1f, 1f, 1f), ghostMaterial))
            holder.addChild(SceneParts.wireBox(mesh(), center, Vec3(1.04f, 1.04f, 1.04f), wireWidth, whiteWire))
        }
        for (cell in preview.overwritten) {
            holder.addChild(SceneParts.wireBox(mesh(), CameraMath.cellCenter(cell, gridSize), Vec3(1.06f, 1.06f, 1.06f), feedbackWireWidth, coralWire))
        }
        val min = Cell(preview.targets.minOf { it.x }, preview.targets.minOf { it.y }, preview.targets.minOf { it.z })
        val max = Cell(preview.targets.maxOf { it.x }, preview.targets.maxOf { it.y }, preview.targets.maxOf { it.z })
        showFootprint(min, max, GridColors.SelectionWhite, MOVE_FOOTPRINT_ALPHA)
    }

    fun setSelection(selection: Set<Cell>) {
        if (selection == selectionShown) return
        selectionShown = selection
        // Rebuild the outlines wholesale: selections are small and this keeps one code path.
        selectionHolder.destroy(true)
        val holder = Entity()
        model.addChild(holder)
        selectionHolder = holder
        for (cell in selection) {
            holder.addChild(
                SceneParts.wireBox(
                    mesh(),
                    CameraMath.cellCenter(cell, gridSize),
                    Vec3(1.04f, 1.04f, 1.04f),
                    wireWidth,
                    SceneParts.unlit(SceneParts.color4(GridColors.SelectionWhite), transparent = false),
                )
            )
        }
    }

    private fun regionCenter(min: Cell, max: Cell): Vec3 {
        val a = CameraMath.cellCenter(min, gridSize)
        val b = CameraMath.cellCenter(max, gridSize)
        return (a + b) * 0.5f
    }

    private fun regionSize(min: Cell, max: Cell): Vec3 =
        Vec3((max.x - min.x + 1).toFloat(), (max.y - min.y + 1).toFloat(), (max.z - min.z + 1).toFloat())

    private fun hideFeedback() {
        ghost?.components?.get(TransformComponent::class.java)?.setScaleVector(Vector3(HIDDEN_SCALE))
        wire?.destroy(true)
        wire = null
        occupiedFill?.destroy(true)
        occupiedFill = null
        paintOverlay?.destroy(true)
        paintOverlay = null
        footprint?.destroy(true)
        footprint = null
        guide?.destroy(true)
        guide = null
        moveHolder?.destroy(true)
        moveHolder = null
    }

    private fun showGhost(center: Vec3, size: Vec3) {
        val entity =
            ghost ?: SceneParts.box(mesh(), center, size, SceneParts.unlit(SceneParts.color4(GridColors.Mint, 0.45f), transparent = true)).also {
                model.addChild(it)
                ghost = it
            }
        entity.components[TransformComponent::class.java]?.apply {
            setPosition(SceneParts.vector(center))
            setScaleVector(SceneParts.vector(size))
        }
    }

    private fun showWire(center: Vec3, size: Vec3, color: androidx.compose.ui.graphics.Color, width: Float) {
        wire = SceneParts.wireBox(mesh(), center, size, width, SceneParts.unlit(SceneParts.color4(color), transparent = false)).also { model.addChild(it) }
    }

    /** Occupied cell: 5 dp coral wireframe plus a translucent coral fill so it reads from afar. */
    private fun showOccupied(center: Vec3) {
        showWire(center, Vec3(1.06f, 1.06f, 1.06f), GridColors.Coral, feedbackWireWidth)
        occupiedFill =
            SceneParts.box(mesh(), center, Vec3(1.05f, 1.05f, 1.05f), SceneParts.unlit(SceneParts.color4(GridColors.Coral, OCCUPIED_FILL_ALPHA), transparent = true))
                .also { model.addChild(it) }
    }

    /**
     * Paint preview: the cube shows the new colour at 70 % (spec F.2). A translucent unlit overlay
     * read as white on the emulator (QA D-14), so the overlay is an opaque shaded cube in the
     * blended colour (70 % new, 30 % current) drawn just outside the original.
     */
    private fun showPaint(cell: Cell, color: Int) {
        val current = voxelColors[cell] ?: color
        val blended = ColorMath.mix(current, color, PAINT_PREVIEW_WEIGHT)
        paintOverlay =
            SceneParts.box(mesh(), CameraMath.cellCenter(cell, gridSize), Vec3(1.02f, 1.02f, 1.02f), SceneParts.cubeMaterial(blended))
                .also { model.addChild(it) }
    }

    fun destroy() {
        voxelEntities.clear()
        entityCells.clear()
        root.destroy(true)
    }

    private companion object {
        // Entity creation still round-trips to the server; 64 per frame keeps a frame under ~16 ms.
        const val CHUNK = 64
        const val PLATE_THICKNESS = 0.1f
        const val SELECTION_WIRE_DP = 2f
        const val FEEDBACK_WIRE_DP = 5f
        const val OCCUPIED_FILL_ALPHA = 0.35f
        const val PAINT_PREVIEW_WEIGHT = 0.7f
        // Footprint highlight (spec R4 O.1): 1.12 x 0.06 x 1.12 cells, 0.03 above the plate.
        const val FOOTPRINT_OUTSET_CELLS = 0.06f
        const val FOOTPRINT_MIN_DP = 3f
        const val FOOTPRINT_THICKNESS = 0.06f
        const val FOOTPRINT_LIFT = 0.03f
        const val FOOTPRINT_ALPHA = 0.55f
        const val MOVE_FOOTPRINT_ALPHA = 0.35f
        const val GUIDE_SIDE = 0.10f
        const val GUIDE_ALPHA = 0.30f
        /** Drag-move: originals blended halfway to BaseInk (spec R4 O.2.3). */
        const val MOVE_DIM_WEIGHT = 0.5f
        const val BASE_INK_ARGB = 0xFF2D383B.toInt()
        const val HIDDEN_SCALE = 0.0001f
        const val CATCHER_NAME = "OrbitCatcher"
        var registered = false

        fun ensureRegistered() {
            if (registered) return
            registerComponent<PopComponent>()
            registerSystem<PopSystem>()
            registered = true
        }
    }
}
