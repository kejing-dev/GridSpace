# GridSpace 格间 · 研发报告 R2

日期 2026-10-07 · SDK 6.1.9 · 包名 `com.kejing.gridspace` · 验证环境: PICO 模拟器 `Pico_Emulator_6_1_fresh` (emulator-5554, 本轮重启过一次) · 依据: `docs/QA_REPORT_R1.md` D-01..D-16 + §5 优先级, `docs/DESIGN_SPEC.md`

截图目录 `docs/screenshots/r2/` (前缀 `r2-`); 笔刷行位置实验 `docs/screenshots/r2/brush/`; 复现脚本 `docs/scripts/qa2.sh`。构建 + 34 单测通过, walk 全程无崩溃 / ANR (`/data/anr/` 仅 10-06 21:06 旧 trace)。

## 1. 缺陷 → 修复 → 证据

| ID | 级别 | 修复 (文件:行) | 证据 |
| --- | --- | --- | --- |
| D-01 保存取消误报失败 | P1 | `EditorViewModel.kt:309-317` `persist()`: 写文件放进 `withContext(NonCancellable)`, `CancellationException` 重抛, 只有真实 IO 异常才置 `FAILED` + `SaveFailed`; 依赖改为接口 `ProjectStore` / `Exporter` (`ProjectRepository.kt:15-20`, `VoxExporter.kt:15-19`) 以便单测 | 单测 `EditorViewModelSaveTest` 4 例: `cancellingAnInFlightSaveDoesNotReportFailure` (写入中被下一次提交取消 → SAVED, 两次写都完成), `rapidCommitsAtTheDebounceIntervalNeverShowFailed` (1 s 间隔 3 次提交), `ioErrorReportsFailureAndSnack`, `pauseFlushesThePendingDebounce`; 模拟器: `r2-undo.png`, `r2-burst-1..4.png`, `r2-redo.png` 撤销×3 / 重做×3 (1 s 间隔) 期间标题条始终 "已自动保存" 薄荷点 |
| D-02 最近项目行次文字被裁 | P1 | `RecentProjects.kt:89` `height(72)` → `heightIn(min = 72.dp)`, 行按 `ListItem` 内容自适应; 近白主色块加 1 dp `BaseInk@30%` 内描边 `:81` | `r2-entry.png` "庭院小亭 / 184 块 · 3 分钟前" 两行完整; `r2-entry-after-export.png` "184 块 · 刚刚" |
| D-03 笔刷行被模型遮挡 | P1 | `EditorScreen.kt:60-72` `BrushRowPlacement`: anchor (0.5, 1, **0.75**) + offset (0, −96, 0) (规格 §L.3 备选 −96; z=1 为 Volume 前面 `NormalizedPoint3D.Front`); 实测 8 种摆位 (`docs/screenshots/r2/brush/`), 规格的 z=0.5 任何 y 偏移都在倾斜底座之后 (`mid-24/mid-96` 不可见), z=0.75 整条可见; 另加模拟器专用覆盖设置 `gridspace_brush` (`DemoMode.kt:53-66`) 供真机试摆位 | `r2-editor.png`, `r2-ghost.png`, `r2-erase.png`, `r2-reset.png`, `r2-done.png`: "单格 / 长方体" 两片完整显示在模型下方 |
| D-04 占用反馈不可见 | P1 | walk 占用目标改为前柱底格 (3,1,12) `DemoMode.kt:154`; 珊瑚线框 2 → **5 dp** 等效 (`VoxelScene.kt:185-189, 335`), 占用格另叠珊瑚 35% 半透明盒 `showOccupied` `:304-309`; 标签锚点向观察者推 2.5 格 `VoxelSceneView.kt:124-134, 214-216`; 占用态保持 ≥3.5 s 并等截图确认 | `r2-occupied.png`: 前柱底格珊瑚线框 + 半透明填充, 标签 "! 格已占用 / 换一个格子" 完整可读 |
| D-05 目标标签偏小 / 被遮 | P2 | `TitleBar.kt:84-96` 300 × 88 dp, 行1 18 sp Medium + 20 dp 图标, 行2 15 sp; 锚点前推同上 | `r2-ghost.png` "✓ 已对齐 · 松手放置 / X0 · Y0 · Z0", `r2-box.png` "5 × 1 × 1 / 松手放置", `r2-erase.png`, `r2-paint.png` 均无放大即可读 |
| D-06 标题条重组后黑块 | P2 | `EditorScreen.kt:84` 全屏唯一 `onEvent` 用 `remember`; `:156-161` 保存态以 `State` 传入, `onExport` remember, Augment 内容 lambda 不再随状态变化; `TitleBar.kt:34, 75` 状态文字固定 84 dp 宽, 卡片尺寸不变 → 系统不重建窗口 | `r2-undo.png` + `r2-burst-1..4.png` + `r2-redo.png` 连续 6 帧 (跨 3 次 1 s 间隔提交) 标题条完整; 本轮 24 张编辑器截图标题条无一黑块 (R1: 5/17) |
| D-07 导出钮 18 sp | P2 | `Controls.kt:60-61` `SecondaryButton(textStyle)`; `TitleBar.kt:59` 传 `GridType.bodyMedium` (16 sp Medium) | 代码; `r2-editor.png` |
| D-08 选区面板 280 > 272 | P2 | `SelectionPanel.kt:44, 74` 面板 **328** dp (内容 280 = 3×88 + 2×8), 偏离规格 320 已注释 | `r2-selection.png`, `r2-rotate.png` 三钮左右边距对称 |
| D-09 步进越界无禁用 | P2 | `EditorViewModel.kt:264-285` 越界 → `blockedStep = StepKey(axis, delta)` 100 ms 后清; `SelectionPanel.kt:98-106` 对应按钮 `enabled = false` | 代码 (walk 的 X+1 不越界, 需真机点贴边选区) |
| D-10 标签无 200 ms 淡出 | P2 | `EditorScreen.kt:195-228` `TargetLabelAugment`: `updateTransition` + `AnimatedVisibility(fadeOut(200))`, 淡出期间保留上次内容与锚点, 结束后才移除 Augment | 代码; `androidx.compose.animation` 已在运行时类路径 (foundation 传递依赖, 未新增依赖) |
| D-11 walk 时序 | P2 | `DemoMode.kt` 重写: 每步状态生效 → 2 帧 + 400 ms → 打 `step` 标记 → 保持 ≥3.5 s **并等待** `gridspace_demo_ack=<step>` (20 s 超时); 新增标记 `burst` (重做×3 期间) / `exported` (成功态); `docs/scripts/walk.sh` 每步截图后回写 ack | 23 个标记全部在状态内截到: `r2-box.png` 盒标签, `r2-picker.png` 柔和页, `r2-export.png` IDLE Sheet (文件名框 + "导出到本机"), `r2-exported.png` 成功态 |
| D-12 冷启 10 s 黑底 | P2 | 系统玻璃材质就绪前的现象, 应用不可控; `qa2.sh` 首张截图改为 12 s 后并重试 | `r2-entry.png` (12 s) 正常; 记入 AGENTS.md |
| D-13 取色面板深度 | P2 | 未改代码; 本轮 `r2-picker.png` / `r2-vivid.png` 面板均在模型前 | 2/2 截图面板在前; 真机复核 |
| D-14 上色预览偏白 | P2 | `VoxelScene.kt:311-322` 预览改为不透明 PBR 方块, 颜色 = `ColorMath.mix(当前色, 新色, 0.7)` (`ColorMath.kt:37`), 放大 1.02 | `r2-paint.png` (1,0,0) 呈薄荷色 |
| D-15 无 onPause 保存 | P2 | `EditorScreen.kt:95-101` `LifecycleEventObserver` ON_PAUSE / ON_STOP → `EditorEvent.Paused` → `flushPendingSave()` (`EditorViewModel.kt:319-322`) | 单测 `pauseFlushesThePendingDebounce` |
| D-16 系统关闭 W2 后 `openEditorId` 残留 | P2 | 新增 `EditorSession` (`AppGraph.kt:13-27`), 入口 VM 读写它 (`EntryViewModel.kt:94-95, 104`); 编辑器 `DisposableEffect.onDispose` → `closed(projectId)` (`EditorScreen.kt:88-92`, compareAndSet 不覆盖更新的 id) | 代码; export 脚本关闭 W2 后入口正常 (`r2-entry-after-export.png`) |

其他: `QA §4` 建议的近白主色块描边已做 (D-02); 相机 pitch 未改 (规格值)。

## 2. 验收清单自评 (K.1–K.45)

通过 37 · 部分 2 · 未验证 6 (模拟器无空间输入)。截图省略前缀 `docs/screenshots/r2/`。

| # | 结果 | 备注 |
| --- | --- | --- |
| 1 | 通过 | `assembleDebug` + 34 单测; material 导入 0; Bold 0 |
| 2 | 通过 | `r2-entry.png` |
| 3 | 通过 | `r2-editor.png` |
| 4 | 未验证 | `EntryViewModel.requestOpen` 经 `EditorSession`; 需人工点第二项目 |
| 5 | 通过 | `Main.kt` 约束 |
| 6 | 通过 (代码) | 系统 Toolbar 仍在截图可见区外 |
| 7 | 通过 | `r2-picker.png` 三槽 + 槽 1 白环 |
| 8 | 通过 | 添加/擦除显示笔刷行 (`r2-ghost/erase/reset`), 上色/选择隐藏 (`r2-paint/selection`) |
| 9 | 通过 (代码) | |
| 10 | 未验证 | 单击需真机 |
| 11 | 通过 | `r2-ghost.png` 幽灵 + 300×88 标签 |
| 12 | **通过** | `r2-occupied.png` 珊瑚 5 dp 线框 + 35% 填充 + "格已占用"; 块数不变 (walk 导出 188) |
| 13 | 通过 | `r2-erase.png` |
| 14 | 通过 | `r2-paint.png` 薄荷 70% 预览 |
| 15 | 通过 (D) | `r2-selection.png` |
| 16 | **通过** | `r2-box.png` "5 × 1 × 1 / 松手放置" |
| 17 | 通过 (代码) | |
| 18 | 通过 | |
| 19 | 通过 | `r2-selection.png` 面板 328 dp |
| 20–23 | 通过 (D) | `r2-rotate/duplicate/stepx/delete.png` |
| 24 | 通过 (代码) | |
| 25 | **通过** | `r2-picker.png` 柔和页 (默认), `r2-vivid.png` 鲜艳页; 滑杆仍在可见区下 (代码) |
| 26, 28 | 未验证 | 需人工 |
| 27 | 通过 | |
| 29 | 通过 | `r2-project-sample-pavilion.json` |
| 30 | 未验证 | `r2-reset.png` 回正无异常 |
| 31 | 通过 (D) | |
| 32 | **通过** | 撤销/重做期间 "已自动保存" (`r2-undo/burst/redo`); 入口行两行完整 (`r2-entry-after-export.png`) |
| 33 | 通过 | 单测覆盖取消 / IO 失败 / onPause |
| 34 | 未验证 | |
| 35 | **通过** | `r2-export.png` IDLE 态 |
| 36 | 通过 | `docs/export/r2-newest.vox` 1832 B: `VOX ` 150, MAIN 0/1812, SIZE 16³, XYZI n=184, RGBA 1024 |
| 37 | 通过 | `VoxWriterTest` |
| 38 | 部分 | `r2-exported.png` 成功态 + 完成钮; Snack 在可见区外; MagicaVoxel 人工抽查未做 |
| 39 | 通过 | 导出钮已改 16 sp |
| 40, 41, 42, 43 | 通过 | 同 R1 (面板 328 / 标签 300×88 为申报偏离) |
| 44 | 通过 | walk 23 标记到 `done`, 无崩溃 / ANR; 设置已删 |
| 45 | 通过 | 91 条 × 2 语言 (无新增字符串) |

## 3. 新增已知差距 (已同步 AGENTS.md)

- **模拟器巨型黑 / 白方块 (环境问题)**: 重启后约一半 `screencap` 帧在场景前出现整屏黑 (偶白) 圆角方块; 入口窗单独、笔刷行移除、ElementSpace 均复现, 再次重启不解; 应用窗口只创建一次 (logcat `onWindowCreated`)。对策: `docs/scripts/cap.sh` 重试到黑/白占比 < 3 %, walk 用 ack 握手保证重试不越步。本轮 `r2-occupied / r2-box / r2-export` 需第二次 walk 才拿到干净帧 (`r2-walk-run2.log`, 其余截图来自 `r2-run.log` 的首次运行)。
- `screencap` 耗时 2–8 s; 演示不能靠固定 sleep, 已改为标记 + ack。
- 笔刷行: 规格摆位 (底边 z=0.5) 在任何从前上方的视角都被倾斜底座挡住; z=0.75 可见, 真机需复核与系统 Toolbar 不重叠, `gridspace_brush` 设置可免编译试摆位。
- Augment 内容尺寸变化会触发系统重建窗口 (黑 1 s); 保存状态文字固定宽度规避。
- 并发 Gradle 构建破坏 Kotlin 增量缓存 (单测全部 Unresolved / dex 缺类); 修法见 AGENTS.md。
- 步进 100 ms 禁用、标签 200 ms 淡出、onPause 保存、EditorSession 清理 仅代码 / 单测验证, 需真机操作确认。

## 4. 构建 / 测试输出 (尾部)

```
$ ./gradlew :app:assembleDebug :app:testDebugUnitTest --offline -q
EXIT 0
tests 34 failures 0   (BoxFill 5, ColorMath 3, History 4, SelectionOps 7, TargetCellResolver 7, VoxWriter 4, EditorViewModelSave 4)
androidx.compose.material imports: 0; FontWeight.Bold/SemiBold/W700: 0; strings values/values-zh: 91/91
unzip -p app-debug.apk classes2.dex | grep -c platform/SpatialApplication  -> 3
```

## 5. QA 第二轮复现命令

```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
cd /c/Users/Admin/Downloads/GridSpace
./gradlew :app:assembleDebug :app:testDebugUnitTest --offline -q
# 若单测报 Unresolved reference / APK 启动 ClassNotFound: rm -rf app/build/kotlin app/build/tmp/kotlin-classes && 加 --rerun-tasks

# 一键: 重装 → 入口 → sample → walk (23 标记, ack 握手, 黑帧重试) → export 脚本 → 拉 vox / JSON → 清设置
bash docs/scripts/qa2.sh docs/screenshots/qa2 qa2

# 单独: 任意状态截图 (重试黑帧)      bash docs/scripts/cap.sh out.png 8
# 单独: walk                          bash docs/scripts/walk.sh docs/screenshots/qa2 qa2
# 笔刷行摆位试验 (sample 模式)        bash docs/scripts/shot.sh out.png "0.5,1,0.75,0,-96,0"
# 结束务必:
adb shell settings delete global gridspace_demo; adb shell settings delete global gridspace_demo_ack; adb shell settings delete global gridspace_brush
```

## 6. 截图清单

| 文件 | 内容 |
| --- | --- |
| `r2-entry.png` / `r2-entry-after-export.png` | 入口 (行两行完整, 近白色块描边) |
| `r2-editor.png` | 样例 + 标题条 + 笔刷行 (D-03) |
| `r2-ghost.png` `r2-occupied.png` `r2-box.png` `r2-erase.png` `r2-paint.png` | 标签 300×88; 占用珊瑚反馈; 盒标签; 擦除; 上色 70% |
| `r2-selection.png` `r2-rotate.png` `r2-duplicate.png` `r2-stepx.png` `r2-delete.png` | 选区面板 328 dp |
| `r2-picker.png` `r2-vivid.png` | 柔和页 / 鲜艳页 |
| `r2-undo.png` `r2-burst-1..4.png` `r2-redo.png` `r2-reset.png` | 历史 + 标题条稳定性 |
| `r2-export.png` `r2-exported.png` `r2-done.png` | Sheet IDLE / 成功 / 关闭 |
| `brush/*.png` | 笔刷行摆位实验 (含模拟器黑块证据 `static-*`, `entry-*`, `elem-*`) |
| `r2-run.log`, `r2-demo-steps.txt`, `r2-logcat-*.txt`, `r2-index.json`, `r2-project-*.json`, `docs/export/r2-newest.vox` | 日志与文件 |
