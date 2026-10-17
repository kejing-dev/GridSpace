# GridSpace 格间 · 空间 UI 设计规格 (Design Spec) v1.0

| 项 | 值 |
| --- | --- |
| 平台 | PICO OS 6 · PICO Spatial SDK 6.1.9 · Kotlin + Jetpack Compose SpatialUI + ECS |
| 依据 | Figma V2 帧 `docs/design/V2-00..04`, `V1-08`; PICO 设计规范 (window / typesetting / button / toolbar / augment / popup / sheet / slider / segment); 参考工程 `ElementSpace` 的窗口模型 |
| 读者 | 研发按本文实现; QA 按 §K 验收 |
| 单位 | 2D 面板一律 dp / sp; 3D 内容一律 "格 (cell)" 与米, 换算见 §C.1 |

---

## A. 产品一句话 · v1 范围 · 非目标 (Product / Scope)

**一句话**: 一个漂浮在面前的体素 (方块) 编辑器: 看向格子, 捏合放块, 三个快捷色槽, 导出 .vox。

### A.1 v1 范围

| 能力 | v1 |
| --- | --- |
| 网格 | 单项目 N×N×N, N ∈ {16, 24, 32, 48}, 新建时选, 默认 16; 上限 48³ |
| 模式 | 添加 / 擦除 / 上色 / 选择 |
| 笔刷 | 单格 (全部 4 模式); 长方体 (仅 添加 / 擦除); 面 = v1.1 |
| 颜色 | 3 个快捷色槽 + 3 组预设 (柔和 / 鲜艳 / 大地, 各 24) + HSL 滑杆 + 从方块取色; 模型颜色数不限 (导出 ≤ 255) |
| 选区 | 点选切换; 旋转 90° / 复制 / 删除 / X Y Z 整格移动 / 取消 |
| 历史 | 撤销 / 重做, 100 步 |
| 相机 | 拖拽旋转 (yaw / pitch), 双手缩放, 回正 |
| 存储 | 每次提交后 1 s 防抖自动保存为 JSON (应用私有目录); 最近项目列表 |
| 导出 | .vox (MagicaVoxel) → Downloads/GridSpace/; PNG = v1.1 |
| 入口 | Planar 窗口: 新建空白项目 / 最近项目 / 首次使用 3 步提示 |
| 调试 | 全局设置 `gridspace_demo` 驱动脚本模式 (§L.4) |

### A.2 非目标 (v1 不做)

- 同时打开多个项目 (编辑器窗口同一时间只有 1 个)。
- 分组 / 图层 / 命名部件; 面笔刷; 镜像; 颜色数限制以外的调色板管理。
- 云同步 / 分享 / 账号。
- 缩略图生成 (列表用方块数代替, v1.1 加缩略图)。
- 自定义网格尺寸 (只允许 4 档)。
- 任何 Material / Material3 组件; 任何自绘边框或 `backgroundMaterial`。

---

## B. 窗口模型 (Window Model)

### B.1 窗口清单

| # | 窗口 | 容器 | 形态 | 默认尺寸 (dp) | 约束 (dp) | 玻璃 | 世界缩放 | 说明 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| W1 | 入口 `EntryWindow` | `DefaultWindowContainer` (Manifest 声明) | Planar | 1080 × 720 (深 640 固定) | min 960 × 640, max 1600 × 1000 | 系统 `materialbackground=1` | Dynamic | 启动即显示, 系统默认 1.75 m |
| W2 | 编辑器 `EditorVolume` | `WindowContainer(id = "EditorVolume", form = Form.Volumetric)` | Volumetric | 1600 × 1600 × 1600 | min 960³, max 2700³ | **无** (`enableMaterialBackground = false`), 系统底板保留 | Dynamic (同 ElementSpace; 注意 Volume 物理尺寸不随距离缩放) | tag = projectId; 打开新项目前先 `closeWindowContainer` 旧 tag |

- 窗口圆角由系统固定 32 dp, 不自绘。
- 新窗口位置: W2 由系统放在用户正前方; W1 保持原位。W2 打开后 W1 不关闭 (返回入口 = 关闭 W2)。
- 编辑器要求: 窗口始终竖直 (不自动倾斜), 内容主体朝向用户。

### B.2 窗口级附件 (全部为系统材质 MaterialRegular / MaterialThick, 不自绘背景)

| 附件 | 组件 | 宿主 | 位置 | 内容 |
| --- | --- | --- | --- | --- |
| 标题条 `TitleAugment` | `Augment` | W2 | anchor `NormalizedPoint3D(0.5, 0, 0.5)`, alignment BottomCenter, offset (0, −32 dp, 0) | 项目名 · 保存状态 · 导出 (§C.4) |
| 主工具栏 `EditorToolbar` | `Toolbar` (窗口级, 系统置于 Volume 底部居中) | W2 | 系统决定 | §C.2 |
| 笔刷行 `BrushAugment` | `Augment` | W2 | anchor `NormalizedPoint3D(0.5, 1, 0.5)`, alignment BottomCenter, offset (0, −24 dp, 0) → 贴在 Volume 底边内侧, 位于系统 Toolbar 上方 | `SegmentControl` 单格 / 长方体 (§C.3) |
| 选区面板 `SelectionAugment` | `Augment` | W2 | anchor `NormalizedPoint3D(0, yN, 0.5)`, alignment CenterRight, offset (−32 dp, 0, 0); yN = 选区中心在 Volume 内的归一化高度, 夹在 0.15..0.85 | §E.4 |
| 取色面板 `ColorPickerPopup` | `SpatialPopup` | 锚点 = 工具栏当前色槽所在 `Box` | `rememberSpatialPopupPositionProvider(horizontal = center, vertical = above(offset = 16.dp))`; `disableMaterialBackground = false`; `cornerRadius = 24.dp`; `defaultMinWidth = 700.dp` | §D |
| 导出表单 `ExportSheet` | `Sheet` | W2 | 系统: 窗口中心, 前移 32 dp, 父窗变暗 | §H |
| 删除项目确认 | `AlertDialog` | W1 | 系统 | §G.3 |
| 提示 | `Snack` (`LocalSnackbarHostState`) | W1 / W2 | 系统: 窗口底部上方 32 dp | §F |
| 目标标签 `TargetLabel` | `AttachmentPanelComponent` 挂在幽灵方块实体上, 加 `LookAtComponent` 朝向用户 | W2 (3D 内) | 幽灵方块右上角, 偏移 (+0.6, +0.6, 0) 格 | §F.1 |

名词: **Toolbar** = SDK 提供的窗口级底部工具栏容器; **Augment** = 系统放在窗口外沿、自动面向用户的玻璃卡片; **SpatialPopup** = 相对某个 2D 锚点弹出的浮层 (点外关闭); **Sheet** = 模态表单, 父窗变暗, 自带关闭按钮。

### B.3 玻璃与非玻璃

| 有系统玻璃 | 无玻璃 |
| --- | --- |
| W1 窗体, Toolbar, 全部 Augment, SpatialPopup, Sheet, Snack, TargetLabel (面板自身) | W2 Volume 内部 (模型、底座网格、幽灵方块、选区外框) |

---

## C. 编辑器布局 (Editor Layout)

### C.1 3D 内容 (W2 内部)

| 项 | 值 |
| --- | --- |
| 模型可用区 | Volume 中央 1200 × 1200 × 1200 dp (四周留 200 dp 安全边, 防裁切) |
| 格尺寸 | `cellDp = 1200 / N` (N=16 → 75 dp; 48 → 25 dp); 米值用 `LocalPhysicalLengthConverter.dpToLength` |
| 模型原点 | 网格中心 (N/2, 0, N/2) 置于可用区中心偏下: 底座中心在 Volume 中心下方 0.3 × 1200 dp = 360 dp, 使模型"向上生长" |
| 底座 `BasePlate` | 薄盒实体 N × 0.1 × N 格, 顶面 y = 0 (即 y = −0.05 格中心, 厚 0.1 格); 颜色 #2D383B @ 70%, 顶面画 1 格间距网格线 #688681 @ 40%, 线宽 ≈ 1.5 dp |
| 方块实体 `VoxelEntity` | 每格一个 `BoxEntity(cell)` 子实体, `CollisionComponent(box cell³)` + `InteractableComponent` + `HoverEffectComponent`; 不透明 PBR, 无金属度, 粗糙 0.9 |
| 幽灵方块 `GhostVoxel` | 1 个复用实体, `OpacityControllerComponent` 0.45, 薄荷 #A5EDCC (占用时切珊瑚 #F3BC9C 线框, 见 §F) |
| 选区外框 `SelectionOutline` | 每个选中格 1 个白色线框 (1 格 + 0.04 格外扩), 线宽 2 dp 等效; 或单一包围盒外框 (研发二选一, 验收按"可辨认每个被选格") |
| 轨道捕获 `OrbitCatcher` | 不可见实体, box 碰撞体 1200³ dp, 位于模型区; 用于"拖空白处旋转" (§E.5) |
| 初始相机 | yaw = 0.60 rad, pitch = 0.40 rad (接近 Figma 等轴), zoom = 1.0 |

### C.2 主工具栏 `EditorToolbar` (Toolbar 内 `Row`, 左→右)

| 槽位 | 组件 | 尺寸 (dp) | 内容 / 状态 |
| --- | --- | --- | --- |
| 内边距 | — | 左右 16, 上下 12 | 总高 88 |
| M1..M4 模式 | `ToggleButton` × 4 | 高 64, 最小宽 120, 圆角 24; 内容: 图标 24 + 间 8 + 文字 18 sp Medium; 内边距 左右 20 | 添加 / 擦除 / 上色 / 选择; 单选; 选中: 容器薄荷 #A5EDCC, 内容 #1C2A26; 未选: 透明容器 (`containerColor = Transparent`), 内容 labelPrimary; 悬停 = 系统 hover; 禁用 = 系统 alpha |
| 间距 | — | 8 | 模式之间 |
| D1 | `VerticalDivider` | 1 × 32, 左右各 12 | |
| S1..S3 色槽 | `IconButton` (自绘圆形色块作为内容) × 3 | 外 56 (热区), 内圆 48; 间距 12 | 填色 = 槽颜色; 当前槽: 2.5 dp 白色 #FFFFFF 外环 (环在 48 圆外侧, 外径 53); 非当前: 无环; 色为近白 (#F3F5ED 等) 时加 1 dp #2D383B @ 30% 内描边保证可见 |
| D2 | `VerticalDivider` | 1 × 32, 左右各 12 | |
| U / R | `IconButton` × 2 | 56, 图标 24, 间 8 | 撤销 / 重做; 无可撤销 / 重做时 disabled |
| D3 | `VerticalDivider` | 1 × 32, 左右各 12 | |
| V | `IconButton` | 56, 图标 24 | 回正视角 |

总宽 ≈ 1020 dp (4×120 + 3×8 + 3×(1+24) + 3×48 + 2×12 + 2×56 + 8 + 56 + 2×16)。工具栏 `cornerSize` 用默认 (系统)。

### C.3 笔刷行 `BrushAugment`

| 项 | 值 |
| --- | --- |
| 显示条件 | 模式 ∈ {添加, 擦除}; 上色 / 选择 时隐藏 (v1 它们只有单格) |
| 组件 | `SegmentControl`, 2 项 (v1): 单格 / 长方体; v1.1 加 面 |
| 尺寸 | 项 112 × 48, `itemSpace` 4, `contentPadding` 4 → 总 232 × 56; 文字 16 sp Medium; 图标 20 可选 |
| 默认 | 单格; 切换模式时保留上次笔刷 (按模式各记一份) |

### C.4 标题条 `TitleAugment`

| 项 | 值 |
| --- | --- |
| 尺寸 | 内容自适应, 高 64, 左右内边距 24, 圆角 32 (系统) |
| 内容 (左→右) | 项目名 `Text` 22 sp Medium labelPrimary (最长 12 字, 超出省略) · 间 16 · 保存状态 15 sp Regular labelSecondary + 8 dp 圆点 (已自动保存 = 薄荷点; 保存中 = 灰点; 保存失败 = 珊瑚点) · 间 16 · `Button` "导出" 高 48, leading 图标 20 (上传箭头), 文字 16 sp Medium |
| 点击项目名 | v1 不可编辑 (v1.1: 弹 `TextField`) |

### C.5 文本示意图 (正视)

```
                  ┌───────────────────────────────┐
                  │ 庭院小亭  ● 已自动保存  [↑ 导出] │   TitleAugment (顶边外 32 dp)
                  └───────────────────────────────┘
      ╔═══════════════════════════════════════════════════╗
      ║                 (200 dp 安全边)                    ║
      ║      ┌─────────────────────────────────┐          ║
      ║      │                                 │          ║  Volume 1600³
 ┌────────┐ │        模型 (N³ 格)              │          ║
 │已选 5 块 │ │        ▣▣▣  ◫ ← 幽灵方块 + 标签  │          ║
 │旋转 复制 │ │       ▣▣▣▣▣      [✓ 已对齐·松手放置]       ║
 │删除     │ │  ▒▒▒▒▒▒▒▒▒▒▒ 底座 N×N ▒▒▒▒▒▒▒▒   │          ║
 │X − 0 + │ └─────────────────────────────────┘          ║
 │Y − 0 + │           ┌────────────────┐                  ║
 │Z − 0 + │           │ [单格] [长方体] │  BrushAugment    ║
 └────────┘           └────────────────┘  (底边内 24 dp)  ║
 SelectionAugment ╚═══════════════════════════════════════╝
 (左边外 32 dp)   ┌──────────────────────────────────────────────┐
                  │ [+添加][◇擦除][◆上色][↖选择] │ ◯ ● ◯ │ ↶ ↷ │ ⌖ │  Toolbar (系统, 高 88)
                  └──────────────────────────────────────────────┘
```

---

## D. 取色面板 `ColorPickerPopup` (Colour Picker)

### D.1 打开 / 关闭 / 编辑对象

| 事件 | 行为 |
| --- | --- |
| 点击色槽 Si (短按) | Si 成为当前槽 (白环移动); 不打开面板 |
| 长按色槽 Si (≥ 500 ms) 或 点击当前槽 (已是当前槽再点一次) | 打开面板, `editingSlot = i`, 面板顶部 Si 显示白环 |
| 面板内点击另一个槽 Sj | `editingSlot = j`, 同时成为工具栏当前槽 |
| 点击色板 / 拖滑杆 / 取色完成 | 立即写入 `slots[editingSlot]` (无"应用"步骤), 工具栏色块同步 |
| 完成 / 点面板外 / 系统返回 | 关闭 (`onDismissRequest`); 颜色已生效; 不入撤销历史 |
| 切换模式或打开导出 | 面板关闭 |

### D.2 布局 (宽 700 dp, 内边距 32, 行间距 24, 玻璃 = SpatialPopup 系统材质, 圆角 24)

| 行 | 组件 | 尺寸 (dp) | 说明 |
| --- | --- | --- | --- |
| 1 标题 | `Text` "颜色" 22 sp Medium labelPrimary; 右侧 `IconButton` 关闭 40, 图标 20 | 高 40 | |
| 2 色槽 | 3 × 圆 80, 间 24; 当前槽 3 dp 白环 (外径 86) | 高 80 | 右侧说明两行 15 sp Regular labelSecondary: "点击切换当前色" / "长按编辑该槽" (面板内长按无额外含义, 文案照 Figma) |
| 3 预设页签 | `SegmentControl` 3 项: 柔和 / 鲜艳 / 大地; 每项 (636−8)/3 ≈ 209 × 48 | 636 × 56 | 记忆上次页签 |
| 4 色板 | 8 列 × 3 行, 每格 64 × 64 圆角 16, 列间 12, 行间 12 | 596 × 216, 水平居中 | 与当前颜色相等 (RGB 完全相同) 的格显示 2.5 dp 白环; 悬停 = 系统 hover (`spatialHoverEffect`) |
| 5 滑杆 ×3 | 每行: 标签 48 宽 15 sp Regular labelSecondary (色相 / 饱和 / 明度) + `Slider` 轨 560; `sliderSpec`: thumbSize 40, thumbPressedSize 44, thumbAreaSize 56, trackHeight 10; 行高 56, 行间 12 | 636 × 192 | 轨道底色: 色相 = 6 段彩虹渐变; 饱和 = 当前色相 0%→100%; 明度 = 黑→当前色→白; 用 `SliderDefaults` 的 colors 设 trackColor 透明, 渐变由下层 `Box` 绘制。范围: H 0..360, S 0..100, L 0..100 (v1 用普通 HSL; OKHSL 可选) |
| 6 按钮 | `Button` "从方块取色" (次级, 高 64, 宽 200, leading 图标 20 滴管) + 间 16 + `PrimaryButton` "完成" (高 64, 占余宽 420) | 高 64 | 文字 18 sp Medium |

总高 ≈ 32 + 40 + 24 + 80 + 24 + 56 + 24 + 216 + 24 + 192 + 24 + 64 + 32 = 832 dp。所有可点目标 ≥ 64 dp (滑杆拇指 40 但 thumbArea 56, 轨道整条可点)。

### D.3 预设色板 (每组 24, 行优先, 自左上)

**柔和 (Soft)**
`#f3bc9c #f7d3b5 #f5e3c3 #f3f5ed #dfddd0 #cfd6c2 #afd8bb #a5edcc`
`#8fd4c6 #9fd0e2 #b7c5ea #c9b8e8 #e0b4d6 #f0b3c0 #e5a6a0 #ce9278`
`#af755e #8a6a5a #7dad95 #568776 #3a5653 #688681 #aab9b8 #2d383b`

**鲜艳 (Vivid)**
`#ff3b30 #ff6b35 #ff9500 #ffcc00 #d4e157 #7ed321 #34c759 #00c7a0`
`#00bcd4 #2aa7ff #007aff #4f5bd5 #7c4dff #a855f7 #d946ef #ff2d92`
`#ff6b81 #ffa07a #ffd166 #8bd3dd #5e8bff #9b6bff #c8a2ff #ffffff`

**大地 (Earth)**
`#f5efe6 #e8dcc8 #d9c5a0 #c9ad7c #b8905a #a67643 #8c5a2b #6e4423`
`#4f3019 #c7b299 #a89a7b #8b8b6c #6b7351 #4f5a3a #8fa88a #5f7f6b`
`#3f5a4e #b56a4e #9c4f3a #d8a47f #e7c6a5 #7e6a5a #5a4a40 #3a3330`

默认色槽 (新项目): S1 #f3bc9c (浅珊瑚), S2 #a5edcc (薄荷), S3 #f3f5ed (石灰); 当前槽 S1。色槽随项目保存。

### D.4 从方块取色 (Eyedropper)

1. 点击 "从方块取色" → 面板收起为一条 56 dp 高的玻璃提示条 (同一 Popup 内容切换): "点一个方块取色" + 取消按钮 (48 高)。
2. 模型内方块进入取色悬停态 (HoverEffect 正常), 底座 / 空白不响应。
3. 点击方块 → 该色写入 `editingSlot`, 面板恢复完整态, 对应色板格若匹配则显示白环。
4. 取消 / 点外 → 恢复完整态, 颜色不变。
5. 取色期间工具栏模式按钮禁用 (避免误切换)。

---

## E. 交互流程 (Interaction per Mode)

### E.1 通用: 目标格的确定 (Target Cell)

- 输入统一经 `detectSpatialTapGesture` (单击) 与 `detectSpatialDragGesture` (捏合保持 / 拖拽), 两者放在不同 `pointerInput` 作用域。
- 命中实体 `targetEntity` ∈ {VoxelEntity, BasePlate, OrbitCatcher, null}。
- 格坐标 `cell(x, y, z)`, 0 ≤ x, y, z < N。
- 吸附规则: 任何目标点先转换为模型局部坐标 (§L.1), 再 `floor()` 到整格; 超出 0..N−1 → 无效目标 (幽灵不显示, 标签显示 "超出网格")。
- 添加模式目标 = 命中方块的相邻格 (按命中面) 或 底座上的格 (y = 0)。
- 擦除 / 上色 / 选择 目标 = 命中的方块本身; 底座与空白 → 无目标。

### E.2 单格笔刷 (Voxel)

| 模式 | 单击 (tap) | 捏合保持 (drag start → move → release) | 提交 | 历史 |
| --- | --- | --- | --- | --- |
| 添加 | 目标格为空 → 放入当前槽颜色方块 | 按下: 幽灵方块出现在目标格 + 标签 "已对齐 · 松手放置 / X Y Z"; 移动: 按 §L.1 射线每帧更新目标格; 松手: 目标为空格 → 提交 | 1 块 | 1 步 |
| 添加 (占用) | 目标格已有方块 → 不提交, 珊瑚线框闪 1 次 + 标签 "格已占用" | 同左, 松手不提交 | 0 | 0 |
| 擦除 | 命中方块 → 删除 | 按下: 命中方块显示珊瑚线框; 松手删除 (移动中改变目标则换目标) | 1 块 | 1 步 |
| 上色 | 命中方块 → 改为当前槽颜色 (颜色相同则无操作) | 同上, 预览: 方块临时显示新色 @ 70% | 1 块 | 1 步 (颜色相同 0 步) |
| 选择 | 命中方块 → 切换其选中态 (加入 / 移出选区) | 按下显示白框预览, 松手切换 | — | 0 (选区不入历史) |

### E.3 长方体笔刷 (Box) — 添加 / 擦除

1. 捏合按下: 起点格 A = 当前目标格 (添加: 相邻空格 / 底座格; 擦除: 命中方块)。
2. 保持并移动: 终点格 B 每帧按射线更新; 显示 A..B 包围盒预览: 添加 = 薄荷半透明 (0.35) 实体盒; 擦除 = 珊瑚线框盒。标签: "W × H × D" (如 "4 × 1 × 6") + 第二行 "松手放置" / "松手擦除"。
3. 松手: 添加 → 盒内所有空格填入当前色 (已占用格跳过, 不报错); 擦除 → 盒内所有方块删除。空集 (无变化) → 不入历史。
4. 单击 (无移动) = 1 × 1 × 1 盒, 等价单格。
5. 盒子被 0..N−1 裁剪。
6. 上限: 一次盒操作 ≤ 48 × 48 × 48; 超过 1 万格时标签追加 "较大, 稍候"。
7. 历史: 整盒 1 步。

### E.4 选择模式与选区面板 `SelectionAugment`

| 项 | 值 |
| --- | --- |
| 出现 | 选区非空时显示; 空时隐藏 (无动画位移, 淡入 150 ms) |
| 宽 | 320 dp; 内边距 24; 圆角 24 (系统) |
| 行 1 | `Text` "已选 N 块" 18 sp Medium labelPrimary; 右侧 `Button` 无背景 "取消" 高 40, 16 sp Medium labelSecondary |
| 行 2 (上距 16) | 3 × 竖排图文 `Button`: 88 × 72, 圆角 16, 图标 24 上 + 文字 15 sp Medium 下, 间 8: 旋转 90° / 复制 / 删除; 删除 的图标与文字用珊瑚 #F3BC9C, 容器仍为系统次级容器 |
| 行 3 (上距 20) | `Text` "网格移动" 15 sp Regular labelSecondary |
| 行 4..6 (上距 8, 行间 12) | 每行: 轴标 `Text` "X" / "Y" / "Z" 16 sp Medium 宽 24 + `NumberField` (或 自组 `IconButton` − 56 / `Text` 值 16 sp 宽 56 居中 / `IconButton` + 56), `stepLength = 1`, 显示本次会话累计位移 (取消 / 空选区时归零) |

操作语义 (全部围绕 **选区包围盒中心 c** 计算, c 取整到最近格中心):

| 操作 | 规则 | 冲突处理 | 历史 |
| --- | --- | --- | --- |
| 旋转 90° | 绕过 c 的 Y 轴逆时针 (俯视) 90°: (x, y, z) → (c.x + (z − c.z), y, c.z − (x − c.x)) | 目标格越界 → 整体不执行, Snack "旋转后超出网格"; 目标格被非选区方块占用 → 覆盖 | 1 步 |
| 复制 | 选区整体复制, 偏移 (+1, 0, 0)… 若 +1 列有任何选区外方块或越界, 改试 +W (W = 选区宽), 再失败则 Snack "没有空位" | 复制后 **新副本成为选区**, 原方块取消选中 | 1 步 |
| 删除 | 删除选区所有方块, 选区清空, 面板隐藏 | — | 1 步 |
| X / Y / Z ± | 选区整体平移 1 格 | 越界 → 不执行 + 按钮短暂禁用 100 ms; 被占用 → 覆盖 (被覆盖块进入该步历史) | 每次点击 1 步 |
| 取消 | 清空选区, 面板隐藏 | — | 0 |
| 切换到其他模式 | 选区保留但面板隐藏; 回到选择模式再显示 | — | — |
| 撤销 / 重做 | 执行后清空选区 | — | — |

### E.5 相机 (Camera)

| 手势 | 目标 | 行为 | 参数 |
| --- | --- | --- | --- |
| 捏合拖拽 / 手柄扳机拖拽 | `OrbitCatcher` (非方块、非底座) | yaw += dx × 0.006 rad/px, pitch = clamp(pitch + dy × 0.006, −0.2, 1.4) | 同 ElementSpace `RADIANS_PER_PIXEL` |
| 双手捏合缩放 `detectSpatialScaleGesture` | 任意 | zoom = clamp(zoom × scale, 0.5, 2.5) | 缩放中心 = 模型原点 |
| 回正 `IconButton` | — | 300 ms 缓动回到 yaw 0.60 / pitch 0.40 / zoom 1.0 | 唯一的"无用户持续操作"的运动, 由用户点击触发 |
| 在方块 / 底座上拖拽 | 由当前模式消费 (§E.2 / E.3) | 不旋转 | — |

- 相机状态不保存到项目, 不入历史。
- 模型旋转时所有 Augment / Toolbar 位置不变 (它们属于窗口, 不属于模型)。

### E.6 撤销 / 重做 (History)

- 命令栈: 每次 **提交** = 1 步 (单格、整盒、选区操作、每次步进); 上限 100 步, 超出丢最早。
- 色槽改动、模式 / 笔刷切换、选区变化、相机 → 不入历史。
- 撤销后进行新提交 → 重做栈清空。
- 工具栏 撤销 / 重做 按钮 `enabled` 跟随栈是否为空。
- 撤销 / 重做后: 清空选区, 触发自动保存。

### E.7 悬停 (Hover)

- 所有 `VoxelEntity` 带 `HoverEffectComponent` (系统高亮), 不自绘悬停色。
- 底座不带 HoverEffect (大面积高亮会干扰)。
- 取色流程中保持方块悬停; 导出 Sheet 打开时 Volume 内交互全部忽略 (模态)。

---

## F. 反馈与状态 (Feedback & States)

### F.1 目标标签 `TargetLabel` (挂在幽灵方块上)

| 项 | 值 |
| --- | --- |
| 面板 | `attachmentPanelComponent(size = panelSize(220.dp, 64.dp))`, 玻璃 (面板默认材质), 圆角 16, 内边距 12/16 |
| 内容 | 行 1: 图标 16 + 文字 15 sp Medium labelPrimary; 行 2: 13 sp Regular labelSecondary |
| 朝向 | `LookAtComponent` 朝向用户, 对齐世界上方向 |
| 位置 | 幽灵格中心 + (0.6, 0.6, 0) 格 (右上), 随幽灵移动 |
| 出现 | 仅在捏合保持期间 (drag 进行中) 显示; 单击不显示 (动作已完成); 松手后 200 ms 内淡出 |

### F.2 状态表

| 状态 | 视觉 | 文案 | 位置 | 时长 |
| --- | --- | --- | --- | --- |
| 添加 · 可放置 | 幽灵方块 薄荷 #A5EDCC @ 45% 实体 | "✓ 已对齐 · 松手放置" / "X7 · Y5 · Z0" | TargetLabel | 捏合期间 |
| 添加 · 格已占用 | 幽灵改为珊瑚 #F3BC9C 线框 (2 dp), 松手时闪 1 次 (120 ms) | "格已占用" / "换一个格子" | TargetLabel | 捏合期间 |
| 添加 · 超出网格 | 无幽灵 | "超出网格" / "N × N × N" | TargetLabel (挂在最后有效格) | 捏合期间 |
| 擦除 · 预览 | 命中方块 珊瑚线框 | "松手擦除" | TargetLabel | 捏合期间 |
| 上色 · 预览 | 方块临时新色 @ 70% | "松手上色" | TargetLabel | 捏合期间 |
| 长方体 · 预览 | 盒 (薄荷实体 / 珊瑚线框) | "4 × 1 × 6" / "松手放置" | TargetLabel 挂在盒角 B | 捏合期间 |
| 选择 · 已选 | 白色线框 | "已选 N 块" | SelectionAugment | 持续 |
| 手势跟踪丢失 (drag 取消 / 无 release) | 幽灵、预览盒、标签全部隐藏; 不提交 | 无 | — | 立即 |
| 提交成功 | 新方块 150 ms 从 0.8 → 1.0 缩放 (仅新块, 不移动相机) | 无 | 3D | 150 ms |
| 自动保存中 / 已保存 / 失败 | 标题条圆点 灰 / 薄荷 / 珊瑚 | "保存中" / "已自动保存" / "保存失败" | TitleAugment | 持续 |
| 保存失败 (首次) | `Snack` leadingIcon 珊瑚警示 | "保存失败, 请检查存储空间" + Action "重试" | W2 Snack | 直到操作 |
| 导出完成 | Sheet 内成功态 (§H) + `Snack` | "已导出到 下载/GridSpace/名称.vox" | W2 Snack | 4 s |
| 导出失败 | Sheet 内错误态 | "导出失败: 原因" | Sheet | 直到关闭 |
| 旋转越界 / 没有空位 | `Snack` | "旋转后超出网格" / "没有空位放副本" | W2 Snack | 3 s |
| 项目已删除 | `Snack` | "已删除 名称" | W1 Snack | 3 s |
| 颜色超过 255 种 (导出) | Sheet 内提示 | "颜色超过 255 种, 导出时将合并相近颜色" | Sheet | 持续 |

颜色纪律: 薄荷只用于 当前模式 / 主按钮 / 成功 / 可放置; 珊瑚只用于 错误 / 占用 / 擦除预览 / 删除; 二者不与用户方块颜色混淆 (用户色槽可自由选色, 但系统反馈元素永远是线框或半透明, 用户方块永远不透明)。

---

## G. 入口页 `EntryWindow` (Planar, 1080 × 720)

### G.1 布局

```
┌──────────────────────────────────────────────────────────────┐
│ TitleBar: "GridSpace 格间"                           [设置 v1.1]│  高 64
├────────────────────┬─────────────────────────────────────────┤
│ 左列 400           │ 右列 (余宽 ≈ 600)                        │  内边距 32, 列间 32
│ [ + 新建空白项目 ]  │ 最近项目                 (15 sp 次文字)  │
│  网格尺寸          │ ┌─────────────────────────────────────┐ │
│  [16][24][32][48]  │ │ ▣ 庭院小亭   128 块 · 2 小时前   [🗑] │ │ ListItem 72
│                    │ │ ▣ 小房子     64 块 · 昨天        [🗑] │ │
│ 开始之前           │ │ …                                   │ │
│ ① 看向方块         │ └─────────────────────────────────────┘ │
│ ② 捏合并移动       │ 空态: "还没有项目, 先新建一个"            │
│ ③ 松手放入格子     │                                         │
└────────────────────┴─────────────────────────────────────────┘
```

| 元素 | 组件 | 尺寸 / 样式 |
| --- | --- | --- |
| 标题栏 | `TitleBar`, title `Text` 22 sp Medium | 高 64 |
| 新建空白项目 | `PrimaryButton` (accent 薄荷, onAccent #1C2A26), leading 图标 24 "+" | 400 × 64, 18 sp Medium |
| 网格尺寸 | `Text` 15 sp Regular labelSecondary + `SegmentControl` 4 项 16 / 24 / 32 / 48, 每项 94 × 48 | 总 400 × 56; 默认 16; 记忆上次 |
| 首次使用 3 步 | 标题 `Text` "开始之前" 16 sp Medium; 3 行: 圆形序号 28 dp (薄荷底 #A5EDCC, 数字 14 sp Medium #1C2A26) + 文字 16 sp Regular labelPrimary; 行高 44 | 始终显示 (v1), 不可关闭; v1.1 加 "进入引导" 按钮与 Coachmark |
| 最近项目 | `Text` 15 sp Regular labelSecondary "最近项目" + 垂直列表 `ListItem` × ≤ 20 | 行 72: leading 40 × 40 圆角 8 色块 (项目主色 = 出现最多的颜色; 无方块时 #688681); 主文字 18 sp Medium 项目名; 次文字 15 sp Regular "128 块 · 2 小时前"; trailing `IconButton` 48 删除 (图标 labelSecondary, 悬停时珊瑚) |
| 空态 | `Text` 16 sp Regular labelSecondary, 列表区居中 | |

### G.2 流程

| 动作 | 结果 |
| --- | --- |
| 新建空白项目 | 创建 `{id: uuid, name: "未命名 k", gridSize: N, voxels: [], slots: 默认, createdAt, updatedAt}` → 立即保存 → 打开 W2 (tag = id) |
| 点击最近项目行 | 读取 JSON → 打开 W2; 若 W2 已开 (其他 tag) 先关闭 |
| 删除图标 | `AlertDialog` "删除 名称? 不可恢复" · 取消 / 删除 (Danger) → 删除文件 + 索引 → Snack |
| 返回入口 | 用户关闭 W2 (系统关闭钮) → 列表刷新 (`updatedAt` 排序) |

### G.3 自动保存与持久化

| 项 | 值 |
| --- | --- |
| 触发 | 每次提交 (§E.6 的"步") 后 1 s 防抖; 另在 W2 `onPause` / 关闭时立即保存 |
| 位置 | `filesDir/projects/{id}.json`; 索引 `filesDir/projects/index.json` (id, name, gridSize, voxelCount, dominantColor, updatedAt) |
| 格式 | `{"version":1,"id","name","gridSize","slots":["#rrggbb"×3],"activeSlot":0,"voxels":[[x,y,z,"#rrggbb"],…],"createdAt","updatedAt"}` |
| 写入 | 先写 `.tmp` 再原子重命名; 失败 → 标题条珊瑚点 + Snack (§F) |
| 读取失败 | 入口行显示 "文件损坏" 次文字, 点击提示 Snack, 不崩溃 |
| 上限 | 48³ = 110 592 块, JSON ≈ 3 MB, 可接受; 保存在 IO 线程 |

---

## H. 导出 `ExportSheet` (Export)

| 项 | 值 |
| --- | --- |
| 打开 | 标题条 "导出" 按钮 |
| 组件 | `Sheet`, title `Text` "导出" 22 sp Medium; leadingAction = 默认关闭按钮; 内容宽 560 |
| 行 1 格式 | `Text` "格式" 15 sp labelSecondary + `SegmentControl` 1 项 (v1): "VOX · MagicaVoxel" 宽 272 × 48; v1.1 加 "PNG · 快照" (Segment 不能禁用, 所以 v1 不显示 PNG 项); 下方说明 15 sp labelSecondary "可在 MagicaVoxel 等软件继续编辑" |
| 行 2 文件名 | `Text` "文件名" + `TextField` 高 56, 默认 = 项目名 (非法字符替换为 `_`), 后缀 ".vox" 固定显示为 trailing 文本 |
| 行 3 (条件) | 颜色 > 255 种 → 珊瑚图标 + 15 sp "颜色超过 255 种, 导出时将合并相近颜色" |
| bottom | `PrimaryButton` "导出到本机" 高 64, 宽 100%; 导出中 → 按钮内 `CircularProgress` 20 + "导出中…", 禁用 |
| 成功态 | 内容区替换为: 薄荷 ✓ 图标 48 + `Text` 18 sp Medium "已导出" + 15 sp labelSecondary "下载/GridSpace/名称.vox · 128 块 · 12 KB"; bottom 按钮变 "完成" (关闭 Sheet); 同时 W2 Snack |
| 失败态 | 珊瑚 ! 图标 + "导出失败" + 原因; bottom "重试" |
| 写入 | `MediaStore.Downloads`, `RELATIVE_PATH = Download/GridSpace`, MIME `application/octet-stream`; 同名 → 系统自动追加 (1) |

### H.1 VOX 文件规则 (QA 可用十六进制查看器验证)

| 偏移 | 内容 |
| --- | --- |
| 0 | ASCII `VOX ` (含空格) |
| 4 | int32 LE `150` |
| 8 | 块 `MAIN`, 内容 0, 子块总长 = 后续所有块 |
| 后续 | `SIZE` (12 字节: x = N, y = N, z = N; MagicaVoxel Z 向上, 映射 app(x, y_up, z) → vox(x, z, y_up)); `XYZI` (int32 n + n × 4 字节 x y z i, i = 调色板索引 1..255); `RGBA` (256 × 4 字节, 调色板索引 i 存在数组位置 i−1, 第 256 位填 0) |
| 调色板 | 模型中不同颜色按出现顺序分配索引 1..255; > 255 时按最近 RGB 距离合并到已分配颜色 |
| 空模型 | 允许导出 (XYZI n = 0) |

---

## I. 色彩与文字 (Colour & Type)

### I.1 色彩令牌

| 令牌 | 值 | 角色 | 用于 |
| --- | --- | --- | --- |
| `Mint` | #A5EDCC | 当前模式 / 主操作 / 成功 / 可放置; 设为 `PicoTheme.colorScheme.accent` | 选中的模式 ToggleButton, PrimaryButton, 幽灵方块, 保存成功点, 序号圆, Snack 容器 (系统随 accent) |
| `OnMint` | #1C2A26 | `onAccent` | accent 上的文字 / 图标 |
| `Coral` | #F3BC9C | 错误 / 删除 / 占用 / 擦除预览 | 删除按钮图标文字, 占用线框, 擦除预览线框, 保存失败点, 失败图标 |
| `SelectionWhite` | #FFFFFF | 选中高亮 | 选区线框, 当前色槽外环, 匹配色板环 |
| `TextPrimary` | #F3F5ED | = 使用 `labelPrimary` 系统角色 (值仅供 Figma 对照) | 标题、按钮、主文字 |
| `TextSecondary` | #AAB9B8 | = 使用 `labelSecondary` | 说明、次文字、轴标 |
| `BaseInk` | #2D383B | 底座 (3D) | 底座填充 @ 70% |
| `GridLine` | #688681 | 底座网格线 (3D) | @ 40% |
| 面板 / 工具栏 / 弹层背景 | 系统 MaterialRegular / MaterialThick | 不自绘 | 全部 2D 容器 |
| 禁用 | `LocalDisableAlpha` | 系统 | 撤销 / 重做 / 模式按钮禁用态 |

禁止: 大面积饱和色块 (预设色板格 64 dp 为上限); 自定义 hover 颜色; 替换 accent / onAccent 以外的任何系统颜色角色。

### I.2 文字

| 用途 | 大小 / 字重 | 行高 | 对应 PicoTheme |
| --- | --- | --- | --- |
| 窗口 / 面板标题 | 22 sp Medium | 28 | `titleLarge.copy(fontSize 22, Medium)` |
| 按钮 / 模式标签 | 18 sp Medium | 24 | `labelLarge.copy(18, Medium)` |
| 段控 / 正文 | 16 sp Regular (玻璃上若可读性不足, QA 可要求改 Medium) | 20 | `bodyLarge.copy(16, Normal)` |
| 说明 / 次文字 / 轴标 | 15 sp Regular | 20 | `bodyMedium.copy(15, Normal)` |
| 标签第二行 / 序号 | 13–14 sp Medium | 18 | `bodySmall.copy(13/14, Medium)` |
| 最小 | 12 sp, 仅非交互辅助 | — | — |

规则: CJK ≤ 17 sp 一律不用 Bold (Medium 或 Regular); 字体 = 系统 PICO 字体 (不捆绑 Noto, 与 ElementSpace 不同, 减小 APK); 单行按钮文字 `maxLines = 1`。

---

## J. 可达性与舒适度 (Accessibility & Comfort)

| 规则 | 值 |
| --- | --- |
| 可点目标 | ≥ 56 × 56 dp (工具栏色槽热区 56, 图标钮 56, 按钮 64, 色板 64, 滑杆拇指区 56, 步进钮 56) |
| 目标间距 | ≥ 10 dp (实际: 模式 8 + 按钮内边距 → 视觉间距 ≥ 10; 色槽 12; 色板 12; 步进钮间 8 + 值域 56) |
| 视野 | 默认距离下, 工具栏 + 标题条 + 选区面板 均落在 ±33.5° 水平 / ±20° 垂直内 (1600 dp Volume 在 1.75 m 约占 ±25° × ±25°; 选区面板在 Volume 左外 32 dp; 工具栏在底边下方) |
| 运动 | 仅用户触发: 回正 300 ms, 幽灵淡出 200 ms, 新块缩放 150 ms, 面板淡入 150 ms; 无自动旋转、无自动漂移 |
| 对比 | 文字用系统 label 角色 (自动混合); 3D 内只有薄荷 / 珊瑚 / 白 三种反馈色, 全部线框或半透明 |
| 饱和 | 不在暗环境放大面积饱和色; 色板格 ≤ 64 dp, 模型方块由用户决定 |
| 手势丢失 | 任何未完成的捏合 → 不提交, 不保留半状态 |
| 模态 | 只有 Sheet / AlertDialog 为模态; 取色 Popup 点外即关 |
| 单手 | 全部功能可单手 + 视线完成; 双手仅用于缩放 (可用回正替代) |
| 手柄 | 射线 + 扳机 与 眼手捏合 走同一套 spatial gesture, 不另做分支 |

---

## K. 验收清单 (Acceptance Checklist)

标注: [E] 模拟器点击可验; [C] 读代码可验; [D] 需 `gridspace_demo` 脚本 (§L.4)。

**构建 / 窗口**
1. [C] `./gradlew :app:assembleDebug` 与 `:app:testDebugUnitTest` 通过; 无 `androidx.compose.material*` 导入。
2. [E] 启动显示 Planar 入口窗 1080 × 720, 有系统玻璃, 标题 "GridSpace 格间"。
3. [E] 点击 "新建空白项目" 打开 Volumetric 编辑器 1600³, Volume 内部无玻璃, 系统底板可见; 入口窗保持打开。
4. [E] 打开第二个项目时, 第一个编辑器窗口被关闭 (同一时间只有 1 个 EditorVolume)。
5. [C] `WindowContainer` 约束 min 960³ / max 2700³; 入口 min 960 × 640 / max 1600 × 1000。

**工具栏 / 笔刷**
6. [E] 工具栏从左到右: 添加 / 擦除 / 上色 / 选择 (图标 + 文字) · 3 色槽 · 撤销 / 重做 · 回正; 模式按钮高 64, 文字 18 sp Medium; 当前模式容器薄荷 #A5EDCC。
7. [E] 新项目色槽为 #F3BC9C / #A5EDCC / #F3F5ED, 第 1 槽带 2.5 dp 白环; 短按另一槽, 白环移动。
8. [E] 添加 / 擦除 模式显示笔刷行 (单格 / 长方体); 上色 / 选择 时笔刷行隐藏。
9. [E] 撤销 / 重做 在栈空时禁用 (灰), 有步时可点。

**添加 / 擦除 / 上色 / 选择 (单格)**
10. [E/D] 添加: 点击底座格放入当前色方块; 点击已有方块的顶面, 新块出现在其上方。
11. [D] 添加: 捏合保持时出现薄荷半透明幽灵方块与标签 "已对齐 · 松手放置 / X Y Z"; 松手后方块落位, 幽灵与标签消失。
12. [D] 添加到已占用格: 不新增方块, 出现珊瑚线框并显示 "格已占用"; 历史步数不变。
13. [E/D] 擦除: 点击方块后方块消失, 为 1 步历史。
14. [E/D] 上色: 点击方块变为当前槽颜色; 颜色相同时不产生历史步。
15. [E/D] 选择: 点击方块出现白色线框并弹出选区面板 "已选 1 块"; 再点同块取消选中; 全部取消后面板隐藏。

**长方体笔刷**
16. [D] 添加 + 长方体: 从 A 拖到 B 显示薄荷盒预览与 "W × H × D"; 松手后盒内空格全部填充; 撤销一次全部消失 (1 步)。
17. [D] 擦除 + 长方体: 珊瑚线框盒预览; 松手删除盒内全部方块; 1 步。
18. [C] 上色 / 选择 的长方体与 面 笔刷在 v1 不可达 (代码中标注 v1.1), 不出现在 UI。

**选区面板**
19. [E/D] 面板位于 Volume 左侧外 32 dp, 宽 320, 含 旋转 90° / 复制 / 删除 (珊瑚) / X Y Z 步进 / 取消。
20. [D] 旋转 90°: 选 5 块一字排 (沿 X), 旋转后沿 Z 排列, 中心不变; 1 步历史; 撤销恢复。
21. [D] 复制: 副本出现在 +X 方向相邻; 副本成为新选区; 原块取消选中。
22. [D] 删除: 选区方块消失, 面板隐藏; 撤销恢复方块 (选区为空)。
23. [D] X+: 选区整体 +1 格; 越界时不动; 每次点击 1 步。
24. [E] 取消: 选区清空, 面板隐藏, 历史不变。

**取色面板**
25. [E] 长按色槽 (或再次点击当前槽) 在色槽上方弹出 700 dp 宽面板: 3 槽 (80) / 柔和 鲜艳 大地 段控 / 3 × 8 色板 (64, 间 12) / 色相 饱和 明度 滑杆 (拇指 40) / 从方块取色 (64 高) / 完成 (64 高, 薄荷)。
26. [E] 点击色板格后工具栏对应色槽立即变色; 点面板外关闭; 颜色保留。
27. [E] 三组预设各 24 色, 与 §D.3 十六进制一致 (读代码核对)。
28. [E/D] 从方块取色: 面板收起为提示条, 点击模型方块后该色进入当前编辑槽, 面板恢复。
29. [C] 色槽颜色随项目 JSON 保存与恢复。

**相机 / 历史 / 保存**
30. [D] 拖拽空白处模型旋转; 在方块上拖拽不旋转 (由当前模式消费); 回正 300 ms 内回到初始视角。
31. [D] 连续 3 次添加后撤销 3 次模型回空; 重做 3 次恢复; 其间 Augment / Toolbar 位置不变。
32. [E] 每次提交 1 s 后标题条显示 "已自动保存" (薄荷点); 关闭编辑器回到入口, 列表显示该项目名、方块数、相对时间; 重新打开内容一致。
33. [C] 保存走 `.tmp` + rename, 在 IO 线程; 失败路径弹 Snack 并显示珊瑚点。
34. [E] 入口删除项目: 出现 AlertDialog, 确认后列表移除并 Snack "已删除 名称"。

**导出**
35. [E] 标题条 "导出" 打开 Sheet (父窗变暗, 有关闭钮), 文件名默认为项目名, 底部 "导出到本机" 64 高。
36. [E] 导出后 `Download/GridSpace/名称.vox` 存在; 文件头 0..3 = `VOX `, 4..7 = int32 150; 含 `MAIN` `SIZE` `XYZI` `RGBA` 块; XYZI 的 n = 模型方块数; SIZE = N N N。
37. [C] 坐标映射 app(x, y_up, z) → vox(x, z, y_up); 调色板索引 1 起, RGBA 第 i 色在位置 i−1; > 255 色时最近色合并且 Sheet 有提示。
38. [E] 导出成功态显示路径 + Snack; 文件在 MagicaVoxel 0.99 可打开 (人工抽查 1 次)。

**文字 / 颜色 / 舒适**
39. [C] 全部 Text 使用 PicoTheme 排版拷贝: 标题 22 Medium, 按钮 18 Medium, 正文 16 Regular, 说明 15 Regular, 最小 12; 代码中无 `FontWeight.Bold` 用于 ≤ 17 sp。
40. [C] 珊瑚 #F3BC9C 只出现在: 删除按钮、占用线框、擦除预览、保存失败点、失败图标; 薄荷只出现在: accent、幽灵、成功、序号、当前模式。
41. [C] `PicoTheme(colorScheme = copy(accent = Mint, onAccent = OnMint))`, 其余颜色角色未改; 无自绘 `backgroundMaterial` / 边框; hover 用系统 `spatialHoverEffect` / `HoverEffectComponent`。
42. [C] 所有可点目标 ≥ 56 dp (检查 `IconButtonDefaults.iconButtonSize`, `ButtonDefaults` 自定义尺寸, 色板 64, 滑杆 `thumbAreaSize` 56)。
43. [C] 除 回正 / 淡入淡出 / 新块缩放 外无动画; 无自动旋转。
44. [D] `adb shell settings put global gridspace_demo walk` 后启动: 自动打开样例项目并依次演示四模式、取色面板、选区面板、导出; `settings delete global gridspace_demo` 后行为恢复正常。
45. [C] 所有用户可见字符串在 `res/values-zh/strings.xml` (默认 `values` 为英文或同为中文均可, 但不得硬编码在 Kotlin)。

---

## L. 已知限制 / 给研发的提示 (Known Limits & Dev Notes)

### L.1 没有表面法线: 目标格推导

SDK 的 `SpatialTapValue` 只有 `position: Offset3D` (视图局部像素) + `targetEntity`, 没有法线; `SpatialDragValue` 另有 `inputDevicePose.rawPosition / rawRotation` (设备原始位姿)。按下面顺序实现:

1. **实体即格**: 每个方块一个 `VoxelEntity`, 维护 `entity → cell` 与 `cell → entity` 两张表。擦除 / 上色 / 选择 直接查表, 不需要法线。
2. **添加 · 由命中点推面**: `p_px = tap.position` → `convertPxToMeter` → 减去 Volume 内 SpatialView 原点 → 乘模型根变换逆矩阵 (yaw / pitch / zoom / 平移) → 模型局部米 → 除以 cellMeters → 局部格坐标 `q`。命中方块中心 `c = cell + 0.5`; `d = q − c`; 取 `|d.x| |d.y| |d.z|` 最大分量的轴与符号 → 相邻格 = `cell + sign(d.axis) × unit(axis)`。误差来源: `position` 可能是射线与碰撞体 (AABB) 的交点, 正好在面上, 所以最大分量 ≈ ±0.5, 判定稳定。
3. **添加 · 命中底座**: `targetEntity == BasePlate` → 格 = `(floor(q.x), 0, floor(q.z))`。
4. **捏合保持中的移动 (预览)**: 每帧用 `inputDevicePose.rawPosition + rawRotation` 构造射线 (先 `content.convertRotation(..., ViewCoordinateSpace.Global, content.localSpatialCoordinateSpace)` 转到 SpatialView 坐标, 再转模型局部), 对网格做 3D-DDA 步进: 第一个被占用格 = 命中; 其前一个格 = 添加目标; 未命中方块但穿过 y = 0 平面 → 底座格。若 `rawRotation` 不可靠 (眼手模式下手的朝向 ≠ 视线), 退化为: 以 **按下时的目标格** 为基准, 用 `dragAmount` 在视平面上的位移 (换算为格) 平移目标格 (只在 x / z 或按下面所在平面内), 这也满足 "捏合并移动 / 松手放入"。研发先验证方案 4 主路径, 不可用则用退化路径, 并在代码注释中说明选择。
5. **单击直接提交**: tap 事件只有一个点, 直接按 2 / 3 计算并提交, 不走预览。
6. **底座实体**: 薄盒, 厚 0.1 格, 顶面 y = 0, 碰撞体同尺寸 (不要用 0 厚度, 否则射线可能穿透)。
7. **旋转捕获**: `OrbitCatcher` 盒碰撞体包住模型区; 若 SDK 命中测试不按"最近优先"而把 Catcher 排到方块前面, 改为: 拖拽回调中检查 `targetEntity` 是否为方块 / 底座, 是则交给模式逻辑, 否则旋转 (两者仍放在不同 `pointerInput`, 用 `TargetEntity.any()` 捕获后在回调分流)。
8. **手势消费**: tap 与 drag 必须在不同 `pointerInput` 作用域 (SDK 文档的消费规则); scale 再一个作用域。

### L.2 性能

- 48³ 最多 110 592 个实体不可接受; 实现: 只为 **可见方块** (至少一面暴露) 建实体, 内部方块跳过; 或用实例化网格 + 单一碰撞体数组。v1 验收上限: 16³ 全满 (4096) 60 fps; 32³ 外壳正常; 48³ 允许掉帧但不崩溃。
- 长方体填充 > 2000 格时分帧创建实体 (每帧 ≤ 500), 期间标签显示 "较大, 稍候"。
- 不要跨场景重建保留 `MeshResource` (ElementSpace 的真实崩溃教训): 每次重建新建 mesh。

### L.3 面板位置

- `Augment` 是 2D 卡片, 只能相对窗口锚定, 不能真正"贴在选区旁"; v1 用 §B.2 的左侧锚 + 随选区高度的 yN; 选区→面板的引导线 (Figma 虚线) 为 v1.1。
- `SpatialPopup` 的锚点 `Box` 内只放色槽按钮与 Popup, 不加其他子视图 (文档的 padding / 子视图对齐陷阱)。
- `Toolbar` 由系统定位, 不要再给它 offset; 笔刷行是 Augment, 若实机上与系统 Toolbar 重叠, 把 offset.y 改为 −96 dp。

### L.4 脚本 / 演示模式 `gridspace_demo`

模拟器不能注入空间手势, 必须提供脚本模式供 QA 截图:

| 设置值 | 行为 |
| --- | --- |
| `sample` | 启动后自动创建并打开样例项目 "庭院小亭" (16³, 约 128 块: 底板 10 × 1 × 10 石灰 #F3F5ED, 四根 1 × 4 × 1 珊瑚柱 #F3BC9C, 顶部 5 根 10 × 1 × 1 石灰横梁, 一个 3 × 2 × 3 薄荷块 #A5EDCC) |
| `walk` | `sample` + 每 4 s 推进一步: 添加 (放 3 块, 其中 1 次故意放到占用格以展示珊瑚反馈) → 擦除 (删 1 块) → 上色 (改 2 块) → 选择 (选 5 块, 显示面板, 旋转, 复制, X+1, 删除, 取消) → 长按色槽打开取色面板并切到 "鲜艳" → 关闭 → 撤销 × 3 → 重做 × 3 → 回正 → 打开导出 Sheet → 导出 → 完成 |
| `export` | `sample` + 立即导出到 Downloads 并退出编辑器 (用于验收 36 / 37) |

- 读取: `Settings.Global.getString(cr, "gridspace_demo")`, 在入口 `LaunchedEffect(Unit)` 与编辑器 `LaunchedEffect(projectLoaded)` 各读一次; 不存在时立即返回 (同 ElementSpace `DemoMode`)。
- 脚本通过 ViewModel 事件驱动 (与真实手势同一入口), 幽灵 / 标签 / 预览状态由事件 `PreviewStarted(cell)` / `PreviewMoved(cell)` / `Committed` 显式触发, 以便截图。
- 录完后 `settings delete global gridspace_demo`。

### L.5 其他

- 系统玻璃在明亮房间偏亮 (ElementSpace 实测); 3D 反馈色放在 Volume 内不受影响, 但 2D 文字必须用 label 角色, 不用自定义浅色。
- Volume 不随距离缩放: 1600 dp 是在臂长距离读作"桌面大小"的经验值, 不要改回 960。
- 项目名 v1 不可编辑, 自动命名 "未命名 1, 2, …"; 导出文件名可改。
- 未在真机验证: 帧率、眼手 / 手柄输入、`AttachmentPanelComponent` 在 Volume 内的实际可读性 (若标签过小, 把面板尺寸改为 300 × 88 并在真机复核)。
