# GridSpace 格间 · 研发报告 R4

日期 2026-10-13 · SDK 6.1.9 · 包名 `com.kejing.gridspace` · 验证环境: PICO 模拟器 `Pico_Emulator_6_1_fresh` (emulator-5554, 末尾因黑块重启一次, 见 §2 备注) · 依据: `docs/DESIGN_SPEC_R4.md` §M–Q + §R (用户反馈修订) + K4.1–K4.55, `docs/R4_BRIEF.md`, `AGENTS.md`

范围: R4 全部 7 项 (尺寸体系 / 空间排布 / 吸附与直接操控 / 多模态 / 双语与 Noto / 演示脚本 / AGENTS) 加 §R 修订 (无顶部标题条, 工具栏成为唯一主栏, 侧面面板紧凑化, 默认白色, 长条笔刷, UX 守则)。截图目录 `docs/screenshots/r4/zh/` 与 `docs/screenshots/r4/en/` (各由 `bash docs/scripts/r4.sh zh|en` 一次生成, `small/` 为缩放证据, `rail/` 为侧卡摆位对比), 日志 `r4-logcat-walk.txt` / `r4-logcat-export.txt` 两套均无 `AndroidRuntime` / `FATAL` / ANR, `/data/anr/` 无新 trace (`r4-anr-ls.txt`, 最新仍为 10-07 15:27)。

## 0. 探针结果与所选方案

探针 (`docs/screenshots/probe/`, 上一轮) 证明: 在 Volume 内 `closeWindowContainer("GridSpaceMainWindow", null)` 能关闭 Manifest 默认窗, 之后 `openWindowContainer("GridSpaceMainWindow", null, Bundle())` 能重开它, `EditorVolume` 全程保持, 进程存活 → **方案 A**。实现:

| 环节 | 代码 | 证据 |
| --- | --- | --- |
| 入口开编辑器后关闭自身 (先开后关) | `EntryScreen.kt:71-85` `LaunchedEffect(openRequest)`: `openWindowContainer(EDITOR_VOLUME)` → `OpenHandled` → `closeWindowContainer(ENTRY_WINDOW, null)` | `r4-*-workbench.png` 画面中无入口窗 |
| 最后一个编辑器关闭 → 重开入口 | `EditorScreen.kt:130-146` `onDispose`: `editorSession.closed(id)` 后若 `openProjectId == null` 用合成期捕获的 `navigator.openWindowContainer(ENTRY_WINDOW, null, Bundle())` | `r4-*-entry-after.png` (export 脚本关闭 Volume 后入口重现, 列表 "庭院小亭 · 刚刚"); `r4-export-log.txt`: `GridSpaceEditor: last editor closed; entry window reopen requested` |
| 重开的入口是新 Activity + 新 ViewModel | `AppGraph.demoAutoOpened` (`AppGraph.kt:54`) 一次性标志守住 `EntryScreen.kt:64-70` 的 `LaunchedEffect(Unit)`; 列表由 `EntryViewModel.init` 的 `repository.refresh()` 重新读取 | export 脚本结束后入口没有再次自动打开样例 (否则会无限循环) |
| 切换项目 (ProjectSheet) | `EditorViewModelProjects.kt` `requestSwitch` → `EditorScreen.kt:158-168`: `editorSession.opened(new)` → 开新 Volume → `SwitchHandled` → 关旧 Volume; 旧编辑器 `onDispose` 的 compareAndSet 失败 → 不重开入口 | 代码路径; 模拟器无法点击 Sheet 行, 待真机 |

## 1. K4 逐条: 做了什么 → 证据

截图省略前缀 `docs/screenshots/r4/`; `zh/` 与 `en/` 同名文件内容对应。

### 尺寸 / 文字 (§M, §R.2)

| # | 做了什么 (文件:行) | 证据 |
| --- | --- | --- |
| K4.1 | `GridTypography.kt:24-56` 七个角色 title 28 M / label 20 M / body 18 R / bodyMedium 18 M / caption 16 R / captionMedium 16 M / small 14 M; `smallMedium` 删除 | `grep -c "sp" ` 无 < 14 sp; 单测与构建通过 |
| K4.2 / K4.38 | `EditorToolbar.kt:52-62` `ToolbarSizes` (模式 80 高 / 最小宽 160 / 间 12, 分隔 16+1×40+16, 图标钮 72 / 图标 28, 中/EN 144×72); `EditorToolbarContent` 顺序 = 4 模式 · 分隔 · 撤销 重做 · 分隔 · 回正 · 分隔 · 项目 导出 (`ic_folder` / `ic_upload`, contentDescription `projects` / `export`) · 分隔 · 中/EN (`:70-93`); 常量合计 40+676+33+156+33+72+33+156+33+144 = 1376 | 工具栏在模拟器截图区之外 (已知差距), 由代码代验; 真机补拍 |
| K4.3 | `grep -rE "FontWeight\.(Bold|SemiBold|W600|W700)" app/src/main` = 0。字体族按 Q.2 仍注册 600 / 700 两档供 SDK 内置样式用, 写法是 `FontWeight(600)` / `FontWeight(700)` (`GridTypography.kt:65`), 应用自身角色只用 400 / 500 | grep 结果 0 |
| K4.4 | `Controls.kt:31-45` `GridSizes` (Primary 80 / Control 72 / Icon 28 / 段控项 64 in 72); `ActionIcon` 默认 72 / 28, `PrimaryButton` 80, `SecondaryButton` 72; 色槽 80 (§R.2) / 色板 80 / `sliderSpec(thumbAreaSize 72)` (`HslSliders.kt:69`) | `grep -rnE "\b(56|48|40)\.dp"` 仅剩非可点尺寸: 导出成功图标 56、入口步骤行高 56、列表色块 56、表头 48、滑杆拇指 48 (热区 72) |
| K4.5 | **已被 §R.1 取代**: 无标题条 | — |
| K4.6 | `TargetLabel.kt:22-25` 440 × 120, 内边距 24/18, 行 1 图标 24 + `label` 20, 行 2 `caption` 16, 行间 6 | `zh/r4-ghost.png` "✓ 已对齐 · 松手放置 / X1 · Y2 · Z2"; `en/r4-ghost.png` "✓ Aligned · release to place" 一行不省略 |
| K4.7 / K4.41 | `SelectionPanel.kt:50-61` 400 宽, 瓦片 112 × 88 ×3 间 8, 步进钮 72 / 值宽 72 / 轴标 32, 末行 `selection_drag_hint` (`:101`) | `zh/r4-dragmove.png` 面板 "已选 5 块 / 旋转 复制 删除 / 网格移动 X 2 Y 0 Z −1 / 拖动已选方块可整体移动"; `en/` "Drag a selected cube to move all" |
| K4.8 | `NewProjectColumn.kt` 新建 440×80 (`PrimaryButton` 默认 80, 列宽 440 `EntryScreen.kt:40`), 网格尺寸 4 × 104 × 64 in 72, 步骤行 56 / 圆 36; `RecentProjects.kt:37-39` 行 ≥ 88 / 色块 56 r12 / 删除 72; `DeleteDialog.kt` 按钮 72 | `zh/r4-entry.png`, `en/r4-entry.png` |
| K4.9 | `ExportSheet.kt:34-37` 内容 640, 格式项 320×64 / 72, 文件名 72 (`textStyle = body`), 主钮 80, 结果图标 56, 进度 24 | `zh/r4-export.png` / `r4-exported.png` 与 `en/` 同名 |

### 空间排布 (§N, §R.1–R.2)

| # | 做了什么 | 证据 |
| --- | --- | --- |
| K4.10 / K4.39 / K4.40 | 顶部无卡片; 右侧常驻颜色栏 `ColorRail.kt` 400 宽 (`RailSizes` `:63-80`): 表头 48 (名固定 200 + 点 10 + 状态固定 120, `:148-162`) / 3 槽 80 热区 64 圆 3 dp 环 (`:182-196`) / 页签 112×64 ×3 in 352×72 / 色板 4×6 × 80 间 10 圆角 18 (`:217-247`) / 编辑颜色 352×72; 行间 12, 内边距 24 → 898 高; `EditorScreen.kt:107-108` anchor (1, 0.5, **0.5**) CenterLeft (32, 0, 0) | `zh/r4-workbench.png`: 顶部无卡片, 右栏 "庭院小亭 · 已自动保存 / 3 槽 (槽 1 白环) / 柔和|鲜艳|大地 / 24 色 / 编辑颜色", 下方笔刷行, 四者不重叠。**侧卡深度偏离规格**: z = 0.75 时颜色栏右缘在模拟器默认视点下贴着画面边缘 (第一轮截图中第 4 列色板与取色面板右半被裁), 规格 §N.2 允许回退 z = 0.5; 对比图 `rail/montage.png` = `rail/z0.75_x32.png` (规格值; 重启后的视点下整张颜色栏已在画面右侧之外) / `z0.5_x32.png` (采用, 完整可见) / `z0.25_x32.png` (过远) |
| K4.11 | 选区面板 `EditorScreen.kt:109` anchor (0, yN, 0.5) CenterRight (−32, 0, 0), 与颜色栏同时显示 | `zh/r4-dragmove.png` 左面板 + 右颜色栏 |
| K4.12 | 工具栏无色槽; `SlotLongPressed` / `PickerSlotSelected` 事件删除; `combinedClickable` 0 次 | `grep -rn "combinedClickable\|onLongClick\|SlotLongPressed" app/src/main` = 0 |
| K4.13 | `EditorViewModelColour.kt:19` `SwatchPicked` → `writeActiveSlot` (只改 `project.slots`, 不进 `History`) | `zh/r4-rail.png` (= vivid 步): 鲜艳页高亮, 24 色, 槽 1 已变为 #FF3B30 红并带白环; 之后放的 (0,0,1) 块为红色 (`r4-edit.png` 左下) |
| K4.14 | `ColorEditPopup.kt` 640 宽 / 内边距 32 / 行间 24: 标题行 72 (`edit_colour_slot` + 关闭 72), 预览 88 圆 + 十六进制 `small` 固定 96 + 从方块取色 220×72, 3 滑杆 (标签 72 / 轨 480 / 拇指 48 / 热区 72, `HslSliders.kt`), 完成 80; 锚点 = 颜色栏 "编辑颜色" 所在 `Box` (`ColorRail.kt:131-142`), `VerticalPlacement.above(16.dp)` | `zh/r4-edit.png`: "编辑颜色 · 槽 1 / #FF3B30 / 从方块取色 / 色相 饱和 明度 / 完成" 覆盖色板, 栏顶 3 槽仍可见 |
| K4.15 | `EyedropperStarted` → 同一 Popup 切为 88 高提示条 (`ColorEditPopup.kt:67-76`); `SceneTapped(Voxel)` 命中 → `pickFromCube` 写当前槽并恢复面板 (`EditorViewModel.kt:tapped`, `EditorViewModelColour.kt:pickFromCube`) | `zh/r4-eyedrop.png` 提示条 "点一个方块取色 · 取消"; walk 点 (3,1,12) 珊瑚柱 → `r4-edit-close.png` 槽 1 变回珊瑚 |
| K4.16 | 三个 Augment 内容各取一个 `State` (`rememberUpdatedState(BrushRowState / ColorRailState / SelectionPanelState)`, `EditorScreen.kt:224-271`), 固定宽高; `gridspace_rail` / `gridspace_selpanel` / `gridspace_brush` 由 `DemoMode.placement` 读取 (`DemoMode.kt:54-66`), 默认值 = §R.2 (z 改 0.5) | `zh/r4-burst-1..4.png` 连续 4 帧颜色栏表头 "已自动保存" 无黑卡; `rail/` 两张由 `gridspace_rail` 生成 |
| K4.17 | 方案 A (见 §0); `ProjectSheet.kt` 720 宽: 新建 80 / 网格尺寸 172×64 ×4 / 最近项目 ≤ 548 高滚动 / 当前项目行 trailing "正在编辑" Mint `captionMedium` 固定 96 且不可点 / 其它行删除 72 → `DeleteDialog` | `zh/r4-projects.png` "项目 / + 新建空白项目 / 网格尺寸 / 最近项目 / 庭院小亭 193 块 · 正在编辑 / 未命名 1 …"; `r4-entry-after.png` 入口重开 |
| K4.18 | [H] 系统关闭钮路径与 `onDispose` 相同 | 真机 |

### 吸附与直接操控 (§O)

| # | 做了什么 | 证据 |
| --- | --- | --- |
| K4.19 | `VoxelScene.kt:276-293` `showFootprint`: 薄盒 (w + 2×0.06, 0.06, d + 0.06×2) 格, 贴底座上方 0.03; N ≥ 32 外扩取 `max(0.06 格, 3 dp)` (`:197`); 引导柱 0.10 × y × 0.10 @30% 仅 y > 0; 幽灵 Mint @55% / 占用 Coral @55% / 长方体 & 长条 = 区域 XZ 包围 (`:248-262`); 与预览同生同灭 (`hideFeedback`) | `zh/r4-ghost.png` (1,2,2) 幽灵 + 底座薄荷落点 + 引导柱 + 标签; `zh/r4-occupied.png` (1,0,0) 珊瑚线框 + 珊瑚落点圈; walk 目标改为 (1,2,2) / (1,0,0): 规格的 (3,2,5) / (3,1,12) 落在样例地板方块之上, 落点被地板盖住看不见 (第一轮截图确认后改) |
| K4.20 | `VoxelSceneView.kt:45-56` 文件注释 + `:166-176` 回调注释: "gaze+pinch / directPinch / poke / 手柄射线 / 手柄近场 走同一路径"; 选择模式分流 `:182-205`: 命中已选格 → `SelectionDragStarted`; 其它方块 / 底座 → `PreviewStarted`; 空白 → ORBIT | 代码 |
| K4.21 | `EditorViewModel.kt:255-272` `dragStarted` → `Preview.MoveSelection(origin, Δ0)`, `dragMoved` 再夹取 (`SelectionOps.clampDelta`) + `targets` + `overwritten`; `VoxelScene.kt:300-323` `showMove`: 原块 50% 混 BaseInk 不透明覆盖 + 白线框保留, 目标格 Mint @45% + 白线框, 白落点 @35%, 覆盖格 Coral 5 dp 线框; 标签 `EditorScreen.kt:345-355` `label_move_delta` (`signed()` 用 U+2212) / `label_move_hint` / `label_move_release` / `label_move_overwrite`; 面板值 `translation + dragDelta` (`:258`), 拖动中步进禁用 | `zh/r4-dragmove.png`: 变暗原块 (8..12,0,14) + 薄荷幽灵 (10..14,0,13) + 白落点 + "ΔX +2 · ΔY 0 · ΔZ −1 / 松手移动 5 块" + 面板 2 / 0 / −1; `en/` "Release to move 5 cubes" |
| K4.22 | `dragEnded` (`:274-287`): `SelectionOps.translate` → `commit` 1 步, 新位置成为选区, `translation += Δ`; Δ = 0 只清预览 | `zh/r4-dragmove-commit.png` 5 块在新位置带白框; 后续 rotate / duplicate / stepx 在新位置进行; 导出 vox `docs/export/r4-zh-newest.vox` (见 §2) |
| K4.23 | 单测 `CameraMathDragTest` 6 例: 死区 / yaw 0.60 屏幕 x → +X 且屏幕上 → −Z / LIFT Δy 2 / GROUND (1,0,−1) / 平面锁定后不变 / 贴边夹取 | `./gradlew :app:testDebugUnitTest` 51/51 |
| K4.24 | 单测 `SelectionMoveTest` 3 例: 覆盖非选区方块同一步且一次撤销复原 / Δ0 空命令 / clampDelta | 同上 |
| K4.25 | [H] | 真机 |

### 双语与字体 (§Q)

| # | 做了什么 | 证据 |
| --- | --- | --- |
| K4.26 | `values` / `values-zh` 103 / 103 键一致 (脚本比对 `en ^ zh = ∅`); 删 `slot_hint_1/2`, `colour_title`; 新增 `projects lang_zh lang_en editing_now no_other_projects edit_colour edit_colour_slot selection_drag_hint label_move_delta label_move_hint label_move_release label_move_overwrite brush_bar bar_length bar_axis`; Kotlin 内 CJK 0 处, `Text("` 0 处; 未用键仅 `app_name` (Manifest) | grep 结果 |
| K4.27 | `app/src/main/assets/fonts/` 4 文件 (APK 内 `assets/fonts/*` 已核对); `GridTypography.kt:72-93` 按语言选族, 文件缺失 / 加载异常 → `null` → 系统字体; `BuildFlags` 对象整体删除 | `unzip -Z1 app-debug.apk | grep assets/fonts` 4 条; APK 46.9 MB |
| K4.28 | `Localization.kt:40-55` `LanguageResolver.resolve`: `gridspace_lang` > SharedPreferences `gridspace/language` > 系统; `AppGraph.init` 解析一次 (`AppGraph.kt:62`) | `en/r4-entry.png` 标题 "GridSpace", "New blank project", Noto Sans 字形 (小写 g 双层); `zh/r4-entry.png` "格间 GridSpace" Noto Sans SC |
| K4.29 / K4.54 | `EditorEvent.LanguageSelected` → `AppGraph.selectLanguage` (`EditorViewModel.kt:101`); `AppLocale` 两窗共用 `AppGraph.language` 流 (`Localization.kt:72-80`) | `zh/r4-lang-toggle.png` (= lang 步): 工作台一帧内颜色栏 "Soft|Vivid|Earth / Edit colour / Auto-saved", 笔刷行 "Voxel|Bar|Box", 卡片尺寸不变; `r4-lang-back.png` 切回中文 |
| K4.30 | `LanguageResolver.persist` 写 `SharedPreferences("gridspace").language` (`Localization.kt:52`); 冷启动无 `gridspace_lang` 时该值优先于系统语言 (`resolve` 第 2 级) | 代码 (模拟器流程全程带 `gridspace_lang`, 清掉后冷启动读偏好 = walk 最后一次 `lang-back` 的值 = 原语言) |
| K4.31 | 英文宽度: 模式钮最小宽 160 内边距 24; 项目 / 导出改图标钮; 颜色栏 "Edit colour" 352 宽; "Pick from cube" 220 宽内边距 16; 标签 440; 列表行自适应 | `en/` 全套目检: workbench / dragmove / edit / export / entry / projects 单行无省略 |

### §R 修订 (K4.38–K4.55)

| # | 做了什么 | 证据 |
| --- | --- | --- |
| K4.42 | `Palette.kt:35` `DEFAULT_SLOTS` = #FFFFFF / #A5EDCC / #F3BC9C, `activeSlot 0`; 样例用 `SAMPLE_SLOTS` (`:38`, `SampleProject.kt`) 不变; 近白描边: 槽 (`ColorDisc`), 色板 (`SwatchGrid` 近白 1 dp 内描边, 匹配环改 `BaseInk` @60%), 列表色块 (`ProjectRow`) | `zh/r4-newproject.png` (demo `newproject`): 新建项目颜色栏槽 1 白色 + 描边 + 白环, 柔和页 #f3f5ed 格带描边 |
| K4.43 | `BrushRow.kt:39-51` 905 × 72: 段控 3 项 120×64 (376) · 分隔 33 · 长度 [−] 72 / 值 72 / [+] 72 · 16 · 轴 3 × 64 (208); 非长条时后三段 `alpha 0` 且 `enabled = false` / onClick 忽略 (`:81, 86, 94, 104`), 行尺寸不变 | `zh/r4-workbench.png` (单格, 右侧空白) vs `zh/r4-bar.png` (长条: "− 4 + · X Y Z") 卡片同尺寸 |
| K4.44 | `BarFill.kt` 区域规则; VM: 预览 `BoxAdd/BoxErase(BarFill.region)` (`EditorViewModel.kt:210,217`), 单击直接提交 (`:137,145`); walk 步 `bar`: 轴 Y, 长度 4, `PreviewStarted(Target(0,0,8))` — 规格的 (12,0,2) 在默认视点被顶梁挡住, 改到地板左侧空带 (0,0,8); 4 格全空 → +4 块 | `zh/r4-bar.png`: 薄荷 1×1×4 竖条 + 底座落点 + 标签 "1 × 4 × 1 / 松手放置" + 笔刷行 "长条 · 4 · Y"; 导出核对见 §2 |
| K4.45 | `BarFillTest` 7 例 (a)–(g) | 51/51 |
| K4.46 | `brush_bar` / `bar_length` / `bar_axis` 两语言; 长度 / 轴仅 `contentDescription` (`BrushRow.kt:83-85, 97-100`) | 键集比对 |
| K4.47 | `combinedClickable` / `onLongClick` 0 次 | grep |
| K4.48 | 常驻栏: 模式 / 撤销 / 重做 / 回正 / 项目 / 导出 / 语言 = Toolbar; 笔刷 / 长度 / 轴 = 笔刷行; 当前槽 / 色板 / 预设 = 颜色栏; 二层仅 编辑颜色 / 导出 / 项目 | 代码结构 |
| K4.49 | `spatialHoverEffect` 在 `ColorSlot` (`ColorRail.kt:186`), 色板格 (`:230`), `ProjectRow` (`RecentProjects.kt:103`); SDK 组件不再额外包裹 | grep `spatialHoverEffect` 3 处 = 3 个自绘可点容器 |
| K4.50 | 默认视点下颜色栏 / 选区面板 / 笔刷行 全部在截图内 (z 0.5) | `zh/r4-dragmove.png` 三者同时完整; 工具栏仍在截图下沿外 (真机) |
| K4.51 | 撤销 / 重做常驻 Toolbar (`EditorToolbar.kt:78-80`) | — |
| K4.52 | walk: 长条 (Y, 4) → `ModeSelected(ERASE)` → `ModeSelected(ADD)` 后立即用 BOX… 修正: 回到 ADD 时 `brushes[ADD]` 仍为 BAR、`barLength 4`、`barAxis Y` (`EditorUiState.brushes` 按模式记忆, `barLength / barAxis` 会话级) | `DemoMode.kt:178-181`; 状态由代码保证 (截图未单独标记) |
| K4.53 | walk vivid 步 `SwatchPicked(#FF3B30)` 后 `SceneTapped(0,0,1)` | `zh/r4-edit.png` 左下红色方块; 导出 vox 含 #ff3b30 (§2) |
| K4.55 | `previewStarted / previewMoved / dragMoved` 同步更新 `preview` (无 delay); `VoxelSceneView.update` 同帧 `setPreview` (幽灵 + 落点 + 标签锚点) | 代码 |

### 构建 / 脚本 / 截图 (K4.32–K4.37)

| # | 结果 |
| --- | --- |
| K4.32 | 见 §2: EXIT 0, 51 单测 (R3 35 + BarFill 7 + CameraMathDrag 6 + SelectionMove 3), dex refs 3 |
| K4.33 | walk 27 标记 (`r4-demo-steps.txt`): ghost occupied bar box erase paint selection dragmove dragmove-commit rotate duplicate stepx delete vivid edit eyedrop edit-close lang lang-back undo burst redo reset export exported projects done; 两套均到 done, logcat 无 fatal / ANR |
| K4.34 | `r4.sh zh` 36 张 / `r4.sh en` 35 张 (含 burst ×4, `.try` 已清); 命名: entry / newproject / workbench / ghost / occupied / bar / box / … / rail (= vivid) / edit / eyedrop / edit-close / export / exported / projects / entry-after; `zh/r4-lang-toggle.png` (= lang, 仅 zh 套) |
| K4.35 | 每张经 `cap.sh` 黑块重试 (`r4-run.log` 记录 try 次数); 标记后 ≥ 1.5 s 再拍 (`walk.sh`) |
| K4.36 | 颜色: Coral 新增 = 占用落点 / 覆盖线框; Mint 新增 = 可放置落点 / 引导柱 / "正在编辑" 文字; SelectionWhite 新增 = 拖动落点 (`VoxelScene.kt` 常量, `ProjectRow`) |
| K4.37 | `gradle/libs.versions.toml` / `app/build.gradle.kts` 未改; 字体从 assets 读取 |

## 2. 构建 / 测试 / 导出

```
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
./gradlew :app:assembleDebug --offline -q          → EXIT 0
./gradlew :app:testDebugUnitTest --offline -q      → EXIT 0
单测 51/51: BarFill 7 · BoxFill 5 · ColorMath 3 · History 4 · SelectionMove 3 · SelectionOps 7 ·
            TargetCellResolver 7 · VoxWriter 4 · CameraMathDrag 6 · EditorViewModelSave 5; 0 失败 0 跳过
dex: SpatialApplication refs across all dex: 3  (exit 0); APK 46.9 MB (含 Noto 20 MB)
静态: material 0 · Bold/SemiBold/W600/W700 0 · combinedClickable/onLongClick 0 · CJK 硬编码 0 ·
      values/values-zh 103/103 · FORCE_CHINESE/ProbeMode/TitleAugment 0
r4.sh zh / en: 各 36 张, walk 27 标记到 done, logcat fatal/ANR 0, 设置全部清除 (settings list global | grep gridspace 为空)
```

导出核对 (`docs/scripts/voxdump.py`, 新增的 .vox 解析脚本):

```
docs/export/r4-zh-walk.vox  (walk 导出 "庭院小亭 (18).vox")   size 16³ · 193 块
  #f3f5ed 147 · #f3bc9c 25 · #a5edcc 20 · #ff3b30 1
  (0,0..3,8) = #f3bc9c ×4   ← 长条 Y / 长度 4, 槽 1 珊瑚 (K4.44: 4 格全空 → +4)
  (0,0,1)    = #ff3b30      ← 色板选色后放的 1 块 (K4.53)
  (1,0,0) (3,0,3) = #a5edcc ← 上色 2 块
  (1,2,2) 空 (幽灵放置后被擦除); (14,0,11..15) 空 (拖动 → 旋转 → 复制 → X+1 → 删除)
  计数: 184 +1 −1 +1 +4 +5 −2 (旋转覆盖 2 格地板) +5 +0 −5 +1 = 193 ✓
docs/export/r4-en-walk.vox  ("Garden Pavilion.vox")           193 块, 逐色逐格与 zh 相同 ✓
docs/export/r4-zh-newest.vox / r4-en-newest.vox (export 脚本, 样例原样) 184 块 (150 / 18 / 16) ✓
```

模拟器备注: 第二轮 `zh` 跑到 export 脚本时模拟器进入已知的 "整屏黑块" 状态 (`cap.sh` 5 次都 > 0.79, 应用已停仍如此), 按 AGENTS 重启模拟器 (`pico-cli emulator stop / start --avd Pico_Emulator_6_1_fresh`) 后用同一 export 脚本重拍了两套 `r4-entry-after.png` (重启后场景不同, 第 1 次即干净); 其余截图均来自同一轮 walk。

## 3. 未能满足 / 有偏离的条目

| 条目 | 说明 |
| --- | --- |
| 侧卡深度 (§N.2 / §R.2 z = 0.75) | 改为 z = 0.5 (`EditorScreen.kt:107`): 模拟器默认视点下 0.75 把颜色栏第 4 列、取色面板右半、选区面板左列推到画面边缘之外 (第一轮 `zh` 截图); 规格 §N.2 明文允许此回退并要求并列截图 → `rail/montage.png` (z 0.75 / 0.5 / 0.25 并列, 用 `gridspace_rail` 生成) 与本轮全套 (z 0.5)。真机可用 `gridspace_rail` / `gridspace_selpanel` 复核 0.75 |
| K4.19 / K4.44 的 walk 目标格 | 规格给的 (3,2,5) / (3,1,12) / (12,0,2) 分别落在样例地板之上 (落点被地板盖住) 与顶梁之后 (长条不可见); walk 改用 (1,2,2) / (1,0,0) / (0,0,8), 规则与视觉不变 |
| K4.2 / K4.39 工具栏 | 模拟器截图区看不到 Volume 底边 (R1 起已知), 1376 宽由常量代验; 真机补拍 |
| K4.3 与 §Q.2 字重冲突 | Q.2 要求注册 600 / 700, K4.3 禁止 `FontWeight.Bold/SemiBold/W600/W700` 字面量 → 用 `FontWeight(600)` / `FontWeight(700)` 注册, 应用角色全部 400 / 500 |
| 中/EN 后 `gridspace_lang` 再读 (§Q.1 "每次切换后再读一次") | 未按字面实现: 若切换后再读设置, 设置值会立刻覆盖用户选择, K4.29 的 walk `lang` 步在 `gridspace_lang zh` 下就无法切到 EN。实现为: 设置只在冷启动解析时优先 (`resolve`), 会话内用户 / 脚本切换直接生效并持久化 |
| 目标标签与颜色栏重叠 | 目标在视野右半时标签卡向右伸, 可能压在颜色栏左缘上 (`zh/r4-dragmove.png` 右侧); 标签在前方, 可读; 可在 R5 把右侧标签改为向左伸 |
| `newproject` 演示首次命名 | 第一轮入口在索引加载前就建项目, 出现两个 "未命名 1"; 已改为建项目前 `refresh()` (`EntryViewModel.kt:createProject`), 第二轮为 "未命名 2" |

## 4. 仅真机可验 [H]

- K4.18 系统关闭钮关闭 Volume → 入口重开 + 列表刷新; ProjectSheet 内切换项目 (先开后关) 与删除。
- K4.25 眼手 / 直接捏合 / 手柄 三种方式拖动已选方块的手感 (0.5 格死区, LIFT 判定 60°); 位姿射线主路径被拒绝退化到视平面的比例 (§P.3.5)。
- K4.2 / K4.39 / K4.50 工具栏 1376 × 112 在默认距离下完整可见; 项目 / 导出 图标钮与 中/EN 段控可点。
- §P.3 1–7: 72 dp 目标的眼手命中率, 直接捏合下 `spatialHoverEffect` 高亮, poke 滑杆跳值, 两手缩放, 侧卡 z 0.5 在绕到侧面时的可读性 (并与 `gridspace_rail 1,0.5,0.75,32,0,0,1` 对比)。
- K4.30 冷启动 (无 `gridspace_lang`) 读 SharedPreferences 优先于系统语言。
- 颜色栏 / 取色面板 / 选区面板的 Noto 字形在真机分辨率下的可读性 (模拟器 2160 px 截图中 16 sp 文字已可读)。
