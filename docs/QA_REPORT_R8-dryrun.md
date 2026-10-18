# GridSpace 格间 · QA 报告 R8 (空跑 / dry run)

日期 2026-10-17 · 被测构建: **本轮未重建、未重拍、未碰模拟器** (空跑指令); 复核对象为已有产物: `docs/gate/r8-{gate,logs}.txt`、`docs/gate/r8-measure.json`、研发 2026-10-13 的截图 / 导出 / run.log。`dexcheck.sh` 在现有 `app/build/outputs/apk/debug/app-debug.apk` (2026-10-17 17:27, 47.7 MB, **不是 R8 的 10-13 11:47 APK**) 上跑: refs 3, PASS · 设备: PICO 模拟器 emulator-5554 (研发重启后视点 = R5–R7 视点; 研发截图) · 依据: `docs/DESIGN_SPEC_R4.md` §T + K4.69–76 (无 `DESIGN_SPEC_R8.md`, K8 列表即 §T.6 指向的 K4.69–76), `docs/DEV_REPORT_R8.md`, `docs/QA_REPORT_R7.md` N7-01…05, `AGENTS.md`

证据: `docs/screenshots/r8/{zh,en}/` (zh 36 / en 35 帧), `montage-{zh,en}.jpg` (6 帧: workbench / ghost / dragmove / vivid / edit-close / 第 6 帧, **不含 `size` 帧**, 故全分辨率打开 `zh/r8-size.png`); 本轮 QA 产物 `docs/screenshots/r8/qa/crop-dryrun-{zh,en}-size-ghost.png`, `crop-dryrun-zh-brushrow-size.png`; 测量 `r8-measure.json` (7 项 × 2 语言, 已给) + 本轮 PIL 量笔刷行外沿与 K4.73 网格色占比; 导出 `docs/export/r8-{zh,en}-{walk,newest}.vox` (`python -I docs/scripts/voxdump.py` 本轮复核)。

> 可追溯性提醒 (N8-08): 仓库只有一个提交 `826c1da`, 无 R8 标签; 工作树已是 R11 (§T.7–T.9: 笔刷行 1062 × 80 + `VerticalDivider`, 面板开关, 坐标轴标注), 所以本报告的 `file:line` 指向**当前树**, 而截图 / 导出来自 R8 构建。`r8-gate.txt` 的 "104 keys" 与当前树一致, 研发 R8 报告为 100 键; 单测当前树 66 个 `@Test`, 研发 R8 报告 60/60 (空跑未验证)。

## 1. 构建 / 测试 / 静态检查 (`gate.sh`, `logs.sh`)

| 项 | 结果 |
| --- | --- |
| Gradle (一次一个) | **空跑未执行**; 研发报告 assembleDebug / testDebugUnitTest EXIT 0 (10-13) |
| 单测 | 研发 **60/60** (R7 64 − BarFill 7 − BoxFill 5 + SizedBrush 8); 当前树 `SizedBrushTest` 8 例 (`app/src/test/java/com/kejing/gridspace/domain/SizedBrushTest.kt:19-78`), K4.70 (h) 缺 (N8-02) |
| dex / 字体 / 依赖 | `dexcheck.sh`: refs 3, PASS (现有 APK, 见页首); `r8-gate.txt`: `libs.versions.toml` / dependency block 与参考相同 ✓ |
| grep | material 0 · Bold/SemiBold/W600/W700 0 · 长按 0 · backgroundMaterial/hoverable/googlefonts 0 · CJK / `Text("` 硬编码 0 · `spatialHoverEffect` 5 处 vs 自绘可点容器 0 ✓ |
| 字串 | 键集 104 / 104 对称差 ∅ (当前树); R8 时 100 / 100 (研发) |
| 崩溃 / ANR | `r8-logcat-walk.txt` 两套 0 行; `r8-logcat-export.txt` 仅系统 `SysPerfMonitorService.getSysFatal` 栈 (不计); `/data/anr/` 最新 2026-10-07 15:27, 早于运行日 10-13 ✓ |
| walk 步序 | 两套 26 标记 ghost → occupied → **size** → erase → paint → selection → dragmove → dragmove-commit → rotate → duplicate → stepx → delete → vivid → edit → eyedrop → edit-close → lang → lang-back → undo → burst → redo → reset → export → exported → projects → done, PASS 顺序一致、到达 done ✓ |
| 导出 | zh / en walk 各 195 块, 两文件**字节相同**; `#f3f5ed` 147 · `#f3bc9c` 27 · `#a5edcc` 20 · `#ff3b30` 1; (1,0,1) (3,1,1) = #f3bc9c, (4,0,1) (2,2,1) 空 → 3 × 2 × 1 块 x 1..3, y 0..1, z 1 ✓; (0,0,1) = #ff3b30 ✓; (1,0,8) 空 (长条已无) ✓; (12,0,14) = #f3bc9c, (8,0,14) 空 (五块拖动后) ✓; 195 = R7 193 + 2 (长条 4 → 块 6) ✓; newest 184 ✓ |
| 项目索引 | `r8-index.json`: 未命名 2 (36 块) / 未命名 3 / Untitled 4 / 未命名 5 递增 ✓ (R7 以前的 "未命名 1" ×5 / "Untitled 1" ×4 为历史遗留); 编号跨语言共用 (Untitled 4 接 未命名 3), 不重名 ✓ |
| 下载目录 | `r8-download-ls.txt`: 最新 `庭院小亭.vox` / `Garden Pavilion (10).vox` 存在 ✓; 旧 `庭院小亭 (N)` 已移到 archive (zh 清单); **3 个 `.pending-*-庭院小亭.vox` 孤儿 (11:31–11:32, 1876 / 1876 / 1832 B 有内容)** → N8-04 |
| 黑块帧 | run.log "no clean frame" 仅 zh `burst-2` (bad 0.489) / en `burst-4` (0.092), 模拟器整屏黑块, burst 只看表头; 其余近黑 ≤ 0.010 ✓ |
| run.log 一致性 | zh `r8-rail.png`、`r8-lang-toggle.png`, en `r8-rail.png` **无 cap.sh 记录** (K4.66 / N4-09) → N8-05 |
| 清理 | 两套 "no gridspace settings left" ✓ |

### 1.1 像素测量 (zh / en 相同 ±2; `r8-measure.json` + 本轮 PIL)

| 目标 | 设计 dp × scale (z) | 实测 px | 阈值 | 结论 |
| --- | --- | --- | --- | --- |
| 入口主钮 | 80 (系统 Planar) | 572 × 105 | — | PASS |
| 颜色栏 | 430 × 1038 × 1.5 (z 0.28) | 411 × 596 (bbox 1702..2113, 右缘 ≤ 2160) | 完整在画面内 | PASS |
| 目标标签 ghost | 360 × 96 × 1.0 (z 0.92) | 558 × 158 / 553 × 158 | 不压颜色栏 | PASS |
| 目标标签 occupied | 360 × 96 | 590 × 156 / 591 × 156 | — | PASS |
| 目标标签 size | 360 × 96 | 本轮 `r8-size.png` 目测 ≈ 585 × 150 (x 345..930, y 1030..1180), 文字 "3 × 2 × 1" 字高 ≈ 30 px | 字高 ≥ 20 | PASS (脚本未量此帧) |
| 笔刷行步进钮 | 72 (z 0.97, 未缩放) | 117 × (中带 28) 深色圆钮, 直径 ≈ 117 | ≥ 92 | PASS |
| 笔刷行整行 | 884 × 80 | **x 324..1936 × y ≈ 1880..2056 = 1612 × ≈ 176** (1.82 px/dp; 研发写 "1414 × 128 (x 324..1922)" 自相矛盾, 实为 ≈ 1600 宽), 下缘距画面底 101 px | 下缘 < 2158, 整行在内 | PASS |
| 笔刷行 K4.73 网格色占比 | — | 行内 0.0000 (行下方参照 0.0002; 裁剪 `crop-dryrun-zh-brushrow-size.png` 网格线止于行缘) | 0.00 | PASS |
| 取色卡 | 560 × 624 × 1.0 (z 0.90) | 888 × 992 / 888 × 990 | 完整 | PASS |
| 取色卡 完成钮下近黑 | — | 0.000 | 0.000 | PASS |
| 色槽热区 / 选区步进 / 取色条取消钮 | 88 / 84 / 72 | 研发 ≈ 93 / 94 / 113 (`measure.json` 未含) | ≥ 92 / ≥ 86 / ≥ 72 | 研发值, 未复核 |

## 2. 上轮缺陷复验 (N7-xx)

| ID | 级别 | 结论 | 证据 |
| --- | --- | --- | --- |
| N7-01 | P2 | **已修复** | `r8-index.json` 未命名 2 → 3 → Untitled 4 → 未命名 5 递增, 无新重名; `UntitledPatternTest` 在树中 |
| N7-02 | P2 (模拟器) | 未再出现 (保留帧) / 第一次 zh 整套 `selection` 帧曾复现 (`probe/zh-run1/`), 研发加 `WINDOW_SETTLE_MS` 后重跑 | 两套 run.log 近黑 ≤ 0.010; montage dragmove 帧选区面板完整; 真机 [H] |
| N7-03 | P2 | 未修 (接受, 顺延) | 研发 §3 "取色条 … R7 不变"; montage 无 eyedrop 帧, 未重判 |
| N7-04 | P2 (工具) | 已修复 / 取代 | 长条已删; `measure.json` "brush row stepper" 117 px PASS 自动量 |
| N7-05 | P2 | 未修 (接受, 顺延) | 研发 §3 取色卡 "R7 不变" (888 × 992 盖颜色栏上部) |

## 3. 验收清单 K4.69–76 (+ 相关 K4.33 / 50 / 52 / 57 / 58 / 66 / 67): **通过 8 · 部分 3 · 取代 3 · 不通过 1 · 未验证 (真机) 0**

| # | 结果 | 证据 / 说明 |
| --- | --- | --- |
| K4.69 [C] | 通过 (R8 帧) / 行常量已被 T.7 取代 | `r8-*-size.png` 行为 `X [−] 3 [+] × Y [−] 2 [+] × Z [−] 1 [+]` 单行 (884 式); 当前树 `BrushRow.kt:35-39` 已是 T.7 的 1062 × 80; 删除项 grep `enum class Brush` / `BarFill` / `barLength` / `barAxis` / `boxAnchor` / `brushes[` = 0 ✓; `BrushSize.UNIT` 默认、1..min(16, N) `SizedBrush.kt:30-38`, `EditorUiState.kt:137` ✓ |
| K4.70 [C] | **部分 (7/8)** | `SizedBrushTest.kt:19-78`: (a) unitSize (b) blockExtends (c)(d) eachAxisFlipsOnItsOwn (e) whenNeitherDirectionFits (f) addingSkipsOccupied (g) erasingOnlyTouchesExisting + regionIsNormalised; **(h) > 2000 格 `largeFill` 无用例** (grep `2000|LARGE|large` 在 test/ 为 0) → N8-02 |
| K4.71 [D] | 通过 | `r8-*-size.png` 行 3 · 2 · 1, 薄荷块 + 底座落点 + 标签 "3 × 2 × 1 / 松手放置" (`crop-dryrun-zh-size-ghost.png`); 导出 6 格: (1,0,1) (3,1,1) 槽色, (4,0,1) (2,2,1) 空, 总数 +6 → 195 ✓; 历史 1 步 = 单个 `CellsChange` (`SizedBrush.kt:73`); "撤销一次全部消失" 无帧 (walk 的 undo 撤的是红块), 以代码计 |
| K4.72 [E] | 通过 | 行 1612 × ≈ 176 px (x 324..1936), 下缘 y 2056 < 2158, 步进钮 117 ≥ 92 (`measure.json`); 规格预估 1414 × 128 偏小 (前面平面实测 1.82 px/dp 而非 1.6), 不影响结论 |
| K4.73 [E] | 通过 | 行包围盒内网格色占比 0.0000 (zh / en × size / workbench), 裁剪可见网格线止于行玻璃下缘; 底座近角不在行前 (`gridspace_brush` z 0.97 / −320 探针 `probe/brush-0.5,1,0.97,0,-320,0,0.png`) |
| K4.74 [C] | 通过 (R8) / `size_times` 已被 T.7 取代 | `brush_voxel` / `brush_box` / `brush_bar` / `bar_length` / `bar_axis` 两语言 grep 0 ✓; `label_size_dims` `strings.xml:97` / `values-zh:97`, `brush_size` :93 ✓; `size_times` 当前树已删 (T.7), R8 帧中 "×" 存在 ✓; 键集一致 (gate) |
| K4.75 [D] | **部分** | U6 走的是 ERASE → ADD (`DemoMode.kt:287-288`), 不是条目写的 上色 → 添加; 尺寸记忆由导出间接证实: 往返后 X+2 / Y−1 单击 (8,0,14) 放了 **5** 块 ((12,0,14) 槽色) 而非 3 块 ✓; 上色强制 1 × 1 × 1 仅代码 `EditorUiState.kt:168-169`; 往返后 "行显示 3 · 2 · 1" 无帧 (下一帧 erase 前脚本已调回 1) |
| K4.76 [C/E] | 通过 | `combinedClickable` 0 (gate); 步进为 `IconButton` (`BrushRow.kt:84,88`, 到界 enabled=false); 捏合保持只移幽灵 (`EditorViewModel.kt:210-225` previewFor 每帧重算, 无 A..B); 标签 zh "3 × 2 × 1 / 松手放置", en "3 × 2 × 1 / Release to place" (`crop-dryrun-{zh,en}-size-ghost.png`) ✓ |
| K4.33 [D] | 通过 | `r8-logs.txt` 两套步序 (box → size) 一致, 到 done, 无崩溃 / ANR |
| K4.50 [E/H] | 通过 (模拟器) | montage workbench 帧: 颜色栏 / 笔刷行完整, 模型居中; 工具栏 [H] |
| K4.52 | 取代 → K4.75 | §T.6 |
| K4.57 / K4.58 | 取代 → K4.72 | §T.6 |
| K4.66 [C] | **不通过** | zh `r8-rail.png` / `r8-lang-toggle.png`, en `r8-rail.png` 无 cap.sh 记录 → N8-05 |
| K4.67 [E] | 通过 | 两套帧齐全 (zh 多一张 lang-toggle), 每帧标记后 ≥ 5 s (run.log), `entry-after` 入口重开 (`r8-export-log.txt` "entry window reopen requested") |

## 4. 申报偏离的判定

| 偏离 | 判定 | 理由 / 条件 (真机复核命令) |
| --- | --- | --- |
| 笔刷行 offset −24 → −320 (z 0.97) | 可接受 | §T.3 本身给出 "下缘出画则加大 offset" 备选; 探针 `probe/brush-off*.png` 证明 −24 / −120 / −160 整行出画; −320 时行在画面内且 K4.73 占比 0。真机: `adb shell settings put global gridspace_brush 0.5,1,0.97,0,-24,0,0` 复核规格值, 并看 −320 是否把行抬到底座中部 [H] |
| `BrushSize` 而非 `Cell` | 可接受 | 语义相同, 带 `isUnit / count / with / clamped` 更安全 (`SizedBrush.kt:8-39`) |
| 事件名 `SizeStep(axis, delta)` | 不是偏离 | 与 §T.1 一致 |
| 选择步的五块改为静默 5 × 1 × 1 单击 | 可接受 | 导出 (12,0,14) 槽色 / 195 块核对通过; 顺带证明 U6 (§3 K4.75) |
| 模拟器视点漂移 + 两次重启 + 黑块期 | 可接受 (环境) | 全部 R8 帧在重启后视点; 已写入 AGENTS |
| zh 整套重跑 (旧导出移 archive, `selection` 前加 `WINDOW_SETTLE_MS`), en 集来自前一个 APK | 可接受, 有条件 | 两 APK 仅 demo 等待时间不同; 但两集非同一构建, 下轮两语言必须同一 APK (N8-08 顺带要求打标签) |
| 真机同名 ≥ 32 次导出失败 "本轮未改" | **不接受** | 这是应用缺陷 (N8-01), 不是环境限制: 用户在真机上对同一项目导出 32 次后导出即失败, 且错误文案为系统原文 |

## 5. 新增缺陷

无 P0 / P1。N8-01…03 与 2026-10-13 的 `QA_REPORT_R8.md` 同号 (ID 不重编), 本轮独立复核; N8-04 起为本轮新增。

| ID | 级别 | 标题 | 位置 (文件:行) | 期望 (规格 §) | 实际 (帧 / 数值) | 复现 |
| --- | --- | --- | --- | --- | --- | --- |
| N8-01 | P2 | 同名导出 ≥ 32 次后失败 ("Failed to build unique file …") | `data/VoxExporter.kt:31-39` 只给 `DISPLAY_NAME`, 唯一名交给 MediaStore (上限 32) | v1.0 §H 导出永远成功 | 研发 §5: 第一次 zh walk `exported` 帧 "导出失败: Failed to build unique file … 庭院小亭.vox"; `en/r8-download-ls.txt` 列出 庭院小亭 (1)…(31) | 同一项目导出 32 次 (真机同样会发生) |
| N8-02 | P2 | K4.70 (h) 缺单测: > 2000 格 `largeFill` 路径无覆盖 | `test/…/domain/SizedBrushTest.kt` 8 例; `BoxFill.kt:42` `LARGE_THRESHOLD = 2_000`; `EditorViewModel.kt:246` | K4.70 (a)–(h) | 7/8; test/ 中 grep `2000|LARGE|large` = 0 | 读代码 |
| N8-03 | P2 | 1 × 1 × 1 时三个 "−" 全灰, 行观感"无效" | `BrushRow.kt:84` `enabled = value > BrushSize.MIN` | 系统禁用 alpha (规格), 但整行不应像禁用态 | montage workbench / edit-close 帧三灰钮 | 任一空项目 / 工作台 |
| N8-04 | P2 | 导出发布失败时留下 `.pending-*.vox` 孤儿 (有内容) | `VoxExporter.kt:40-45`: `catch (e: IOException)` 才 `delete(uri)`; MediaStore 发布 (`IS_PENDING` 0) 抛的是 `IllegalStateException` ("Failed to build unique file"), 不被捕获 | 失败 = 0 残留文件 | `{zh,en}/r8-download-ls.txt` 3 个 `.pending-17925210{72,88}-庭院小亭.vox` (1876 B) + `.pending-1792521127-…` (1832 B), 11:31–11:32 (第一次 zh 跑) | 触发 N8-01 后 `adb shell ls -la /sdcard/Download/GridSpace` |
| N8-05 | P2 (流程) | 3 张 png 无 cap.sh 记录 (K4.66 / N4-09 回归) | `zh/r8-run.log`, `en/r8-run.log` | 每张截图一条记录, 重拍追加 | zh `r8-rail.png` + `r8-lang-toggle.png`, en `r8-rail.png` 不在 run.log; `r8-rail.png` 是 `measure.json` "rail" 行的输入 | `for p in *.png; grep -q $p r8-run.log` |
| N8-06 | P2 (UX) | 笔刷行 "X − 3 + × Y − 2 + × Z" 连成一串, 三组不可分 | R8 `BrushRow.kt` (884 式; 当前树已按 T.7 重写) | 用户可一眼分出 宽 / 高 / 深 三组 (T.7 已定案为 R9) | `r8-*-size.png` / `crop-dryrun-zh-brushrow-size.png`: 分隔 "×" 24 dp 淡灰, 与轴字母同基线 | 任一 ADD 帧; 验收在 R9 |
| N8-07 | P2 (演示) | walk `size` 目标 (1,0,1) 被 ghost 步 (1,2,2) 与 occupied 步 (1,0,0) 放的两块挡住, 预览块只露一角 | `DemoMode.kt:272-285` | K4.71 "薄荷 3 × 2 × 1 幽灵块 + 落点" 清晰可见 | `crop-dryrun-{zh,en}-size-ghost.png`: 薄荷块大半在两块桃色方块与盆栽后, 只剩透明一角与落点 | walk `size` |
| N8-08 | P2 (可追溯) | R8 构建不可复现: 单提交 `826c1da`、无 R8 标签, 工作树已到 R11; gate / dex / 键数 (104) / 测试数 (66) 都来自当前树 | 仓库 | 每轮一个标签或提交, gate 与截图同一构建 | 研发 R8: 100 键 / 60 测 / APK 10-13 11:47; 本轮 APK 10-17 17:27 | `git log --oneline` |

## 6. 发布结论

**可发布 (模拟器范围, 空跑结论)**: R8 目标 (统一尺寸笔刷 + 笔刷行移到底座前) 达成: `size` 帧状态正确 (行 3 · 2 · 1, 幽灵 / 落点 / 标签, Z "−" 系统禁用态), 导出逐格核对 6 块 + 5 块, 笔刷行 1612 px 宽、下缘留 101 px、行内网格色 0, 步进钮 117 px, 两语言字串对应, 无崩溃 / ANR, 上轮 N7-01 / N7-04 修复、其余三项顺延。K4.69–76 通过 5 / 部分 2 (K4.70 (h) 单测、K4.75 的上色往返与帧证据) / 取代 2; K4.66 不通过 (流程)。无 P0 / P1。空跑未重建, 构建 EXIT 0 与 60/60 以研发报告为准; 当前 APK 与 gate 来自 R11 树 (N8-08)。

R9 修复顺序 (每项附验收标准):

1. **N8-04 + N8-01** `VoxExporter`: 捕获 `Exception` 并 `delete(uri)`; 唯一名改为应用自管 (查询同名计数或时间戳后缀), 不依赖 MediaStore 32 次上限; 验收: 单测 `VoxExporter` 失败路径 0 残留 (fake resolver); 模拟器对同一项目连续导出 35 次全部成功, `ls /sdcard/Download/GridSpace` 无 `.pending-*`。
2. **N8-06** 按 §T.7 重写笔刷行 (1062 × 80, 三组 + `VerticalDivider`); 验收: r9 `workbench` / `size` 两帧三组肉眼可分、行宽 ≤ 1920 px、无裁切、K4.73 占比 0.00。
3. **N8-02** 补 `SizedBrushTest` (h): 16 × 16 × 8 = 2048 > 2000 触发 `large`; 验收: 用例存在且 `largeFill` / 标签 "较大, 稍候" 路径断言。
4. **N8-07** walk `size` 目标改到两块之外 (例如 (0,0,8) 或把 `size` 排在 ghost / occupied 之前); 验收: `r9-*-size.png` 薄荷块六格全部可见, 差分裁剪能量 ≥ 200 × 120 px。
5. **N8-05** 补记 / 重拍 `rail`、`lang-toggle` 并 `tee -a` 到 run.log; 验收: `for p in *.png` 全部命中 run.log, logs.sh WARN 0。
6. **N8-03** 1 × 1 × 1 时 "−" 的禁用表现: 保留系统 alpha 但值文字用 `labelPrimary`、"+" 常亮 (或规格确认现状可接受); 验收: workbench 帧行不呈整体禁用态。
7. **N8-08** 每轮打 git 标签 (`r8`, `r9`), gate / dex / 截图必须同一构建; 验收: QA 报告页首 APK 时间 = run.log install 时间。

真机 [H] 清单 (沿用 R7, 加 R8 项):

- 笔刷行 `gridspace_brush 0.5,1,0.97,0,-24,0,0` (规格值) vs −320 (研发值): 行是否在 Volume 底边内、是否被底座近角压住; z 1.0 备选 `0.5,1,1,0,-24,0,0`。
- 系统 `Toolbar` (未包 `EditorScaled`) 在真机默认距离下完整可见, 模式钮命中 ≥ 72 等效 dp。
- 侧卡 `gridspace_rail 1,0.5,0.5,32,0,0,1` / `gridspace_selpanel`; 取色卡 `gridspace_popup 1,0.5,0.95,…` vs (0.88, 0.42, 0.90); `LabelAnchor` x 夹取 [0.20, 0.58]。
- 新建空间窗玻璃滞后 (N7-02) 是否出现; 不出现则缩短 `SETTLE_REDRAW_MS`。
- 眼手 / 捏合保持 3 × 2 × 1 预览随目标格每帧重算 (§T.2 步 6), 松手提交, 跟踪丢失不提交; 步进钮命中率; 两手缩放。
- 同一项目真机导出 ≥ 32 次 (N8-01) 与失败后 `.pending` 残留 (N8-04)。
- 系统关闭钮关闭 Volume → 入口重开 + 列表刷新; 冷启动 (无 `gridspace_lang`) 读 SharedPreferences 语言。
