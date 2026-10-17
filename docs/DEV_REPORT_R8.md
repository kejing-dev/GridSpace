# GridSpace 格间 · 研发报告 R8

日期 2026-10-13 · SDK 6.1.9 · 模拟器 `Pico_Emulator_6_1_fresh` (本轮中途重启一次, 见 §4) · 依据: 协调者 R8 三项, `docs/DESIGN_SPEC_R4.md` **§T (统一尺寸笔刷)**, orchestrator 已合入的 N7-01 修复 (`Localization.kt` `Context.untitledPattern()`, `ProjectRepository.kt` `Regex.untitledNumber()`, `UntitledPatternTest`) 原样保留

截图 `docs/screenshots/r8/zh/` 与 `en/` (完整集, `bash docs/scripts/r8.sh zh|en`, 每张的 cap.sh 记录在 `r8-run.log`), 探针 `docs/screenshots/r8/probe/`。

## 1. 修改

| # | 修改 (文件) | 说明 |
| --- | --- | --- |
| 1 一个笔刷 = 选定尺寸 | 新 `domain/usecase/SizedBrush.kt`: `BrushSize(x, y, z)` (1..min(16, N), `isUnit` / `count` / `with(axis)` / `clamped(N)`) + `SizedBrush.span / region / cells / fill / erase` (沿 +X/+Y/+Z 展开, 每轴独立翻转, 都不够则取格数多的一侧裁到边, 添加跳过占用、擦除只删存在, 一步历史); `BarFill.kt` 删除, `BoxFill` 保留为区域填充 / 清除 (`LARGE_THRESHOLD` 10 000 → **2 000**, §T.2 步 8); `Brush` 枚举、`brushes[mode]`、`barLength` / `barAxis`、`BrushSelected` / `BarLengthChanged` / `BarAxisSelected`、拖拽定盒 (`boxAnchor`) 全部删除; `EditorUiState.brushSize: BrushSize` (会话级) + `effectiveBrushSize` (上色 / 选择 强制 1 × 1 × 1 不改写), 事件 `SizeStep(axis, ±1)`; `EditorViewModel`: 单击按块提交, 捏合保持 `previewFor` 每帧按目标格重算 `Preview.BlockAdd(region, color, blocked)` / `BlockErase(region)` (1 × 1 × 1 仍是 `Ghost` / `EraseCell`, 保留 "✓ 已对齐" 文案), 目标格被占用且尺寸 > 1 → `blocked` (珊瑚线框 + 占用文案, 松手不提交); 标签 `label_size_dims` "W × H × D" / `label_release_place`, 大块追加 " · 较大, 稍候" | 单测 `SizedBrushTest` 8 例 (单格 / 展开 / 每轴翻转 / 裁边 / 尺寸夹取 / 跳过占用 / 擦除 / 区域归一), `BarFillTest` + `BoxFillTest` 删除; 字串删 `brush_voxel / brush_box / brush_bar / bar_length / bar_axis`, `label_box_dims` → `label_size_dims`, 新增 `brush_size` (尺寸 / Size), `size_times` (×, 不翻译); 两语言 100 / 100 键一致 |
| 2 笔刷行 | `BrushRow.kt` 重写: `X [−] n [+] × Y [−] n [+] × Z [−] n [+]`, 884 × 80 (组 252: 轴字母 28 · 8 · 钮 72 · 8 · 值 56 · 8 · 钮 72; 分隔 "×" 24 + 12 × 2), 到界钮禁用, 行 `contentDescription` = `brush_size`; Augment anchor **(0.5, 1, 0.97) BottomCenter (0, −320, 0)** (协调者 / §T.3 给 −24, 见 §5), 不包 `EditorScaled` (`BRUSH_UI_SCALE` 删除), 保留 `settleRedraw` | §2 实测; 遮挡 / 下缘校核见 §3 |
| 3 演示 / 脚本 | walk: `bar` + `box` 步 → `size` (3 × 2 × 1 预览 (1,0,1) 并提交, +6 块); 选择步所需的 z = 14 五块改为静默 5 × 1 × 1 单击 (8,0,14) (+5), 之后尺寸回到 1 × 1 × 1; R7 的 `box` 探针脚本改为 `size`; `docs/scripts/r8.sh` (= r5.sh 的 r8 版, 完整集); `measure_r4.py` 新增笔刷行步进钮 (前面平面, 深色圆钮) 与 `size` 标签 | §2 |

## 2. 实测 (2160 px 截图, `python -P docs/scripts/measure_r4.py docs/screenshots/r8 r8`)

zh / en 数值相同 (±2 px); 新增的笔刷行步进钮与 `size` 标签两行是本轮加入脚本的测量项。

| 表面 (scale) | 目标 | 设计 dp | R7 实测 px | **R8 实测 px** | px / Volume-dp | 阈值 |
| --- | --- | --- | --- | --- | --- | --- |
| 笔刷行 (1.0, z 0.97, offset −320) | 步进钮 `IconButton` | 72 | R7 行在 z 0.75 ×1.3: 项 190 × 112 | **117 宽 (中位; 90–124) × 28 高的深色游程** (圆钮直径 ≈ 117), 行 y 1882..1908 px 处为钮的中带 | 1.62 | ≥ 92 ✓ |
| 笔刷行 | 整行 884 × 80 | 884 × 80 | — | ≈ 1414 × 128 (x 324..1922, y ≈ 1880..2063; 下缘距画面底 95 px) | 1.6 | 全行在画面内、在底座玻璃前方 ✓ |
| 目标标签 ghost (1.0, z 0.92) | 卡 | 360 × 96 | 558 × 158 | **558 × 158** | 1.55 / 1.65 | 不压颜色栏 ✓ |
| 目标标签 occupied | 卡 | 360 × 96 | 590 × 156 | **590 × 156** | 1.64 / 1.62 | ✓ |
| 目标标签 size (3 × 2 × 1) | 卡 | 360 × 96 | — (R7 为 bar / box) | 差分 386 × 112 (差分区被薄荷块截去左部; 目检与 ghost 同尺寸) | — | 在块上方、不压颜色栏 ✓ |
| 颜色栏 (1.5, z 0.28) | 色板格 | 88 | 96 × 95 | **96 × 95** | 0.73 | ≥ 92 ✓ |
| 颜色栏 | 色槽热区 | 88 | ≈ 93 | **≈ 93** | 0.70 | ≥ 92 ✓ |
| 选区面板 (1.5, z 0.28) | 步进钮 | 84 | 94 | **94** | 0.75 | ≥ 86 ✓ |
| 取色卡 (1.0, z 0.90) | 卡 / 完成钮高 | 560 × 624 / 80 | 888 × 992 / 126 | **888 × 992 / 126** | 1.59 | 完成钮下近黑 0.000 ✓ |
| 取色条 (1.0) | 取消钮 高 | 72 | 113 | **113** | 1.57 | ✓ |
| 入口 / Sheet (系统 Planar) | 主钮 高 | 80 | 168 | **168** (Sheet 内容宽 ≈ 414 dp) | 2.1 px/dp | ✓ |

## 3. 截图逐张

截图省略前缀 `docs/screenshots/r8/`; zh / en 同名对应 (每语言 35–36 张, 9 宫格预览在 `probe/grid-{zh,en}-N.png`)。

| 帧 | 观察 |
| --- | --- |
| `entry` / `entry-after` | 入口窗 1200 × 800, 列表含样例与各轮新建项目; `entry-after` 为 export 脚本关闭编辑器后重开 |
| `newproject` / `newproject-vivid` | 空白项目: 槽 1 白 + 描边, 柔和页 / 鲜艳页 (白格深环); **笔刷行 X 1 × Y 1 × Z 1 在底部** (空项目也显示, 添加模式) |
| `workbench` | 样例 + 颜色栏 + 笔刷行 `X [−] 1 [+] × Y [−] 1 [+] × Z [−] 1 [+]` (− 钮禁用态灰), 整行在底座近边前方、画面内 |
| `ghost` / `occupied` | 1 × 1 × 1: "✓ 已对齐 · 松手放置 / X1 · Y2 · Z2" 与 "格已占用 / 换一个格子" (R7 不变) |
| **`size`** | 行显示 **3 / 2 / 1**, (1,0,1) 起 3 × 2 × 1 薄荷块 + 底座落点 (块的 XZ 投影) + 标签 "3 × 2 × 1 / 松手放置" ("3 × 2 × 1 / Release to place") 在块上方 |
| `erase` / `paint` | 1 × 1 × 1 擦除 "松手擦除" / 上色 "松手上色"; 行回到 1 × 1 × 1 (U6: 尺寸在 ERASE→ADD 往返后仍 3 × 2 × 1, 之后脚本把它调回 1) |
| `selection` … `delete` | 选择模式: 笔刷行隐藏, 选区面板 + 五块 (静默 5 × 1 × 1 单击放的) 拖动 / 旋转 / 复制 / 步进 / 删除 与 R7 相同; `dragmove` 面板末行 "ΔX +2 · ΔY 0 · ΔZ −1 · 松手移动 5 块" |
| `vivid` / `edit` / `eyedrop` / `edit-close` | 鲜艳页红块、取色卡 (888 px, 完整)、取色条、关闭 (R7 不变); 笔刷行在 ADD 模式重新出现 |
| `lang` / `lang-back` | 一帧切换 EN / 回中文; 笔刷行轴字母 X Y Z 不变, `×` 不翻译 |
| `undo` / `burst-1..4` / `redo` / `reset` | 历史与回正; burst 帧表头 "已自动保存" |
| `export` / `exported` / `projects` / `done` | 导出表单 → "已导出 … 195 块 · 2 KB" → 项目 Sheet (庭院小亭 195 块 · 正在编辑) → 关闭 |

## 4. 构建 / 测试 / 导出

```
./gradlew :app:assembleDebug --offline -q          → EXIT 0
./gradlew :app:testDebugUnitTest --offline -q      → EXIT 0, 单测 60/60 (R7 64 − BarFill 7 − BoxFill 5 + SizedBrush 8)
dex: SpatialApplication refs across all dex: 3 (exit 0)
r8.sh zh / en: zh 32 次捕获 (第二次整套, 见 §5) / en 33 次; walk 26 标记到 done (bar / box → size, 少 1); 保留 36 / 35 张; logcat 无 AndroidRuntime / FATAL; 黑块帧仅 zh burst-2、en burst-4 (burst 只看表头)
settings list global | grep gridspace 为空
```

导出核对 (`python -P docs/scripts/voxdump.py`):

```
docs/export/r8-en-walk.vox  (walk 步 exported, "Garden Pavilion (10).vox")   size 16³ · 195 块
  #f3f5ed 147 · #f3bc9c 27 · #a5edcc 20 · #ff3b30 1
  (1,0,1) (3,1,1) = #f3bc9c ← 3 × 2 × 1 块的两个角 (x 1..3, y 0..1, z 1) ✓; (4,0,1) 空 ✓ (块宽 3)
  (0,0,1) = #ff3b30 (鲜艳页) ✓; (1,0,8) 空 (R4 长条位置, 已无) ✓
  计数: 184 +1 (幽灵) +1 (占用步先放的块) +6 (size) +5 (五块) −1 (擦除) −2 (旋转覆盖地板) +5 (复制) −5 (删除) +1 (红块) = 195 ✓ (R4/R7 为 193: 长条 4 → 尺寸块 6)
docs/export/r8-zh-walk.vox  (第二次 zh 整套, "庭院小亭.vox")                      195 块, 逐色逐格与 en 相同 ✓
docs/export/r8-{zh,en}-newest.vox (export 脚本, 样例原样) 184 块 ✓
```

模拟器: R8 开始时发现视点已变 (模型旋转、颜色栏右缘出画, `probe/p1-workbench.png`; `uptime` 8 h, 非重启; 推测模拟器窗口被拖动改变了头部朝向), 按 AGENTS 重启 (`pico-cli emulator stop / start --avd Pico_Emulator_6_1_fresh`) 后视点回到 R5–R7 的"重启后视点" (`probe/p3-vs-r7.png`), 全部 R8 截图在此视点下拍摄。

## 5. 偏离 / 备注

| 条目 | 说明 |
| --- | --- |
| 笔刷行 offset −24 → **−320** | z 0.97 的底面在本视点投影在 ≈ 2580 px (画面 2158), −24 / −120 / −160 时整行在画面外, −200 只露出行顶 (`probe/brush-off*.png`); §T.3 的"下缘出画则加大 offset"备选: −320 时行在 y 1900..2063 px, x 324..1922 (`probe/brush-0.5,1,0.97,0,-320,0,0.png`), 完整在底座玻璃前方; 备选 z 1.0 / −340 (`probe/brush-0.5,1,1,0,-340,0,0.png`) 效果相同、行稍大 (1.83 px/dp), 未采用。真机上 −320 = 行在 Volume 底边上方 320 dp (约底座高度处), 可用 `gridspace_brush 0.5,1,0.97,0,-24,0,0` 复核规格值 |
| `BrushSize` 而非 `Cell` | §T.1 写 `brushSize: Cell`; 实现用专用 `BrushSize(x, y, z)` (带 `isUnit / count / with / clamped`), 语义相同 |
| 事件名 | §T.1 `SizeStep(axis, delta)` 照用 |
| 选择步的五块 | §T 删除 `box` 步后, walk 的选择 / 拖动 / 旋转 / 复制 / 删除仍需 z = 14 的一排五块, 用 5 × 1 × 1 单击 (8,0,14) 静默补上 (+5, 不截图), 之后尺寸回 1 × 1 × 1 |
| 模拟器 | 见 §4: 视点漂移 + 两次重启 + 黑块期 |
| zh 第一次整套的导出失败 → 整套重跑 | 第一次 zh walk 的 `exported` 帧为 "导出失败: Failed to build unique file … 庭院小亭.vox": 模拟器 `Download/GridSpace/` 里已有 R1–R8 各轮留下的 31 个 `庭院小亭 (N).vox`, MediaStore 的唯一文件名最多尝试 32 次后放弃 (环境限制, 非应用缺陷; en 名 "Garden Pavilion" 只有 11 个所以成功)。处理: 把旧的 `庭院小亭*.vox` **移到** `Download/GridSpace-archive/` (未删除), 同时给 walk 的 `selection` 标记前加 `WINDOW_SETTLE_MS` (第一次 zh `selection` 帧面板下半未绘出), 重建 (BUILD / TEST / dex 同上) 后 zh 整套重跑; 第一次的 zh 集保留在 `probe/zh-run1/`。en 集来自前一个 APK (差别仅 demo 等待时间, UI 代码相同) |
| 真机 | 若真机也出现同名文件 ≥ 32 的导出失败, 需在导出器里换唯一名策略 (如时间戳), 本轮未改 |
