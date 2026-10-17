# GridSpace 格间 · QA 报告 R8 (发布门更新, 取代 R7 结论)

日期 2026-10-13 · 被测构建: 本轮重新 `./gradlew :app:assembleDebug :app:testDebugUnitTest --offline -q` (EXIT 0, APK 47.3 MB 11:47, 全部 dex 含 `SpatialApplication` 3 处) · 设备: PICO 模拟器 emulator-5554 (研发重启后视点 = R5–R7 视点; 研发截图, 本轮未重拍, 全部帧可读) · 依据: `docs/DESIGN_SPEC_R4.md` §T + K4.69–76, `docs/DEV_REPORT_R8.md`, `docs/QA_REPORT_R7.md` N7-01…05, `AGENTS.md`

证据: `docs/screenshots/r8/{zh,en}/` 完整 walk 集 (zh 36 / en 35 帧, `size` 取代 `bar` / `box`); 本轮 QA 产物 `docs/screenshots/r8/qa/` (`view-*` 预览, `crop-*-brushrow-*.png` / `crop-*-size-preview.png` 裁剪); 测量 `python -P docs/scripts/measure_r4.py docs/screenshots/r8 r8`; 导出 `docs/export/r8-{zh,en}-walk.vox` / `-newest.vox` (研发拉取, 本轮 `voxdump.py` 复核)。

## 1. 构建 / 测试 / 静态检查

| 项 | 结果 |
| --- | --- |
| Gradle (一次一个) | EXIT 0 |
| 单测 | **60/60**: ColorMath 3 · History 4 · SelectionMove 3 · SelectionOps 7 · **SizedBrush 8** · TargetCellResolver 7 · VoxWriter 4 · CameraMathDrag 6 · DragRouting 6 · EditorViewModelSave 5 · LabelAnchor 3 · **UntitledPattern 4**; BarFill / BoxFill 测试已删; 0 失败 |
| dex / 字体 / 依赖 | refs 3; `assets/fonts/*` 4 条; `libs.versions.toml` 与 ElementSpace 相同 |
| grep | material 0 · Bold/SemiBold/W600/W700 0 · `combinedClickable|onLongClick` 0 · `BarFill|brushes[|barLength|barAxis|boxAnchor|BRUSH_UI_SCALE|enum class Brush` 0 (`BoxFill` 仅作区域填充助手保留, 研发申报) · CJK / `Text("` 硬编码 0 · 残留 56/48/40 dp 7 处非可点 · `spatialHoverEffect` 3 处 |
| 字串 (K4.74) | `brush_voxel/brush_box/brush_bar/bar_length/bar_axis` 两语言均不存在; `brush_size` (尺寸 / Size)、`size_times` (`translatable="false"` ×)、`label_size_dims` 存在; 键集 **100 / 100**, 对称差 ∅; 未用键仅 `app_name` |
| 崩溃 / ANR | `r8-logcat-walk.txt` 两套 0 字节; `r8-logcat-export.txt` 各 1 行 = 系统 `SysPerfMonitorService.getSysFatal` 栈 (非本应用); `/data/anr/` 最新仍 10-07 |
| walk 步序 | 两套 26 标记: ghost occupied **size** erase paint selection dragmove dragmove-commit rotate duplicate stepx delete vivid edit eyedrop edit-close lang lang-back undo burst redo reset export exported projects done ✓ |
| 导出 .vox (K4.71) | `r8-zh-walk.vox` = `r8-en-walk.vox`: 195 块 (#f3f5ed 147 / #f3bc9c 27 / #a5edcc 20 / #ff3b30 1); **(1..3, 0..1, 1) 六格全为 #f3bc9c (槽 1), (4,0,1) 空** → 3 × 2 × 1 +6 ✓; (1,0,8) 空 (长条已无) ✓; (0,0,1) 红 ✓; 拖动 / 旋转链 (12,0,11..15) ✓; 计数 184+1+1+6+5−1−2+5−5+1 = 195 ✓。`r8-*-newest.vox` 184 = 样例 ✓ |
| 项目索引 (N7-01) | `r8-*-index.json`: 新建依次 "未命名 2 (36 块)"、"未命名 3"、"Untitled 4"、"未命名 5" — 两语言共用 max+1, 不再重名 ✓ (旧的 "未命名 1" ×5 / "Untitled 1" ×3 为历史数据) |
| 黑块帧 | run.log "no clean frame" 仅 zh `burst-2`、en `burst-4` (模拟器整屏黑块, burst 只看表头); 其余帧颜色栏 / 笔刷行 / 左侧区域近黑 0.00–0.02 |
| 清理 | `settings list global | grep gridspace` 为空 ✓ |

### 1.1 像素测量 (zh / en 相同)

| 目标 | 设计 dp | 实测 px | 阈值 | 结论 |
| --- | --- | --- | --- | --- |
| 笔刷行 步进钮 (z 0.97, scale 1.0) | 72 | **117** (90–124; 禁用态同径浅灰) | ≥ 86 (K4.72 ≥ 92) | ✓ |
| 笔刷行 整行 | 884 × 80 | ≈ 1414 × 128, x 324..1922, y 1880..2063 (下缘距画面底 95 px); 12 帧 (workbench / size / ghost / occupied / erase / vivid / edit-close / undo / reset / done / newproject / lang) 位置相同 | 画面内 | ✓ |
| 笔刷行 遮挡 (K4.73) | — | 行包围带 (330..1915 × 1890..2050) 内底座网格线 / 灰蓝层像素占比 **0.0000** (同帧行上方底座带 0.0003–0.0005 作对照); `crop-zh-brushrow-size.png` 网格线在行两侧可见、行内无 | 0.00 | ✓ 行在底座前方 |
| 颜色栏 色板 / 色槽热区 / 选区步进钮 | 88 / 88 / 84 | 96 / ≈ 93 / 94 | ≥ 92 / 92 / 86 | ✓ (R7 不变) |
| 目标标签 ghost / occupied / size | 360 × 96 | 558 × 158 / 590 × 156 / ≈ 560 × 158 (差分 386 × 112 被薄荷块截去, 目检同尺寸); 右缘 ≤ 793 < 颜色栏 1676 | 不压栏 | ✓ |
| 取色卡 / 完成钮 / 取色条 | 560 × 624 / 80 / 72 | 888 × 992 / 126 / 113, 完成钮下近黑 0.000 | — | ✓ (R7 不变) |
| 入口 / Sheet | 80 高 | 168 (2.1 px/dp) | — | 基准 |

## 2. R7 缺陷复验

| ID | 级别 | 结论 | 证据 |
| --- | --- | --- | --- |
| N7-01 新建项目编号 | P2 | **已修复** | `Localization.kt` `untitledPattern()` 按 `%1$d` 切分后分段 `Regex.escape` + `(\d+)`, 两语言合并; `UntitledPatternTest` 4 例; `r8-*-entry-after.png` 列表 "未命名 5 / Untitled 4 / 未命名 3 / 未命名 2 (36 块, 白色块)"; 索引同 |
| N7-02 新建窗口玻璃滞后 (模拟器) | P2 | **未再出现** (研发给 `selection` 标记前加 `WINDOW_SETTLE_MS`) | r8 两套 `selection` / `newproject` / `rail` 近黑 ≤ 0.02; 根因在模拟器, 真机 [H] |
| N7-03 取色条压颜色栏页签行 | P2 | **未修 (接受)** | `r8-*-eyedrop.png` 同 R7; 栏已禁用, 无功能影响; 留作润色 |
| N7-04 测量脚本 brush-bar 块 | P2 | **已修复** | 脚本改量前面平面深色圆钮 (117 px), 两套一致 |
| N7-05 取色卡盖住颜色栏表头 / 色槽 | P2 | **未修 (接受)** | `r8-*-edit.png` 同 R7; 关闭路径齐全 (✕ / 完成 / 切模式 / 开 Sheet) |

## 3. 验收清单 K4.69–76 (§T): **通过 7 · 部分 1**

| # | 结果 | 证据 / 说明 |
| --- | --- | --- |
| 69 | 通过 (申报偏离) | `BrushRow.kt` 仅三组 `SizeStepper` (`ActionIcon` 72 ×6, 值 `label` 20 宽 56, 轴字母宽 28, "×" `caption` 16 宽 24, 行 884 × 80, 内边距 16 / 4); `Brush` 枚举、`BarFill`、`brushes` / `barLength` / `barAxis`、`boxAnchor` 拖拽定盒全部删除; `brushSize` 为专用 `BrushSize(1,1,1)` (非 `Cell`, 语义同), `clamped()` 1..min(16, N); `BoxFill` 保留为 `SizedBrush.fill/erase` 的区域助手 (非用户路径) |
| 70 | **部分** | `SizedBrushTest` 8 例覆盖 (a) 单格 (b) 展开 (c)(d) 每轴独立翻转 (e) 两向不够取多侧裁边 (f) 跳过占用 / 全占用空集 (g) 擦除只删存在 + 尺寸夹取 + 区域归一; **缺 (h) "> 2000 格触发 largeFill"** (`LARGE_THRESHOLD` 改 2000 只在 `EditorViewModel.kt:232` / `EditorScreen.kt:416` 使用, 无单测) → N8-02 |
| 71 | 通过 | `r8-*-size.png` (`crop-*-size-preview.png`): 行 3 · 2 · 1, (1,0,1) 起薄荷 3 × 2 × 1 块 + 底座落点 (块 XZ 投影) + 标签 "3 × 2 × 1 / 松手放置" / "3 × 2 × 1 / Release to place" 在块上方; vox 六格 ✓ (§1); 样例在 (1..3, 0..1, 1) 无方块 (全空 → +6, 与 195 计数一致) |
| 72 | 通过 | 行 ≈ 1414 × 128 px 居中 (324..1922), 下缘 2063 < 2158, 步进钮 117 px ≥ 92 |
| 73 | 通过 | 行内底座网格 / 灰蓝层占比 0.0000 (12 帧 × 2 语言), 目检行玻璃完整遮住其后的底座近边; 用 z 0.97 + offset −320 达成 (见 §4) |
| 74 | 通过 | §1 字串 |
| 75 | 通过 (代码 + 截图) | `DemoMode.walk`: 3 × 2 × 1 → ERASE → ADD (U6, `effectiveBrushSize` 在 PAINT / SELECT 强制 1 × 1 × 1 不改写 `brushSize`); `r8-*-paint.png` / `selection` / `dragmove` 行隐藏 (底部带无玻璃), `r8-*-vivid.png` 回 ADD 行重现; 三值常显, 改任一轴 1 次点击 |
| 76 | 通过 | U1 `combinedClickable` 0; U2 模式 / 尺寸 / 槽 / 色板 / 撤销 / 重做 / 回正 / 语言 均在常驻栏 (二层仅 编辑颜色 / 导出 / 项目); U3 步进钮为 SDK `IconButton` (内置 hover), 自绘容器 3 处 `spatialHoverEffect`; U4 颜色栏 / 选区面板 / 笔刷行 / 标签 / 取色卡 全在画面内 (工具栏 [H]); U5 撤销常驻 (`EditorToolbar.kt`); U6 见 K4.75; U7 `r8-*-vivid.png` 红槽 + vox 红块; U8 `r8-zh-lang-toggle.png` / `en/r8-lang.png` 一帧切换, X Y Z / × 不变; U9 `previewFor` 同步更新, 无 delay; 标签文案按 §T.4 (ghost "✓ 已对齐 · 松手放置 / X1 · Y2 · Z2", size "3 × 2 × 1 / 松手放置", erase "松手擦除", occupied "格已占用 / 换一个格子", en 同) |

R7 已验行为复核 (全部不变): 颜色栏 (行 2 ▤ ↑ 中|EN, 色板 96 px) ✓; 选区拖动面板末行 "ΔX +2 · ΔY 0 · ΔZ −1 · 松手移动 5 块" / "… · Release to move 5 cubes" ✓; 取色卡 / 取色条 / 关闭 ✓; 标签在目标上方不压栏 ✓; 语言切换 ✓; 入口重开 (`r8-*-entry-after.png`, `r8-export-log.txt` "entry window reopen requested") ✓; 新建项目白槽 + 鲜艳页深环 (`r8-*-newproject(-vivid).png`, 行显示 1 × 1 × 1) ✓; EN 全部单行无省略 ✓。K4.1–68 其余结论沿用 R7 (K4.2 / 39 工具栏 [H], K4.18 / 25 真机, K4.61 部分 = N7-05 接受)。

## 4. 申报偏离的判定

| 偏离 | 判定 | 理由 |
| --- | --- | --- |
| 笔刷行 offset −24 → **−320** (anchor (0.5, 1, 0.97) BottomCenter) | **可接受** | §T.3 自带 "下缘出画则加大 offset" 备选; 研发探针 (`r8/probe/brush-off*.png`) 证明本视点 −24 / −120 / −160 整行出画, −200 只露行顶; −320 时整行在画面内且在底座前方 (K4.72 / 73 全过)。含义: 行在 Volume 底边上方 320 dp (约底座高度), 真机视场更大时可用 `gridspace_brush 0.5,1,0.97,0,-24,0,0` 复核规格值 [H]; 若真机从更低视点看, 底座近角 (z ≈ 0.98, y ≈ 0.93) 理论上可能露到行前, 届时改 z 1.0 |
| `BrushSize` 而非 `Cell`; `BoxFill` 保留为助手; `LARGE_THRESHOLD` 2000 | 可接受 | 语义同 §T.1 / T.2; 见 K4.69 / N8-02 |
| 选择步五块由静默 5 × 1 × 1 单击补上 | 可接受 | walk 结果与 R7 等价 (vox 链一致) |
| zh 首次整套导出失败 → 归档旧文件后重跑 | 可接受 (环境) | 但暴露了导出器的唯一名上限, 记 N8-01 |

## 5. 新增缺陷

无 P0 / P1。

| ID | 级别 | 标题 | 位置 | 期望 | 实际 | 复现 |
| --- | --- | --- | --- | --- | --- | --- |
| N8-01 | P2 | 同名导出 ≥ 32 次后导出失败 ("Failed to build unique file") | `VoxExporter` 依赖 MediaStore 自动加 " (N)" 唯一名, 系统最多尝试 32 次 | 导出永远成功 (v1.0 §H) | 研发首次 zh 整套 `exported` 帧报错 (`r8/probe/zh-run1/`), 移走 31 个旧 `庭院小亭 (N).vox` 后成功; 真机长期使用同名项目会复现 | 同一项目导出 32 次 |
| N8-02 | P2 | K4.70 (h) 缺单测: > 2000 格的 `largeFill` 路径无覆盖 | `SizedBrushTest` 8 例无 (h); `BoxFill.LARGE_THRESHOLD` 2000 | K4.70 (a)–(h) | 7/8 条目覆盖 | — |
| N8-03 | P2 | 笔刷行在空项目 / 1 × 1 × 1 时三个 "−" 钮全部禁用态灰, 行视觉偏"无效" | `SizeStepper` enabled = value > 1 | 系统禁用 alpha (规格) | `r8-*-workbench.png` 三灰钮; 规格行为, 仅观感 | 打开任意项目 |

N7-03 / N7-05 保持 "接受" (润色项)。

## 6. 发布结论

**可发布 (模拟器范围)**: §T 统一尺寸笔刷按规格落地 (三组 X/Y/Z 步进 117 px, 行完整在画面内且在底座前方, 3 × 2 × 1 预览 / 落点 / 标签 / +6 提交 / 导出逐格一致, 上色 / 选择隐藏行, U1–U9 成立), R7 全部已验行为无回归, N7-01 / N7-02 / N7-04 已修, 构建 60/60, 无崩溃 / ANR, 新增缺陷均 P2。本报告取代 R7 结论。

建议 (不阻塞, 可与真机验证并行):

1. **N8-01** 导出唯一名: 同名存在时自行追加时间戳 (或在 32 次失败后回退到时间戳名), 加单测; 真机首轮验证前完成为佳。
2. N8-02 补 `SizedBrushTest` (h): 16 × 16 × 8 = 2048 > 2000 → `largeFill` / 标签 "较大, 稍候"。
3. N7-03 / N7-05 / N8-03 润色: 取色条锚到取色卡顶边; 规格 §S.5 改文; 可考虑 "−" 禁用态用更淡的描边而非实心灰。

真机 [H] 清单 (沿用 R7, 新增第 1 条):

- 笔刷行 anchor (0.5, 1, 0.97) offset −320 在真机视场下的位置与底座近角遮挡 (`gridspace_brush 0.5,1,0.97,0,-24,0,0` 与 `0.5,1,1,0,-340,0,0` 对比); 步进钮眼手 / 手柄命中。
- 系统 `Toolbar` (1010 设计 dp, 不缩放) 完整可见与模式钮 ≥ 72 等效 dp。
- 侧卡 z 0.28 / 8、取色卡 (0.88, 0.42, 0.90)、`LabelAnchor` 夹取 [0.20, 0.58] 与 `VIEWER_DISTANCE 0.5` 在真机视点下的位置。
- 新建空间窗玻璃滞后 (N7-02) 是否在真机出现。
- 眼手 / 直接捏合 / 手柄: 72 等效 dp 命中率, 已选方块拖动, 选择模式拖底座 = 轨道, 滑杆 poke, 两手缩放; 系统关闭钮重开入口; ProjectSheet 切换 / 删除; 冷启动语言偏好。
