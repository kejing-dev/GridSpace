# GridSpace 格间 · QA 报告 R7 (发布门, 覆盖 R5–R7)

日期 2026-10-13 · 被测构建: 本轮重新 `./gradlew :app:assembleDebug :app:testDebugUnitTest --offline -q` (EXIT 0, APK 47.3 MB 05:30, 全部 dex 含 `SpatialApplication` 3 处) · 设备: PICO 模拟器 emulator-5554 (`Pico_Emulator_6_1_fresh`, R5–R7 同一"近"视点, 研发截图; 本轮未重拍, 全部帧可读) · 依据: `docs/QA_REPORT_R4.md` N4-01…N4-09, `docs/DESIGN_SPEC_R4.md` §S + K4.56–68 (及 K4.1–55 未被取代部分), `docs/DEV_REPORT_R5.md` / `R6.md` / `R7.md`, 更新后的 `AGENTS.md`

证据: `docs/screenshots/r5/{zh,en}/` 全套 walk (各 33 帧), `r6/{zh,en}/` 10 帧, `r7/{zh,en}/` 7 帧 (ghost / occupied / bar / box / edit / eyedrop / edit-close, 取代 r5 / r6 同名帧); 本轮 QA 产物 `docs/screenshots/r7/qa/` (`view-*` 1000 px 预览, `crop-*` 放大裁剪); 像素测量 `python -P docs/scripts/measure_r4.py docs/screenshots/r{5,6,7} r{5,6,7}` (研发已改为按帧跳过; `-P` 而非 `-I`, Pillow 在用户 site); 导出核对: 本轮从模拟器拉取 R7 walk 导出 `docs/export/r7-{zh,en}-walk.vox` (R5 只拉了 export 脚本的 `r5-*-newest.vox`)。

## 1. 构建 / 测试 / 静态检查

| 项 | 结果 |
| --- | --- |
| `./gradlew :app:assembleDebug :app:testDebugUnitTest --offline -q` | EXIT 0 |
| 单测 | **60/60** (R4 51 + DragRouting 6 + LabelAnchor 3; R5 的 LabelSide 4 随 R6 删除); 0 失败 0 跳过 |
| dex `platform/SpatialApplication` | 全部 dex 求和 3, exit 0 |
| `androidx.compose.material*` / `FontWeight.Bold|SemiBold|W600|W700` / `combinedClickable|onLongClick` | 0 / 0 / 0 |
| Kotlin 内 CJK / `Text("` | 0 / 0 |
| `values` / `values-zh` | 103 / 103, 键集对称差 ∅; 未用键仅 `app_name` |
| 残留 56 / 48 / 40 dp | 7 处同 R4, 全部非可点 (分隔线 40, 表头 48, 结果图标 56, 滑杆拇指 48 (热区 72), 步骤行 56, 列表色块 56) |
| `spatialHoverEffect` | 3 处 = 全部自绘可点容器 (`ColorSlot`, 色板格, `ProjectRow`) |
| `backgroundMaterial` / `hoverable(` / `googlefonts` / `FORCE_CHINESE` / `BuildFlags` / `TitleAugment` / `smallMedium` / **`SpatialPopup`** | 全部 0 (取色面板已改 Augment) |
| `Modifier.border(` | 5 处, 全为色环 / 近白描边 |
| K4.68 取代常量 | grep `1376|898.dp|905.dp|640.dp|720.dp|GridType.small` 仅 `Main.kt` 入口窗 `windowConstraints(minHeight 720)` (与 Sheet 无关) |
| 字体 | `assets/fonts/` 4 文件, APK 内 4 条 |
| 依赖 | `gradle/libs.versions.toml` 与 ElementSpace 相同; `app/build.gradle.kts` 仅 namespace / applicationId 不同 |
| 崩溃 / ANR | r5 两套 logcat 0 字节; r6 / r7 的 `*-logcat-walk.txt` 各 1 行 = 系统 `SysPerfMonitorService.getSysFatal` 调用栈 (grep "FATAL" 误中, 非本应用); `/data/anr/` 最新仍为 10-07 15:27 |
| walk 步序 | r5 / r6 / r7 两套各 27 标记到 done, 顺序同 R4 |
| 导出 .vox (K4.44 / K4.22) | `r7-zh-walk.vox` = `r7-en-walk.vox`: 193 块 (#f3f5ed 147 / #f3bc9c 25 / #a5edcc 20 / #ff3b30 1); (0,0..3,8) 珊瑚 ×4, (0,4,8) 空 → 长条 +4 ✓; (0,0,1) #ff3b30 ✓; (8..11,0,14) 空, (12,0,11..15) 珊瑚 ×5 → 拖动 (+2,0,−1) → 旋转 链一致 ✓; (14,0,11..15) 空 ✓。`r5-*-newest.vox` 184 块 = 样例 ✓ |
| 项目 JSON | 新建项目槽 `#ffffff/#a5edcc/#f3bc9c`, `activeSlot 0` (R4 已验, 代码未改) |
| 清理 | `settings list global | grep gridspace` 为空 ✓ |
| 黑卡扫描 (颜色栏 / 笔刷行 / 左侧区域近黑占比) | r6 / r7 全部 0.00–0.02 ✓; r5: `burst-1/2/3` (zh/en) 与 zh `stepx` 为模拟器整屏黑块 (run.log "no clean frame", 已申报, zh stepx 以 en 帧代验); zh `rail`/`vivid` 笔刷行 0.65、en `ghost` 笔刷行 0.37、`newproject` 颜色栏下部 0.16/0.21、en `selection` 选区面板下半 0.24 = 新建窗口玻璃滞后 (N7-02) |

### 1.1 像素测量 (§S 阈值, 2160 px 截图; zh / en 相同)

| 目标 | 设计 dp × scale | R4 实测 | **R5/R7 实测 px** | 阈值 | 结论 |
| --- | --- | --- | --- | --- | --- |
| 颜色栏 色板格 | 88 × 1.5 (z 0.28) | 56–60 | **96 × 96** (中位 94) | ≥ 92 | ✓ |
| 颜色栏 色槽 圆 / 热区 | 72 / 88 × 1.5 | 42 / 56 | 76 / **≈ 93** | ≥ 92 | ✓ (临界) |
| 选区面板 步进钮 | 84 × 1.5 | 50–56 | **94** (en 114 含阴影) | ≥ 86 | ✓ |
| 笔刷段控项 | 120 × 72 × 1.3 (z 0.75) | 110 × 60 | **190 × 112** (workbench / ghost); 长条步进钮 ≈ 110 (`crop-r7-zh-brushrow-bar.png`) | 高 ≥ 86 | ✓ (脚本对 bar 帧的 "125 × 46 FAIL" 是定位到别的元素, N7-04) |
| 目标标签 (R7, 前面平面 z 0.92, 不放大) | 360 × 96 × 1.0 | 319 × 89 | **558–591 × 156–158** (1.55–1.65 px/dp); 行 1 字形高 zh "已" **28** / en "A" **25** | 大写高 ≥ 20 | ✓ |
| 取色卡 (R7, z 0.90, 不放大) | 560 × 624 × 1.0 | 485 × 448 | **888 × 992**, 完成钮 790 × **126**, 右缘 2089 < 2160; 完成钮下近黑占比 **0.000** | 黑带 = 0 | ✓ |
| 取色条 取消钮 | 72 | — | 113 高, 右缘 2051 | — | ✓ |
| 标签 vs 颜色栏 (r7) | — | 压入 ≈ 100 px | ghost 770 / occupied 789 / bar 840 / **box 1636** < 栏玻璃左缘 1676 | 无交集 | ✓ |
| 入口 / Sheet (系统 Planar) | 80 高主钮 | 100 (1.27) | 168 (2.10, 本视点更近); Sheet 内容宽 ≈ 414 dp | — | 基准 |

换算: 侧卡 0.70–0.75 px/Volume-dp (与 R4 同 z 口径一致), 笔刷行 1.2, 前面平面 1.55–1.65; 以本视点入口 2.1 px/dp 折算, 色板 = 46 入口等效 dp, 以 R4 的 1.27 基准折算 = 76 等效 dp (R4: 44–47)。§S 以像素阈值定义的目标全部达到。

## 2. R4 缺陷复验 (N4-01…N4-09)

| ID | 级别 | 结论 | 证据 |
| --- | --- | --- | --- |
| N4-01 Volume 侧有效尺寸 | P1 | **已修复** | `EditorScaled.kt` (`LocalDensity` × scale, fontScale 不变) 包在四个 Augment 与取色卡 content 最外层, anchor / offset 在外 (`EditorScreen.kt:262,283,304,317,321,357`); §1.1 全部阈值达标; 项目 / 导出 / 中|EN 移入颜色栏行 2 (`ColorRail.kt:110-123`), 工具栏只剩 模式 · 撤销 / 重做 · 回正 (`EditorToolbar.kt:63-78`, 1010) |
| N4-02 标签压颜色栏 | P1 | **已修复 (R7)** | `LabelAnchor.place`: 卡片在前面平面 z 0.92 目标上方, 夹取 x ∈ [0.20, 0.58]; `r7-*-box.png` 卡右缘 1636 < 1676, `r7-*-ghost/occupied/bar` 在画面内且目标格 + 落点可见 (`crop-r7-{zh,en}-ghost-label.png`); R5 的翻边规则与 R6 的 1.4 倍 (卡 ≈ 900 px, box 压栏 "YES") 均已被取代 |
| N4-03 取色面板黑带 | P2 | **已修复** | 根因 = 模拟器新建空间窗玻璃 / 纹理滞后 (`r5/probe/settle-*.png`), `Modifier.settleRedraw()` 六次空绘制; 面板改为固定 560 × 624 的 Augment (`ColorEditCard`), `r7-*-edit.png` 完成钮下近黑 0.000 |
| N4-04 Sheet 宽 | P2 | **已修复** | `ProjectSheet.kt` / `ExportSheet.kt` `fillMaxWidth()` + `weight(1f)`, 无 720 / 640 / 172; `r5-*-projects.png` 四项等宽, 新建钮 868 × 168 px; 研发申报 "SDK 固定宽 ≈ 414 dp" |
| N4-05 选择模式拖底座 | P2 | **已修复 (代码 + 单测)** | `DragRouting.route`: SELECT 且 cell == null → ORBIT, 已选 → MOVE, 其它方块 → EDIT; `VoxelSceneView.kt:170` 调用; `DragRoutingTest` 6 例; 真机手感 [H] |
| N4-06 鲜艳页白格证据 | P2 | **已修复** | `r5-*-newproject-vivid.png`: 槽 1 白 + 描边 + 白环, #FFFFFF 格深色匹配环 (`crop-r5-zh-newproject-vivid.png`) |
| N4-07 负号 | P2 | **已修复** | `Numbers.kt` `signed(v, plus)`; `r6-*-dragmove.png` 面板 Z "−1" 与末行 "ΔZ −1" 同字形 (`crop-r6-en-selpanel.png`) |
| N4-08 十六进制 | P2 | **已修复** | `caption` 16, 前面平面 1.6 px/dp → "#FF3B30" 在 `r7-*-edit.png` 直接可读 |
| N4-09 证据日志 | P2 | **已修复** | r5 / r6 / r7 `*-run.log` 每帧一条 cap.sh 记录, 重拍命令追加 (`r7/zh/r7-run.log:92,98`, `r6/en/r6-run.log:92,102,112`) |

无回归: R2 N-01 (目标格不被盖) 在 R7 标签规则下保持; N-02 / N-03 不受影响。

## 3. 验收清单

K4.1–55 (R4 结论沿用, 变化项): K4.6 / 7 / 9 / 10 / 14 / 20 / 40 / 41 / 42 / 43 / 50 由 "部分" 转 **通过**; K4.2 / K4.39 仍 "部分" (工具栏只能真机); K4.5 取代; K4.18 / 25 真机。合计 **通过 50 · 部分 2 · 取代 1 · 真机 2**。

K4.56–68 (§S): **通过 12 · 部分 1**。截图省略前缀 `docs/screenshots/`。

| # | 结果 | 证据 / 说明 |
| --- | --- | --- |
| 56 | 通过 (申报偏离) | `EditorScaled.kt` 存在; 常量 `EDITOR_UI_SCALE 1.5` / `BRUSH_UI_SCALE 1.3` / **`LABEL_UI_SCALE 1.0`** / `FRONT_PLANE_SCALE 1.0` (规格 1.4 → R7 决定: 前面平面已 1.6 px/dp, 1.4 使卡 ≈ 900 px); 五处包裹各一次, anchor / offset 在外; `GridSizes` / `RailSizes` / `SelectionSizes` / `BrushRowSizes` 为设计 dp |
| 57 | 通过 | §1.1: 色板 96 / 色槽热区 ≈ 93 / 步进 94 / 笔刷项高 112 / 标签字形高 25–28 |
| 58 | 通过 | `r5/zh/r5-workbench.png` 颜色栏玻璃 x 1676..2130 完整在画面内, 上下缘可见; 笔刷行玻璃 374..1852 (1478 px) 完整; 选区面板 (r6 dragmove) 完整; [C] 颜色栏 430 × 1038, 选区 400 × 584, 笔刷 929 × 80, 取色卡 560 × 624, 标签 360 × 96 (R7) |
| 59 | 通过 | `EditorToolbarContent` 4 模式 · 分隔 · 撤销 重做 · 分隔 · 回正; `r5/zh/r5-rail.png` 表头下一行 "▤ ↑ 中|EN" (`crop-r5-zh-rail.png`), 项 64 × 64 in 144 × 72; 工具栏 [H] |
| 60 | 通过 (规则取代) | R7 `LabelAnchor` 取代 0.78 / 0.22 阈值: `r7-*-box.png` 卡右缘 1636 < 1676, `r7-*-ghost/occupied/bar` 目标格 + 落点 + 标签同时完整; `LabelAnchorTest` 3 例; 拖动移动不再有浮动卡, 文案进选区面板末行 (`r6-*-dragmove.png` "ΔX +2 · ΔY 0 · ΔZ −1 · 松手移动 5 块" / "… · Release to move 5 cubes" 单行无省略, `r6-*-dragmove-commit.png` 回到 "拖动已选方块可整体移动") |
| 61 | **部分** | 取色卡右缘 2089 ≤ 2160 ✓, 近黑 0.000 ✓, 十六进制可读 ✓, 完成钮四周玻璃 ✓; 但卡片在模型右半前方 (0.88, 0.42, 0.90) 而非颜色栏旁, 颜色栏的表头 / 动作行 / 色槽被卡片盖住 (`r7-*-edit.png` 仅露出色板右列) — 规格 "栏顶可见" 未满足, 属申报偏离 (§4 判定可接受) |
| 62 | 通过 | 代码无固定宽; `r5-*-projects.png` / `r5-*-export.png` 四项等宽、钮 80 高 (168 px)、文件名行、列表行 ≥ 88 |
| 63 | 通过 | `DragRouting` + 6 单测; `HitResolver` 未改 (SELECT 下 Base 不再到达它) |
| 64 | 通过 | `r6-*-dragmove.png` "−1"; `SelectionPanel.kt:154` `signed(value, plus = false)` |
| 65 | 通过 | `r5-*-newproject.png` (柔和) + `r5-*-newproject-vivid.png` (鲜艳, 深环) |
| 66 | 通过 | 三轮六份 run.log 每帧有记录; "no clean frame" 只出现在 burst / stepx / exported (模拟器黑块) 且与文件状态一致; 重拍命令均追加 |
| 67 | 通过 | r5 两套 33 帧齐全 (含 newproject ×2 / entry / workbench / entry-after); 语言与 `gridspace_lang` 一致 (`r5-zh-lang-toggle.png` 一帧内颜色栏 "Soft|Vivid|Earth / Edit colour / Auto-saved", 笔刷 "Voxel|Bar|Box", 卡片尺寸不变; `r5-en-…` 反向); `r5-*-entry-after.png` 入口重开 (方案 A), `r5-export-log.txt` "entry window reopen requested" |
| 68 | 通过 | §1 grep |

目检补充 (协调者清单): 无卡片盖住模型大半 (取色卡占画面 41 % 宽, 模型左半与底座可见; box 标签只压住一根梁) ✓; 标签在目标上方且不压颜色栏 / 选区面板 ✓; EN 全部单行无省略 (选区面板末行、"Rotate 90°"、"Pick from cube"、"Edit colour · Slot 1") ✓; 白色默认槽 ✓; 长条 +4 与拖动提交 vox ✓; 入口重开 ✓; 语言切换帧 ✓。

## 4. 研发申报偏离的判定

| 偏离 | 判定 | 理由 / 条件 |
| --- | --- | --- |
| 标签锚点 x 夹取 [0.14, 0.80] → **[0.20, 0.58]** (`LabelAnchor.X_MIN/X_MAX`) | **可接受** | 以实测为据 (0.80 时 box 卡压栏, 0.60 相触 1 px); 验收标准 (无交集 + 目标可见) 满足; 代价是右半目标的卡片偏左于目标 (box 卡在梁上方偏左), 可读性不受影响; 真机视场更宽时可放宽 [H] |
| 取色卡改 Augment, 锚 **(0.88, 0.42, 0.90) CenterRight**, 无 "点外关闭" | **可接受** | `SpatialPopup` 无深度参数 (javap 核实), 规格位置 (1, 0.5, 0.95) 在本视点出画 (`r6/probe/p-left-edit.png`); 关闭路径齐全: ✕ / 完成 (`ColorEditCard`), 切换模式 (`EditorViewModel.kt:121` picker = null), 打开导出 / 项目 Sheet (`EditorViewModelExport.kt:15`, `EditorViewModelProjects.kt:22`); 规格 §N.3.3 / §S.5 的 "点外关闭" 与 "栏顶可见" 需改文 (K4.61 部分) |
| 侧卡 z 0.5 / offset 32 → **z 0.28 / offset 8** | **可接受** | 规格 §N.2 明文允许回退并要求并列截图 (`r5/probe/rail-*.png`, `rail-z-montage.png`); 放大后的卡在本视点 z 0.5 出画; 真机用 `gridspace_rail 1,0.5,0.5,32,0,0,1` 复核 [H] |
| `LabelAnchor.VIEWER_DISTANCE = 0.5` 按模拟器拟合 | **可接受** | 只影响深处格子标签的横向偏移, 夹取保证卡片在面内; 真机距离不同时按截图调常数 [H] |

## 5. 新增缺陷

无 P0 / P1。

| ID | 级别 | 标题 | 位置 | 期望 | 实际 | 复现 |
| --- | --- | --- | --- | --- | --- | --- |
| N7-01 | P2 | 新建项目编号失效: 每个新项目都叫 "未命名 1" / "Untitled 1" | `EntryScreen.kt` / `EditorScreen.kt`: `untitledPattern = Regex(Regex.escape(appString(R.string.untitled, 0)).replace("0", "(\\d+)"))` — `Regex.escape` 产生 `\Q未命名 0\E`, 替换后的 `(\d+)` 落在 `\Q…\E` 引号内被当作字面量, `nextUntitledNumber` 永远匹配不到 → 返回 1 | §G.1 / N.6.A "未命名 k" 递增 (R4 研发报告称第二轮为 "未命名 2") | `r5/zh/r5-entry.png` / `r5-entry-after.png` 列表 "未命名 1" ×3 + "Untitled 1" ×2 (不同时间); `r5-index.json` 9 条中 5 条 "未命名 1"、3 条 "Untitled 1"; `r5-*-projects.png` 同 | 连续两次 新建空白项目 |
| N7-02 | P2 (模拟器) | 新建空间窗首次出现数秒内下半部无玻璃 (黑) | 模拟器合成器玻璃滞后 (研发 R5 §2.2); `settleRedraw` 0.5–31.5 s 六次重绘只能逐步补齐 | AGENTS: ≥ 1.5 s 后无黑 | en `r5-selection.png` 选区面板下半黑 (left_black 0.24, 标记后 1.5 s), `r5-*-newproject.png` 颜色栏下部黑 (0.16 / 0.21, 启动后 40 s), zh `r5-rail.png` 笔刷行下半黑 (重建后 4 s); 同一卡片在后续帧全部正常 | `gridspace_demo walk` selection 步; 真机复核 [H] |
| N7-03 | P2 | 取色条 (滴管提示) 压在颜色栏预设页签行上 | `ColorEditPlacement` (0.88, 0.42, 0.90) 用于取色条, 条高 88 落在颜色栏中部 | §N.3.3: 颜色栏可见但禁用 | `r7-*-eyedrop.png` "点一个方块取色 · 取消" 盖住变暗的 柔和|鲜艳|大地 行; 栏已禁用, 无功能影响 | walk `eyedrop` |
| N7-04 | P2 (工具) | `measure_r4.py` 的 "brush row (bar)" 块定位错误, 三轮都报 125 × 46 FAIL | 脚本在 bar 帧沿行搜索亮游程时命中别的元素 | 与 workbench 帧一致 (190 × 112) | 人工裁剪 `crop-r7-zh-brushrow-bar.png`: 长条项 ≈ 192 × 112, 步进钮 ≈ 110 | 运行脚本 |
| N7-05 | P2 | 取色卡打开时盖住颜色栏表头 / 动作行 / 色槽 | 同 §4 第 2 项 | §S.5 栏顶 384 Volume-dp 可见 | `r7-*-edit.png` 色槽不可见 (K4.61 部分) | walk `edit` |

## 6. 可用性 / 与设计对照

| 画面 | 观察 |
| --- | --- |
| 工作台 (r5 / r7) | 颜色栏 454 px 宽、色板 96 px, 在画面右侧完整; 笔刷行 1478 px 宽贴底; 模型居中无遮挡; 与 §R.2 示意一致且观感尺度终于与入口窗同量级 (R4 的"小一半"已消除) |
| 目标标签 (r7) | 卡在目标正上方、前面平面, 字形 25–28 px, 不被梁柱挡, 不碰颜色栏; box 卡因夹取偏左于目标约 1 格, 仍指向明确 |
| 选区拖动 (r6) | 面板末行实时 "ΔX +2 · ΔY 0 · ΔZ −1 · 松手移动 5 块" 取代浮动卡, 步进值同步, 变暗原块 + 幽灵 + 白落点不变; 松手后末行回提示文案 |
| 取色卡 (r7) | 888 × 992 px 在模型右半前方, 全部控件可见可读, 无黑带; 代价是盖住颜色栏上部与模型右半 (可接受, 模态语义) |
| 入口 / Sheet | 2.1 px/dp 清晰; Sheet 内容自适应固定宽; 列表因 N7-01 出现同名项目, 用户无法分辨 |
| 双语 | 三轮全部帧中英对应, 切换一帧生效 |

## 7. 发布结论

**可发布 (模拟器范围)**: R4 的 9 项缺陷全部修复且无回归, §S 全部像素阈值达标 (色板 96 / 色槽 ≈ 93 / 步进 94 / 笔刷项 112 / 标签字形 25–28 px, 黑带 0), 构建 / 60 单测 / 导出 / 窗口流 / 双语 / 字体 / 吸附 / 拖动移动 全部通过, 新增缺陷均为 P2。四项申报偏离均可接受, 但都是按模拟器视点调出的常数, 需真机复核。

建议发布前顺手修 (不阻塞):

1. **N7-01** 编号: 用 `Regex("^" + Regex.escape(prefix) + "(\\d+)$")` (prefix = 去掉 `%1$d` 的 `untitled` 串) 或 `replace("0", "\\E(\\d+)\\Q")`; 加单测 "已有 未命名 1 → 下一个为 2"; 两语言各验一次。
2. N7-03 / N7-05: 取色条改锚在取色卡顶边 (或 y 0.2) 让颜色栏页签露出; 规格 §S.5 改文为 "卡片在前面平面, 关闭 = ✕ / 完成 / 切换模式 / 打开 Sheet"。
3. N7-04: 修脚本 brush-bar 块 (从分隔线右侧搜索), 使 R8 以后的阈值检查全自动。

真机 [H] 清单 (发布后首轮真机验证):

- 系统 `Toolbar` (1010 设计 dp, 未包裹 `EditorScaled`) 在真机默认距离下完整可见, 模式钮命中 ≥ 72 等效 dp; 若系统不按比例渲染, 需包裹 1.5 (1515 ≤ 1600)。
- 侧卡 z 0.28 / 8 vs 规格 0.5 / 32 (`gridspace_rail`, `gridspace_selpanel`); 取色卡 (0.88, 0.42, 0.90) vs 规格 (1, 0.5, 0.95) (`gridspace_popup`); `LabelAnchor.VIEWER_DISTANCE` 与 x 夹取 [0.20, 0.58] 在真机视场下的位置。
- 新建空间窗玻璃滞后 (N7-02) 是否在真机出现; 不出现则可缩短 `SETTLE_REDRAW_MS`。
- 眼手 / 直接捏合 / 手柄: 72 等效 dp 目标命中率, 已选方块拖动 (0.5 格死区, LIFT 60°), 选择模式拖底座 = 轨道 (N4-05), 滑杆 poke 跳值, 两手缩放。
- 系统关闭钮关闭 Volume → 入口重开 + 列表刷新; ProjectSheet 切换 / 删除; 冷启动 (无 `gridspace_lang`) 读 SharedPreferences 语言。
