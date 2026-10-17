# GridSpace 格间 · 研发报告 R5

日期 2026-10-13 · SDK 6.1.9 · 包名 `com.kejing.gridspace` · 验证环境: PICO 模拟器 `Pico_Emulator_6_1_fresh` (emulator-5554, 本轮未重启; 视点为 R4 末尾重启后的"近"视点, 见 §2.1) · 依据: `docs/QA_REPORT_R4.md` §4 N4-01…N4-09 与 §6 修复顺序, `docs/DESIGN_SPEC_R4.md` **§S (R5 尺度修正)**, `AGENTS.md`

范围: QA R4 缺陷清单全部 9 项 (N4-01 … N4-09), 不含新功能。截图 `docs/screenshots/r5/zh/` 与 `docs/screenshots/r5/en/` (各由 `bash docs/scripts/r5.sh zh|en` 一次生成, `r5-run.log` 含每张的 cap.sh 记录), 摆位探针 `docs/screenshots/r5/probe/`。

## 1. 逐缺陷: 修复 → 证据

截图省略前缀 `docs/screenshots/r5/`; `zh/` 与 `en/` 同名文件内容对应。

| ID | 修复 (文件:行) | 证据 |
| --- | --- | --- |
| **N4-01** Volume 侧尺寸 (P1) | 新增 `ui/editor/components/EditorScaled.kt:30` `EditorScaled(scale) { }` = `CompositionLocalProvider(LocalDensity provides Density(density × scale, fontScale))`, 常量 `EDITOR_UI_SCALE 1.5` / `LABEL_UI_SCALE 1.4` / `BRUSH_UI_SCALE 1.3` (`:49-55`); 包裹位置 = 每个 Augment / Popup 的 content 最外层: 笔刷行 `EditorScreen.kt:242`, 颜色栏 `:262`, 选区面板 `:281`, 目标标签 `:341`, 取色 Popup `ColorEditPopup.kt:67`; anchor / offset 仍在包裹外 (Volume-dp)。设计 dp 按 §S.2 上调: 色板 88 圆角 20 (`ColorRail.kt:86-88`), 色槽 热区 88 / 圆 72 (`:82-83`), 页签项 122 (`:86`), 栏宽 430 / 内容 382 (`:71-73`); 选区步进钮 / 值宽 84 (`SelectionPanel.kt:62-63, 140-145`); 笔刷行 929 × 80, 段控项 120 × 72 / 轴项 72 × 72 在 80 容器内 (`BrushRow.kt:43-51`); 目标标签 行 1 `title` 28 / 行 2 `body` 18 (`TargetLabel.kt:42-44`); 系统 Toolbar 内容按 §S.1 **不**包裹 (`EditorScreen.kt:229`) | §2 实测表: 色板 96 × 95 px, 色槽热区 ≈ 93 px, 步进钮 94 px, 笔刷项高 112 px (阈值 92 / 92 / 86 / 86); `zh/r5-workbench.png` / `r5-rail.png` / `r5-dragmove.png` / `r5-bar.png` / `r5-ghost.png` 与 `en/` 同名 |
| **N4-01** 工具栏 ≤ 1600 | 项目 / 导出 / 中|EN 移出 Toolbar, 进颜色栏行 2 (`ColorRail.kt:110-123`: `ActionIcon` 72 ×2 间 12 + `LanguageSegment` 144 × 72, 项 64 × 64); `EditorToolbarContent` 只剩 4 模式 · 撤销 / 重做 · 回正 (`EditorToolbar.kt:51-78`, 1010 设计 dp); `ColorRailState.language` (`ColorRail.kt:58`) 由 `AppGraph.language` 填入, `LanguageSegment` 新增取值重载 (`Controls.kt:160`) | `zh/r5-workbench.png` 颜色栏第 2 行 ▤ ↑ [中|EN]; `r5-lang.png` 从颜色栏切换生效; Toolbar 仍在截图区外 [H] |
| **N4-02** 标签压颜色栏 (P1) | `VoxelSceneView.kt` `labelCardOnRight(cellX, default, selectionPanelShown)`: 以目标格 (侧移前) 的归一化 x 判定, x > **0.56** → 卡片向左伸 (锚点也改到格子左侧, `CenterRight` −12 dp), x < **0.44** 且选区面板可见 → 向右伸, 否则 R3 规则; 阈值由几何得出 (格 75 + 间 12 + 卡 616 = 703 / 1600 = 0.44), §S.6 的 0.78 / 0.22 实测仍让卡片压进颜色栏 (§3); 单测 `LabelSideTest` 4 例 | `zh/r5-dragmove.png`: "ΔX +2 · ΔY 0 · ΔZ −1" 卡片在模型与颜色栏之间, 与颜色栏包围盒无交集 (§2 表 bbox); `zh/r5-box.png` "5 × 1 × 1" 同; `en/` 同名 |
| **N4-03** 取色面板黑带 | `ColorEditPopup.kt:60-65`: `defaultMinWidth 560 / defaultMinHeight 624` (= 内容, 取色条态 88); 水平 `HorizontalPlacement.alignEnd()` (javap 确认存在) 让 Popup 右缘对齐锚点右缘; 另外发现黑带是模拟器上"新建空间窗的玻璃 / 内容纹理滞后于窗口尺寸、要等内容再绘制才补齐"(§2.2): 新增 `ui/common/SettleRedraw.kt` `Modifier.settleRedraw()` 在合成后 0.5 / 1.5 / 3.5 / 7.5 / 15.5 / 31.5 s 各触发一次空绘制, `EditorScaled` (`EditorScaled.kt:24`) 与入口窗根 (`EntryScreen.kt` `EntryContent`) 都挂它; walk 在 `edit` / `eyedrop` / `edit-close` / 每个标签 / `vivid` (笔刷行重建) 标记前等 4 s (`DemoMode.kt` `WINDOW_SETTLE_MS`) | `zh/r5-edit.png` 完成钮下近黑占比 0.000 (zh / en) (measure 脚本 "near-black share under Done"); 面板实测 612 × 681 px = 0.73 px/Volume-dp, 与颜色栏一致 → SDK 按包裹内密度换算 `defaultMin*` (§3) |
| **N4-04** Sheet 宽 | `ProjectSheet.kt:49-67` / `ExportSheet.kt:57-104`: 内容 `fillMaxWidth()`, 段控项 `weight(1f)`, 删除 720 / 640 / 172 / 320 固定宽 | `zh/r5-projects.png` 新建钮 868 px 宽 × 168 (导出 Sheet 同钮) 高 → Sheet 内容宽 ≈ **414 dp** (按钮高 80 dp 换算); `r5-export.png` 同宽; 四个网格尺寸项等分 |
| **N4-05** 选择模式拖底座 | 新文件 `ui/editor/DragRouting.kt:20` 纯函数 `route(locked, mode, cell, plate, inSelection)`: SELECT 且 cell == null (底座 / 空白) → ORBIT, 已选方块 → MOVE, 其它方块 → EDIT; `VoxelSceneView.kt:184` 调用; 单测 `DragRoutingTest` 6 例 | `./gradlew :app:testDebugUnitTest` 61/61 |
| **N4-06** 鲜艳页白格证据 | `DemoMode.kt:88-97` `newproject` 脚本: 标记 `newproject` (柔和页) → `PresetSelected(VIVID)` → 标记 `newproject-vivid` → `done`, 由 `walk.sh <out> r5 newproject` 同步截图 (walk.sh 新增第 3 参数 = 脚本名) | `zh/r5-newproject-vivid.png`: 槽 1 白 + 描边 + 白环, 鲜艳页 #FFFFFF 格 (右下) 带 BaseInk 60 % 深环; `r5-newproject.png` 柔和页 |
| **N4-07** 负号 | `ui/common/Numbers.kt:8` `signed(v, plus)` U+2212; 标签 `plus = true` ("+2"), 面板 `plus = false` (累计值不带 "+", §S.8) (`SelectionPanel.kt:143`, `EditorScreen.kt` `labelTexts`) | `zh/r5-dragmove.png` 面板 Z "−1" 与标签 "ΔZ −1" 同字形 |
| **N4-08** 十六进制 | `ColorEditPopup.kt:102-105` `GridType.caption` 16, 固定宽 112; `small` 角色在应用内不再使用 | `zh/r5-edit.png` "#FF3B30" 可辨 (16 × 1.5 sp) |
| **N4-09** 证据日志 | `docs/scripts/r5.sh` 全程 `tee r5-run.log`; 重拍命令一律 `bash docs/scripts/cap.sh <png> 5 \| tee -a <out>/r5-run.log` (AGENTS 已写); 本轮重拍见 §2.3 | `zh/r5-run.log` / `en/r5-run.log` |

## 2. 实测

### 2.1 px/dp 表 (2160 px 截图, `python -P docs/scripts/measure_r4.py docs/screenshots/r5 r5`)

脚本按 R5 布局改写: 色板 / 色槽 / 笔刷项按颜色游程定位, 选区面板 / 标签 / Popup 按差分包围盒; 参数 `[root] [prefix]`, 用 `-P` 而非 `-I` (Pillow 装在用户 site, `-I` 会屏蔽)。

| 表面 (scale) | 目标 | 设计 dp | 实测 px (zh / en) | px / 设计 dp | px / Volume-dp | 阈值 | R4 实测 (未缩放, 远视点) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 入口 Planar 窗 | 新建钮 | 440 × 80 | 入口 2.1 px/dp (导出 Sheet 主钮 80 → 168 px) | 2.10 | — | — | 1.27 |
| 颜色栏 (1.5, z 0.28) | 色板格 | 88 | **96 × 95** (中位 94) / 96 × 95 | 1.09 | 0.73 | ≥ 92 ✓ | 80 → 60 (0.75) |
| 颜色栏 | 色槽 圆 / 热区 | 72 / 88 | 76 × 67 / 76 × 71 → 热区 ≈ **93** | 1.06 | 0.70 | ≥ 92 ✓ (临界) | 64 → 42 (0.65) |
| 选区面板 (1.5, z 0.28) | 步进钮 | 84 | **94** × 77 (竖向被 "−" 字形打断) / 94 | 1.12 | 0.75 | ≥ 86 ✓ | 72 → 50–56 |
| 选区面板 | 卡片 | 400 × 584 | ≈ 421 × 627 (差分 448 × 643 含被移动方块) | 1.05 | 0.70 | — | 319 × 423 (0.80 / 0.77) |
| 取色 Popup (1.5) | 面板 | 560 × 624 | **612 × 681** / 616 × 684 | 1.09 | 0.73 | — | 485 × 448 (0.76 / 0.72) |
| 取色 Popup | 完成钮 高 | 80 | **87** / 89 | 1.09 | 0.72 | — | 60 |
| 取色 Popup | 完成钮下近黑占比 | — | **0.000** / 0.000 | — | — | = 0 ✓ (N4-03) | 0.04 / 0.13 |
| 笔刷行 (1.3, z 0.75) | 段控项 | 120 × 72 | **190 × 112** / 190 × 112 | 1.58 / 1.56 | 1.22 / 1.20 | 高 ≥ 86 ✓ | 110 × 60 (0.93) |
| 目标标签 (1.4) | 卡片 | 440 × 120 | 672 × 189 (dragmove 差分) | 1.53 | 1.09 | — | 319 × 89 (0.72) |
| 目标标签 vs 颜色栏 | 卡右缘 / 栏左缘 | — | dragmove 1644 / 1676, box 1619 / 1676 (zh 与 en 相同; 脚本对 en box 报 1694 是差分行里混入薄荷幽灵条, 裁图 `probe/crop-box-label-en-zh.png` 两语言卡右缘同在 ≈ 1620) | — | — | 无交集 ✓ | 压入 ≈ 100 px (N4-02) |
| 项目 / 导出 Sheet | 主钮 | 80 高 | 868 × 168 / 870 × 168 → 内容宽 **≈ 414 dp** | 2.10 | — | — | 495 × 95 (1.19) |

R4 → R5 换算成"入口等效 dp" (实测 px ÷ 2.10, 本轮入口 2.1 px/dp): 色板 46 → 96 px, 色槽热区 44 → 93 px, 步进钮 50 → 94 px, 笔刷项高 60 → 112 px, 完成钮 60 → 87 px; 以 R4 的 1.27 px/dp 入口基准折算, 色板 = 76 等效 dp, 色槽 73, 步进 74, 笔刷项高 88, 完成钮 69 (R4: 44–47 / 44 / 39–53 / 47 / 47)。

说明: "设计 dp" = 规格数值; Volume-dp = 设计 dp × scale; 入口窗 / Sheet 不缩放。模拟器视点: 本轮为 R4 末尾重启后的视点, 比 R4 主截图近 (R4 颜色栏 z 0.5 时 0.70–0.75 px/Volume-dp, 本轮同 z 约 0.98), 因此侧卡深度改为 z 0.28 (§3)。

### 2.2 玻璃滞后 (模拟器)

`docs/screenshots/r5/probe/settle-{20,35,50}.png`: 放大后的颜色栏 (1557 Volume-dp 高) 在冷启动后 20 s 仍有 75 % 面积黑色, 35 s / 50 s 稳定在 10.6 % (底部两行 + 编辑颜色钮无玻璃); 加入创建后重绘 (`settle2-35.png`) 后 35 s 为 0.0 %。R4 的取色面板黑带 (N4-03) 是同一现象 (窗口按内容建好, 系统玻璃 / 内容纹理只覆盖到较早一次布局的尺寸, 直到内容再次绘制); 冷启动的入口窗同样 (40 s 时下半部分未绘出, `probe/f3.png` 右), 故入口窗根也挂 `settleRedraw`。四次重绘不够时 (笔刷行 36 s 仍半黑, `probe/crop-brush.png`) 加到六次 (到 31.5 s)。walk 中标签 / Popup / 重建的笔刷行标记前等 4 s (第 3 次重绘之后); 新建的窗口在最初 ~2 s 内仍可能黑 (已知 AGENTS 规则)。

### 2.3 重拍记录

本轮没有单张重拍: 每次修正后都整套重跑 `r5.sh` (zh 三次, en 两次; 第一次 en 在发现 §3 标签阈值问题后中止), 每套的 `r5-run.log` 随 `rm -rf docs/screenshots/r5/<lang>` 一起替换, 现存日志即最终 APK 的那一套。日志中 "no clean frame" 的帧 (模拟器整屏黑块, AGENTS 已知): zh `stepx` (8 次均 > 0.66, 用 `en/r5-stepx.png` 作 K4 证据), zh `burst-1/2`, en `burst-3` (burst 只看表头保存点, 其余 burst 帧正常)。另两类残留的模拟器现象: zh `r5-vivid.png` (= `r5-rail.png`) 笔刷行下半黑 — 该行在 SELECT/PAINT 隐藏、切回 ADD 时重建, 4 s 的等待不够 (同一行在 10 s 后的 `r5-edit.png` 已正常); en `r5-bar.png` 左侧新建的标签卡 (出画, 见 §3) 内容尚未绘出。

### 2.4 构建 / 测试 / 静态

```
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
./gradlew :app:assembleDebug --offline -q          → EXIT 0
./gradlew :app:testDebugUnitTest --offline -q      → EXIT 0
单测 61/61: R4 51 + DragRouting 6 + LabelSide 4; 0 失败 0 跳过
dex: SpatialApplication refs across all dex: 3 (exit 0)
r5.sh zh / en: 各 33 次捕获 (walk 27 标记到 done + newproject 3 标记 + entry / workbench / entry-after); logcat 无 AndroidRuntime / FATAL (grep 命中的唯一一行是系统 SysPerfMonitorService 的 getSysFatal 调用栈, 与本应用无关); /data/anr 最新仍为 10-07;
设置全部清除 (settings list global | grep gridspace 为空)
```

## 3. 偏离 / 未达成

| 条目 | 说明 |
| --- | --- |
| 标签翻边阈值 §S.6 0.78 / 0.22 → **0.56 / 0.44**, 且按目标格 x (侧移前) 判定 | 0.78 时 `dragmove` / `box` 两张的卡片右缘仍压进颜色栏约 100 px (`probe/n1.png`, `n2.png`, 第一轮截图): 卡 616 Volume-dp 从 x 0.62–0.72 向右伸必然越过 x = 1 的右面; 翻边时锚点一并改到格子另一侧, 否则卡片会盖住目标格 (N-01)。§S.6 的验收标准 (包围盒无交集) 以新阈值满足 |
| 侧卡 anchor z 0.5 / offset 32 (§S.3) → **z 0.28 / offset 8** (`EditorScreen.kt:113-116`) | 本轮视点下 z 0.5 把放大后的颜色栏右 1/3 推出画面 (`probe/p1-workbench.png`); 探针 (`probe/rail-*.png`): z 0.35/16 色板止于画面边, z 0.3/16 玻璃角被裁, z 0.25/32 整卡可见但色板 85–91 px, z 0 在模型后; z 0.28/8 整卡距画面边 ≈ 20 px 且色板 90–94 px。选区面板同参数镜像。真机用 `gridspace_rail 1,0.5,0.5,32,0,0,1` 复核 |
| Popup `defaultMin*` 单位 | 以设计 dp (560 / 624) 传入且 `EditorScaled` 在 Popup 内容内; 实测面板 0.73 px/Volume-dp 与颜色栏相同, 说明 SDK 按内容实际像素 (= 包裹内密度) 建窗, **不需要**改为 840 / 936 |
| 中|EN 段控 144 × 64 (§S.3 行 2) | 实现为 144 × 72 (项 64 × 64, `contentPadding` 4): SDK `SegmentControl` 内边距 4 使容器 72; 行高 72 与 IconButton 等高 |
| 面板正值不带 "+" | 按 §S.8 (面板为累计值); 简报写 "U+2212 minus in stepper values" 与之一致 |
| Toolbar 不包裹 `EditorScaled` | 按 §S.1 (假定系统按比例渲染); 模拟器截图看不到, 真机 [H] |
| 翻边后的标签被近处立柱遮挡 | `dragmove` / `box` 标签改为向左伸后, 卡片有一部分落在样例右前立柱之后 (`zh/r5-dragmove.png` "松手移动 5 块" 右半、`r5-box.png` "5 ×" 被柱子挡), 文字主体仍可读; 这是 3D 遮挡 (标签 z 只比目标格前 0.8 格), 不是与颜色栏的重叠; 加大前推会把卡片推到格子上 (AGENTS R3 教训), 留给 R6 评估 |
| 左侧无选区面板时标签伸出画面 | `ghost` (1,2,2) / `bar` (0,0,8) / `occupied` (1,0,0) 的标签按规则向左伸, 在本轮较近的视点下左缘出画 (`zh/r5-ghost.png` 只见 "齐 · 松手放置"); §S.6 只在选区面板可见时向右翻, 按规格保留; R4 远视点下同一规则在画面内 (`r4/zh/r4-ghost.png`) |
| 模拟器视点 | 与 R4 主截图不同 (更近), 不可控; 两轮 px/dp 不能直接比较, 表中给出 Volume-dp 口径 |
| 玻璃滞后重绘 (`EditorScaled` 四次 `drawBehind` 失效) | 仅为模拟器现象的规避, 对真机无副作用 (四次空绘制) |

## 4. 仅真机可验 [H]

- 系统 Toolbar (1010 设计 dp, 不缩放) 在真机距离下的可读性与命中; 若系统不按比例渲染, 需同样包裹 `EditorScaled(1.5)` (1515 ≤ 1600)。
- 侧卡 z 0.28 vs 规格 0.5 在真机默认放置距离下的可见性 (`gridspace_rail` / `gridspace_selpanel`)。
- 选择模式下底座捏合 = 轨道 (N4-05) 的手感; 眼手 / 手柄三种输入。
- 取色 Popup 在真机上是否仍有玻璃滞后 (若有, 重绘次数 / 间隔可调 `SETTLE_REDRAW_MS`)。
- 系统关闭钮 / ProjectSheet 切换与删除 (R4 遗留)。
