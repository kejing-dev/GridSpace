# GridSpace 格间 · QA 报告 R1

日期 2026-10-06 · 被测构建: 本轮 `assembleDebug` 重新编译并重装 (APK 21:12) · 设备: PICO 模拟器 emulator-5554 (`Pico_Emulator_6_1_fresh`) · 依据: `docs/DESIGN_SPEC.md` §K (45 项) + Figma V2-01..04

复现脚本: `bash docs/scripts/qa1.sh` (重装 → 入口 → sample → walk → export → 拉文件/JSON → 清设置)。产物: `docs/screenshots/qa1/`、`docs/export/qa1-walk.vox`、`docs/export/qa1-export-script.vox`。

## 1. 构建 / 测试 / 静态检查

| 项 | 结果 |
| --- | --- |
| `./gradlew :app:assembleDebug --offline` | 通过 (exit 0) |
| `./gradlew :app:testDebugUnitTest --offline` | 通过, **30/30** (BoxFill 5 · ColorMath 3 · History 4 · SelectionOps 7 · TargetCellResolver 7 · VoxWriter 4; 0 失败 0 跳过) |
| `androidx.compose.material*` 导入 | 0 |
| `FontWeight.Bold / SemiBold / W700` | 0; `GridType` 全部 Medium/Normal (`ui/theme/GridTypography.kt`) |
| Kotlin 内硬编码 CJK / 英文用户可见串 | 0 (全部经 `str(R.string.*)`; `values` + `values-zh` 各 100 条) |
| `backgroundMaterial` | 0 |
| `hoverable(` | 0; 2D 用 `spatialHoverEffect` (9 处), 3D 用 `HoverEffectComponent` (`VoxelScene.kt:169`) |
| `Modifier.border(` | 3 处, 全部是规格要求的环: 色槽白环 `EditorToolbar.kt:157`、近白色 1 dp 内描边 `:161`、色板等色白环 `ColorPickerPopup.kt:155`。无容器边框 |
| 预设色板 vs §D.3 | 柔和 / 鲜艳 / 大地 各 24 色, 与规格 72 个十六进制逐项一致 (脚本比对, `Palette.kt:5-25`) |
| 默认色槽 | `#f3bc9c / #a5edcc / #f3f5ed`, activeSlot 0 (`Palette.kt:34`) ✓ |
| 模拟器 ANR / 崩溃 | walk + export 全程 `logcat` 无 `AndroidRuntime/FATAL/ANR`; `/data/anr/` 只有 21:06 的旧 trace (QA 之前), 本轮无新增 |
| 清理 | `settings get global gridspace_demo` → `null` ✓ |

### 1.1 dp / sp 核对 (规格 → 代码)

| 规格 | 代码 | 结果 |
| --- | --- | --- |
| §C.2 模式钮 高 64 / 最小宽 120 / 圆角 24 / 图标 24 + 间 8 + 18 sp Medium / 内边距 20 / 选中薄荷+#1C2A26 / 未选透明 | `EditorToolbar.kt:112-125` | ✓ |
| §C.2 模式间距 8; 分隔 1×32 左右 12 | `:71`, `:100-104` | ✓ |
| §C.2 色槽 热区 56 / 圆 48 / 间 12 / 当前 2.5 dp 白环 (外径 53) / 近白 1 dp 内描边 | `:76`, `:138-145`, `:151-162` | ✓ (IconButton → Box+combinedClickable, 已申报替换) |
| §C.2 撤销/重做/回正 56, 图标 24, 间 8, 栈空禁用 | `:92-96`, `Controls.kt:423-444` | ✓ |
| §C.3 笔刷行 232×56, 项 112×48, itemSpace 4, padding 4, 16 sp Medium | `BrushRow.kt:19-27` | ✓ |
| §C.4 标题条 高 64 / 左右 24 / 项目名 22 Medium 省略 / 间 16 / 8 dp 点 + 15 sp / 导出钮 48 高 图标 20 | `TitleBar.kt:34-50` | ✓ 除 **导出文字 18 sp (规格 16)** `Controls.kt:416-417` |
| §D.2 面板 700 / 内边距 32 / 行距 24 | `ColorPickerPopup.kt:46-48, 89` | ✓ |
| §D.2 标题 22 Medium + 关闭 40/图标 20 | `:90-94` | ✓ |
| §D.2 槽 80 间 24, 编辑槽 3 dp 白环 (86), 两行 15 sp 说明 | `:95-108` | ✓ |
| §D.2 段控 636×56, 项 48 | `:109-118` (fillMaxWidth=636) | ✓ |
| §D.2 色板 8×3, 64, 间 12, 圆角 16, 等色 2.5 白环, hover | `:142-161` | ✓ |
| §D.2 滑杆 标签 48 / 轨 560 / thumb 40 / pressed 44 / area 56 / track 10 / 行高 56 / 行间 12 / 透明轨+渐变 Box | `HslSliders.kt:27-29, 40-74` | ✓ |
| §D.2 取色钮 200×64 图标 20 + 间 16 + 完成 64 薄荷 占余宽 | `:121-129`, `Controls.kt:368-389` | ✓ |
| §D.4 取色提示条 56 高, 取消 48, 模式钮禁用 | `:73-83`, `EditorUiState.kt:314` | ✓ |
| §E.4 面板 320 / 内边距 24 | `SelectionPanel.kt:38` | ✓ (但 3×88+2×8 = 280 > 内宽 272, 规格自身不自洽, 见 D-08) |
| §E.4 行1 18 Medium + 取消 40 高 16 Medium 次色 | `:40-49` | ✓ |
| §E.4 3 钮 88×72 圆角 16 图标 24 文字 15 Medium 间 8, 删除珊瑚 | `:52-56, 70-84` | ✓ |
| §E.4 网格移动 15 Regular 次色; 轴标 16 Medium 宽 24; 步进 56 / 值 56 / 56 | `:58-65, 91-100` | ✓ |
| §G.1 入口: 标题 22 / 主钮 400×64 18 Medium 图标 24 / 段控 48 / 序号 28 薄荷 14 Medium / 行高 44 / 列表 72, 色块 40 圆角 8, 18 Medium / 15, 删除 48 | `EntryScreen.kt:34-35`, `NewProjectColumn.kt:37-80`, `RecentProjects.kt:48-65` | ✓ (72 行高导致次文字被裁, 见 D-02) |
| §H Sheet 内容宽 560, 段控 272×48, 文件名框 56, 底钮 64 | `ExportSheet.kt:31, 76-90` | ✓ |
| §B.1 约束 入口 960×640..1600×1000; Volume 960³..2700³, 1600³, 无玻璃, Dynamic | `Main.kt:196, 202-214` | ✓ |
| §C.1 cellDp 1200/N, 底座下沉 360, 初始 yaw 0.60 / pitch 0.40, pitch −0.2..1.4, 0.006 rad/px, zoom 0.5..2.5, 回正 300 ms | `CameraMath.kt:17-28` | ✓ |

### 1.2 逻辑核对 (代码)

| 项 | 结论 | 证据 |
| --- | --- | --- |
| 历史: 每次提交 1 步; 整盒 1 步; 空命令不入栈; 撤销后新提交清重做; 上限 100; 撤销/重做清空选区+翻译归零+触发保存 | ✓ | `EditCommand.kt:192-234`, `EditorViewModel.kt:208-244`, `HistoryTest` |
| 色槽/模式/笔刷/选区/相机不入历史 | ✓ | `EditorViewModelColour.kt:379-388` 只改 project.slots; 相机在 View 内 |
| 持久化: JSON 字段 = 规格 (version 1, id, name, gridSize, slots[3], activeSlot, voxels[[x,y,z,"#hex"]], createdAt, updatedAt); 索引 (id, name, gridSize, voxelCount, dominantColor, updatedAt, +corrupt) | ✓ | `ProjectJson.kt`; 设备文件 `qa1-project-sample-pavilion.json` (184 voxels, slots 3, activeSlot 0), `qa1-index.json` |
| `.tmp` + rename, IO 线程, Mutex 串行 | ✓ | `ProjectRepository.kt:444-451, 379-389` |
| 1 s 防抖; 关闭 Volume 立即保存 (应用级 scope) | ✓ 但见 **D-01** (取消中的保存被判为失败) | `EditorViewModel.kt:276-301` |
| 损坏文件 → 索引标 corrupt → 行显示 "文件损坏", 点击 Snack | ✓ | `ProjectRepository.kt:392-399, 418-423`, `RecentProjects.kt:51`, `EntryViewModel.kt:196-202` |
| VOX: `VOX ` + 150, MAIN(0, 子块长), SIZE N N N, XYZI n + (x, z, y, i), i 从 1 起, RGBA 256×4 (i 在 i−1, 未用填 0), >255 最近色合并 | ✓ | `VoxWriter.kt:59-92, 108-122`, `VoxWriterTest` 4 例 |
| Sheet >255 色提示 | ✓ | `EditorScreen.kt:132`, `ExportSheet.kt:92-97` |
| 演示模式读 `Settings.Global`, 入口 `LaunchedEffect(Unit)` + 编辑器 `LaunchedEffect(project != null)`; 覆盖 §L.4 全部步骤 (添加 3 含 1 占用 → 长方体 → 擦除 → 上色 2 → 选 5 → 旋转 → 复制 → X+1 → 删除 → 取消 → 长按色槽 → 鲜艳 → 关闭 → 撤销×3 → 重做×3 → 回正 → 导出 Sheet → 导出 → 完成) | ✓ | `DemoMode.kt:37-43, 93-174`; `qa1-demo-steps.txt` 17 步全部打点 |
| 色彩纪律 (K.40) | ✓ 珊瑚仅: 选区删除钮 `SelectionPanel.kt:55`、占用线框/擦除线框/擦除盒 `VoxelScene.kt:227,229,234`、保存失败点 `TitleBar.kt:45`、失败图标 `ExportSheet.kt:95,118`、保存失败 Snack 图标 `EditorScreen.kt:177`、删除对话框删除钮 `DeleteDialog.kt:26`; 薄荷仅: 主钮 `Controls.kt:40`、当前模式 `EditorToolbar.kt:115`、幽灵 `VoxelScene.kt:279`、保存成功点 `TitleBar.kt:44`、序号 `NewProjectColumn.kt:73`、导出成功图标 `ExportSheet.kt:105` | — |
| 导出文件核对 | ✓ | 见 §2 K.36 |

## 2. 验收清单 K.1–K.45

结果: **通过 34 · 失败 1 · 部分 4 · 未验证 6**。"未验证" = 模拟器无法产生空间输入 (单击/捏合/长按/拖拽), 需真机。截图路径省略前缀 `docs/screenshots/qa1/`。

| # | 结果 | 证据 / 说明 |
| --- | --- | --- |
| 1 | 通过 | 构建 + 30 测试; material 导入 0 |
| 2 | 通过 | `qa1-entry-22s.png`: 1080×720 玻璃窗, 标题 "GridSpace 格间"。注: 重装后首次冷启 10 s 时窗体仍为黑底 (`qa1-entry.png`, D-12) |
| 3 | 通过 | `qa1-editor.png`: Volume 内无玻璃, 入口窗保留在后; 系统底板在模拟器不可辨 |
| 4 | 未验证 | 代码 `EntryViewModel.kt:212-216` 先关旧 tag; 需人工点第二个项目 |
| 5 | 通过 | `Main.kt:196, 210-214` |
| 6 | 通过 (代码) | 系统 Toolbar 在所有截图可见区之下 (已知差距), 顺序/尺寸/颜色按 `EditorToolbar.kt:70-97, 112-121` 判定 ✓; 需真机截图 |
| 7 | 通过 (部分) | 默认色 `Palette.kt:34`; `qa1-picker.png` 顶部 3 槽颜色正确, 槽 1 白环; 短按换环需人工 |
| 8 | 通过 | 裁剪比对: 添加 (vivid/undo/reset) 显示笔刷行, 上色/选择 (paint/selection/rotate/stepx/delete) 隐藏; `EditorUiState.kt:304` |
| 9 | 通过 (代码) | `EditorToolbar.kt:92-94` enabled = canUndo/canRedo |
| 10 | 未验证 | 需真实单击; `TargetCellResolver.adjacentCell/baseCell` + 7 测试 |
| 11 | 通过 | `qa1-ghost.png`: 薄荷半透明幽灵 (0,0,0) + 标签 "✓ 已对齐 · 松手放置 / X0 · Y0 · Z0"; 松手后方块落位 (occupied 截图可见 2 个新珊瑚块) |
| 12 | 部分 | 历史/块数不变 ✓ (导出 188 块与期望集合逐格一致, 含 0 个占用格); 但 `qa1-occupied.png` 看不到珊瑚线框与 "格已占用" 标签: 目标 (6,1,6) 在亭子内部被横梁/柱遮挡, 线宽 2 dp 在 1600 dp Volume 里约 1.5 px (D-04) |
| 13 | 通过 | `qa1-erase.png` 标签 "松手擦除"; 导出块数 −1 ✓ |
| 14 | 通过 | `qa1-paint.png` 标签 "松手上色"; 导出中 (1,0,0)/(3,0,3) 为薄荷 ✓; 同色 0 步 `EditCommand.paint` + HistoryTest ✓ |
| 15 | 通过 (D) | `qa1-selection.png`: 5 个白线框 + 面板 "已选 5 块"; 再点取消需人工 |
| 16 | 通过 | 导出含 (10..14,0,13) 5 块 ✓, BoxFillTest 1 步; 截图在 PreviewMoved 之前抓到, 未拍到 "5 × 1 × 1" 文案 (D-11) |
| 17 | 通过 (代码) | `BoxFill.erase` + BoxFillTest |
| 18 | 通过 | `Brush.FACE` 不在 `BrushRow`; `brushes` 仅 ADD/ERASE (`EditorUiState.kt:281`) |
| 19 | 通过 | `qa1-selection.png` 面板在 Volume 左外, 含 旋转 90° / 复制 / 删除 (珊瑚) / X Y Z 步进 / 取消 |
| 20 | 通过 | `qa1-rotate.png` 行变为沿 Z; 导出校验 (12,0,11..15), 中心 (12,0,13) 不变; 2 格覆盖地板 (规格允许); SelectionOpsTest |
| 21 | 通过 | `qa1-duplicate.png` 副本在 +X 相邻且带白框, 原块无框 |
| 22 | 通过 | `qa1-delete.png` 副本消失, 面板隐藏; `qa1-undo.png` 撤销后恢复 |
| 23 | 通过 | `qa1-stepx.png` X 显示 1, 副本整体 +1; 越界不动有测试 |
| 24 | 通过 (代码) | `EditorViewModel.kt:74`; walk 中取消发生在删除后为空操作 |
| 25 | 部分 | `qa1-picker.png`: 700 宽, 3 槽 80, 段控, 8×3 色板 64/12 ✓; 滑杆与两个按钮在截图可见区之下, 按代码判定 ✓ |
| 26 | 未验证 | 需人工点色板; `SwatchPicked → writeEditingSlot` 立即写槽 |
| 27 | 通过 | 72 色脚本比对一致 |
| 28 | 未验证 | 需人工; `EyedropperStarted/pickFromCube` 路径完整 |
| 29 | 通过 | 设备 JSON `slots/activeSlot` ✓; `ProjectJson.decode` 回读 |
| 30 | 未验证 | 需拖拽; 回正事件执行无异常 (`qa1-reset.png`) |
| 31 | 通过 | undo/redo 截图模型按预期变化; 标题/选区/笔刷 Augment 位置不变 |
| 32 | **失败** | (a) 撤销×3 期间标题条显示 **"保存失败" 珊瑚点** (`qa1-redo.png`, D-01); (b) 入口列表 "庭院小亭 / 184 块 · 刚刚" 存在但次文字被裁掉下半 (`qa1-entry-after-export.png`, D-02); 重开需人工 |
| 33 | 通过 (代码) | `.tmp`+rename, IO; 失败 Snack + 珊瑚点路径存在 (本轮被误触发, 见 D-01) |
| 34 | 未验证 | 需人工; `DeleteDialog` + `deleteConfirmed` 完整 |
| 35 | 部分 | `qa1-export.png` 为成功态; IDLE 态 (文件名框、"导出到本机") 未拍到 (脚本 1 s 后即确认, D-11); 代码 `ExportSheet.kt:76-90` ✓ |
| 36 | 通过 | `qa1-export-script.vox` 1832 B: `56 4f 58 20 | 96 00 00 00 | MAIN 0/1812 | SIZE 16 16 16 | XYZI n=184 | RGBA 1024`; n = JSON voxels 184 ✓。`qa1-walk.vox` n=188 与 walk 期望集合逐格一致 |
| 37 | 通过 | 样本 (3,0,3) → vox (3,3,0) ✓; 索引 1..3; RGBA[0]=f3f5edff; `VoxWriterTest` |
| 38 | 部分 | 成功态 "下载/GridSpace/庭院小亭 (5).vox · 188 块 · 2 KB" + 完成钮 ✓; Snack 在可见区外; MagicaVoxel 人工抽查未做 |
| 39 | 通过 | `GridType` 22/18/16/15/13-14; 无 Bold; 例外: 导出钮 18 sp (规格 16, D-07) |
| 40 | 通过 | 见 §1.2 |
| 41 | 通过 (替换) | SDK 6.1.9 无 accent 角色, 未改任何系统角色, Mint 逐组件设 (申报替换); 无 backgroundMaterial; border 仅规格环; hover 用系统 |
| 42 | 通过 | 56/56/64/64/56/56 ✓; 关闭 40 / 取消 40 / 取色取消 48 / 导出钮 48 / 列表删除 48 为规格各节自定值 (规格与 K.42 自身不一致) |
| 43 | 通过 | 仅 回正 300 / 新块 150 / 占用闪 120; 无自动旋转 |
| 44 | 通过 | walk 17 步全部到 "done", 无崩溃/ANR; 删除设置后入口正常 (`qa1-entry-22s.png`) |
| 45 | 通过 | 100 条字符串在 values + values-zh |

## 3. 缺陷列表

无 P0。

| ID | 级别 | 标题 | 位置 | 期望 (规格) | 实际 | 复现 |
| --- | --- | --- | --- | --- | --- | --- |
| D-01 | **P1** | 连续提交时被取消的保存被判为 "保存失败" 并弹 Snack | `EditorViewModel.kt:276-294` (`scheduleSave` 取消上一 job; `persist` 用 `runCatching` 吞掉 `CancellationException` 后仍写 `SaveState.FAILED` + `SaveFailed` 通知); `qa1-redo.png` | §F.2 "自动保存中/已保存/失败: 灰/薄荷/珊瑚"; K.32 "每次提交 1 s 后显示已自动保存" | 撤销×3 (间隔 1 s = 防抖时长) 期间标题条变 "保存失败" 珊瑚点, 并触发 "保存失败, 请检查存储空间 / 重试" Snack; 文件其实已正确写入 | walk 脚本 undo/redo 步; 或任何 1 s 内连续两次提交 |
| D-02 | **P1** | 最近项目行的次文字 ("184 块 · 刚刚") 被裁剪 | `RecentProjects.kt:440` `Modifier.height(72.dp)` 强制行高, SDK `ListItem` 内容 (18 sp 主 + 15 sp 次 + 内边距) 超出; `qa1-entry-after-export.png` | §G.1 ListItem 72: 主文字 18 Medium, 次文字 15 "128 块 · 2 小时前" | 次文字只露出上半行, 不可读 | 打开含项目的入口页 |
| D-03 | **P1** | 笔刷行 Augment 被模型遮挡 | `EditorScreen.kt:110` anchor (0.5, 1, 0.5) 处于 Volume 中深度, 模型底座/地板前缘在其前方; `qa1-editor.png`, `qa1-vivid.png` 底部只露出 "方体" 两字 | §B.2 "贴在 Volume 底边内侧, 位于系统 Toolbar 上方"; Figma V2-01 笔刷行完整显示在模型下方 | 段控被亭子地板与柱子盖住, 仅角落可见 | 打开样例项目 (添加模式) |
| D-04 | **P1** | 占用反馈 (珊瑚线框 + "格已占用") 在 walk 中不可见 | `DemoMode.kt:103` 目标 (6,1,6) 在亭内被梁柱遮挡; `VoxelScene.kt:227` 线宽 = 2 dp 等效 (`:183` 2/75 格), 远距离约 1.5 px; 标签 Augment 锚在模型内部被几何体遮挡; `qa1-occupied.png` | K.12 "出现珊瑚线框并显示格已占用"; §F.2 占用 = 珊瑚线框 2 dp + 标签 | 截图中既无线框也无标签, QA 无法验收 K.12 | walk "occupied" 步 |
| D-05 | P2 | 目标标签偏小且可能被模型遮挡 | `TitleBar.kt:252-263` 15/13 sp, 188 dp 最小宽; 锚点 = 幽灵格 + (0.6,0.6,0) 投影 (`VoxelSceneView.kt:124-131`); `qa1-ghost.png` 局部放大才可读 | §L.5 "若标签过小, 改 300×88 并真机复核" | 1600 dp Volume 中标签约等于 11-12 px 文字; 目标在模型内部时被遮挡 (D-04) | walk ghost / occupied |
| D-06 | P2 | 标题条 Augment 在保存状态变化后渲染为黑块/局部空白 (模拟器) | `TitleBar.kt:33-42` 每次提交触发 SAVING→SAVED 重组; `qa1-occupied.png`, `qa1-box.png` (名称与导出钮空白), `qa1-reset/export/done.png` (整条黑, 持续 ≥10 s) | §B.2 Augment 系统材质, 内容稳定 | 17 张截图中 5 张标题条异常; 真机需复核 | 任何提交后 1-2 s 截图 |
| D-07 | P2 | 标题条 "导出" 按钮文字 18 sp (规格 16 sp Medium) | `Controls.kt:416-417` `SecondaryButton` 固定 `GridType.label`; `TitleBar.kt:243` | §C.4 "导出 高 48, 图标 20, 文字 16 sp Medium" | 18 sp | 代码 |
| D-08 | P2 | 选区面板三钮行 280 dp 超出内容宽 272 dp | `SelectionPanel.kt:38` 320−2×24 = 272; `:52` 3×88 + 2×8 = 280 | §E.4 (规格自身不自洽) | 删除钮右侧压入 8 dp 内边距, 右边距比左边距窄 (`z-selpanel` 可见右边距偏小) | 选区面板 |
| D-09 | P2 | 步进越界时未 "按钮短暂禁用 100 ms" | `EditorViewModel.kt:260-272` 直接忽略 | §E.4 "越界 → 不执行 + 按钮短暂禁用 100 ms" | 无任何反馈 | 选区贴边后点 + |
| D-10 | P2 | 目标标签无 200 ms 淡出; 面板淡入由系统 | `EditorScreen.kt:122` preview 为 null 即移除 | §F.1 "松手后 200 ms 内淡出" | 立即消失 | 任意捏合松手 |
| D-11 | P2 | walk 脚本关键态保持时间短于截图延迟, 多个态拍不到 | `DemoMode.kt:97-98` 单格幽灵→盒 2.5 s; `:145-151` 面板打开→切鲜艳 2 s; `:64-67` Sheet 打开 1 s 即确认; `:169-172`。模拟器 screencap 约 1.5 s | §L.4 "每步 4 s"; K.16/25/35 需拍到 盒标签 / 柔和页 / IDLE Sheet | 本轮 picker 抓到鲜艳态, vivid 抓到已关闭, export 只抓到成功态 | `walk.sh` |
| D-12 | P2 | 重装后首次冷启动 10 s 时入口窗仍为黑底 (玻璃未就绪, 深色文字不可见) | `qa1-entry.png` (10 s) vs `qa1-entry-22s.png` (22 s 正常) | §B.1 启动即显示 | 只见薄荷钮/序号/色块/垃圾桶; 二次启动 10 s 内正常 | `adb install -r` 后立即启动 |
| D-13 | P2 | 取色面板与模型深度关系不稳定 | 研发截图 `walk/r1-vivid.png` 中模型梁柱画在面板之上; 本轮 `qa1-picker.png` 面板在前 | §B.2 SpatialPopup 浮层在前 | 可能是 Popup 首秒透明态; 需真机确认 | walk picker |
| D-14 | P2 | 上色预览 (70% 覆盖) 观感偏白, 未呈现薄荷色 | `VoxelScene.kt:293-296` unlit 透明材质 1.02 格; `qa1-paint.png` (1,0,0) | §E.2 "方块临时显示新色 @ 70%" | 珊瑚块上叠薄荷 70% 应偏薄荷, 截图呈浅灰白 | walk paint |
| D-15 | P2 | 未在 `onPause` 保存 (仅 Volume dispose) | `EditorScreen.kt:57` | §G.3 "W2 onPause / 关闭时立即保存" | 后台切换时依赖 1 s 防抖 | 代码 |
| D-16 | P2 | 用户用系统关闭钮关 W2 后 `openEditorId` 未清, 下次打开他项目会对已关闭 tag 调 `closeWindowContainer` | `EntryViewModel.kt:154, 212-216` | §G.2 | 多半无害, 需确认 SDK 对不存在 tag 的行为 | 关 W2 → 开另一项目 |

## 4. 可用性 / 与 Figma V2 对照

| 画面 | 对照 Figma | 观察 |
| --- | --- | --- |
| 工作台 (V2-01) | 模型悬浮 + 标题条在上 + 笔刷行与工具栏在模型正下方 | 标题条位置与样式贴近 Figma (深色导出钮 ✓)。系统 Toolbar 在模拟器可见区外, 无法与 Figma 的 "一条工具栏" 对照; 笔刷行被模型遮挡 (D-03), 视觉上像残片。模型视角 (yaw 0.60 / pitch 0.40) 比 Figma 等轴略低, 顶梁更挤; 可考虑 pitch 0.5 |
| 幽灵 + 标签 (V2-01 右侧 "✓ 已对齐 · 松手放置 / 吸附到表面") | 标签紧贴幽灵右上, 清晰可读 | 标签玻璃卡片与文案正确, 但在 1600 dp Volume 中文字小 (D-05); 目标在模型内部时标签被遮 (D-04) |
| 取色面板 (V2-02) | 700 宽, 三槽 + 两行说明, 段控, 8×3 色板, 三滑杆, 取色/完成 | 上半部分与 Figma 几乎一致 (槽大小、白环、色板圆角与间距)。面板锚在工具栏色槽上, 因此在模拟器里贴在画面底部, 下半截 (滑杆/按钮) 看不到; 真机上面板会覆盖模型大部, 符合 Figma "面板贴在色槽上方" |
| 选区 (V2-03) | 面板在选区旁, 带虚线引导 | 面板内容/层级与 Figma 一致 (已选 N 块 / 取消 / 三钮 / 网格移动 / 三行步进), 珊瑚删除钮 ✓。无引导线 (v1.1)。面板文字在 Volume 左外 32 dp 处偏小 (15-18 sp 在远处约 12 px), 步进值 "0" 很淡。三钮行右边距比左边距窄 (D-08) |
| 工具栏规范 (V2-04) | 64 高模式钮 / 圆角 24 / 色槽 | 代码值一致; 无法截图 |
| 入口页 (规格 G.1) | — | 布局与规格一致, 主钮/段控/三步/空态文案正确; 列表行次文字被裁 (D-02); 白色 (#f3f5ed) 主色块在浅玻璃上几乎看不见, 建议近白色块加 1 dp 内描边 (同色槽规则) |
| 导出 Sheet (规格 H) | — | 成功态清晰: 薄荷 ✓ 48 + "已导出" + 路径/块数/KB + 满宽 "完成"; 关闭钮在左上 ✓。同名自动 "(5)" ✓ |
| 整体 | — | 3D 反馈线框 (白 2 dp) 在选区上清楚可辨; 珊瑚线框因位置/线宽本轮未见。样例模型中心方块的 "发白" 为系统 hover 高亮 (模拟器光标在画面中心), 非缺陷 |

## 5. 第二轮优先修复 (按顺序)

1. **D-01** 保存取消误报失败: `persist` 中区分 `CancellationException` (重抛或 `ensureActive`), 或把写文件放进 `withContext(NonCancellable)`; 加单测 "连续两次 scheduleSave 不产生 FAILED"。
2. **D-02** 去掉 `RecentProjects.kt:440` 的固定 72 高, 改 `heightIn(min = 72.dp)` 或按 SDK ListItem 自适应; 近白主色块加 1 dp 内描边。
3. **D-03** 笔刷行: 按 §L.3 把 offset.y 改为 −96 dp 并/或把 z 锚到 Volume 前沿 (z = 0 或 1, 以 SDK 约定为准), 确保不被模型遮挡; 真机截图确认与系统 Toolbar 不重叠。
4. **D-04** walk 的占用目标改为可见格 (如柱底 (3,1,3) 或地板 (5,0,5)); 珊瑚/擦除线框线宽提到 3 dp 等效; 标签锚点向观察者方向偏移 (+z) 避免被几何体遮挡。
5. **D-05** 目标标签改为 300×88 变体 (文字 17/15 sp) 并真机复核可读性。
6. **D-06** 标题条: 避免每次提交整条重组 (保存点改为仅颜色变化的稳定节点, 或把保存状态拆成独立小 Augment); 真机确认是否仅模拟器问题。
7. **D-11** walk 时序: 单格幽灵→盒预览、面板打开→切页、Sheet 打开→确认 各保持 ≥4 s, 并在状态生效后再打 `step` 日志, 让 K.16/25/35 可截图。
8. **D-07** 导出钮文字 16 sp (`SecondaryButton` 增加 textStyle 参数)。
9. **D-08** 选区面板宽改 336 或三钮改 80×72 (与设计确认), 保证左右内边距对称。
10. **D-09 / D-10** 步进越界 100 ms 禁用; 标签 200 ms 淡出 (AnimatedVisibility)。
11. **D-14** 核实上色预览材质 alpha 混合 (70% 薄荷应可辨), 必要时改用 PBR 半透明或调整 alpha。
12. **D-15 / D-16** onPause 立即保存; 系统关闭 W2 时清 `openEditorId` (或对不存在 tag 的 close 做保护)。另请研发在真机补一张含系统 Toolbar 的编辑器截图 (K.6)。
