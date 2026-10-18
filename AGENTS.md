# GridSpace (格间)

A PICO OS 6 Shared Space voxel editor: an entry Planar window and one Volumetric window holding the
model, with a window-level Toolbar under it. Design: Figma "格间 GRIDSPACE" section
"GRIDSPACE V2 · 空间化改版" (frames saved under `docs/design/`). Spec: `docs/DESIGN_SPEC.md`.

- Package / applicationId: `com.kejing.gridspace`
- PICO Spatial SDK BOM 6.1.9 (same Gradle setup as `C:\Users\Admin\Downloads\ElementSpace`)

## Build and run (this Windows machine)

```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
cd /c/Users/Admin/Downloads/GridSpace
./gradlew :app:assembleDebug --offline -q
./gradlew :app:testDebugUnitTest --offline -q
```

- `--offline` is mandatory: Gradle cannot reach the network from the wrapper JDK (old cacerts).
  Every dependency in `gradle/libs.versions.toml` and `app/build.gradle.kts` is already cached.
  **Do not add dependencies.** kotlinx-coroutines, lifecycle, core-ktx, appcompat and the Spatial
  BOM artifacts are available; nothing else is.
- JSON persistence: use `org.json` (in the Android platform) — no kotlinx-serialization, no Gson.
- Install / launch on the emulator (device `emulator-5554`, already booted):
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  pico-cli app launch com.kejing.gridspace
  adb exec-out screencap -p > docs/screenshots/<name>.png     # Git Bash: write to a local path
  adb logcat -d | grep -i "gridspace\|AndroidRuntime" | tail -80
  ```
  Nobody can click inside the emulator from the CLI (`input tap` does not reach spatial windows),
  so the app ships a scripted demo mode driven by a global setting:
  `adb shell settings put global gridspace_demo <script>` / `settings delete global gridspace_demo`.
  Screenshots only show the upper part of a Full stage; this app uses windows, not a stage, so
  screenshots are fine.
- Git Bash mangles `/sdcard/...` paths in `adb push`/`pull`; prefix with `MSYS_NO_PATHCONV=1`.
- Write Kotlin source files with the Write tool, not shell heredocs (backticks break).

## House rules (same as Element Space)

- All 2D UI is SpatialUI (`com.pico.spatial.ui.design.*`) inside `PicoTheme`. No Material /
  Material3 imports, no `androidx.compose.material*`.
- Use system components: `Button` (with `ButtonDefaults.buttonColors(containerColor =
  PicoTheme.colorScheme.interaction, contentColor = ...)` for the one emphasised action; there is
  no SDK `PrimaryButton`, SkyboxSpatial wraps `Button` in `ui/main/components/Controls.kt`),
  `IconButton`, `ToggleButton`, `SegmentControl`/`SegmentItem`, `Slider`, `Text`, `Icon`,
  `VerticalDivider`, window fittings `Toolbar` (window-level, under the Volume), `Augment`
  (content-sized card anchored to a face of the window), `SpatialPopup`, `SnackbarHost` +
  `LocalSnackbarHostState` (`com.pico.spatial.ui.design.windows`).
- The api-reference `.md` files list classes only, not top-level composables. To confirm a
  composable or its parameters, read `documentation/spatial-ui_*.md` or inspect the aar:
  `C:\Users\Admin\.gradle\caches\modules-2\files-2.1\com.pico.spatial.ui\design\6.1.9\*\design-6.1.9.aar`
  (unzip `classes.jar`, then `"$JAVA_HOME/bin/javap" -cp classes.jar com.pico.spatial.ui.design.ButtonKt`).
  Already unpacked for this session:
  `C:\Users\Admin\AppData\Local\Temp\claude\C--Users-Admin-Downloads\4e851491-b5f1-4dd9-9af7-6cdee172813b\scratchpad\sdk-design\classes.jar`
  and `...\scratchpad\sdk-foundation\classes.jar`. Confirmed there: `Toolbar(viewPoints: Set<ViewPoint>, …, content: RowScope.() -> Unit)`
  (ElementSpace calls it as `Toolbar { … }`), `SpatialPopup` lives in `com.pico.spatial.ui.design.windows`,
  `Slider`/`SegmentSlider` in `com.pico.spatial.ui.design.SliderKt`, `ToggleIconButton` exists.
  Opening a window: `LocalSpatialNavigator.current.openWindowContainer(id, tag = …, bundle = Bundle())`
  and `closeWindowContainer(id, tag)` (`com.pico.spatial.ui.platform.containers.LocalSpatialNavigator`).
- No app-authored borders or `backgroundMaterial`; window glass comes from the manifest / DSL.
- Hover on 2D uses `Modifier.spatialHoverEffect`; hover on 3D entities uses `HoverEffectComponent`.
- App colours live in `ui/theme/GridColors.kt`; never replace `PicoTheme.colorScheme` roles.
- State flows ViewModel -> UiState -> Screen; composables never touch storage directly.
- Per-frame 3D motion belongs in an ECS `System`, not in recomposition.
- Strings in `res/values` (English) and `res/values-zh` (Chinese), key sets identical; app name
  格间 / GridSpace. Language: `ui/theme/Localization.kt` resolves `gridspace_lang` global setting
  (QA only) > SharedPreferences `gridspace/language` (the 中/EN segment) > system locale, into
  `AppGraph.language` (StateFlow); `AppLocale` (both windows) provides resources + the Noto font
  family (`GridTypographyTheme`) and switches both windows live. No `FORCE_CHINESE` any more.
- Fonts: `assets/fonts/NotoSans-VF.ttf` (EN) / `NotoSansSC-VF.ttf` (ZH) + OFL texts, copied from
  ElementSpace; registered at 400/500/600/700 on the `wght` axis; missing asset = system font.
- Type roles are the seven in `GridType` (28/20/18/18/16/16/14 sp); nothing Bold, nothing < 14 sp.
  Tappable targets are >= 72 dp (`GridSizes`), primary buttons 80, segment items 64 in a 72 track.
- No long press anywhere (`combinedClickable` banned); every custom clickable Box has
  `Modifier.spatialHoverEffect()`.

## SDK facts verified in the 6.1 API reference (agent-vault)

Docs on disk: `C:\Users\Admin\AppData\Local\PICO\sdk\6.1\agent-vault\spatial\{documentation,api-reference,sample-snippets}`.
Grep there before inventing an API.

- Window DSL (`com.pico.spatial.ui.foundation.dsl`): `DefaultWindowContainer { }` for the launcher
  window (size/glass from `AndroidManifest.xml` meta-data), `WindowContainer(id, form =
  Form.Volumetric, defaultSize = WindowContainerSize(w, h, depth), enableMaterialBackground = false,
  worldScale = WorldScale.Dynamic) { bundle?.get... }` for the editor. Open it with
  `LocalSpatialNavigator.current` (see ElementSpace `PeriodicTableScreen.kt` for the exact call).
  Volumetric size range 960..2700 dp per axis; a Volume keeps its physical size, so 1600 dp reads
  as arm's-length.
- 3D content: `SpatialView(modifier, update = { content, _ -> }) { content, _ -> content.addEntity(root) }`.
  Entities: `Entity()`, `ModelEntity(mesh, material)`, `MeshResource.createBox(Vector3)`,
  `UnlitMaterial.create(BlendingMode.TRANSPARENT)`, `PhysicallyBasedMaterial.create()`,
  `TransformComponent` (`setPosition`, `setQuaternion`, `setScaleVector`), instancing via
  `ModelComponent.meshInstances = MeshInstancesResource.create(name, listOf(Instance(id, Transform)))`.
  Never keep a `MeshResource` across a rebuild that destroys the entities using it.
- Interaction: an entity needs `InteractableComponent()` + `CollisionComponent(listOf(ShapeResource.createBox(size)), PhysicsMaterialResource())`.
  Gestures on the `SpatialView` modifier: `detectSpatialTapGesture(context, TargetEntity.any()) { tap -> }`
  gives `tap.targetEntity: Entity?` and `tap.position: Offset3D` (**view-local pixels**, no surface
  normal); `detectSpatialDragGesture(context, targetedToEntity = TargetEntity.any()) { drag -> }`
  gives `dragAmount: Offset3D` (pixels), `inputDevicePose.rawPosition/rawRotation`, `targetEntity`;
  `detectSpatialScaleGesture(context) { it.scaleValue }`. `TargetEntity.hit(entity)` and
  `TargetEntity.any { predicate }` filter targets. `scene.rayCast(origin, dir, length,
  CollisionCastHitMode.NEAREST, CollisionGroup(CollisionGroup.COLLISION_GROUP_DEFAULT), referenceEntity)`
  returns `results[i].entity/position/normal/distance` (coordinates in the reference entity's frame).
- Pixels <-> metres: `LocalPhysicalLengthConverter.current.dpToLength(dp, LengthUnit.Meters)` and
  `LocalDensity.current` for px -> dp (see `AtomSceneView.kt`).
- `Slider(value, onValueChange, onValueChangeFinished, valueRange, sliderSpec = SliderDefaults.sliderSpec(thumbAreaSize, thumbSize, thumbPressedSize, thumbHoverSize, trackHeight), colors = SliderDefaults.sliderColors(...))`.
- `Toolbar { RowScope content }` is a window-level fitting (ElementSpace `AtomScreen.kt`);
  `Augment(anchor = NormalizedPoint3D.Right, alignment = AugmentContentAlignment.CenterLeft, offset = DpOffset3D(...)) { }`.
- R4, verified with `javap` on the 6.1.9 jars (design / platform): `TitleBar(title, modifier,
  leadingActions, trailingActions, …)`; `SegmentControl(modifier, color, itemSpace, contentPadding,
  …, content)` + `SegmentItem(selected, onClick, modifier, …, title, icon, contentPadding, gap)`;
  `Sheet(modifier, properties, onDismissRequest, …, title, leadingAction, trailingAction, bottom, content)`;
  `ListItem(headlineContent, modifier, supportingContent, leadingContent, trailingContent, colors, padding, shape)`;
  `SpatialPopup(onDismissRequest, modifier, popupPositionProvider, cornerRadius, defaultMinWidth, defaultMinHeight, …)`;
  `LocalDisableAlpha` (the system disabled alpha) is `com.pico.spatial.ui.design.LocalDisableAlpha`
  (`CompositionLocalsKt`); `Button`/`ToggleButton` take `contentPadding` and `gap` but expose no
  public default padding, so `GridSizes.ButtonPadding` (24 dp) is passed explicitly;
  `SpatialNavigator.openWindowContainer(id, tag, Bundle)` / `closeWindowContainer(id, tag)` also
  work on the manifest default window (`WindowIds.ENTRY_WINDOW`), see the window flow below.

## Editor UI scale (R5, spec §S)

- The Volume and everything attached to it (Augment, SpatialPopup, Toolbar) keep their physical
  size and do not scale with distance; the system Planar windows (entry, Sheet) do. On the 2160-px
  emulator captures one dp is ~1.27 px in the entry window but only 0.70–0.80 px on a side card at
  z 0.5 (QA R4 §1.1). **Every editor Augment / popup content is wrapped in
  `EditorScaled(scale) { }`** (`ui/editor/components/EditorScaled.kt`: provides
  `LocalDensity = Density(density × scale, fontScale)`, so sp scales too): side cards + colour
  popup `EDITOR_UI_SCALE` 1.5, target label `LABEL_UI_SCALE` 1.4, brush row `BRUSH_UI_SCALE` 1.3.
  All spec numbers stay design dp; the Augment's anchor / alignment / offset stay outside the
  wrapper, in Volume-dp. Footprints must stay ≤ 1600 Volume-dp per axis (rail 430 × 1038 design =
  645 × 1557). The system `Toolbar` content is **not** wrapped (spec S.1;
  it is invisible on the emulator, the system presumably scales it [H]). R10 (spec §T.8): the
  Toolbar holds 4 modes · undo / redo · reset · **尺寸 / 颜色 `ToggleIconButton`s** (`ic_cube` /
  `ic_palette`, 72) · projects / export (1376 design dp); the size row and the colour rail are
  **closed by default** and shown only while their toggle is checked (`EditorUiState.showSizeRow`
  / `showColorRail`, events `ToggleSizeRow` / `ToggleColorRail`, remembered per process in
  `AppGraph.panels: PanelMemory`, never persisted). Hiding the rail also clears `picker`
  (colour card / eyedropper). The rail's action row keeps only 中|EN. Demo scripts open the
  panels explicitly (`walk`: size row before `size`, rail before `vivid`; `size` / `edit` probes
  toggle first; `panels` script = workbench → size → rail → dragmove → edit, `r10.sh`).
- `SpatialPopup(defaultMinWidth/Height)` is resolved in the *unscaled* density: with the content
  under `EditorScaled(1.5)` the window stayed 560 × 624 Volume-dp and clipped the 840 × 936 content
  (R5 `edit` shots). R6 dropped the popup for an Augment (see "Colour-edit card" below); if a
  popup is ever needed again, pass the scaled size (design dp × scale) as `defaultMin*`.
- Measured R5 (`python -P docs/scripts/measure_r4.py docs/screenshots/r5 r5`; `-P` not `-I`,
  Pillow is in the user site), post-restart emulator viewpoint: side cards at z 0.28 render
  0.69–0.75 px per Volume-dp (= 1.04–1.12 px per design dp), the popup 0.73, the brush row at
  z 0.75 1.2 px/Volume-dp; swatch 94–96 px, slot hit ≈ 92–93 px, stepper 94 px, brush item
  112 px high, popup Done 87 px high; entry / Sheet buttons 2.1 px/dp (the Sheet's content width
  is ≈ 414 dp). Targets: swatch / slot ≥ 92 px, stepper ≥ 86, brush item height ≥ 86.
- **Side-card placement (R5):** with the scaled rail, spec z 0.5 / offset 32 projects the rail's
  right third out of the capture from the post-restart emulator viewpoint; `SIDE_CARD_Z` = 0.28
  with an 8 dp offset keeps both side cards ~20 px inside the frame with swatches 90–94 px
  (`docs/screenshots/r5/probe/rail-*.png`; z 0 is behind the model). The emulator viewpoint changes
  after a restart (R4 main shots were further away), so re-probe with `gridspace_rail` before
  blaming a layout.
- **Front-plane scale rule (R7/R8):** cards on the Volume's front plane (z ≥ 0.9) render at ≈ 1.6
  px/Volume-dp from the emulator viewpoint (the side cards at z 0.28 get 0.73), so they are
  **not** scaled up: `LABEL_UI_SCALE` = `FRONT_PLANE_SCALE` = 1.0 and the brush row (z 0.97) is
  unscaled too (R5's 1.3 / 1.4 / 1.5 made them 900–1240 px wide). Only the side cards (1.5) keep
  an EditorScaled factor > 1.
- **Sized brush (R8, spec §T):** the only brush is a W × H × D block (`domain/usecase/SizedBrush.kt`,
  `BrushSize` 1..min(16, grid), `EditorUiState.brushSize`, event `SizeStep(axis, ±1)`): it extends
  +X/+Y/+Z from the target cell, flips per axis when it would leave the grid, else clips; occupied
  cells are skipped (ADD) / only existing cubes removed (ERASE); a tap commits at once, a pinch-hold
  previews (`Preview.BlockAdd` mint ghost + footprint, `blocked` = coral wire when the target cell is
  occupied; `BlockErase` coral wire); a pinch-drag only moves the ghost (the R1–R7 box drag and the
  Voxel / Bar / Box brushes are gone). The brush row (`BrushRow.kt`, R9: 1062 × 80 design dp, three groups
  `宽 X [−] n [+] | 高 Y … | 深 Z …` with 24 + VerticalDivider 1 × 48 + 24 between them, spec §T.7) is an
  Augment at (0.5, 1, 0.97) BottomCenter (0, −320, 0),
  unscaled, hidden in PAINT / SELECT (which always act on 1 × 1 × 1 without changing the size) and,
  since R10, only shown while the toolbar's 尺寸 toggle is checked.
  Demo: walk step `size` (3 × 2 × 1 at (1, 0, 1), +6 cubes) replaced `bar` / `box`; the row of five
  at z 14 for the selection steps is now a silent 5 × 1 × 1 tap; probe script `gridspace_demo size`.
- **Axis guides (R11, spec §T.9):** a small gizmo outside the near (+z) edge's midpoint — origin at
  model (0, 0.04, N/2 + 0.6), X and Z bars 1.2 cells along +x / +z with arrowheads and letter chips
  0.5 cell beyond the tips, no Y. It lies outside the grid, so cubes never occlude it and it never
  covers the model. **The Volume clips scene content farther than ≈ 830–890 dp from the model
  centre**: the plate corner (848 dp) still renders, the ±4.7-cell edge bars (≤ 770 dp) rendered,
  but a gizmo beyond the plate corner (≥ 896 dp) vanished completely — keep every 3D marker
  inside ≈ 800 dp (10.6 cells of a 16 grid). History: full plate-edge bars → near-edge bars (left
  the frame) → back/right-edge bars (X hidden behind the model, its chip projected onto the roof)
  → corner gizmo (clipped); all rejected or invisible. `AxisGuides`
  pure geometry → `SceneParts.axisGuides` one instanced entity, colour `GridColors.AxisGuide`
  #AAB9B8 opaque, lifted 0.04 cell above the plate, rebuilt in `rebuildPlate`; letters X/Z are two 56-dp `Augment` chips (`AxisChip`, `EditorScreen`) on the front plane at the
  projected arrow tips (`LabelAnchor.projectToFront`, recomputed with yaw / pitch in
  `VoxelSceneView.onAnchors`; clamped to x [0.03, 0.97], y [0.03, 0.80] — the lower part of the
  front face is below the emulator frame, so a chip left at its natural y ≈ 0.85 was cut in half). The SDK attachment path was tried and rejected on the emulator:
  `attachmentPanelComponent(size = panelSize(56.dp, 56.dp))` (foundation) + core
  `AttachmentPanelComponent` / `LookAtComponent` (`setViewerAsTarget()`, `NEGATIVE_Z`) on tip
  entities under the unscaled `root` creates three 112 × 112 px attachment windows at the right
  positions, but each renders as a blank pale square — the Compose content never shows (even with
  the components `remember`ed; a fresh instance per recomposition additionally churned the windows).
  The demo cannot orbit (yaw is view-local, no event), so a
  rotated-view capture is not possible from the CLI.
- **Target-label placement (R6/R7, replaces the R3/R5 side rules):** `LabelAnchor.place()`
  (`ui/editor/components/LabelAnchor.kt`, unit-tested) hangs the 360 × 96 design-dp card
  (`label` 20 + 24 icon / `caption` 16) *above* the target on the front plane (z 0.92,
  `BottomCenter`, grows upward): the cell-top point (cell top + 1.5 cells of air) is projected
  onto that plane along the line to a viewer 0.5 Volume depths in front of the face (offsets from
  the centre scale by (D + 0.08) / (D + 1 − z_cell)), then clamped to x ∈ [0.20, 0.58] and y ∈ [0.08, 0.97]: on the
  emulator the front plane maps to x_px ≈ 1092 + (x − 0.5) × 2560, so 0.58 keeps the 360-wide
  card (half-width 0.1125) ≈ 40 px left of the rail's projection and 0.20 keeps it in frame
  (the spec's 0.14 / 0.80 put the box label over the rail). No card at all during a
  selection drag-move: its "ΔX · ΔY · ΔZ · release…" text is the selection panel's last line
  (`SelectionPanelState.liveHint`).
- **Colour-edit card (R6):** `SpatialPopup` has no depth parameter (javap: cornerRadius /
  defaultMinWidth / defaultMinHeight only; `SpatialPopupDefaults` width / height / corner;
  foundation `Modifier.depth()` is a content-layout modifier), its window was sized from the
  unscaled `defaultMin*` so the ×1.5 content was clipped, and at the rail's depth the model's
  near cubes poked through it. It is now a window-level `Augment` (`ColorEditCard`, fixed
  560 × 624 design dp rendered unscaled, plus a separate `EyedropperBar` Augment) at `ColorEditPlacement`
  (0.88, 0.42, 0.90) CenterRight — in front of every cube, above the plate, inside the capture
  (`EditorScreen.kt`; override `gridspace_popup`, demo script `gridspace_demo edit` opens it).
- **Spatial-window glass lag (emulator, R5):** a freshly created Augment / SpatialPopup is drawn
  with its system glass covering only part of the window (black band under the popup's Done =
  QA N4-03; the scaled rail's lower rows for the first ~40 s after a cold launch) until the
  content is drawn again. `EditorScaled` invalidates its draw at 0.5 / 1.5 / 3.5 / 7.5 / 15.5 /
  31.5 s after composition, which cleared every frame of the R5 walk; the demo waits
  `WINDOW_SETTLE_MS` (8 s since R6) between creating a label / popup and its marker and holds a
  marked state up to `ACK_TIMEOUT_MS` 40 s (cap.sh may retry 8 × ~3 s), and `r5.sh` waits
  40 s (entry, cold start) / 45 s (sample workbench) after a launch.
- SDK `Sheet` content width is fixed (~410 dp measured, 1.19–1.31 px/dp); ProjectSheet /
  ExportSheet use `fillMaxWidth()` and `weight(1f)` segment items, never a fixed 720 / 640.
- Drag-start routing is the pure `DragRouting.route()` (`ui/editor/DragRouting.kt`, unit-tested):
  in SELECT mode the plate orbits like empty space; only cubes toggle / drag-move.
- Scripts: `docs/scripts/r5.sh zh|en` (writes `docs/screenshots/r5/<lang>/r5-*.png` + `r5-run.log`),
  `r6.sh zh|en` (workbench + walk, keeps only the R6 steps); `walk.sh <out> <prefix> [script]`
  runs any ack-synced demo script (`walk`, `newproject`, `edit`, `size`); `r8.sh zh|en` is the
  full R8 set. Any
  manual re-capture must be appended to the run log: `bash docs/scripts/cap.sh <png> 5 | tee -a
  <out>/r5-run.log` (QA N4-09).

## Window flow (R4, branch A)

- The entry window closes once an editor is open and reopens when the last editor closes
  (`DESIGN_SPEC_R4.md` §N.6.A). Probe result (`docs/screenshots/probe/`): from inside the Volume,
  `closeWindowContainer("GridSpaceMainWindow", null)` closes the default window and
  `openWindowContainer("GridSpaceMainWindow", null, Bundle())` reopens it while `EditorVolume` stays
  open; the process survives. Order is always **open first, then close** (never zero windows):
  `EntryScreen` opens the Volume then closes itself; `EditorScreen` handles a project switch by
  opening the new Volume, marking the session, then closing its own; `EditorScreen.onDispose`
  reopens the entry window when `EditorSession.openProjectId` is null afterwards.
- Caveat: a reopened entry window is a new `LaunchActivity` with a fresh `EntryViewModel`, so every
  `LaunchedEffect(Unit)` there runs again. The demo auto-open is guarded by the process-wide
  `AppGraph.demoAutoOpened` flag and the recent list is re-read from the repository.
- Placement overrides (emulator): `gridspace_brush`, `gridspace_rail`, `gridspace_selpanel`,
  `gridspace_popup` (R6 colour-edit card) (`ax,ay,az,ox,oy,oz[,align]`, negative ax hides the card; `DemoMode.placement`). Defaults (R5):
  brush (0.5, 1, 0.75) BottomCenter (0, −96, 0); rail (1, 0.5, 0.28) CenterLeft (8, 0, 0);
  selection (0, yN, 0.28) CenterRight (−8, 0, 0). Delete the settings afterwards.
- Before captures on the emulator run `adb shell am force-stop com.kejing.skyboxspatial`: a leftover
  window from that app was seen in front of ours.

## Known gaps

(Append here as they are found. Verified only on the emulator; eye/hand input is unverified.)

- **ANR lesson (2026-10-06):** `MeshResource.createBox` is a blocking round trip into the spatial
  server (geometry generation). One mesh per cube (~190 cubes) stalled the main thread for seconds
  and produced "GridSpace isn't responding". `VoxelScene` now creates one unit-box mesh and one unit
  collider per scene, held alive by a hidden keeper entity, and every cube/wire/plate/ghost is a
  scaled instance of it; cubes are created at most 64 per frame. Materials are still per entity.
- A system ANR dialog survives `am force-stop` and app relaunches on the emulator and pushes the
  next Volume back; only an emulator restart (`pico-cli emulator stop` / `start --avd
  Pico_Emulator_6_1_fresh`) clears it. Check `/data/anr/` timestamps before blaming new code.
- Freshly created spatial windows (Augment, SpatialPopup, Sheet) render black for the first ~1 s on
  the emulator; screenshot at least 1.5 s after the state change (`docs/scripts/walk.sh` syncs on
  the `GridSpaceDemo` logcat tag and the `gridspace_demo_ack` handshake). An Augment whose content
  size changes is rebuilt by the system (black again), so keep Augment content fixed-size
  (`TitleAugmentContent` gives the save-status text a fixed 84 dp width) and pass changing values
  as `State` so the content lambda stays stable.
- The system places the editor Volume so its bottom Toolbar is usually below the screenshot area.
- **Brush-row Augment placement (R2 → R8):** the spec anchor (0.5, 1, 0.5) on the bottom face at
  half depth is behind the tilted plate; R2–R7 used (0.5, 1, 0.75) / −96 dp, where the plate's
  near-corner glass still covered the row's right half (user feedback on R7). R8: (0.5, 1, 0.97)
  BottomCenter (0, −320, 0), unscaled (spec T.3 says −24, but the bottom face at z 0.97 projects
  ≈ 420 px below the emulator capture, so the T.3 "raise the offset" fallback is used; z 1.0 / −340
  looks the same; z = 1 is the Volume's front face,
  `NormalizedPoint3D.Front`; `Back` is z = 0). Placements can be tried without a rebuild:
  `adb shell settings put global gridspace_brush ax,ay,az,ox,oy,oz[,align]` (align 0 BottomCenter /
  1 CenterLeft / 2 CenterRight / 3 TopCenter; a negative ax hides the row), read once per editor
  composition (`DemoMode.placement`). Delete the setting afterwards.
- **Side-card depth (R4 → R5):** the spec's rail / selection-panel anchor z = 0.75 projects both
  cards past the emulator's screen edge from the default viewpoint (rail's 4th swatch column and
  the right half of the colour popup cut, panel's axis letters cut). R4 used z = 0.5
  (`docs/screenshots/r4/rail/montage.png`); with the R5 scaled cards it is z = 0.28 / offset 8
  (see "Editor UI scale"). Re-check 0.5–0.75 on a headset with `gridspace_rail`.
- **Walk targets must sit over free plate (R4):** the footprint highlight lies on the base plate,
  so a ghost above a floor cube (e.g. (3, 2, 5) of the sample) shows no rim; the walk uses the free
  strip x < 3 / z > 12 for ghost (1, 2, 2), occupied (1, 0, 0), bar (0, 0..3, 8) and the box row
  (8..12, 0, 14). Cells behind the beams (12, 0, 2) are invisible from the default camera.
- Fonts: K4.3 forbids the `FontWeight.Bold/SemiBold/W600/W700` names while Q.2 wants 600 / 700
  registered for SDK styles; the family uses `FontWeight(600)` / `FontWeight(700)` literals.
- **Augment content must be one `State` (R4):** every Augment (`BrushRow`, `ColorRail`,
  `SelectionPanel`) takes a single `State<…>` built with `rememberUpdatedState`, so the content
  lambda never changes identity and the system never rebuilds the card (black for ~1 s). The brush
  row keeps the Bar-only controls laid out at alpha 0 so the card size never changes.
- **Target-label Augment placement (history):** R3 anchored the card beside the cell (1.2 cells
  up, 1 cell sideways away from the Volume centre, 0.8 cells towards the viewer; QA N-01) and R5
  flipped the side near the rail (spec S.6); both left the card behind near pillars or outside
  the capture. Superseded in R6 by `LabelAnchor` (above the target on the front plane, see
  "Editor UI scale"). Lesson kept: any push towards the viewer at the cell's depth shifts the
  card outwards/downwards on screen by perspective, which is why R6 projects onto one plane.
- **Export file-name cap (R8):** MediaStore builds unique names up to "name (32).vox"; after ~31
  demo exports of the same project the export fails ("Failed to build unique file"). Move old test
  exports out of `/sdcard/Download/GridSpace/` (e.g. to `GridSpace-archive/`) before a capture run.
- **Two emulator instances (R11):** `pico-cli emulator stop` can leave the running emulator alive and
  `start` then boots a second one (`emulator-5556` next to the user's `emulator-5554`, each with its
  own app set); every plain `adb` call then fails with "more than one device". Pin the tooling with
  `ANDROID_SERIAL=<id>` (adb, cap.sh, r5–r11 scripts) — `walk.sh` passes it to `pico-cli app launch
  -d` — and kill only the instance you started (`adb -s <id> emu kill`). Also force-stop any
  third-party app the user opened on the emulator before a capture (their windows sit in front of
  the Volume: `com.technomancer.neonlong`, `com.uaybalam.conjure` seen in R11).
- **Emulator viewpoint can drift without a restart (R8):** between R7 and R8 the head pose changed
  (uptime unchanged; probably the emulator window was dragged), moving the Volume so the rail ran off
  the frame. A `pico-cli emulator stop` / `start --avd Pico_Emulator_6_1_fresh` restored the default
  pose used since R5. Right after that restart every frame was the black quad below for ~15 min
  (28 consecutive bad captures, also with only the entry window and with all third-party apps
  stopped); a second restart plus a 90 s wait before the first capture gave clean frames.
- **Emulator giant black / white quad (R2, 2026-10-07):** after an emulator restart, roughly half
  of all `screencap` frames show a full-view black (sometimes white) rounded quad in front of the
  scene, regardless of app state: it appears with the entry window alone, with every Augment
  placement, with the brush row removed, and with ElementSpace. The app's spatial windows are
  created once (logcat `FWKWrapper, onWindowCreated`: every Augment starts at 3200 x 3200 px and is
  resized to its content), so it is the emulator's compositor, not the app, and a further restart
  did not clear it. `docs/scripts/cap.sh` therefore retries a capture until the near-black / white
  coverage is under 3 %, and the walk demo holds each marked state until the capture script
  acknowledges it (`gridspace_demo_ack`), so retries never drift into the next step.
- `adb exec-out screencap` takes 2-8 s on this emulator; never pace a demo by wall-clock sleeps
  alone, sync on the `GridSpaceDemo` markers and the ack setting (`docs/scripts/walk.sh`).
- SDK substitutions made in this app: no `accent`/`onAccent` roles exist in `ColorScheme` 6.1.9
  (Mint/OnMint are applied per component); the target label is an `Augment` whose anchor follows
  the ghost cube instead of `AttachmentPanelComponent` + `LookAtComponent`; the colour slots are
  `Box` + `clickable` (no long press anywhere since R4); X/Y/Z steppers are two
  `IconButton`s instead of `NumberField` (which opens a keyboard).
- Tap positions, device-pose rays and two-hand scale cannot be exercised on the emulator; the
  mapping lives in `ui/editor/components/ViewMapper.kt` and falls back to the top face / the
  drag-offset target when the derived point does not agree with the SDK's hit entity.
- Concurrent Gradle builds on this project (two agents at once) corrupt the Kotlin incremental
  caches: symptoms are "Unresolved reference" for every main class in the unit-test compile, or an
  APK whose dex lacks `SpatialApplication` (ClassNotFoundException at launch). Fix: delete
  `app/build/kotlin`, `app/build/tmp/kotlin-classes` and rerun with `--rerun-tasks`. Verify before
  installing by scanning **every** dex (the class moves between `classesN.dex` from build to build;
  R2 had it in `classes10.dex`, so a fixed `classes2.dex` check misreports a healthy APK):
  ```bash
  APK=app/build/outputs/apk/debug/app-debug.apk
  for d in $(unzip -Z1 "$APK" 'classes*.dex'); do unzip -p "$APK" "$d" | grep -c platform/SpatialApplication; done \
    | awk '{s+=$1} END {print "SpatialApplication refs: " s; exit (s>0 ? 0 : 1)}'
  ```
  Expect a count of 2-3 (R3: 3) and exit 0; 0 / exit 1 means the dex is broken.
- Cold start right after `adb install -r` shows the entry window with a black (not yet glass)
  background for ~10 s (QA D-12); it is the system's window material, not app content. Wait 12 s
  or more before the first screenshot.
- `EditorViewModel.Factory` now needs the untitled-name lambda and pattern (Projects sheet creates
  "Untitled k"); `EditorViewModel` takes an optional `ProjectCatalog` (recent list, delete) and a
  language sink, both defaulted so the JVM tests still construct it with `(id, store, exporter)`.
- Unit tests for `EditorViewModel` run on the JVM with `kotlinx-coroutines-test`
  (`Dispatchers.setMain(StandardTestDispatcher())`); the ViewModel depends on the `ProjectStore`
  and `Exporter` interfaces so tests pass fakes (`EditorViewModelSaveTest`).
