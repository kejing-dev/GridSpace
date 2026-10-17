# GridSpace 格间 · QA 报告 R2

日期 2026-10-07 · 被测构建: 本轮重新 `assembleDebug` (APK 00:46, dex 含 `SpatialApplication`, 01:06 重装) · 设备: PICO 模拟器 emulator-5554 (`Pico_Emulator_6_1_fresh`) · 依据: `docs/QA_REPORT_R1.md` D-01..D-16、`docs/DEV_REPORT_R2.md`、`docs/DESIGN_SPEC.md` §K + Figma V2-01..04

复现: `bash docs/scripts/qa2.sh docs/screenshots/qa2 qa2` (重装 → 入口 → sample → walk 19 标记/ack 握手 → export 脚本 → 拉 vox/JSON → 清设置)。产物 `docs/screenshots/qa2/` (`small/` 为缩放与裁剪证据)、`docs/export/qa2-newest.vox` (export 脚本)、`docs/export/qa2-walk.vox` (walk 导出 `庭院小亭 (13).vox`)。`qa2-editor.png` 首拍为模拟器黑块 (占比 0.43), 已在 sample 模式用 `cap.sh` 重拍 (第 1 次即干净)。

## 1. 构建 / 测试 / 静态检查

| 项 | 结果 |
| --- | --- |
| `./gradlew :app:assembleDebug :app:testDebugUnitTest --offline -q` | EXIT 0 |
| 单测 | **34/34** (BoxFill 5 · ColorMath 3 · History 4 · SelectionOps 7 · TargetCellResolver 7 · VoxWriter 4 · **EditorViewModelSave 4** 新增); 0 失败 0 跳过 (`app/build/test-results`) |
| dex 含 `platform/SpatialApplication` | classes10.dex 3 处 (R2 起 dex 分片变化, AGENTS.md 的 `classes2.dex` 命令已不适用, 需遍历全部 dex) |
| `androidx.compose.material*` | 0 |
| `FontWeight.Bold/SemiBold/W600/W700` | 0; `GridType` 全部 Medium/Normal |
| 硬编码用户串 (CJK / `Text("`) | 0; 未用键仅 `app_name` (Manifest 用) |
| `values` / `values-zh` | 91 / 91, 键集一致 (R1 的 100 为旧计数) |
| `backgroundMaterial` / `hoverable(` | 0 / 0 |
| `Modifier.border(` | 4 处, 全是规格白环或近白 1 dp 内描边 (`ColorPickerPopup.kt:155`, `EditorToolbar.kt:157,161`, 新增 `RecentProjects.kt:81`); 无容器边框 |
| 模拟器 ANR / 崩溃 | walk + export + 重拍全程 `logcat` 无 `AndroidRuntime/FATAL/ANR`; `/data/anr/` 仅 10-06 21:06 旧 trace |
| 清理 | `gridspace_demo` / `_ack` / `gridspace_brush` 均 `null` |

### 1.1 回归检查 (ViewModel 接口化重构)

| 项 | 结论 |
| --- | --- |
| `AppGraph.projects: ProjectRepository`, `exporter: VoxExporter` 以 `ProjectStore` / `Exporter` 注入 `EditorViewModel.Factory` (`EditorViewModel.kt:336`); `AppGraph.init` 在 `SpatialApplication.onCreate` | ✓ |
| 导出仍写 MediaStore `Download/GridSpace` (`VoxExporter.kt:27-53`); 实机文件 `庭院小亭 (13)/(14).vox` 生成 | ✓ |
| 入口列表刷新: `ProjectRepository.save` 直接更新 `summaries` StateFlow, `EntryViewModel` 收集 (`EntryViewModel.kt:38`); `qa2-entry-after-export.png` "184 块 · 刚刚" | ✓ |
| `EntryViewModel` 经 `EditorSession` 关旧 tag (`EntryViewModel.kt:93-97`, `EntryScreen.kt:61-70`) | ✓ (代码) |
| walk 导出 188 块与脚本期望逐格一致 (见 K.36) | ✓ |

## 2. R1 缺陷复验

| ID | 级别 | 结论 | 证据 |
| --- | --- | --- | --- |
| D-01 保存取消误报失败 | P1 | **已修复** | `EditorViewModel.kt:309-316`: `withContext(NonCancellable)` 包住写文件, `CancellationException` 重抛, 仅真实异常置 FAILED; 4 个新单测存在且断言取消/1 s 连发不出现 FAILED、IO 失败才 FAILED+Snack、Paused 刷新防抖; 模拟器 `qa2-undo` + `qa2-burst-1..4` + `qa2-redo` 6 帧标题条均 "已自动保存" 薄荷点 (`small/strip-title-undo-burst.png`)。遗留一个小问题见 N-02 |
| D-02 列表次文字被裁 | P1 | **已修复** | `RecentProjects.kt:89` `heightIn(min = 72.dp)`; 近白色块 1 dp 内描边 `:81`; `qa2-entry.png` "188 块 · 6 分钟前"、`qa2-entry-after-export.png` "184 块 · 刚刚" 两行完整 |
| D-03 笔刷行被遮挡 | P1 | **已修复** | `EditorScreen.kt:69-73` anchor (0.5, 1, 0.75) + offset (0, −96, 0); `qa2-editor / ghost / occupied / box / erase / undo / redo / reset / done` 全部 "单格 / 长方体" 整条可见于模型下方 |
| D-04 占用反馈不可见 | P1 | **已修复** | 目标改前柱底 (3,1,12) `DemoMode.kt:154` (样例中该格确为珊瑚柱 `SampleProject.kt:32`); 线宽 5 dp `VoxelScene.kt:189,335` + 35 % 填充 `:304-309`; `qa2-occupied.png` 珊瑚线框+填充、标签 "! 格已占用 / 换一个格子" 清晰; 块数不变 (walk 导出 188) |
| D-05 标签偏小 | P2 | **已修复** | `TitleBar.kt:85-96` 300 × 88, 18/15 sp; `qa2-occupied / box` 不放大可读。但前推 2.5 格的锚点引入 N-01 |
| D-06 标题条黑块 | P2 | **已修复 (模拟器)** | `EditorScreen.kt:84,158-162` 稳定 onEvent/State/onExport, `TitleBar.kt:34,75` 固定 84 dp; 本轮 21 张编辑器截图标题条无一黑块 (R1 5/17)。注: 内容 lambda 仍捕获每次提交都变化的 `project` 对象 (`:161` 取 `project.name`), 真正起作用的是尺寸固定; 真机复核 |
| D-07 导出钮 18 sp | P2 | **已修复** | `Controls.kt:61-63` `textStyle` 参数, `TitleBar.kt:59` `GridType.bodyMedium` 16 sp Medium |
| D-08 面板 280 > 272 | P2 | **已修复 (申报偏离 328)** | `SelectionPanel.kt:44,74`; `small/crop-selpanel.png` 三钮完整, 左右边距对称, 删除珊瑚 |
| D-09 步进越界无禁用 | P2 | **已修复 (代码)** | `EditorViewModel.kt:264-286` `blockedStep` 100 ms; `SelectionPanel.kt:102,106` `enabled`; walk 不越界, 需真机 |
| D-10 标签无淡出 | P2 | **已修复 (代码)** | `EditorScreen.kt:200-228` `updateTransition` + `AnimatedVisibility(fadeOut(200))`, 淡出后才移除 Augment; 截图无法验证时序 |
| D-11 walk 时序 | P2 | **已修复** | `DemoMode.kt:112-131` 标记后 2 帧 + 400 ms, 保持 ≥3.5 s 并等 `gridspace_demo_ack`; 19 标记全部在状态内截到: `qa2-box` 盒标签 "5 × 1 × 1"、`qa2-picker` 柔和页、`qa2-export` IDLE Sheet、`qa2-exported` 成功态 |
| D-12 冷启黑底 | P2 | **未修 (环境, 接受)** | 系统玻璃就绪前现象; `qa2.sh` 12 s 后首拍正常 (`qa2-entry.png`) |
| D-13 面板深度 | P2 | **未改代码, 本轮未复现** | `qa2-picker / vivid` 面板均在模型前; 真机复核 |
| D-14 上色预览偏白 | P2 | **已修复** | `VoxelScene.kt:316-322` 不透明 PBR 混色 70 %; `small/crop-paint.png` (1,0,0) 呈薄荷 (露出标签下沿) |
| D-15 无 onPause 保存 | P2 | **已修复 (代码+单测)** | `EditorScreen.kt:96-102` ON_PAUSE/ON_STOP → `Paused` → `flushPendingSave()` (`EditorViewModel.kt:319-322`); 单测 `pauseFlushesThePendingDebounce` |
| D-16 openEditorId 残留 | P2 | **已修复 (代码)** | `AppGraph.kt:13-25` `EditorSession.closed` compareAndSet; `EditorScreen.kt:88-93` onDispose 清理; `EntryViewModel.kt:93-97` 读写 session; export 脚本关闭 W2 后入口正常 |

合计: 已修复 14 (其中 4 项仅代码/单测可验), 环境接受 1 (D-12), 未复现 1 (D-13)。

## 3. 验收清单 K.1–K.45

结果: **通过 36 · 部分 3 · 未验证 6**。"未验证" = 需真实空间输入。截图省略前缀 `docs/screenshots/qa2/`。

| # | 结果 | 证据 / 说明 |
| --- | --- | --- |
| 1 | 通过 | 构建 + 34 单测; material 0 |
| 2 | 通过 | `qa2-entry.png` 玻璃窗 + "GridSpace 格间" |
| 3 | 通过 | `qa2-editor.png` Volume 无玻璃, 入口窗保留 |
| 4 | 未验证 | `EditorSession` 路径 (§1.1); 需人工点第二项目 |
| 5 | 通过 | `Main.kt:21,30-37` |
| 6 | 通过 (代码) | 系统 Toolbar 仍在截图可见区外 (已知差距) |
| 7 | 通过 | `qa2-picker.png` 三槽 + 槽 1 白环; 短按换环需人工 |
| 8 | 通过 | 添加/擦除显示笔刷行 (`ghost/erase/reset`), 上色/选择隐藏 (`paint/selection`) |
| 9 | 通过 (代码) | `EditorToolbar.kt` enabled = canUndo/canRedo |
| 10 | 未验证 | 需真实单击 |
| 11 | **部分** | 标签 "✓ 已对齐 · 松手放置 / X0 · Y0 · Z0" 可读, 但幽灵方块被标签卡片盖住 (N-01, `small/crop-ghost-full.png`) |
| 12 | **通过** | `qa2-occupied.png` 珊瑚线框 + 填充 + "格已占用"; 块数不变 |
| 13 | 部分 | 标签 "松手擦除" 可读, 珊瑚线框被标签盖住 (N-01); 导出块数 −1 ✓ |
| 14 | 通过 | `small/crop-paint.png` 薄荷预览; 导出中 (1,0,0)/(3,0,3) 为 mint (walk vox mint 20 = 18 + 2) |
| 15 | 通过 (D) | `qa2-selection.png` 5 白框 + "已选 5 块" |
| 16 | **通过** | `qa2-box.png` 薄荷盒 + "5 × 1 × 1 / 松手放置"; BoxFillTest 1 步 |
| 17 | 通过 (代码) | `BoxFill.erase` + 测试 |
| 18 | 通过 | `BrushRow.kt:20` 仅 VOXEL/BOX |
| 19 | 通过 | 面板 Volume 左外, 328 dp (申报), 内容齐全 |
| 20 | 通过 | `small/montage-selops.png` rotate 行沿 Z; walk vox 校验 (12,0,11..15), 覆盖 2 格地板 (lime 150→147 含上色 −1) |
| 21 | 通过 | duplicate 副本 +X 带框, 原块无框 |
| 22 | 通过 | delete 副本消失、面板隐藏; undo 后恢复 |
| 23 | 通过 | stepx X 显示 1, 副本 +1 |
| 24 | 通过 (代码) | `SelectionClear` |
| 25 | **通过** | `qa2-picker.png` 700 宽、3 槽 80、段控、8×3 色板; 滑杆与两钮仍在可见区下 (代码 ✓) |
| 26 | 未验证 | 需人工 |
| 27 | 通过 | R1 脚本比对, `Palette.kt` 未改 |
| 28 | 未验证 | 需人工 |
| 29 | 通过 | `qa2-project-sample-pavilion.json` slots/activeSlot ✓ |
| 30 | 未验证 | 回正事件无异常 (`qa2-reset.png`) |
| 31 | 通过 (D) | undo/redo 期间标题/笔刷 Augment 位置不变 (6 帧) |
| 32 | **通过** | 6 帧 "已自动保存"; `qa2-entry-after-export.png` 行完整; 重开需人工 |
| 33 | 通过 | `.tmp`+rename (`ProjectRepository.kt:40-50`); 失败路径有单测 |
| 34 | 未验证 | 需人工 |
| 35 | **通过** | `qa2-export.png` IDLE: 关闭钮、格式段控、文件名 "庭院小亭" + ".vox"、"导出到本机" 薄荷 64 |
| 36 | 通过 | `qa2-newest.vox` 1832 B: `VOX ` 150 / MAIN 0,1812 / SIZE 16³ / XYZI n=184 / RGBA 1024; n = JSON 184, 逐格坐标与颜色一致。`qa2-walk.vox` n=188: mint 20 / lime 147 / coral 21 = 脚本期望 |
| 37 | 通过 | (3,0,3)#f3f5ed → vox (3,3,0) i=1, RGBA[0]=f3f5edff, 未用槽 0 |
| 38 | 部分 | `qa2-exported.png` "已导出 / 下载/GridSpace/庭院小亭 (13).vox · 188 块 · 2 KB / 完成"; Snack 在可见区外; MagicaVoxel 人工抽查未做 |
| 39 | 通过 | 导出钮 16 sp 已改; Bold 0 |
| 40 | 通过 | 珊瑚/薄荷用途同 R1, 新增珊瑚 35 % 占用填充 (占用反馈, 合规) |
| 41 | 通过 (替换) | 同 R1 |
| 42 | 通过 | 同 R1 |
| 43 | 通过 | 新增标签 200 ms 淡入淡出 (规格 F.1 允许) |
| 44 | 通过 | walk 19 标记到 done, 无崩溃/ANR; 设置删除后入口正常 |
| 45 | 通过 | 91 条 × 2 |

## 4. 新增缺陷

无 P0。

| ID | 级别 | 标题 | 位置 | 期望 (规格) | 实际 | 复现 |
| --- | --- | --- | --- | --- | --- | --- |
| N-01 | **P1** | 目标标签卡片盖住目标格本身 (视线中心左侧的格子) | `VoxelSceneView.kt:126-133, 214-216` 锚点 = 幽灵 + (0.6, 0.6, 0) 格 + **2.5 格朝观察者**; `EditorScreen.kt:215-219` CenterLeft + 12 dp (卡片从锚点向右延伸)。前推使锚点投影向视线外侧偏移, 左半视野的目标格被 300 × 88 卡片完全覆盖 | §F.1 标签在幽灵右上角, 幽灵 / 珊瑚线框 / 上色预览与标签同时可见 (K.11/13/14) | `small/crop-ghost-full.png` (0,0,0) 幽灵不可见, `small/crop-erase.png` 珊瑚线框不可见, `small/crop-paint.png` 仅露出一角; 右半视野的 (3,1,12)、盒 (10..14,0,13) 正常。研发 `r2-ghost.png` 同样现象 (报告未察觉) | walk ghost/erase/paint 步; 真机: 向模型左侧任一格捏合 |
| N-02 | P2 | 被取消的保存完成后仍写 SAVED, 覆盖新提交的 SAVING | `EditorViewModel.kt:311-315`: `withContext(NonCancellable)` 返回后未检查 `isActive`, 取消的 job1 写完旧快照即置 `SAVED`; 此时 job2 (新提交) 仍在 1 s 防抖中 | §F.2 "已自动保存" 表示当前内容已落盘 | 写入中再提交 → 最多 1 s 内标题显示 "已自动保存" 而磁盘缺最新一步; 单测 `cancellingAnInFlightSaveDoesNotReportFailure` 只在 idle 后断言, 未覆盖 | 1 s 内两次提交且第一次写入耗时 > 0 |
| N-03 | P2 | AGENTS.md 的 dex 校验命令失效 | AGENTS.md "verify with `unzip -p app-debug.apk classes2.dex`" | 校验命令应能检出缓存损坏 | 本轮 `SpatialApplication` 在 classes10.dex, classes2.dex 为 0 → 会误判为损坏; 应遍历全部 `classes*.dex` | 任意构建 |

## 5. 可用性 / 与 Figma V2 对照

| 画面 | 观察 |
| --- | --- |
| 工作台 (V2-01) | 标题条 + 模型 + 笔刷行层级现已与 Figma 一致, 笔刷行完整居中于模型下方; 本轮 Volume 距观察者更近 (模拟器重启后视点变化), 模型占画面更大、透视更强, 顶梁依旧偏挤 (pitch 0.40, 规格值, 未改) |
| 幽灵 + 标签 | 300 × 88 卡片字号已合适 (18/15 sp 不放大可读); 但卡片现在 "压" 在目标上 (N-01), 与 Figma "标签紧贴幽灵右上、不遮挡" 相悖; 建议按锚点 x 自动选边 (x < 0.5 → CenterRight 向左延伸) 或把前推改为视平面 +x 方向 1.5 格、前推降到 ≤ 0.8 格 |
| 占用反馈 | 5 dp 珊瑚线框 + 35 % 填充在臂长距离清楚可辨, 与规格 "线框 2 dp" 的偏离属合理放大 |
| 取色面板 (V2-02) | 柔和 / 鲜艳页与 Figma 一致 (槽环、段控、8×3 圆角色板、等色白环); 面板仍贴画面底, 滑杆/按钮在可见区外 (模拟器视点限制) |
| 选区 (V2-03) | 面板 328 dp 内容与 Figma 一致, 三钮等距; 面板在 Volume 左面上斜向观察者, 远处文字偏小 (同 R1 备注); 无引导线 (v1.1) |
| 入口 | 行高自适应后与规格 G.1 一致; 近白色块描边后可辨 |
| 导出 Sheet | IDLE / 成功态与规格 H 一致, 父窗变暗, ".vox" trailing 固定 |

## 6. 发布结论

**需第 3 轮 (轻量)**: 16 项 R1 缺陷已全部按承诺修复, 构建/单测/导出/持久化/ANR 全部通过, 但 D-04/D-05 的标签前推引入了 P1 回归 N-01 (左半视野的幽灵 / 擦除线框 / 上色预览被标签盖住), 直接影响 K.11/13 的核心放置反馈。修复预计 2 处改动 + 复拍 3 张。

第 3 轮修复顺序:

1. **N-01** 标签锚点: 前推量降到 ≤ 0.8 格 (只需越过本格前面) 并按锚点 x 自动选 CenterLeft/CenterRight, 或改为视平面 +x 偏移; 用 walk ghost/erase/paint 三步复拍, 要求幽灵/线框/预览与标签同时可见。
2. **N-02** `persist()` 在 `withContext(NonCancellable)` 之后 `coroutineContext.ensureActive()` (或校验 `saveJob === coroutineContext[Job]`) 再写 SAVED; 单测补 "写入中被取消后 saveState 仍为 SAVING 直到第二次写完"。
3. **N-03** AGENTS.md dex 校验改为遍历 `classes*.dex`。
4. (可选) D-06 彻底化: 标题 Augment 内容 lambda 改为只捕获 `name: String` 与 State, 不捕获 `project`。
5. (真机) K.4/10/26/28/30/34 + D-09/D-10/D-15/D-16 人工验证; 补一张含系统 Toolbar 的编辑器截图 (K.6)。
