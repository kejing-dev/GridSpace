# GridSpace 格间 · QA 报告 R4

日期 2026-10-13 · 被测构建: 本轮重新 `./gradlew :app:assembleDebug :app:testDebugUnitTest --offline -q` (EXIT 0, APK 47.2 MB, 全部 dex 含 `SpatialApplication` 3 处) · 设备: PICO 模拟器 emulator-5554 (`Pico_Emulator_6_1_fresh`, 研发截图; 本轮未重拍, 所有截图可读) · 依据: `docs/DESIGN_SPEC_R4.md` §M–R + K4.1–K4.55 (§R.6 取代项以 §R 为准), `docs/R4_BRIEF.md` §3 目标 1–6, `docs/DEV_REPORT_R4.md`, `docs/QA_REPORT_R2.md` / `DEV_REPORT_R3.md` (N-01..N-03), `docs/DESIGN_SPEC.md` v1.0 §K

证据: 研发截图 `docs/screenshots/r4/zh/` (36 张) 与 `en/` (35 张), 全部 2160 × 2158; 本轮 QA 产物在 `docs/screenshots/r4/qa/`: `view-*` 为 1000 px 预览, `crop-*` 为 2× 局部放大 (颜色栏 / 笔刷行 / 标签卡 / 选区面板 / 取色面板 / 标签压颜色栏 / 入口 / 新建项目 / 占用 / 拖动预览与提交 / EN 字形), `view-rail-montage.png` 为研发的 z 对比图缩放。像素测量脚本 `docs/scripts/measure_r4.py` (Pillow, `python -P`), 结果见 §1.1。导出核对 `docs/export/r4-{zh,en}-{walk,newest}.vox` (`docs/scripts/voxdump.py`), 项目 JSON 直接从模拟器 `run-as` 读取。

## 1. 构建 / 测试 / 静态检查

| 项 | 结果 |
| --- | --- |
| `./gradlew :app:assembleDebug :app:testDebugUnitTest --offline -q` | EXIT 0 |
| 单测 | **51/51** (BarFill 7 · BoxFill 5 · ColorMath 3 · History 4 · SelectionMove 3 · SelectionOps 7 · TargetCellResolver 7 · VoxWriter 4 · CameraMathDrag 6 · EditorViewModelSave 5); 0 失败 0 跳过 (`app/build/test-results`); R3 35 + 16 新增, 满足 K4.23 / K4.24 / K4.45 |
| dex 含 `platform/SpatialApplication` | 遍历全部 `classes*.dex` 求和 = 3, exit 0 (AGENTS 新命令) |
| `androidx.compose.material*` | 0 |
| `FontWeight.Bold/SemiBold/W600/W700` | 0; 字体族注册用 `FontWeight(600)` / `FontWeight(700)` 字面量 (`GridTypography.kt:65`), 应用角色只有 Normal / Medium |
| `combinedClickable` / `onLongClick` | 0 / 0 |
| Kotlin 内 CJK / `Text("` 硬编码 | 0 / 0 (Python 扫描 `app/src/main/java/**/*.kt`, 排除注释) |
| `values` / `values-zh` | 103 / 103, 键集对称差 ∅; §Q.3 新键 16 个齐全 (`entry_title projects lang_zh lang_en edit_colour edit_colour_slot label_move_* selection_drag_hint editing_now no_other_projects brush_bar bar_length bar_axis`); `slot_hint_1/2` / `colour_title` 已删; 未用键 0 (`app_name` 由 Manifest 用) |
| 残留 56 / 48 / 40 dp | 7 处, 全部非可点: 分隔线高 40 (`BrushRow.kt:50`, `EditorToolbar.kt:60`), 颜色栏表头 48, 导出结果图标 56, 滑杆拇指 48 (热区 72, `HslSliders.kt:70`), 入口步骤行 56, 列表色块 56 |
| `spatialHoverEffect` | 3 处 = 全部自绘可点容器: `ColorSlot` (`ColorRail.kt:186`), 色板格 (`:230`), `ProjectRow` (`RecentProjects.kt:104`); 其余 `clickable`/`onClick` 都在 SDK `Button` / `IconButton` / `SegmentItem` / `ToggleButton` 上 |
| `backgroundMaterial` / `hoverable(` / `googlefonts` / `FORCE_CHINESE` / `BuildFlags` / `TitleAugment` / `smallMedium` | 全部 0 |
| `Modifier.border(` | 5 处, 全是色环 / 近白 1 dp 内描边 (`ColorRail.kt:204,208,235,236`, `RecentProjects.kt:93`); 无容器边框 |
| 字体资产 | `app/src/main/assets/fonts/` 4 文件 (NotoSans-VF 2.0 MB, NotoSansSC-VF 17.8 MB, 两份 OFL); APK 内 `assets/fonts/*` 4 条 |
| 依赖 | `gradle/libs.versions.toml` 与 ElementSpace **逐字节相同**; `app/build.gradle.kts` 仅 namespace / applicationId 两行不同 |
| 模拟器 ANR / 崩溃 | `r4-logcat-walk.txt` / `r4-logcat-export.txt` 两套均 0 字节 (无 `AndroidRuntime` / `FATAL` / `ANR `); `r4-anr-ls.txt` 两套最新仍为 10-07 15:27 |
| walk 步序 (K4.33) | 两套 27 标记: ghost occupied **bar** box erase paint selection dragmove dragmove-commit rotate duplicate stepx delete vivid edit eyedrop edit-close lang lang-back undo burst redo reset export exported projects done; 与 K4.33 顺序一致 (多出的 `bar` 为 K4.44 要求) |
| 导出 .vox | 见 §1.2 |
| 项目 JSON (新建项目默认槽) | 模拟器 `files/projects/d9f0…json` (Untitled 1) 与 `fc4f…json` (未命名 1): `slots = [#ffffff, #a5edcc, #f3bc9c]`, `activeSlot = 0` ✓; 样例 `sample-pavilion.json` 仍 `[#f3bc9c, #a5edcc, #f3f5ed]` ✓ |
| 清理 | `settings list global | grep gridspace` 为空 ✓ |
| 黑卡扫描 | 71 张编辑器截图的颜色栏区域 (1660..1960 × 1050..1650) 近黑像素占比全部 0.00, 仅 `r4-edit.png` 0.04 (zh) / 0.13 (en) = 取色面板底部黑带 (N4-03); 笔刷行区域在 上色 / 选择 模式按规格隐藏 |

### 1.1 像素测量: 入口 Planar 窗 vs Volume 侧 Augment (本轮第一问题)

方法: `docs/scripts/measure_r4.py` 在 2160 px 原图上按颜色 / 差分取包围盒 (薄荷钮, 鲜艳页饱和色板格, ghost−workbench / dragmove−workbench / edit−rail 差分); 笔刷行与色槽圆由 2× 裁剪图 (`qa/crop-zh-brushrow-bar.png`, `crop-zh-rail-vivid.png`) 量取。两套语言结果相同 (±2 px)。

| 表面 | 目标 (规格 dp) | 实测 px | **px / dp** |
| --- | --- | --- | --- |
| 入口 Planar 窗 (1200 dp) | 新建空白项目 440 × 80 | 564 × 100 | **1.28 / 1.25** |
| 导出 Sheet (系统窗) | 主钮 高 80 | 519 × 105 | 1.31 |
| 项目 Sheet | 新建钮 高 80 | 495 × 95 | 1.19 (宽被 Sheet 固定宽裁到 ≈ 410 dp, 见 N4-04) |
| 颜色栏 Augment (z 0.5) | 色板网格 350 × 530 | 261 × 373 | **0.75 / 0.70** |
| 颜色栏 | 色板格 80 | ≈ 60 × 56 | 0.72 |
| 颜色栏 | 色槽圆 64 (热区 80) | 42 × 40 (热区 ≈ 56) | 0.65 |
| 目标标签 Augment | 卡片 440 × 120 | 319 × 89 | **0.72 / 0.74** |
| 选区面板 Augment (z 0.5) | 卡片 400 × 548 | 319 × 423 | **0.80 / 0.77** |
| 选区面板 | 步进钮 72 | ≈ 50–56 | 0.70–0.78 |
| 取色面板 SpatialPopup (锚在颜色栏) | 640 × 624 | 485 × 448 | **0.76 / 0.72** |
| 取色面板 | 滑杆拇指 48 / 完成 高 80 | 33 / 60 | 0.69 / 0.75 |
| 笔刷行 Augment (z 0.75, 更近) | 行 905 × 72; 段控项 120 × 64; 步进钮 72 | ≈ 840 × 67; ≈ 110 × 60; ≈ 67 | **0.93** |

结论: 入口窗 (及 Sheet) 每 dp ≈ **1.27 px**; 挂在 Volume 上的全部 Augment 与 Popup 每 dp 只有 **0.70–0.80 px** (前方 z 0.75 的笔刷行 0.93)。比值 **0.55–0.63** (笔刷行 0.73)。原因与简报 §2 一致: Volumetric 窗及其附件保持物理尺寸, 不按距离动态缩放 (`Main.kt` 对 Volume 传了 `WorldScale.Dynamic`, 截图证明无效); 系统 Planar 窗 (入口 / Sheet) 动态缩放。

换算成"入口窗等效 dp" (实测 px ÷ 1.27):

| 目标 | 规格 dp | 入口等效 dp | ≥ 72 ? |
| --- | --- | --- | --- |
| 颜色栏 色板格 | 80 | **44–47** | ✗ |
| 颜色栏 色槽 圆 / 热区 | 64 / 80 | **33 / 44** | ✗ |
| 颜色栏 预设页签项 | 112 × 64 | 63 × **35** | ✗ |
| 颜色栏 编辑颜色钮 | 352 × 72 | 199 × **41** | ✗ |
| 笔刷段控项 | 120 × 64 | 87 × **47** | ✗ |
| 长条 步进钮 / 轴项 | 72 / 64 | **53 / 47** | ✗ |
| 选区瓦片 / 步进钮 | 112 × 88 / 72 | 69 × 53 / **39–44** | ✗ |
| 取色 滑杆拇指 (热区) / 完成钮高 | 48 (72) / 80 | **26 (40) / 47** | ✗ |
| 标签卡文字 行 1 / 行 2 | 20 / 16 sp | **≈ 11 / 9 sp** | — (文字) |
| 颜色栏 表头 / 页签文字 | 20 / 16 sp | ≈ 11 / 9 sp | — |
| 取色面板 十六进制 | 14 sp | ≈ 8 sp | — |
| 入口 新建钮 / 列表行 (基准) | 440 × 80 / ≥ 88 | 444 × 79 / 102 | ✓ |

即: 代码里的 dp 全部达到 §M / §R.2 的数值, 但用户要求的"更大、易选中" (≥ 72 dp) 在 Volume 侧**没有一处达到**, 实际命中面积只有入口窗同类控件的 1/3–1/2 (色槽热区 44 等效 dp 甚至低于系统 56 下限)。要让 Volume 侧达到 72 等效 dp, 侧卡 / 标签 / Popup 需 **≥ 130 Volume-dp** (72 × 1.27 / 0.70), 笔刷行需 **≥ 98** (72 × 1.27 / 0.93); 文字要达到 20 sp 观感需 ≈ 36 sp。详见 N4-01。(系统 Toolbar 同样挂在 Volume 底边, 其 80 dp 模式钮按同一比例约 44 等效 dp, 截图不可见, 真机复核。)

### 1.2 导出核对

```
r4-zh-walk.vox / r4-en-walk.vox   size 16³ · 193 块 · #f3f5ed 147 / #f3bc9c 25 / #a5edcc 20 / #ff3b30 1 (两套逐格相同)
  (0,0,8) (0,1,8) (0,2,8) (0,3,8) = #f3bc9c, (0,4,8) 空      ← 长条 Y / 长度 4 = +4 块 (K4.44) ✓
  (0,0,1) = #ff3b30                                            ← 鲜艳页选色后放的 1 块 (K4.53) ✓
  (1,0,0) (3,0,3) = #a5edcc                                    ← 上色 2 块 ✓
  (1,2,2) 空                                                   ← 幽灵放置后被擦除 ✓
  (8..11,0,14) 空, (12,0,11..15) = #f3bc9c, (14,0,11..15) 空
      ← 盒 (8..12,0,14) 拖动 (+2,0,−1) → (10..14,0,13) → 绕中心 (12,0,13) 旋转 → (12,0,11..15) 留下;
        复制 → X+1 → 删除 的副本 (14,0,11..15) 消失。与 K4.22 的新坐标一致 (间接验证) ✓
r4-zh-newest.vox / r4-en-newest.vox  184 块 (150 / 18 / 16) = 样例原样 ✓
```

## 2. 早期缺陷复验 (R2 N-01..N-03, R3 已修)

| ID | 级别 | 结论 | 证据 |
| --- | --- | --- | --- |
| N-01 标签卡盖住目标格 | P1 | **保持已修复** (目标格侧) | `qa/crop-zh-ghost-label.png` / `crop-zh-occupied.png`: 幽灵 + 落点 + 引导柱 / 珊瑚线框 与卡片同时完整可见; 但卡片在视野右半时压到颜色栏 → 新缺陷 N4-02 |
| N-02 取消的保存仍写 SAVED | P2 | 保持已修复 | 单测 `EditorViewModelSaveTest` 5 例含 `cancelledSaveDoesNotReportSavedWhileANewerCommitIsPending`; `r4-burst-1..4` 颜色栏表头均 "已自动保存" |
| N-03 dex 校验命令 | P2 | 保持已修复 | 本轮用 AGENTS 新命令, 求和 3 |

## 3. 验收清单 K4.1–K4.55

结果: **通过 39 · 部分 13 · 取代 1 · 未验证 (真机) 2**。截图省略前缀 `docs/screenshots/r4/`; "dp ✓" = 代码常量与规格一致, "等效" = §1.1 入口等效尺寸。

| # | 结果 | 证据 / 说明 |
| --- | --- | --- |
| 1 | 通过 | `GridTypography.kt:24-56` 七角色 28M/20M/18R/18M/16R/16M/14M, 无 `smallMedium`, 无 < 14 sp |
| 2 | 部分 | 工具栏内容由 K4.38 取代; 常量 `ToolbarSizes` 80/160/12/72/28/144 合计 1376 (代码 ✓); 两套 71 张截图均看不到 Volume 底边 (R1 起已知) → 真机 [H]; 且按 §1.1 比例模式钮等效约 44 dp (N4-01) |
| 3 | 通过 | grep 0 |
| 4 | 通过 | `GridSizes` Primary 80 / Control 72 / 段控 64 in 72 / `sliderSpec(thumbAreaSize 72)`; 残留 56/48/40 均非可点 (§1) |
| 5 | 取代 | §R.1 无标题条; `TitleAugment` 0 处 |
| 6 | 部分 | `TargetLabel.kt` 440 × 120, 24/18 内边距, 图标 24 + `label` / `caption` (dp ✓); `zh/r4-ghost.png` "✓ 已对齐 · 松手放置 / X1 · Y2 · Z2", `en/` "✓ Aligned · release to place / X1 · Y2 · Z2"; 实测 319 × 89 px, 文字等效 11 / 9 sp (N4-01) |
| 7 | 部分 | §R.2 取代 436 → 400: `SelectionSizes` 400 / 瓦片 112 × 88 ×3 间 8 / 钮 72 / 值 72 / 轴 32, 末行 "拖动已选方块可整体移动" / "Drag a selected cube to move all" (`qa/crop-{zh,en}-selpanel.png`); 等效 251 × 333, 步进钮 39–44 (N4-01) |
| 8 | 通过 | 入口 1.27 px/dp: 新建 564 × 100 px = 444 × 79 dp, 网格段控 440 宽, 列表行 ≈ 102 dp, 色块 56, 删除 72 (`qa/crop-zh-entry-top.png`); 1200 × 800 窗 (Manifest) |
| 9 | 部分 | `ExportSheet.kt` 文件名 72 / 主钮 80 (实测 105 px @1.31) / 结果图标 56; 但内容宽 640 被 SDK Sheet 固定宽裁到约 400 dp (N4-04); 文字无省略 |
| 10 | 部分 | `zh/r4-workbench.png`: 顶部无卡片, 右侧颜色栏 (表头 "庭院小亭 · 已自动保存" / 3 槽 槽 1 白环 / 柔和|鲜艳|大地 / 4 × 6 色板 / 编辑颜色), 下方笔刷行, 模型, 入口窗已关; 四者互不重叠; 尺寸 400 × 898 (dp ✓); 深度 z 0.5 (申报偏离, 规格允许, `rail/montage.png`); 等效尺寸不足 (N4-01) |
| 11 | 通过 | `zh/r4-dragmove.png` / `en/` 左选区面板 + 右颜色栏同时显示 |
| 12 | 通过 | `EditorToolbarContent` 无色槽; `SlotLongPressed` / `combinedClickable` 0 |
| 13 | 通过 | `zh/r4-rail.png` (= vivid): 鲜艳页高亮, 24 色, 槽 1 变 #FF3B30 带白环, 色板首格白环 (`qa/crop-zh-rail-vivid.png`); `EditorViewModelColour.kt` `SwatchPicked → writeActiveSlot` 不经 `History`; vox (0,0,1) = #ff3b30 |
| 14 | 部分 | `zh/r4-edit.png`: "编辑颜色 · 槽 1 / #FF3B30 / 从方块取色 / 色相 饱和 明度 / 完成", 覆盖色板, 栏顶 3 槽可见, 锚在编辑颜色钮上方 (`qa/crop-zh-popup.png`); 640 × 624 (dp ✓, 实测 485 × 448 px); **面板底部约 100 dp 无玻璃呈黑色, "完成" 钮压在黑带上** (两套, N4-03) |
| 15 | 通过 | `zh/r4-eyedrop.png` 88 高提示条 "点一个方块取色 · 取消", 颜色栏槽 / 页签 / 色板变暗 (`qa/crop-zh-eyedrop-bar.png`); `r4-edit-close.png` 槽 1 回到珊瑚 (取自 (3,1,12)); 色板环按新色重算 (代码 `SwatchGrid` `selected = color == current`) |
| 16 | 通过 | 三个 Augment 内容各取一个 `State` (`EditorScreen.kt` `rememberUpdatedState(BrushRowState / ColorRailState / SelectionPanelState)`), 各行固定高; `DemoMode.placement` 读 `gridspace_rail/selpanel/brush`, 默认 = §R.2 (z 改 0.5); 71 张无黑卡 (§1) |
| 17 | 通过 | 方案 A, 探针结果已写 (DEV §0); `r4-*-workbench.png` 无入口窗; `zh/r4-projects.png` "项目 / + 新建空白项目 / 网格尺寸 16 24 32 48 / 最近项目 / 庭院小亭 193 块 · 正在编辑 (薄荷) / 未命名 1 · 删除" (`qa/crop-zh-projects.png`); `r4-entry-after.png` 入口重开, 列表 "庭院小亭 184 块 · 刚刚" + Untitled 1 + 未命名 1; `r4-export-log.txt` "last editor closed; entry window reopen requested"; 宽度 720 未实现 (N4-04) |
| 18 | 未验证 | [H] `EditorScreen.kt` `onDispose` 路径对系统关闭钮同样生效 (代码); ProjectSheet 切换 / 删除需真机 |
| 19 | 通过 | `qa/crop-zh-ghost-label.png`: (1,2,2) 薄荷幽灵 + 底座薄荷落点方片 + 竖引导柱; `crop-zh-occupied.png`: (1,0,0) 珊瑚线框 (y = 0 无引导柱, 落点边缘贴底); 目标格由 (3,2,5)/(3,1,12) 改为 (1,2,2)/(1,0,0) (申报, 原格落点被地板盖住) |
| 20 | 部分 | `VoxelSceneView.kt:45-56, 166-176` 注释写明五种输入同路径; 分流: 已选格 → `SelectionDragStarted` ✓, 非已选方块 → `PreviewStarted` (SelectCell) ✓; **底座 → 走 EDIT 路径而非轨道** (N4-05) |
| 21 | 通过 | `zh/r4-dragmove.png` + `qa/crop-zh-dragmove-preview.png`: 原块变暗带白框, 目标格薄荷幽灵 + 白框, 底座白色落点, 标签 "ΔX +2 · ΔY 0 · ΔZ −1 / 松手移动 5 块", 面板 2 / 0 / -1 (值用 ASCII 连字符, N4-07); `en/` "Release to move 5 cubes" |
| 22 | 通过 | `qa/crop-zh-dragmove-commit.png` 5 块在 (10..14,0,13) 带白框; `dragEnded` → `SelectionOps.translate` → `commit` 1 步, `translation += Δ`; vox 间接验证 (§1.2) |
| 23 | 通过 | `CameraMathDragTest` 6 例 (死区 / 轴 / LIFT / GROUND / 锁定 / 夹取), `dragToDelta` 实现与 §O.2.2 步骤 2–6 一致 (死区只在未锁定时生效, `roundToInt` 半进位) |
| 24 | 通过 | `SelectionMoveTest` 3 例; `translate` 经 `move()` 先腾空后覆盖, 一条命令 |
| 25 | 未验证 | [H] |
| 26 | 通过 | §1 键集 / CJK / `Text("` |
| 27 | 通过 | 4 文件在 assets 与 APK; `notoFontFamily` 缺文件或异常 → null → 系统字体; `BuildFlags` 已删 |
| 28 | 通过 | `en/r4-entry.png` "GridSpace / New blank project / Grid size / Recent projects / Before you start"; `qa/crop-en-entry-g-glyph.png` "8 min ago" 的 g 为双层 (Noto Sans); `zh/` "格间 GridSpace" |
| 29 | 通过 | `zh/r4-lang-toggle.png`: 颜色栏 "Soft|Vivid|Earth / Edit colour / Auto-saved", 笔刷行 "Voxel|Bar|Box", 项目名仍 "庭院小亭" (数据); 卡片包围盒与中文帧相同 (`qa/crop-zh-lang-toggle-rail.png`); `en/r4-lang.png` 反向切中文 ✓; `lang-back` 复原 |
| 30 | 通过 (代码) | `LanguageResolver.persist` 写 `gridspace/language`; `resolve` 顺序 设置 > 偏好 > 系统, `AppGraph.init` 调一次; 冷启动真机 [H]。§Q.1 "切换后再读设置" 未实现 (申报偏离, 理由成立: 否则 QA 设置会立刻覆盖用户选择; 建议规格改文) |
| 31 | 通过 | EN 全套目检 workbench / dragmove / edit / export / entry / projects / ghost / bar: 无省略号; 最紧的是选区瓦片 "Rotate 90°" (112 宽, `qa/crop-en-selpanel.png`) 与 "Pick from cube" (220 宽), 均单行 |
| 32 | 通过 | §1 |
| 33 | 通过 | §1 步序; logcat / anr 干净 |
| 34 | 通过 | zh 36 张 / en 35 张, K4.34 表中文件全部存在且内容对应 (`r4-rail` = vivid, `zh/r4-lang-toggle` = lang); `r4-drawer.png` 不适用 (A) |
| 35 | 通过 | `r4-run.log` 每张记录 cap.sh 尝试次数 (1–5), 标记后 1.5 s 再拍 (`walk.sh`); 注: zh 日志写 entry-after "no clean frame in 5 tries, bad 0.836", 实际文件为重启后重拍 (DEV §2 说明, 文件时间 02:26 晚于日志 02:09), 日志未同步 (N4-09) |
| 36 | 通过 | `VoxelScene.kt` 落点 Mint / Coral / SelectionWhite 常量, `ProjectRow` "正在编辑" Mint; 无其它新角色 |
| 37 | 通过 | §1 依赖比对; 字体从 assets 读 |
| 38 | 通过 | `EditorToolbar.kt:70-93` 顺序 4 模式 · 分隔 · 撤销 重做 · 分隔 · 回正 · 分隔 · 项目 (`ic_folder`, `projects`) 导出 (`ic_upload`, `export`) · 分隔 · 中/EN 144 × 72; 1376 |
| 39 | 部分 | 顶部无卡片 ✓; 表头 48, 名 200 / 状态 120 ✓ (`RailHeader`); 工具栏 ▤ ↑ 中|EN 截图不可见 → [H] |
| 40 | 部分 | `RailSizes` 400 / 24 / 12 / 表头 48 / 槽 80·64·3 / 页签 112 × 64 / 色板 80 r18 间 10 / 编辑颜色 352 × 72 (dp ✓); 实测 0.70–0.75 px/dp, 色板格等效 44–47, 色槽热区等效 44 (N4-01) |
| 41 | 部分 | 同 K4.7 |
| 42 | 部分 | `Palette.DEFAULT_SLOTS` #FFFFFF/#A5EDCC/#F3BC9C, `activeSlot 0`; 三处新建 (入口 / ProjectSheet / JSON 缺省) 都用它; 模拟器 JSON ✓; 近白描边 `ColorDisc` / `SwatchGrid` / `ProjectRow`, 匹配环近白改 BaseInk 60 %; `zh/r4-newproject.png` 槽 1 白 + 描边 + 白环, 柔和页 #f3f5ed 描边 (`qa/crop-zh-newproject-rail.png`); **鲜艳页 #ffffff 深色匹配环无截图** (N4-06) |
| 43 | 部分 | `BrushRowSizes` 905 × 72, 120 × 64 ×3, 分隔 33, 步进 72/72/72, 轴 64 ×3 (dp ✓); `zh/r4-bar.png` "单格 | 长条 | 长方体 · − 4 + · X Y Z" (`qa/crop-zh-brushrow-bar.png`), `r4-workbench.png` 单格时后三段不可见, 两帧行宽均 ≈ 840 px ✓; 等效 87 × 47 / 53 (N4-01) |
| 44 | 通过 | `zh/r4-bar.png` 薄荷 1 × 1 × 4 竖条 + 底座落点 + 标签 "1 × 4 × 1 / 松手放置", 笔刷行 长条 · 4 · Y; 目标 (12,0,2) 改 (0,0,8) (申报, 顶梁遮挡); vox +4 ✓ (§1.2); `BarFill.region` 与 §R.4 1–4 一致 (平局取 +) |
| 45 | 通过 | `BarFillTest` 7 例 |
| 46 | 通过 | 三键两语言; `bar_length` / `bar_axis` 仅 `semantics { contentDescription }` (`BrushRow.kt`) |
| 47 | 通过 | grep 0 |
| 48 | 通过 | 常驻: Toolbar (模式 / 撤销 / 重做 / 回正 / 项目 / 导出 / 语言) + 笔刷行 (笔刷 / 长度 / 轴) + 颜色栏 (槽 / 页签 / 色板); 二层仅 编辑颜色 / 导出 / 项目 |
| 49 | 通过 | §1 三处 |
| 50 | 部分 | 默认视点下颜色栏 / 选区面板 / 笔刷行三者在 `r4-dragmove.png` 内完整 (z 0.5); 但目标在右半时标签卡压在颜色栏上 (N4-02); 工具栏 [H]; 注意 `rail/montage.png` 左图: 模拟器重启后视点变化即可把 z 0.75 的颜色栏推出画面, 摆位对系统落点敏感 |
| 51 | 通过 | `EditorToolbar.kt:78-80` 常驻, `enabled = canUndo/canRedo` |
| 52 | 通过 (代码) | `DemoMode.walk` 长条 (Y, 4) → ERASE → ADD; `brushes[mode]` 记忆 + `barLength/barAxis` 会话级 (`EditorUiState`); `r4-box.png` 之前未单独截帧 |
| 53 | 通过 | vox (0,0,1) = #ff3b30 (§1.2); `zh/r4-edit.png` 左下红块 |
| 54 | 通过 | K4.29 一帧切换 ✓; 冷启动保留 [H] |
| 55 | 通过 | `previewStarted / previewMoved / dragMoved` 同步 `_state.update`, 无 `delay`; `VoxelSceneView.update` 同帧 `setPreview`; `LaunchedEffect(labelCell…)` 同帧更新锚点 |

## 4. 新增缺陷

无 P0。

| ID | 级别 | 标题 | 位置 | 期望 (规格 / 简报) | 实际 | 复现 |
| --- | --- | --- | --- | --- | --- | --- |
| N4-01 | **P1** | Volume 侧所有可点目标的有效尺寸只有入口窗的 55–63 %, 全部低于 72 dp 等效 (简报 §3.1 "更大、易选中" 未达成) | 所有挂在 `EditorVolume` 上的 `Augment` (`EditorScreen.kt`: `BrushRow` / `ColorRail` / `SelectionPanel` / `TargetLabelAugment`) 与锚在颜色栏内的 `SpatialPopup` (`ColorEditPopup.kt`); 系统 `Toolbar` 同理 | 可点 ≥ 72 dp, 文字 20 / 18 / 16 sp 在臂长距离可读 (§M.2) | §1.1: 入口 1.27 px/dp, Volume 侧 0.70–0.80 (笔刷行 0.93); 色板格 80 → 等效 44–47, 色槽热区 80 → 44, 页签 64 高 → 35, 步进钮 72 → 39–53, 滑杆拇指 48 → 26, 编辑颜色 / 完成 钮高 72 / 80 → 41 / 47; 20 sp → ≈ 11 sp 观感 (`qa/crop-zh-rail-vivid.png` 中 "已自动保存" 16 sp 已难辨认) | `zh/r4-entry.png` 与 `zh/r4-workbench.png` 并列; `python -P docs/scripts/measure_r4.py` |
| N4-02 | **P1** | 目标在视野右半时标签卡压在颜色栏上, 盖住第 6 行色板与 "编辑颜色" 钮 | `VoxelSceneView.kt` `LabelPlacement` (`onRight = v.x ≥ 0` → 卡片向右伸 1 格 + 440 dp) + `EditorScreen.kt` `TargetLabelAugment` `CenterLeft`; 颜色栏在 x = 1 面外 32 dp | §N.2 / §R.2 示意: 标签在模型与颜色栏之间, 四周附件互不重叠 (K4.10 / K4.50) | `zh/r4-dragmove.png` (`qa/crop-zh-dragmove-label-over-rail.png`): "ΔX +2 · ΔY 0 · ΔZ −1" 卡片覆盖色板末行右三格的下半与 "编辑颜色" 上沿; `zh/r4-box.png` 盒标签 "5 × 1 × 1" 同样压在颜色栏左下角; en 同 | walk `dragmove` / `box`; 真机: 在 x ≥ 8 的格子捏合保持 |
| N4-03 | P2 | 取色编辑面板底部约 100 dp 无玻璃, "完成" 钮压在黑带上 | `ColorEditPopup.kt` `SpatialPopup(defaultMinHeight = Dp.Unspecified)`; 内容 624 高, 系统材质只覆盖到滑杆行下方 | §N.3.3 面板 640 × 624 系统材质, 完成钮 80 在面板内 (AGENTS: 新窗口 ≥ 1.5 s 后无黑) | `qa/crop-zh-popup.png` / `crop-en-popup-bottom.png`: 面板玻璃止于明度滑杆下, 其下为纯黑圆角带, "完成 / Done" 浮在黑带上; 黑像素占比 zh 0.04 / en 0.13; `r4-edit.png` 已是标记后 ≥ 1.5 s 且 cap.sh 第 2 次 | walk `edit`; 真机: 点 编辑颜色 |
| N4-04 | P2 | 项目 / 导出 Sheet 的内容宽被 SDK Sheet 固定宽裁到约 400–410 dp, 规格 720 / 640 未实现, 研发报告仍按 720 / 640 申报 | `ProjectSheet.kt` `Modifier.width(720.dp)` / `SizeItemWidth 172`, `ExportSheet.kt` 640; SDK `SheetDefaults.fixedWidth / minWidth / maxWidth` (javap) | §N.6.A 内容宽 720, 项 172 × 64; §M.3 导出 640 | `zh/r4-projects.png` 新建钮 495 px 宽 × 95 px 高 → 高按 1.19 px/dp, 宽只合 ≈ 415 dp; 四个网格尺寸项被压到 ≈ 100 dp (`qa/crop-zh-projects.png`); 内容未被裁, 文字单行, 功能正常 | walk `projects` / `export` |
| N4-05 | P2 | 选择模式下在底座 / 空白格上捏合保持走 EDIT 路径 (SelectCell 预览 + 松手切换选中), 不是相机轨道 | `VoxelSceneView.kt` `onDragStart`: `cell == null && !plate → ORBIT`, 底座落入 `else` → `fallbackAnchor(null)` = 网格中心格 → `PreviewStarted(Target(N/2, 0, N/2))`; `HitResolver.kt:68` 非 ADD 模式下 `Target` 只在该格有方块时成 `Cube` → `Preview.SelectCell`, 否则 `Nothing` | §O.2.1 第 3 行 "SELECT 且命中底座 / 空白 → 相机轨道" | 代码路径: 选择模式拖底座要么无任何反应 (中心格空), 要么对与手势无关的中心格 (或位姿射线命中的任意方块) 出现白框预览、松手切换其选中态; 模拟器无法注入 | 真机: 选择模式在底座上捏合拖动 |
| N4-06 | P2 | K4.42 [E] 证据不全: `r4-newproject.png` 停在柔和页, 鲜艳页 #ffffff 格的 BaseInk 60 % 匹配环未截到 | `DemoMode` `newproject` 脚本不切页签 | K4.42 "鲜艳页 #ffffff 格显示深色匹配环" | 代码 `SwatchGrid` 已实现 (`ring = if (nearWhite) BaseInk@0.6 …`), 无截图 | `gridspace_demo newproject` |
| N4-07 | P2 | 负号不一致: 标签用 U+2212 "−1", 选区面板步进值用 ASCII "-1" | `SelectionPanel.kt` `StepperRow` `value.toString()`; `EditorScreen.kt` `signed()` | §O.2.3 负数 U+2212 (标签); 面板同屏显示同一 Δ | `qa/crop-zh-selpanel.png` "Z … -1" vs 标签 "ΔZ −1" | walk `dragmove` |
| N4-08 | P2 | 取色面板十六进制 14 sp 在 Volume 侧只有 ≈ 8 sp 观感, 2160 px 截图已不可辨 | `ColorEditPopup.kt` `GridType.small` | §M.1 最小 14 sp "非交互"; 简报: 文字可读 | `qa/crop-zh-popup.png` "#FF3B30" 糊成一团 (N4-01 的子项) | walk `edit` |
| N4-09 | P2 | 证据日志未同步: zh `r4-run.log` 记 entry-after 5 次均黑块 (0.836), 文件实为重启后重拍; en 日志同段无记录 | `docs/scripts/r4.sh` 重拍未经脚本 | K4.35 每张有 cap.sh 记录 | 研发报告文字说明了重拍; 建议把重拍命令追加到 run.log | — |

## 5. 可用性 / 与设计 (§R.2 示意图, Figma V2) 对照

| 画面 | 观察 |
| --- | --- |
| 工作台 | 布局骨架与 §R.2 示意一致: 顶部干净, 右颜色栏, 左选区面板 (有选区时), 下笔刷行, 入口窗已退场, 模型居中; 四个附件在默认视点下全部可见 (z 0.5)。但**观感尺度**不对: 入口窗按动态缩放占据画面 73 % 宽、文字清晰; 切到工作台后颜色栏只有画面 14 % 宽, 24 格色板每格约 56 px (2160 px 画面), 表头 / 页签 / 编辑颜色 文字需放大才能读; 用户看到的是"小了一半"的 UI, 与 R3 相比只是把 dp 数字加大, 没有改变 Volume 不缩放这一根本 (N4-01) |
| 颜色栏 | 结构 (表头 / 3 槽 / 页签 / 4 × 6 / 编辑颜色) 与 Figma V2-02 的"常驻取色"意图一致, 色槽白环、近白描边、匹配环都对; 一步换色 (U7) 成立 (vivid → 红块) |
| 取色编辑面板 | 内容与 §N.3.3 一致, 覆盖色板、露出 3 槽; 黑带 (N4-03) 与 8 sp 十六进制 (N4-08) 影响观感 |
| 目标标签 | 卡片 440 × 120 比例正确, 两行文案正确, 目标格不再被盖 (N-01 保持); 但右半视野与颜色栏打架 (N4-02): 卡片锚点只按"远离 Volume 中心"选边, 没有考虑右侧已有常驻卡 |
| 选区拖动 | 变暗原块 + 白框 / 薄荷目标 + 白框 / 白落点 / Δ 标签 / 面板实时值 五件套齐全, 提交后白框随选区移动, 与 §O.2.3 一致; 落点与引导柱在 (1,2,2) 清晰; 占用落点 (y = 0) 的 0.06 格珊瑚边在当前比例下约 3 px, 几乎看不出 (可接受, 线框本身足够) |
| 笔刷行 | 三段控 + 长度 + 轴一行, 单格时后段隐藏且行宽不变 (K4.43), 与示意一致; 它在 z 0.75 比侧卡近 30 %, 是 Volume 侧最可读的卡 (0.93 px/dp), 说明把侧卡也推到前面 (z → 1) 能部分缓解 N4-01 |
| 入口 / 项目 Sheet / 导出 Sheet | 入口 1200 × 800 两列与 §N.6.A 一致, 1.27 px/dp 下 20 sp 清晰; Sheet 由系统固定宽 (≈ 410 dp) 渲染, 内容自适应后仍整齐, 比规格的 720 更合适, 建议规格改为"Sheet 系统宽" (N4-04) |
| 双语 / 字体 | 中英全套对应, 切换一帧生效且卡片尺寸不变; Noto Sans 双层 g 可辨; EN 最长串无省略 |
| 多模态 | 代码层满足 §P.1 (统一手势入口、hover 覆盖全部自绘容器、3D hover 在 `VoxelEntity`); 底座拖动在选择模式的分流与 §O.2.1 不符 (N4-05); 其余 [H] |

## 6. 发布结论

**需 R5**: 构建 / 单测 / 导出 / 持久化 / 双语 / 字体 / 吸附 / 拖动移动 / 方案 A 窗口流 全部通过且无崩溃, 但本轮核心目标 "UI 更大、更好用" 在 Volume 侧未达成 (N4-01: 全部可点目标等效 26–53 dp, 无一 ≥ 72), 且标签卡与常驻颜色栏重叠 (N4-02)。两项都是 R4 目标项, 定为 P1。

R5 修复顺序 (每项附验收标准):

1. **N4-01 Volume 侧有效尺寸**: 让颜色栏 / 选区面板 / 标签卡 / 取色面板 / 笔刷行 在模拟器默认视点下**实测** ≥ 92 px (= 72 dp × 1.27) 的可点目标, 文字 20 sp 角色 ≥ 25 px 字高 (≈ 20 sp × 1.27)。推荐做法: 不改 `GridSizes` 数值, 在每个 Augment / SpatialPopup 内容外层统一 `CompositionLocalProvider(LocalDensity provides Density(density × k, fontScale))`, 侧卡 / 标签 / Popup k ≈ 1.8 (0.70 → 1.27), 笔刷行 k ≈ 1.4 (0.93 → 1.27); 随之把颜色栏 898 × 1.8 = 1616 > 1600 压回 (如色板 6 × 4 或行间 10 → 8, 目标 ≤ 1540), 并重新调整右侧卡的 anchor / offset 使放大后的卡仍完整在画面内 (当前右缘已在 1960 px, 放大 1.8 倍会出画); 若放大后无法同时满足"≤ Volume 边"与"默认视点可见", 退而把侧卡推到前面 (z 0.75→1.0 用 `gridspace_rail` 实测) 或改为系统 Planar `WindowContainer`(动态缩放) 并在 DEV_REPORT 写明取舍。验收: `python -P docs/scripts/measure_r4.py` (按新布局调整区域) 输出每个目标的 px, 全部 ≥ 92; 两套语言重拍 workbench / dragmove / edit / bar / ghost。
2. **N4-02 标签卡避让颜色栏**: `LabelPlacement` 增加右侧上限: 当投影 x 落在右侧 1/3 (或卡片右缘 > Volume 右面 − 32 dp) 时改为 `CenterRight` 向左伸 (或把锚点 x 夹到 ≤ 1 − 卡宽), 左侧同理避开选区面板。验收: walk `dragmove` 与 `box` 两张中标签卡与颜色栏包围盒无交集 (用差分 bbox 检查), 目标格仍不被盖 (N-01 不回退)。
3. **N4-03 取色面板黑带**: `SpatialPopup(defaultMinHeight = 624.dp)` 或固定内容高, 让窗口按最终尺寸一次创建; 验收: `r4-edit.png` 颜色栏区域近黑占比 0.00, "完成" 钮四周为玻璃。
4. **N4-05 选择模式拖底座 = 轨道**: `onDragStart` 中 `currentMode == SELECT && cell == null → ORBIT`; 补单测或 demo 步 (选择模式 `PreviewStarted(Target)` 不产生 SelectCell 预览)。
5. **N4-04 Sheet 宽**: 代码改用 `fillMaxWidth()` + 段控项按权重 (去掉 172 固定宽), 规格 §N.6.A / §M.3 改为 "Sheet 系统固定宽"; DEV_REPORT 不再申报 720 / 640。
6. **N4-07 / N4-08 润色**: 面板步进值用 `signed()`; 十六进制改 `caption` 16 (或随 N4-01 放大后复测可读性)。
7. **N4-06 / N4-09 证据**: `newproject` 脚本加 `PresetSelected(VIVID)` 一帧; 重拍命令写入 run.log。
8. (真机 [H]) K4.2 / 18 / 25 / 39 / 50 / 54: 工具栏 1376 × 112 完整可见与可点, 系统关闭钮重开入口, 三种输入的拖动手感, 冷启动语言偏好, 侧卡 z 0.5 vs 0.75 (`gridspace_rail 1,0.5,0.75,32,0,0,1`) 在真机距离下的可见性与命中率; 并在真机上复测 §1.1 的比例 (真机默认放置距离可能不同, 但 Volume 不缩放的结论不变)。
