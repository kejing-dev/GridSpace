# GridSpace 格间 · 研发报告 R3 (轻量)

日期 2026-10-07 · SDK 6.1.9 · 包名 `com.kejing.gridspace` · 验证环境: PICO 模拟器 `Pico_Emulator_6_1_fresh` (emulator-5554, 本轮未重启) · 依据: `docs/QA_REPORT_R2.md` §4 N-01..N-03 + §6 顺序, `docs/DESIGN_SPEC.md` §F.1 / §L.3

范围仅 N-01 / N-02 / N-03 + 可选 D-06 收尾, 其余未动。截图目录 `docs/screenshots/r3/` (前缀 `r3-`, `small/` 为缩放与裁剪证据), 完整 walk 19 标记 (23 张) 到 `done`, 日志 `r3-logcat-walk.txt` 无 `AndroidRuntime` / `FATAL` / ANR, `/data/anr/` 仅 10-06 21:06 旧 trace (`r3-anr-ls.txt`)。

## 1. 缺陷 → 修复 → 证据

| ID | 级别 | 修复 (文件:行) | 证据 |
| --- | --- | --- | --- |
| N-01 标签卡片盖住目标格 | P1 | `VoxelSceneView.kt:43` 新增 `LabelPlacement(anchor, cardOnRight)`; `:126-149` 锚点 = 目标格中心投影到 Volume 空间后 **上移 1.2 格、向远离 Volume 中心的一侧平移 1.0 格** (按投影 x 的符号选边), 朝观察者仅 **0.8 格** (原 2.5 格, 透视外推是遮挡主因); 常量 `:233-235`。`EditorScreen.kt:221-225` 按边选对齐: 右侧 `CenterLeft` + 12 dp (卡片向右伸), 左侧 `CenterRight` − 12 dp (向左伸), 卡片永远背向目标格; 卡片尺寸仍 300 × 88 (`TitleBar.kt:85-96` 未改); 摆位原理与透视注意事项记入 `AGENTS.md:129-138` | 5 个目标全部"格 + 卡片同时完整可见": `r3-ghost.png` (0,0,0) 薄荷幽灵在卡片右下方, `small/crop-ghost.png`; `r3-occupied.png` (3,1,12) 珊瑚线框 + 35 % 填充完整, 卡片 "! 格已占用 / 换一个格子" 在左侧, `small/crop-occupied.png`; `r3-box.png` 右侧盒 (10..14,0,13) 卡片在右, `small/crop-box.png`; `r3-erase.png` (0,0,0) 珊瑚线框完整, `small/crop-erase.png`; `r3-paint.png` (1,0,0) 薄荷预览完整, `small/crop-paint.png`。左半视野的卡片会伸出 Volume 左面 (与选区面板同样在窗外), 可读 |
| N-02 取消的保存仍写 SAVED | P2 | `EditorViewModel.kt:318` `withContext(NonCancellable)` 之后 `coroutineContext.ensureActive()`; job 已被新提交取消时抛 `CancellationException`, 不再写 SAVED, 状态保持 SAVING 直到新提交自己的写入完成 (`:306-312` 注释) | 新单测 `EditorViewModelSaveTest.cancelledSaveDoesNotReportSavedWhileANewerCommitIsPending` (`:84-108`): t=1200 写入中再提交, t=1600 旧写入已完成 (`completed = 1`) 但 `saveState == SAVING`, idle 后才 SAVED 且 `completed = 2`。去掉 `ensureActive` 该测试失败 (`expected:<SAVING> but was:<SAVED>`), 已回验; 模拟器 `small/strip-title-undo-burst.png` 6 帧 (撤销 ×3 / 重做 ×3, 1 s 间隔) 标题条均 "已自动保存" 薄荷点, 无黑块 |
| N-03 AGENTS.md dex 校验失效 | P2 | `AGENTS.md:160-171`: 改为 `unzip -Z1 … 'classes*.dex'` 遍历全部 dex 求和 `platform/SpatialApplication` 引用数, 0 则 exit 1 | 本 APK 12 个 dex, 求和 = 3, exit 0 (与 R2 的 "classes10.dex 3 处" 一致, `classes2.dex` 单独为 0) |
| D-06 收尾 (可选) | P2 | `EditorScreen.kt:163-165` 标题 Augment 内容 lambda 改为只捕获 `projectName: String` (+ State + remembered 回调), 不再捕获每次提交都变的 `project` | 同上 6 帧标题条; 本轮 23 张编辑器截图标题条无黑块 |

## 2. 构建 / 测试

```
./gradlew :app:assembleDebug :app:testDebugUnitTest --offline -q   → EXIT 0
单测 35/35 (BoxFill 5 · ColorMath 3 · History 4 · SelectionOps 7 · TargetCellResolver 7 · VoxWriter 4 · EditorViewModelSave 5), 0 失败 0 跳过
dex: SpatialApplication refs across all dex: 3  (exit 0)
adb install -r … Success; walk.sh 19 标记 / 23 张全部在状态内截到 (r3-run.log); logcat fatal/anr 0
gridspace_demo / gridspace_demo_ack / gridspace_brush → null
```

walk 导出 `庭院小亭 (15).vox · 188 块` (`r3-exported.png`), 与 R2 期望一致 (标签改动不影响提交结果)。

## 3. 验收清单变更行 (相对 QA_REPORT_R2 §3)

| # | R2 | R3 | 说明 |
| --- | --- | --- | --- |
| 11 | 部分 | **通过** | `r3-ghost.png`: 幽灵 (0,0,0) 与 "✓ 已对齐 · 松手放置 / X0 · Y0 · Z0" 同时可见 |
| 13 | 部分 | **通过** | `r3-erase.png`: 珊瑚线框与 "松手擦除" 同时可见 |
| 14 | 通过 | 通过 | `r3-paint.png` 薄荷预览完整露出 (R2 仅一角) |
| 32 | 通过 | 通过 (+单测) | "已自动保存" 现在严格表示最新提交已落盘 (N-02) |

其余行不变。

## 4. 待真机验证 (复制自 QA_REPORT_R2 §6 第 5 项, 模拟器无法做)

- K.4 打开第二个项目 (EditorSession 关旧 tag) · K.10 真实单击放置 · K.26 / K.28 取色面板滑杆与按钮 (模拟器视点下在可见区外) · K.30 回正手势 · K.34 重开项目列表刷新
- D-09 贴边选区步进按钮 100 ms 禁用 · D-10 标签 200 ms 淡出时序 · D-15 onPause 保存 · D-16 系统关闭 W2 后入口状态
- 补一张含系统 Toolbar 的编辑器截图 (K.6; 模拟器截图区看不到 Volume 底部)
- 新增: 真机上确认标签卡片在 Volume 左 / 右面之外仍可读 (N-01 左半视野卡片伸出 Volume), 以及眼手输入下 `LabelPlacement` 选边是否自然 (靠近视线中心的格子按投影 x 符号选边)
