# GridSpace 格间 · 研发报告 R1

日期 2026-10-06 · SDK 6.1.9 · 包名 `com.kejing.gridspace` · 验证环境: PICO 模拟器 `Pico_Emulator_6_1_fresh` (emulator-5554)

## 1. 实现范围 (按规格章节)

| 章节 | 状态 | 说明 |
| --- | --- | --- |
| A 范围 | 完成 | 16/24/32/48 网格; 4 模式; 单格 + 长方体 (添加/擦除); 3 色槽 + 3 预设 + HSL + 取色; 选区 5 操作; 100 步历史; 相机; 自动保存; .vox 导出; 入口; 演示模式 |
| B 窗口 | 完成 | W1 `DefaultWindowContainer` 1080×720 (manifest), 约束 960×640..1600×1000; W2 `WindowContainer("EditorVolume", Volumetric, 1600³, 无玻璃, Dynamic)`, 约束 960³..2700³, tag = projectId, 打开新项目前 `closeWindowContainer` 旧 tag |
| B.2 附件 | 完成 (有替换) | TitleAugment / BrushAugment / SelectionAugment 为 `Augment`; 主工具栏为窗口级 `Toolbar`; 取色为 `SpatialPopup` (锚在色槽 Box, above 16 dp, 700 dp, 圆角 24); 导出为 `Sheet`; 删除确认 `AlertDialog`; 提示 `SnackbarHost`; **目标标签改为随幽灵移动的 `Augment`** (见 §3) |
| C 布局 | 完成 | cellDp = 1200/N; 底座中心在 Volume 中心下 360 dp; 底座 N×0.1×N 盒 #2D383B@70% + 网格线 #688681@40%; 每可见方块一个实体 (PBR 粗糙 0.9, 无金属) + box 碰撞 + Interactable + HoverEffect; 幽灵薄荷 45%; 选区每格白线框; 初始 yaw 0.60 / pitch 0.40 |
| C.2 工具栏 | 完成 | 4 × `ToggleButton` (高 64, 最小宽 120, 圆角 24, 图标 24 + 8 + 18 sp Medium, 选中薄荷/#1C2A26, 未选透明) · 3 色槽 (56 热区, 48 圆, 当前 2.5 dp 白环, 近白色加 1 dp 内描边) · 撤销/重做 (栈空禁用) · 回正 |
| C.3 笔刷行 | 完成 | `SegmentControl` 232×56, 项 112×48; 仅添加/擦除显示; 按模式各记一份 |
| C.4 标题条 | 完成 | 项目名 22 Medium (省略) · 保存点 (灰/薄荷/珊瑚) + 15 sp · 导出 `Button` 48 高, 图标 20 |
| D 取色面板 | 完成 | 700 dp, 内边距 32, 行距 24: 标题+关闭 40 · 3 槽 80 (编辑槽 3 dp 白环) + 两行说明 · 预设段控 · 8×3 色板 64/间 12 (等色白环, 系统 hover) · 色相/饱和/明度 `Slider` (thumb 40/44, area 56, track 10, 透明轨 + 渐变 Box) · 从方块取色 200×64 + 完成 64 (薄荷). 长按或再点当前槽打开; 颜色立即写槽; 不入历史; 取色时面板收为 56 dp 提示条, 模式按钮禁用 |
| E.1/E.2 单格 | 完成 | `HitResolver` + `TargetCellResolver`: 添加 = 命中方块相邻格 (按命中点主轴) / 底座格; 擦除/上色/选择 = 方块本身; 单击直接提交; 捏合保持走 PreviewStarted/Moved/Committed |
| E.3 长方体 | 完成 | A 固定于按下, B 每次移动更新; 添加 = 薄荷半透明盒, 擦除 = 珊瑚线框盒; 标签 "W × H × D / 松手放置|擦除"; 占用格跳过; 裁剪到网格; 整盒 1 步; >1 万格标签 "较大, 稍候"; 实体分帧创建 (每帧 64) |
| E.4 选区 | 完成 | 320 dp 面板: 已选 N 块 / 取消 / 旋转 90° / 复制 / 删除 (珊瑚) / 网格移动 X Y Z (− 值 +, 56 dp); 绕包围盒中心旋转, 越界 Snack; 复制 +1 → +W 回退 → "没有空位"; 删除清空; 步进越界不动; 撤销重做清空选区; 面板 yN 随选区中心高度 (0.15..0.85) |
| E.5 相机 | 完成 | OrbitCatcher = 模型后方 1500×1500×10 dp 不可见碰撞板; 拖方块/底座由模式消费; 0.006 rad/px; pitch −0.2..1.4; 双手缩放 0.5..2.5; 回正 300 ms 缓动 |
| E.6 历史 | 完成 | `History` 100 步; 撤销后新提交清重做; 色槽/模式/选区/相机不入历史 |
| F 反馈 | 完成 | 幽灵/珊瑚线框/擦除线框/上色 70% 覆盖/选择白框/盒预览; 占用松手闪 120 ms; 新方块 150 ms 0.8→1.0 (`PopSystem`); 保存点; Snack: 保存失败(重试)/旋转越界/没有空位/已导出 |
| G 入口 | 完成 | 标题栏; 新建空白项目 (薄荷 400×64, + 图标); 网格尺寸段控 (记忆); 开始之前 3 步 (28 dp 薄荷序号); 最近项目 ≤20 `ListItem` 72 (40 dp 主色块 / 名称 18 / "N 块 · 时间" 15 / 删除 48); 空态; 删除 `AlertDialog` + Snack; 损坏文件行显示 "文件损坏" 点击 Snack |
| G.3 持久化 | 完成 | `filesDir/projects/{id}.json` + `index.json`, 格式同规格 (version 1); `.tmp` + rename, IO 线程; 提交后 1 s 防抖, 关闭 Volume 立即保存 (应用级 scope) |
| H 导出 | 完成 | `Sheet`: 格式段控 (仅 VOX) + 说明 / 文件名 `TextField` (trailing ".vox") / >255 色提示; 底部 导出到本机 64 → 导出中(进度) → 已导出 (✓ + 路径 · 块数 · KB, 按钮变 完成) / 失败 (重试); MediaStore `Download/GridSpace`, 同名自动 (n) |
| H.1 VOX | 完成 | `VoxWriter`: 'VOX ' 150, MAIN, SIZE/XYZI/RGBA, app(x,y,z)→vox(x,z,y), 索引 1 起, >255 最近色合并; 单元测试 |
| I 色彩/文字 | 完成 (有替换) | `GridColors` (Mint/OnMint/Coral/White/BaseInk/GridLine); `GridType` 22/18/16/15/13-14, ≤17 sp 无 Bold; 系统字体 (不捆绑 Noto) |
| L.4 演示 | 完成 | `gridspace_demo` = sample / walk / export; 事件驱动; 每步打 `GridSpaceDemo` 日志便于截图同步 |

## 2. 验收清单自评 (K.1–K.45)

通过 = 本轮已在模拟器或代码中确认; 未测 = 模拟器无法产生空间输入 (单击 / 捏合 / 双手缩放 / 长按 / 悬停), 需真机或人工点击。

| 项 | 结果 | 备注 |
| --- | --- | --- |
| 1 | 通过 | `assembleDebug` + `testDebugUnitTest` 通过 (30 测试); 无 material 导入 |
| 2 | 通过 | r1-entry.png |
| 3 | 通过 | r1-editor.png (Volume 内无玻璃, 入口窗保持); 系统底板在模拟器截图里不明显 |
| 4 | 代码通过 / 未测 | `EntryViewModel.requestOpen` 先关旧 tag; 需人工点第二个项目 |
| 5 | 通过 | `Main.kt` 约束 |
| 6 | 通过 (部分) | 工具栏顺序/尺寸见 `EditorToolbar.kt`; 模拟器截图中工具栏位于画面下缘之外, 仅在 Volume 被系统放低时露出 (r1-editor 早期一版可见) |
| 7 | 通过 | 默认色槽 + 白环见 r1-colour.png 槽行; 短按换槽需人工 |
| 8 | 通过 | walk 中添加/擦除显示 "单格 长方体", 上色/选择隐藏 |
| 9 | 代码通过 | `enabled = state.canUndo/canRedo` |
| 10 | 未测 | 单击需真实输入; 面推导有单元测试 |
| 11 | 通过 (D) | r1-ghost.png: 幽灵 + "✓ 已对齐 · 松手放置 / X0·Y0·Z0" |
| 12 | 通过 (D) | walk "occupied" 步: 珊瑚线框 + "格已占用", 方块数不变 |
| 13 | 通过 (D) | walk "erase" |
| 14 | 通过 (D) | walk "paint"; 同色 0 步有单元测试 |
| 15 | 通过 (D) | r1-selection.png "已选 5 块"; 再点取消需人工 |
| 16 | 通过 (D) | walk "box": "5 × 1 × 1 / 松手放置", 撤销 1 步 (单元测试) |
| 17 | 代码通过 | `BoxFill.erase` 单元测试; walk 未演示擦除盒 |
| 18 | 通过 | `Brush.FACE` 未在 UI 出现; 上色/选择无笔刷行 |
| 19 | 通过 | r1-selection.png |
| 20 | 通过 (D) | walk "rotate" + 单元测试 (中心不变) |
| 21 | 通过 (D) | walk "duplicate" + 单元测试 |
| 22 | 通过 (D) | walk "delete" |
| 23 | 通过 (D) | walk "stepx" + 单元测试 (越界不动) |
| 24 | 代码通过 | `SelectionClear` |
| 25 | 通过 | r1-colour.png (滑杆与按钮在截图下缘之外, 代码尺寸见 `HslSliders.kt`) |
| 26 | 未测 | 需人工点色板; 事件路径 `SwatchPicked` 立即写槽 |
| 27 | 通过 | `Palette.kt` 与 §D.3 一致; `ColorMathTest` 校验每组 24 色 |
| 28 | 代码通过 / 未测 | 取色提示条 + `pickFromCube`; 需人工点方块 |
| 29 | 通过 | `ProjectJson` slots/activeSlot; 已在设备 JSON 中确认 |
| 30 | 未测 | 旋转/回正需真实拖拽; 回正事件在 walk "reset" 执行无异常 |
| 31 | 通过 (D) | walk undo ×3 / redo ×3, Augment 位置不变 |
| 32 | 通过 | 标题条 "已自动保存"; export 脚本关闭编辑器后入口列表显示 "庭院小亭 184 块 · 刚刚" (r1-entry-after-export.png); 重开需人工 |
| 33 | 通过 | `ProjectRepository.writeAtomic` + `Dispatchers.IO`; 失败路径 Snack + 珊瑚点 (未触发) |
| 34 | 未测 | 需人工点删除; 代码路径完整 |
| 35 | 通过 (D) | walk "export" 打开 Sheet; 文件名默认项目名 |
| 36 | 通过 | `/sdcard/Download/GridSpace/庭院小亭.vox` 1848 B: `VOX ` + 150, MAIN, SIZE 16 16 16, XYZI n=188, RGBA 1024; 样本 `docs/export/pavilion-walk.vox` |
| 37 | 通过 | `VoxWriterTest` |
| 38 | 通过 (部分) | 成功态 + Snack 见 r1-export.png; MagicaVoxel 人工抽查未做 |
| 39 | 通过 | `GridType`; 全库无 `FontWeight.Bold` |
| 40 | 通过 | 珊瑚: 删除按钮 / 占用线框 / 擦除线框 / 保存失败点 / 失败图标 / 保存失败 Snack 图标; 薄荷: 主按钮 / 幽灵 / 成功 / 序号 / 当前模式 / 保存成功点 |
| 41 | 替换 | SDK `ColorScheme` 无 `accent/onAccent` 角色 → 未改任何系统角色, Mint/OnMint 逐组件设置 (`ButtonDefaults.buttonColors`, `ToggleButtonDefaults.toggleButtonColors`); 无自绘 backgroundMaterial; 2D hover `spatialHoverEffect`, 3D `HoverEffectComponent` |
| 42 | 通过 | 色槽 56, 图标钮 56 (`IconButtonDefaults.iconButtonSize`), 按钮 64, 色板 64, thumbArea 56, 步进 56; 面板关闭钮 40 (规格值) |
| 43 | 通过 | 仅回正 300 ms / 新块 150 ms / 占用闪 120 ms; 面板淡入由系统 |
| 44 | 通过 | `docs/scripts/walk.sh` 全程无崩溃; `settings delete` 后恢复 |
| 45 | 通过 | 全部字符串在 values + values-zh |

## 3. API 替换 / 偏离规格

1. **目标标签**: 规格要求 `AttachmentPanelComponent` + `LookAtComponent` 挂在幽灵实体上。两者在 SDK 中存在, 但 Volume 内可读性无法在模拟器验证, 且内容更新需手动重设; 改为 `Augment`, 其 `anchor` 由视图把幽灵格中心 + (0.6, 0.6, 0) 格投影到 Volume 归一化坐标后每帧更新 (`VoxelSceneView.onAnchors`), alignment CenterLeft, offset +12 dp。宿主为 Augment 的玻璃卡片, 内容 15 sp Medium + 13 sp。
2. **accent / onAccent**: 6.1.9 `ColorScheme` 只有 fill/label/error/alert/passable/interaction/dividerLine 等 16 个角色, 没有 accent。未替换 `interaction` (会影响段控/滑杆), Mint/OnMint 按规格逐组件设置。
3. **色槽长按**: `IconButton` 无 onLongClick, 色槽用 `Box(56.dp) + spatialHoverEffect + combinedClickable` 自绘 48 dp 圆与白环。
4. **NumberField**: SDK `NumberField` 是带键盘的输入框, 步进用两个 56 dp `IconButton` + 56 dp 数值 `Text` (规格允许的自组方案)。
5. **OrbitCatcher**: 规格为包住模型区的 1200³ 盒; 射线从外侧先命中包围盒会挡住方块, 改为模型后方 (z = −700 dp) 的 1500×1500×10 dp 碰撞板; `targetEntity == null` 也按旋转处理。
6. **捏合移动的目标格**: 实现了 §L.1 第 4 步主路径 (设备位姿 → `convertRotation/convertPosition` → 模型局部 → 3D-DDA), 但只有当射线步进结果与 SDK 报告的命中实体一致时才信任; 否则退化为 "按下时目标格 + 拖拽像素位移换算格" (`CameraMath.offsetTarget`)。两条路径都无法在模拟器验证。
7. **单击面推导**: `tap.position` 假定为视图局部像素 (左上原点, y 向下); 推导点若离命中方块太远 (>1.5 格) 退化为顶面 (`TargetCellResolver.adjacentCell`)。
8. **语言**: v1 无语言设置, `Localization.kt` 固定使用 `values-zh` (`BuildFlags.FORCE_CHINESE = true`, 模拟器为 en-US); 英文资源在 `values` 供 v1.1 切换。
9. **Sheet/AlertDialog/SpatialPopup** 按 SDK 签名调用 (`title/leadingAction/bottom`, `title/content/buttons`, `popupPositionProvider/cornerRadius/defaultMinWidth`), 与规格一致。

## 4. 已知问题 / 风险

- **主线程 ANR (已修复)**: `MeshResource.createBox` 是阻塞式服务端往返; 第一版每个方块一个 mesh, 188 块即触发 "GridSpace isn't responding"。现在整个场景共用 1 个单位盒 mesh + 1 个单位碰撞形 (隐藏 keeper 实体保活; 若 SDK 释放则捕获 `IllegalArgumentException` 重建), 方块实体每帧最多创建 64 个。材质仍每实体一个 (`PhysicallyBasedMaterial.create`), 48³ 外壳 (~13.8k 实体) 的创建耗时未测。
- 模拟器上 ANR 系统对话框会跨 force-stop 残留并把后续 Volume 推远, 只能重启模拟器 (已记入 AGENTS.md)。
- 新建的 Augment / SpatialPopup 前 ~1 s 渲染为黑块 (模拟器), 截图需延迟 ≥1.5 s。
- 系统把 1600 dp Volume 放在视线中央, 底部 `Toolbar` 多数时候在截图可见区之下; 需 QA 在设备上确认 (代码路径与 ElementSpace 相同)。
- 捏合移动 / 单击面推导 / 双手缩放 / 长按 / 悬停 全部未在真实输入下验证 (§3.6–3.7)。
- 选区面板的 yN 使用选区中心投影, 但 Augment 的 z 分量语义 (前/后) 未验证, 目前固定 0.5。
- `walk` 脚本中 "复制" 成功依赖样例几何 (行放在 z=13 空地); 删除后 "取消" 为空操作。
- 底座 "系统底板" (VolumeBasePanel) 使用默认, 未显式配置。
- 保存失败路径 (Snack + 重试) 未触发过。

## 5. 截图

| 文件 | 内容 |
| --- | --- |
| `docs/screenshots/r1-entry.png` | 入口窗 (空态) |
| `docs/screenshots/r1-editor.png` | 编辑器 Volume + 样例 "庭院小亭" + 标题条 |
| `docs/screenshots/r1-ghost.png` | 添加 · 幽灵方块 + 目标标签 |
| `docs/screenshots/r1-colour.png` | 取色面板 (鲜艳页签) |
| `docs/screenshots/r1-selection.png` | 选区面板 "已选 5 块" + 白线框 |
| `docs/screenshots/r1-export.png` | 导出 Sheet 成功态 |
| `docs/screenshots/r1-entry-after-export.png` | export 脚本关闭编辑器后的入口列表 |
| `docs/screenshots/walk/r1-<step>.png` | walk 每一步: ghost occupied box erase paint selection rotate duplicate stepx delete picker vivid undo redo reset export done |
| `docs/export/pavilion-walk.vox` | 从设备拉回的导出文件 |

## 6. 构建 / 测试输出 (尾部)

```
$ ./gradlew :app:assembleDebug :app:testDebugUnitTest --offline
> Task :app:compileDebugUnitTestKotlin
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 6s
42 actionable tasks: 4 executed, 38 up-to-date
tests 30 failures 0   (HistoryTest 4, BoxFillTest 5, SelectionOpsTest 7, TargetCellResolverTest 6, VoxWriterTest 4, ColorMathTest 3; JUnit 4)
material imports: 0; FontWeight.Bold: 0
```

## 7. QA 复现命令

```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
cd /c/Users/Admin/Downloads/GridSpace
./gradlew :app:assembleDebug :app:testDebugUnitTest --offline -q
adb install -r app/build/outputs/apk/debug/app-debug.apk

# r1-entry.png
adb shell settings delete global gridspace_demo
adb shell am force-stop com.kejing.gridspace; pico-cli app launch com.kejing.gridspace; sleep 10
adb exec-out screencap -p > docs/screenshots/r1-entry.png

# r1-editor.png (样例项目)
adb shell am force-stop com.kejing.gridspace; adb shell settings put global gridspace_demo sample
pico-cli app launch com.kejing.gridspace; sleep 20
adb exec-out screencap -p > docs/screenshots/r1-editor.png

# walk: 每步一张 (r1-ghost / r1-colour=picker / r1-selection / r1-export ...)
bash docs/scripts/walk.sh docs/screenshots/walk r1      # 同步 logcat 标签 GridSpaceDemo, 每步后 1.5 s 截图

# export 脚本 + 文件校验 (K.36)
adb shell am force-stop com.kejing.gridspace; adb shell settings put global gridspace_demo export
pico-cli app launch com.kejing.gridspace; sleep 25
MSYS_NO_PATHCONV=1 adb shell ls -la /sdcard/Download/GridSpace/
MSYS_NO_PATHCONV=1 adb pull "/sdcard/Download/GridSpace/庭院小亭.vox" docs/export/pavilion.vox
xxd docs/export/pavilion.vox | head          # 56 4f 58 20 | 96 00 00 00 | 4d 41 49 4e ...
adb exec-out screencap -p > docs/screenshots/r1-entry-after-export.png   # 编辑器已关闭, 列表含项目

# 项目 JSON (K.29 / K.33)
adb shell run-as com.kejing.gridspace cat files/projects/index.json
adb shell run-as com.kejing.gridspace ls files/projects/

# 结束
adb shell settings delete global gridspace_demo
# 若出现 "GridSpace isn't responding" 残留对话框: pico-cli emulator stop; pico-cli emulator start --avd Pico_Emulator_6_1_fresh
```
