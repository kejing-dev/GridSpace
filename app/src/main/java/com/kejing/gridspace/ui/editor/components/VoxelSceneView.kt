package com.kejing.gridspace.ui.editor.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.EditMode
import com.kejing.gridspace.domain.model.Vec3
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.ui.editor.DragKind
import com.kejing.gridspace.ui.editor.DragRouting
import com.kejing.gridspace.ui.editor.EditorEvent
import com.kejing.gridspace.ui.editor.Preview
import com.kejing.gridspace.ui.editor.SceneHit
import com.kejing.gridspace.domain.model.Axis
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialDragGesture
import com.pico.spatial.ui.foundation.gesture.detectSpatialScaleGesture
import com.pico.spatial.ui.foundation.gesture.detectSpatialTapGesture
import com.pico.spatial.ui.geometry.Offset3D
import com.pico.spatial.ui.platform.LengthUnit
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter

/** Where the target label Augment goes: [anchor] in normalised Volume coordinates (0..1, y down), see [LabelAnchor]. */
data class LabelPlacement(val anchor: Vec3)

/**
 * Hosts the voxel scene and turns spatial input into [EditorEvent]s or camera motion.
 *
 * Unified input path (spec R4 P.1): PICO OS 6 switches between gaze + pinch, direct pinch, poke,
 * controller ray + trigger and controller near-field on its own, and delivers all of them through
 * the same spatial gestures. The three pointerInput scopes below (tap / drag / two-hand scale,
 * separate because of the SDK consumption rules) never read the event source: gaze+pinch /
 * directPinch / poke / controller ray / controller near-field all take the same path and behave
 * the same. Camera state is view-only and never leaves this composable.
 */
@Composable
fun VoxelSceneView(
    grid: VoxelGrid,
    mode: EditMode,
    lastCommitted: Set<Cell>,
    preview: Preview?,
    activeColor: Int,
    selection: Set<Cell>,
    resetToken: Int,
    locked: Boolean,
    labelCell: Cell?,
    selectionCenter: Cell?,
    onEvent: (EditorEvent) -> Unit,
    /**
     * Label placement, the selection anchor and the three axis-letter anchors (R11, projected arrow
     * tips), all in normalised Volume coordinates (0..1, y down).
     */
    onAnchors: (label: LabelPlacement?, selection: Vec3?, axisTips: Map<Axis, Vec3>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val converter = LocalPhysicalLengthConverter.current
    val density = LocalDensity.current
    val scene = remember { VoxelScene() }
    val emit by rememberUpdatedState(onEvent)
    val isLocked by rememberUpdatedState(locked)
    val currentMode by rememberUpdatedState(mode)
    val currentGrid by rememberUpdatedState(grid)
    val currentSelection by rememberUpdatedState(selection)

    var yaw by remember { mutableFloatStateOf(CameraMath.START_YAW) }
    var pitch by remember { mutableFloatStateOf(CameraMath.START_PITCH) }
    var zoom by remember { mutableFloatStateOf(1f) }

    DisposableEffect(scene) { onDispose { scene.destroy() } }

    // Reset view: 300 ms ease-out back to the start pose, the only motion the user does not drive.
    LaunchedEffect(resetToken) {
        if (resetToken == 0) return@LaunchedEffect
        val y0 = yaw
        val p0 = pitch
        val z0 = zoom
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val t = CameraMath.easeOut(((now - start) / 1_000_000f) / CameraMath.RESET_MS)
            yaw = y0 + (CameraMath.START_YAW - y0) * t
            pitch = p0 + (CameraMath.START_PITCH - p0) * t
            zoom = z0 + (1f - z0) * t
            if (t >= 1f) break
        }
    }

    // Grid sync is chunked (64 cubes per frame) so a large box fill never stalls one frame.
    LaunchedEffect(grid) {
        var more = scene.setGrid(grid, lastCommitted)
        while (more) {
            withFrameNanos { }
            more = scene.setGrid(grid, lastCommitted)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val n = grid.size
        val cellDp = CameraMath.MODEL_REGION_DP / n
        val cellMeters = converter.dpToLength(cellDp.dp, LengthUnit.Meters)
        val plateDrop = converter.dpToLength(CameraMath.PLATE_DROP_DP.dp, LengthUnit.Meters)
        val widthM = converter.dpToLength(maxWidth, LengthUnit.Meters)
        val heightM = converter.dpToLength(maxHeight, LengthUnit.Meters)
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val catcherSize = Vec3(converter.dpToLength(CATCHER_SIDE, LengthUnit.Meters), converter.dpToLength(CATCHER_SIDE, LengthUnit.Meters), converter.dpToLength(10.dp, LengthUnit.Meters))
        val catcherZ = -converter.dpToLength(CATCHER_DEPTH, LengthUnit.Meters)
        val rootPos = Vec3(0f, -plateDrop, 0f)

        val mapper =
            remember(n, cellMeters, widthPx, heightPx, density) {
                ViewMapper(scene, n, cellMeters, widthPx, heightPx, density.density) { converter.dpToLength(it.dp, LengthUnit.Meters) }
            }
        mapper.pose(rootPos, yaw, pitch, zoom)

        // Anchors for the window-level label and selection panel follow the camera. The label
        // card hangs above the target near the Volume's front face (R6, LabelAnchor): the cell
        // centre is projected onto that plane and clamped so the whole card stays in the face.
        LaunchedEffect(labelCell, selectionCenter, yaw, pitch, zoom, n, cellMeters) {
            fun viewOf(cell: Cell): Vec3 = CameraMath.localToView(CameraMath.cellCenter(cell, n), rootPos, yaw, pitch, cellMeters * zoom)
            fun normalised(v: Vec3): Vec3 = Vec3(0.5f + v.x / widthM, 0.5f - v.y / heightM, 0.5f + v.z / widthM)
            val label = labelCell?.let { cell -> LabelPlacement(LabelAnchor.place(normalised(viewOf(cell)), cellMeters * zoom / heightM)) }
            // R11: the X / Y / Z letter chips are Augments on the front plane, anchored at the
            // projected arrow tips (the SDK attachment panel drew a blank square on the emulator).
            val tips =
                AxisGuides.guides(n).associate { g ->
                    g.axis to LabelAnchor.projectToFront(normalised(CameraMath.localToView(g.label, rootPos, yaw, pitch, cellMeters * zoom)))
                }
            onAnchors(label, selectionCenter?.let { normalised(viewOf(it)) }, tips)
        }

        var dragKind = remember { DragKind.NONE }
        var dragTotal = remember { Offset3D.Zero }
        var rayUsable = remember { false }
        var anchorCell = remember<Cell?> { null }
        // Drag-move state (spec R4 O.2.2): axes fixed at pinch-down, plane locked once, last delta sent.
        var moveAxes = remember<CameraMath.DragAxes?> { null }
        var movePlane = remember<CameraMath.DragPlane?> { null }
        var moveDelta = remember { Cell.ZERO }

        SpatialView(
            modifier =
                Modifier.fillMaxSize()
                    // Single tap (gaze+pinch, direct pinch, poke, controller trigger or near-field
                    // press all arrive here): the mode's one-shot action on the hit cell.
                    .pointerInput(Unit) {
                        detectSpatialTapGesture(context, TargetEntity.any()) { tap ->
                            if (!isLocked) emit(EditorEvent.SceneTapped(mapper.tapHit(tap.targetEntity, tap.position)))
                        }
                    }
                    // Pinch-hold / trigger-hold drag. One callback for every input source; the
                    // start target decides the path (spec R4 O.2.1, `DragRouting`):
                    //   SELECT + a selected cube  -> MOVE: whole selection, whole cells
                    //   a cube (or, outside SELECT, the plate) -> EDIT: the mode's preview
                    //   the catcher, nothing, or the plate in SELECT -> ORBIT: camera yaw / pitch
                    .pointerInput(Unit) {
                        detectSpatialDragGesture(
                            context,
                            targetedToEntity = TargetEntity.any(),
                            onDragStart = { _, value ->
                                val cell = scene.cellOf(value.targetEntity)
                                val plate = scene.isPlate(value.targetEntity)
                                dragKind = DragRouting.route(isLocked, currentMode, cell, plate, cell != null && cell in currentSelection)
                                when (dragKind) {
                                    DragKind.NONE, DragKind.ORBIT -> Unit
                                    DragKind.MOVE -> {
                                        // Direct manipulation of the selection: the drag's view-plane
                                        // displacement is mapped to grid cells (device-pose rays give a
                                        // hit cell, not a displacement, and the hand's aim is not the gaze).
                                        dragTotal = Offset3D.Zero
                                        moveAxes = CameraMath.dragAxes(yaw, pitch)
                                        movePlane = null
                                        moveDelta = Cell.ZERO
                                        emit(EditorEvent.SelectionDragStarted(cell!!))
                                    }
                                    DragKind.EDIT -> {
                                        dragTotal = Offset3D.Zero
                                        val ray = mapper.rayHit(value.inputDevicePose)
                                        rayUsable = ray != null && mapper.validates(ray, cell, plate, currentGrid)
                                        anchorCell = mapper.fallbackAnchor(cell, currentMode)
                                        val hit = if (rayUsable) ray!! else if (cell != null) SceneHit.Voxel(cell, null) else SceneHit.Target(anchorCell!!)
                                        emit(EditorEvent.PreviewStarted(hit))
                                    }
                                }
                            },
                            onDragEnd = {
                                when (dragKind) {
                                    DragKind.EDIT -> emit(EditorEvent.Committed)
                                    DragKind.MOVE -> emit(EditorEvent.SelectionDragEnded)
                                    else -> Unit
                                }
                                dragKind = DragKind.NONE
                            },
                            onDragCancel = {
                                when (dragKind) {
                                    DragKind.EDIT -> emit(EditorEvent.PreviewCancelled)
                                    DragKind.MOVE -> emit(EditorEvent.SelectionDragCancelled)
                                    else -> Unit
                                }
                                dragKind = DragKind.NONE
                            },
                        ) { value ->
                            when (dragKind) {
                                DragKind.ORBIT -> {
                                    yaw += value.dragAmount.x * CameraMath.RADIANS_PER_PIXEL
                                    pitch = (pitch + value.dragAmount.y * CameraMath.RADIANS_PER_PIXEL).coerceIn(CameraMath.MIN_PITCH, CameraMath.MAX_PITCH)
                                }
                                DragKind.EDIT -> {
                                    dragTotal += value.dragAmount
                                    val ray = if (rayUsable) mapper.rayHit(value.inputDevicePose) else null
                                    val hit = ray ?: SceneHit.Target(mapper.offsetTarget(anchorCell ?: return@detectSpatialDragGesture, dragTotal, cellDp))
                                    emit(EditorEvent.PreviewMoved(hit))
                                }
                                DragKind.MOVE -> {
                                    dragTotal += value.dragAmount
                                    val axes = moveAxes ?: return@detectSpatialDragGesture
                                    val step =
                                        CameraMath.dragToDelta(
                                            dragTotal.x, dragTotal.y, cellDp * density.density * zoom,
                                            axes, movePlane, currentSelection, currentGrid.size,
                                        )
                                    movePlane = step.plane
                                    if (step.delta != moveDelta) {
                                        moveDelta = step.delta
                                        emit(EditorEvent.SelectionDragMoved(step.delta))
                                    }
                                }
                                DragKind.NONE -> Unit
                            }
                        }
                    }
                    // Two-hand pinch / two triggers: uniform zoom, every source alike.
                    .pointerInput(Unit) {
                        detectSpatialScaleGesture(context) { zoom = (zoom * it.scaleValue).coerceIn(CameraMath.MIN_ZOOM, CameraMath.MAX_ZOOM) }
                    },
            update = { content, _ ->
                scene.content = content
                scene.setPose(cellMeters, plateDrop, yaw, pitch, zoom, catcherSize, catcherZ)
                scene.setPreview(preview, activeColor)
                scene.setSelection(selection)
            },
        ) { content, _ ->
            scene.content = content
            scene.setPose(cellMeters, plateDrop, yaw, pitch, zoom, catcherSize, catcherZ)
            content.addEntity(scene.root)
        }
    }
}

/** Backdrop collider behind the model that catches "drag on empty space" for orbiting. */
private val CATCHER_SIDE: Dp = 1500.dp
private val CATCHER_DEPTH: Dp = 700.dp
