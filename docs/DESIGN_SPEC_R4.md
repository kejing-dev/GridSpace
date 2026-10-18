# GridSpace 格间 · 空间 UI 设计规格 R4 (增量, 基于 v1.0)

| 项 | 值 |
| --- | --- |
| 基线 | `docs/DESIGN_SPEC.md` v1.0 (§A–L) + `docs/DEV_REPORT_R3.md` + `docs/QA_REPORT_R2.md` |
| 合同 | `docs/R4_BRIEF.md` §3 目标 1–6, §4 待定方案 |
| 性质 | **增量规格**: 只写相对 v1.0 的变化, 每条标注覆盖的 v1.0 章节; 未提到的条目按 v1.0 执行 |
| 依据 | PICO 6.1 设计文档 (augment / button / icon-button / segmented-controls / slider / typesetting / unit / window / motion-based-interaction / external-devices / switch-interaction-modes / pico-design-guidelines / sheet); R3 截图 `docs/screenshots/r3/`; Figma V2-01..04 |
| 单位 | 2D 一律 dp / sp; 3D 一律 格 (cell); 换算见 v1.0 §C.1 |

变更总览 (本文各节 → 覆盖的 v1.0 章节):

| 本文 | 覆盖 | 一句话 |
| --- | --- | --- |
| §M 尺寸体系 | §C.2 C.3 C.4, §D.2, §E.4 尺寸, §F.1, §G.1, §H, §I.2, §J 可点目标 | 全部控件放大: 可点 ≥ 72, 主按钮 ≥ 80 高, 字号 28/20/18/16/14 |
| §N 空间排布 | §B.1 B.2, §C.5, §D.1, §G.2 | 右侧常驻颜色栏; 取色不再长按; 标题条加 "项目" 与 中/EN; 入口窗方案 A / B |
| §O 吸附与直接操控 | §C.1, §E.2 选择行, §E.4, §F.2 | 底座落点高亮; 选择模式拖动已选方块 = 整体按格移动 |
| §P 多模态输入 | §E.1, §E.7, §J 手柄行 | 每个可交互元素 × 5 种输入的行为与反馈 |
| §Q 双语与字体 | §I.2 字体行, §K.45, AGENTS "Chinese-first" | 跟随系统 + 持久化切换; 捆绑 Noto Sans / Noto Sans SC |
| §K4 验收清单 | §K (追加, 不删除) | K4.1… 带 [E]/[C]/[D]/[H] 标签, 中英两套截图 |

---

## M. 尺寸体系 R4 (覆盖 v1.0 §I.2, §J "可点目标", 以及各节的尺寸列)

### M.1 文字角色 `GridType` (替换 v1.0 §I.2 整表; 文件 `ui/theme/GridTypography.kt`)

| 角色 (代码名) | 大小 / 字重 / 行高 | 派生自 | 用途 |
| --- | --- | --- | --- |
| `title` | 28 sp Medium / 36 | `titleLarge` | 窗口 / 面板 / Sheet 标题, 项目名, 取色编辑面板标题 |
| `label` | 20 sp Medium / 26 | `labelLarge` | 所有按钮与模式标签, 列表主文字, 目标标签第 1 行, "已选 N 块", 中/EN 段控项 |
| `body` | 18 sp Regular / 24 | `bodyLarge` | 正文, 步进值, 入口 3 步文字, 取色提示条, 文件名输入 |
| `bodyMedium` | 18 sp Medium / 24 | `bodyLarge` | 段控项 (笔刷 / 网格尺寸 / 格式), 轴标 X Y Z, "开始之前" |
| `caption` | 16 sp Regular / 22 | `bodyMedium` | 说明 / 次文字 / 保存状态 / 滑杆标签 / 目标标签第 2 行 / 列表次文字 |
| `captionMedium` | 16 sp Medium / 22 | `bodyMedium` | 选区操作瓦片文字, 预设页签项, 序号圆内数字, "正在编辑" |
| `small` | 14 sp Medium / 18 | `bodySmall` | **唯一的最小号**: 取色编辑面板里的十六进制色值 (`#A5EDCC`), 非交互 |

- 删除 v1.0 的 `smallMedium` (13 / 14 sp) 角色; 任何地方不再出现 < 14 sp。
- CJK ≤ 17 sp 不用 Bold 的规则 (v1.0 §I.2) 现在只覆盖 `caption` / `captionMedium` / `small` 三个角色, 它们本就是 Medium / Regular, 所以该规则在 R4 **自然满足**; 验收仍要求 `FontWeight.Bold` / `SemiBold` / `W600` / `W700` 在 `app/src` 出现次数 = 0 (K4.3)。
- 字体族随语言切换 (§Q), 字号 / 字重 / 行高与语言无关。

### M.2 可点目标通则 (替换 v1.0 §J 第 1、2 行)

| 规则 | R4 值 |
| --- | --- |
| 任何可点目标 (热区) | ≥ 72 × 72 dp (系统下限 56 只作兜底, 不作设计值) |
| 主按钮 (`PrimaryButton`, 唯一强调动作) | 高 80; 其它文字按钮高 72; 图标钮 72, 图标 28 |
| 目标间距 | ≥ 12 dp (模式钮间 12, 色槽间 12, 色板间 12, 步进钮间 12) |
| 色板格 | 88 × 88, 圆角 20 |
| 标签卡 | 440 × 120 |
| 段控项 | 高 64, 容器高 72 (`contentPadding` 4, `itemSpace` 4) |
| 滑杆 | `thumbAreaSize` 72, `thumbSize` 48, `thumbPressedSize` 52, `thumbHoverSize` 52, `trackHeight` 12 |
| 面板内边距 | 24 (卡片) / 32 (Popup / Sheet 内容) |
| 面板圆角 | 系统 (Augment / Toolbar / Popup / Sheet 自带), 不自绘 |

### M.3 控件尺寸映射表 (旧 → 新)

"旧" = R3 代码实测 (`docs/DEV_REPORT_R3.md`, `app/src/main/java/com/kejing/gridspace/ui/...`); "新" = R4 必达值。组件名为 SDK `com.pico.spatial.ui.design.*` 的 composable。

| 控件 | 文件 (R3) | 组件 | 旧 | 新 (R4) |
| --- | --- | --- | --- | --- |
| 模式钮 添加 / 擦除 / 上色 / 选择 | `EditorToolbar.kt` `ModeToggle` | `ToggleButton` | 高 64, 最小宽 120, 圆角 24, 图标 24, 文字 18, 内边距 20, 间距 8 | **高 80**, 最小宽 160, `shape = CircleShape` 胶囊 (符合 button 文档 "fully rounded"), 图标 28, 文字 `label` 20, 内边距 24, `gap` 10, 间距 12 |
| 色槽 S1..S3 | `EditorToolbar.kt` `ColorSlot` | `Box` + `clickable` (去掉 `combinedClickable`) | 工具栏内; 热区 56, 圆 48, 环 2.5 | **移到右侧颜色栏 (§N.3)**; 热区 88, 圆 72, 当前槽白环 3 dp (外径 78); 近白色 1 dp `BaseInk` @ 30% 内描边不变 |
| 撤销 / 重做 / 回正 | `Controls.kt` `ToolbarIcon` → `ActionIcon` | `IconButton` | 56, 图标 24, 间 8 | **72, 图标 28**, 间 12 |
| 工具栏分隔 | `EditorToolbar.kt` `ToolbarDivider` | `VerticalDivider` | 1 × 32, 左右 12 | 1 × 40, 左右 16 |
| 工具栏内边距 / 总高 | `Toolbar { }` 内容 | `Toolbar` | 16 / 12, 总高 88 | 左右 20, 上下 16, **总高 112**; 内容总宽 ≈ 4×160 + 3×12 + 2×(1+32) + 2×72 + 12 + 72 + 2×20 = **1010** (< 1600) |
| 笔刷段控 单格 / 长方体 | `BrushRow.kt` | `SegmentControl` / `SegmentItem` | 项 112 × 48, 总 232 × 56, 文字 16 | 项 **160 × 64**, 总 **328 × 72**, 文字 `bodyMedium` 18; 图标 24 可选 (与文字同在一项内, 不混用纯图标项) |
| 标题条 高 | `TitleBar.kt` `TitleAugmentContent` | `Row` in `Augment` | 高 64, 左右 24 | **高 96**, 左右 24; 内容固定宽 (§N.4) |
| 标题条 · 项目名 | 同上 | `Text` | 22 sp, 最大宽 300 | `title` 28 sp, **固定宽 360** (超出省略) |
| 标题条 · 保存状态 | 同上 `SaveStatus` | `Box` 点 + `Text` | 点 8, 文字 15, 固定宽 84 | 点 **10**, 文字 `caption` 16, **固定宽 120** |
| 标题条 · 导出 | 同上 | `SecondaryButton` | 高 48, 图标 20, 文字 16 | **高 72, 固定宽 160**, 图标 24, 文字 `label` 20 |
| 标题条 · 项目 (新) | 同上 | `SecondaryButton` | — | 高 72, 固定宽 160, leading 图标 24 (`ic_folder`), 文字 `label` 20 |
| 标题条 · 中/EN (新) | 同上 | `SegmentControl` 2 项 | — | 项 72 × 64, 总 **152 × 72**, 文字 `label` 20 ("中" / "EN") |
| 目标标签卡 | `TitleBar.kt` `TargetLabel` | `Column` in `Augment` | 300 × 88, 内边距 20/14, 行 1 图标 20 + 18 sp, 行 2 15 sp | **440 × 120**, 内边距 24/18, 行 1 图标 24 + `label` 20, 行 2 `caption` 16, 行间 6 |
| 选区面板 宽 / 内边距 | `SelectionPanel.kt` | `Column` in `Augment` | 328, 内边距 24 | **436**, 内边距 24 (内容宽 388) |
| 选区面板 · 行 1 | 同上 | `Text` + `Button` | "已选 N 块" 18; 取消 高 40, 16 sp | "已选 N 块" `label` 20; 取消 = 无背景 `Button` **高 72**, 最小宽 96, `label` 20 labelSecondary |
| 选区面板 · 操作瓦片 旋转 / 复制 / 删除 | 同上 `ActionTile` | `Button` 竖排图文 | 88 × 72, 圆角 16, 图标 24, 文字 15, 间 8 | **120 × 96**, 圆角 20, 图标 28, 文字 `captionMedium` 16, 图文间 8, 瓦片间 14 (3×120 + 2×14 = 388); 删除 仍 Coral 图标 + 文字 |
| 选区面板 · 步进行 | 同上 `StepperRow` | `Text` + `IconButton` ×2 + `Text` | 轴标 24 宽 16 sp; 钮 56; 值宽 56 16 sp; 间 8; 行间 12 | 轴标 **32 宽** `bodyMedium` 18; 钮 **72, 图标 28**; 值宽 **72** `body` 18 居中; 间 12; 行间 16 (行宽 32+12+72+12+72+12+72 = 284) |
| 选区面板 · 说明 | 同上 | `Text` | "网格移动" 15 | "网格移动" `caption` 16; 新增末行 `caption` 16 labelSecondary "拖动已选方块可整体移动" (§O.2) |
| 取色: 标题行 | `ColorPickerPopup.kt` (改名 `ColorRail.kt` + `ColorEditPopup.kt`) | — | "颜色" 22 + 关闭 40 | 颜色栏无标题 (常驻, 不需要关闭); 编辑面板标题 `title` 28 + 关闭 `IconButton` 72 / 图标 28 |
| 取色: 色槽行 | 同上 | `Box` + `ColorDisc` | 3 × 圆 80 热区 86, 间 24, 右侧两行 15 sp 提示 | 3 × 圆 **72 热区 88**, 间 12, **无提示文字** (长按已取消, 文案 `slot_hint_1/2` 删除) |
| 取色: 预设页签 | 同上 | `SegmentControl` 3 项 | 项 ≈ 209 × 48, 总 636 × 56, 文字 16 | 项 **124 × 64**, 总 **388 × 72**, 文字 `captionMedium` 16 (段控项长度相近: 柔和 / 鲜艳 / 大地, Soft / Vivid / Earth) |
| 取色: 色板 | 同上 `SwatchGrid` | `Box` 网格 | 8 列 × 3 行, 64, 圆角 16, 间 12, 环 2.5 | **4 列 × 6 行, 88**, 圆角 20, 间 12 (388 × 588), 当前色白环 **3 dp** |
| 取色: 滑杆 ×3 | `HslSliders.kt` | `Slider` | 标签 48 宽 15 sp, 轨 560, thumb 40/44/56, 轨高 10, 行高 56, 行间 12 | 标签 **72 宽** `caption` 16, 轨 **480**, `sliderSpec(thumbAreaSize 72, thumbSize 48, thumbPressedSize 52, thumbHoverSize 52, trackHeight 12)`, 行高 72, 行间 16; 渐变底仍由下层 `Box` 绘制 (v1.0 §D.2 行 5) |
| 取色: 按钮行 | 同上 | `SecondaryButton` + `PrimaryButton` | 从方块取色 64 × 200 + 完成 64 | 从方块取色 **72 高, 宽 220**, 图标 24; 完成 **80 高** 占余宽 |
| 取色: 取色提示条 | 同上 `EyedropperBar` | `Row` | 高 56, 取消 48 | 高 **88**, 文字 `body` 18, 取消 `SecondaryButton` 72 高 |
| 入口: 标题栏 | `EntryScreen.kt` | `TitleBar` | 标题 22 | 标题 `title` 28; trailing = 中/EN `SegmentControl` 152 × 72 (§N.5) |
| 入口: 新建空白项目 | `NewProjectColumn.kt` | `PrimaryButton` | 400 × 64, 图标 24, 18 sp | **440 × 80**, 图标 28, `label` 20 |
| 入口: 网格尺寸段控 | 同上 | `SegmentControl` 4 项 | 项 94 × 48, 总 400 × 56, 16 sp | 项 **104 × 64**, 总 **440 × 72**, `bodyMedium` 18 |
| 入口: 开始之前 3 步 | 同上 `StepRow` | `Box` 圆 + `Text` | 行 44, 圆 28 数字 14, 文字 16 | 行 **56**, 圆 **36** 数字 `captionMedium` 16, 文字 `body` 18, 圆文间 16 |
| 入口: 最近项目行 | `RecentProjects.kt` | `ListItem` | 最小高 72, 色块 40 圆角 8, 主 18 次 15, 删除 48 | 最小高 **88**, 色块 **56 圆角 12**, 主 `label` 20 次 `caption` 16, 删除 `IconButton` **72 图标 28** (悬停 Coral 不变) |
| 入口: 删除确认 | `DeleteDialog.kt` | `AlertDialog` | 按钮 56 | 标题 `title` 28, 正文 `body` 18, 按钮 **72** (删除 Coral) |
| 入口窗尺寸 | `AndroidManifest.xml` `defaultsize`, `Main.kt` `windowConstraints` | `DefaultWindowContainer` | 1080 × 720; min 960 × 640; max 1600 × 1000 | 方案 A: **1200 × 800**; min 1080 × 720; max 1600 × 1000。方案 B: **560 × 720**; min 560 × 640; max 800 × 1000 (§N.6) |
| 导出 Sheet 内容宽 | `ExportSheet.kt` | `Sheet` | 560 | **640** |
| 导出: 格式段控 | 同上 | `SegmentControl` 1 项 | 272 × 48 项 / 56 容器 | 项 **320 × 64** / 容器 **72**, `bodyMedium` 18; 说明 `caption` 16 |
| 导出: 文件名 | 同上 | `TextField` | 高 56 | 高 **72**, 文字 `body` 18, trailing ".vox" `body` 18 labelSecondary |
| 导出: 255 色提示 | 同上 | `Icon` + `Text` | 图标 20 + 15 sp | 图标 24 + `caption` 16 |
| 导出: 底部按钮 | 同上 | `PrimaryButton` | 64 | **80**, `label` 20; 导出中 `CircularProgressIndicator` 24 |
| 导出: 成功 / 失败态 | 同上 | `Icon` + `Text` | 图标 48, 18 / 15 sp | 图标 **56**, `label` 20 / `caption` 16 |
| 导出: 标题 / 关闭 | 同上 | `Sheet(title, leadingAction)` | 22 sp / 系统关闭钮 | `title` 28 / 系统默认关闭钮 (尺寸由 SDK 决定, 不改) |
| Snack / AlertDialog 容器 | — | 系统 | 系统 | 系统, 不改 |

高度 / 宽度校核 (全部 ≤ Volume 1600 边, 满足 Augment 文档 "横向不超主窗宽, 纵向不超主窗高"):

| 卡片 | 宽 × 高 (dp) | 计算 |
| --- | --- | --- |
| 标题条 `TitleAugment` | 1098 × 96 | 24 + 160 + 20 + 360 + 20 + (10+8+120) + 20 + 152 + 20 + 160 + 24 |
| 笔刷行 `BrushAugment` | 328 × 72 | 2 × 160 + 4 + 2 × 4 |
| 选区面板 `SelectionAugment` | 436 × 572 | 24 + 72 + 16 + 96 + 24 + 22 + 12 + 3×72 + 2×16 + 12 + 22 + 24 |
| 颜色栏 `ColorRailAugment` | 436 × 916 | 24 + 88 + 16 + 72 + 16 + (6×88 + 5×12 = 588) + 16 + 72 + 24 |
| 目标标签 `TargetLabelAugment` | 440 × 120 | 固定 |
| 取色编辑 `ColorEditPopup` | 640 × 624 | 32 + 72 + 24 + 88 + 24 + (3×72 + 2×16 = 248) + 24 + 80 + 32 |
| 工具栏 (系统) | ≈ 1010 × 112 | 见上表 |

---

## N. 空间排布 R4 (覆盖 v1.0 §B.1 第 "W1/W2 打开后" 行, §B.2 整表, §C.5, §D.1, §G.2 "返回入口" 行)

### N.1 原则 (来自 Augment / window 文档)

- 左 / 上 = 一级导航与信息, 下 = 主要操作, 右 = 辅助操作与信息 → 上 = 标题 / 项目 / 语言 / 导出; 左 = 选区 (有选区时); 右 = 颜色 (常驻); 下 = 系统 `Toolbar` (模式 / 撤销 / 重做 / 回正) + 笔刷行 Augment。
- 每个 Augment 与 Volume 边的距离 ≤ 32 dp; 侧面卡片用 z = 0.75 (靠近观察者的 1/4 深度, 与 R2 验证的笔刷行一致) 以减少被倾斜底座遮挡。
- 优先使用系统 `Toolbar`, 不用 Augment 自造工具栏 (保留)。
- 所有 Augment 内容 **固定尺寸** (AGENTS "Augment 内容尺寸变化会被系统重建"): 文字用固定宽 + `maxLines = 1` + 省略; 变化值以 `State` 传入。

### N.2 窗口级附件清单 (替换 v1.0 §B.2 整表)

| 附件 | 组件 | 宿主 | anchor `NormalizedPoint3D` | alignment | offset (dp) | 尺寸 | 内容 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 标题条 `TitleAugment` | `Augment` | W2 | (0.5, 0, 0.5) | `BottomCenter` | (0, −32, 0) | 1098 × 96 | §N.4 |
| 主工具栏 `EditorToolbar` | `Toolbar` (系统, Volume 底部居中) | W2 | 系统 | — | 无 (不加 offset) | ≈ 1010 × 112 | 模式 ×4 · 分隔 · 撤销 / 重做 · 分隔 · 回正 (**色槽移除**) |
| 笔刷行 `BrushAugment` | `Augment` | W2 | (0.5, 1, 0.75) | `BottomCenter` | (0, −96, 0) | 328 × 72 | 单格 / 长方体 (R2 验证的位置, 不改) |
| 选区面板 `SelectionAugment` | `Augment` | W2 | (0, yN, 0.75); yN = 选区中心归一化高度夹在 0.15..0.85 | `CenterRight` | (−32, 0, 0) | 436 × 572 | v1.0 §E.4 + §O.2 末行提示 |
| **颜色栏 `ColorRailAugment`** (新) | `Augment` | W2 | (1, 0.5, 0.75) | `CenterLeft` | (32, 0, 0) | 436 × 916 | §N.3 |
| 目标标签 `TargetLabelAugment` | `Augment` | W2 | R3 `LabelPlacement` 投影 (上 1.2 格, 侧 1.0 格, 前 0.8 格) | 右 `CenterLeft` / 左 `CenterRight` | (±12, 0, 0) | 440 × 120 | v1.0 §F + §O.2 Δ 标签 |
| **取色编辑 `ColorEditPopup`** (新, 替代 v1.0 `ColorPickerPopup`) | `SpatialPopup` | 锚点 = 颜色栏底部 "编辑颜色" 按钮所在 `Box` | `rememberSpatialPopupPositionProvider(HorizontalPlacement.center(), VerticalPlacement.above(16.dp))`; `cornerRadius = 24.dp`; `defaultMinWidth = 640.dp`; `disableMaterialBackground = false` | — | — | 640 × 624 (取色中收起为 640 × 88 提示条) | §N.3.3 |
| 导出表单 `ExportSheet` | `Sheet` | W2 | 系统 (中心, 前移 32, 父窗变暗) | — | — | 内容宽 640 | v1.0 §H + §M 尺寸 |
| **项目表单 `ProjectSheet`** (新, 仅方案 A) | `Sheet` | W2 | 系统 | — | — | 内容宽 720 | §N.6 A |
| 删除项目确认 | `AlertDialog` | W1 (方案 B) / W2 (方案 A, 从 ProjectSheet 触发) | 系统 | — | — | — | v1.0 §G.3 |
| 提示 `Snack` | `SnackbarHost` | W1 / W2 | 系统 | — | — | — | v1.0 §F |

摆位调试: 除 `gridspace_brush` 外新增 `gridspace_rail` 与 `gridspace_selpanel` 全局设置, 格式同 `DemoMode.brushPlacement` (`ax,ay,az,ox,oy,oz[,align]`), 读一次 / 编辑器合成, 用完删除。研发若发现 z = 0.75 的侧卡在某视点仍被遮挡, 允许回退到 z = 0.5 并在 DEV_REPORT 注明 (两者截图并列)。

### N.3 颜色栏 `ColorRailAugment` (替换 v1.0 §D.1 / §D.2; 文件 `ui/editor/components/ColorRail.kt`)

目标: 取色永远一步可达, **不需要长按**; 常驻在 Volume 右侧; 当前槽 = 编辑槽 (去掉 v1.0 的 `editingSlot` 与 `activeSlot` 分离, `PickerState.editingSlot` 恒等于 `project.activeSlot`)。

#### N.3.1 布局 (宽 436, 内边距 24, 内容宽 388, 行间 16; 玻璃 = Augment 系统材质)

| 行 | 组件 | 尺寸 (dp) | 说明 |
| --- | --- | --- | --- |
| 1 色槽 | 3 × `ColorSlot` (`Box` + `clickable` + `spatialHoverEffect`) | 热区 88, 圆 72, 间 12 → 288 × 88, 左对齐 | 当前槽 3 dp 白环 (外径 78); 点击 = 成为当前槽 (`SlotTapped(i)`); **没有长按** (`SlotLongPressed` 事件删除); 右侧余 100 dp 留空 |
| 2 预设页签 | `SegmentControl` 3 项 柔和 / 鲜艳 / 大地 | 项 124 × 64, 总 388 × 72 | 文字 `captionMedium` 16; 记忆上次页签 (`lastPreset`) |
| 3 色板 | 4 列 × 6 行 `Box` | 88 × 88, 圆角 20, 间 12 → 388 × 588 | 顺序 = v1.0 §D.3 的 24 色按行优先填入 (索引 `row * 4 + col`); 点击 → `SwatchPicked(color)` 立即写入 **当前槽**, 不入历史; 与当前槽颜色完全相同的格显示 3 dp 白环; 悬停 `spatialHoverEffect` |
| 4 编辑 | `SecondaryButton` "编辑颜色" / "Edit colour" | 388 × 72, leading 图标 24 (`ic_sliders`), 文字 `label` 20 | 点击 → `ColorEditOpened` 打开 `ColorEditPopup` (§N.3.3); 这个按钮所在的 `Box` 是 Popup 锚点, `Box` 内只放按钮与 Popup (v1.0 §L.3 陷阱) |

总高 24 + 88 + 16 + 72 + 16 + 588 + 16 + 72 + 24 = 916。

#### N.3.2 行为 (替换 v1.0 §D.1 表)

| 事件 | 行为 |
| --- | --- |
| 点击色槽 Si | Si 成为当前槽 (白环移动); 色板白环按 Si 颜色重算 |
| 点击色板格 | 立即写入当前槽; 工具栏无色槽, 模型下一次添加 / 上色即用新色 |
| 切换预设页签 | 只换色板, 不改任何槽 |
| 点击 "编辑颜色" | 打开 `ColorEditPopup`, 编辑当前槽 |
| 切换模式 / 打开导出 / 打开项目表单 | 颜色栏 **保持显示** (常驻); Popup 关闭 |
| 取色 (滴管) 进行中 | 颜色栏可见但色板 / 页签 / 槽禁用 (`LocalDisableAlpha`), 防止误触; 模式钮禁用同 v1.0 §D.4.5 |
| 选区面板显示时 | 颜色栏不变 (左右两侧各一) |

#### N.3.3 取色编辑面板 `ColorEditPopup` (替换 v1.0 §D.2 行 5–6; 文件 `ColorEditPopup.kt`)

宽 640, 内边距 32, 行间 24, 圆角 24, 系统材质。锚在颜色栏底部的 "编辑颜色" 按钮上方 16 dp, 水平居中 → 面板覆盖色板下半部, **色槽行 (栏顶 88) 保持可见**; Popup 在 Augment 前方由系统决定深度。

| 行 | 组件 | 尺寸 (dp) | 说明 |
| --- | --- | --- | --- |
| 1 标题 | `Text` "编辑颜色 · 槽 2" / "Edit colour · Slot 2" `title` 28 + 右侧 `IconButton` 关闭 72 / 图标 28 | 高 72 | 槽号 = 当前槽 |
| 2 预览 | `ColorDisc` 88 (当前槽色, 无环) + 间 16 + `Text` 十六进制 `small` 14 labelSecondary (固定宽 96) + 间 16 + `SecondaryButton` "从方块取色" / "Pick from cube" 72 高 宽 220 图标 24 | 高 88 | 十六进制随滑杆实时更新 |
| 3 滑杆 ×3 | 标签 72 宽 `caption` 16 (色相 / 饱和 / 明度; Hue / Sat. / Light) + `Slider` 轨 480 (`sliderSpec` 见 §M.2) | 每行 72, 行间 16 → 568 × 248 | 轨底渐变同 v1.0; 拖动 → `HslChanged` 实时写入当前槽 |
| 4 按钮 | `PrimaryButton` "完成" / "Done" 80 高, 宽 100% | 高 80 | 关闭 Popup |

取色 (滴管) 流程 = v1.0 §D.4, 差异: 提示条高 88 (文字 `body` 18 + 取消 72 高), 同一 Popup 内容切换; 取到的颜色写入当前槽, 面板恢复, 色板匹配环更新。关闭条件 = 完成 / 点外 / 系统返回 / 切换模式 / 打开 Sheet。

### N.4 标题条内容 (替换 v1.0 §C.4)

左 → 右, 高 96, 左右内边距 24, 元素间 20, 全部固定宽 (总 1098):

| 元素 | 组件 | 尺寸 | 行为 |
| --- | --- | --- | --- |
| 项目 | `SecondaryButton` leading `ic_folder` 24 + "项目" / "Projects" | 160 × 72 | 方案 A: 打开 `ProjectSheet` (§N.6 A); 方案 B: **不显示**, 标题条总宽固定为 1098 − 180 = 918 (去掉按钮与其 20 dp 间距, 不留空位) |
| 项目名 | `Text` `title` 28, `maxLines 1`, 省略 | 固定宽 360 | 不可编辑 (v1.1) |
| 保存状态 | 点 10 + 8 + `Text` `caption` 16 | 固定宽 138 | 灰 / 薄荷 / 珊瑚 同 v1.0 §F.2 |
| 中/EN | `SegmentControl` 2 项 "中" / "EN" | 152 × 72 | 选中项 = 当前语言; 点击另一项 → `LanguageSelected` (§Q.1), 立即切换两窗文字与字体 |
| 导出 | `SecondaryButton` leading `ic_upload` 24 + "导出" / "Export" | 160 × 72 | 打开 `ExportSheet` |

### N.5 入口窗标题栏 (覆盖 v1.0 §G.1 "标题栏" 行)

`TitleBar(title = { Text(entry_title, GridType.title) }, trailing = { 中/EN SegmentControl 152 × 72 })`。`entry_title`: zh "格间 GridSpace", en "GridSpace" (英文界面不混排 CJK, 避免字体回退; v1.0 K.2 的 "GridSpace 格间" 以此替代)。语言切换行为同 §N.4。

### N.6 入口窗与 Volume 重叠 (覆盖 v1.0 §B.1 "W2 打开后 W1 不关闭", §G.2 "返回入口"; 对应简报 §4)

研发先跑探针: 在 `DefaultWindowContainer` 内 `closeWindowContainer("GridSpaceMainWindow")` 后, 从 W2 调用 `openWindowContainer("GridSpaceMainWindow", tag = null, bundle = Bundle())` 能否重开入口窗 (记录 logcat `FWKWrapper, onWindowCreated`)。**能 → 方案 A; 不能 → 方案 B**。两者都是完整子规格, 只实现一个, 在 `DEV_REPORT_R4.md` 写明探针结果。

#### N.6.A 方案 A (首选): 编辑时关闭入口窗, 标题条 "项目" Sheet

| 项 | 值 |
| --- | --- |
| 入口窗 | 1200 × 800 (§M.3); 内容同 v1.0 §G.1 两列 (左列 440, 列间 32, 右列余宽 ≈ 664), 尺寸按 §M.3 |
| 打开编辑器 | `EntryViewModel.requestOpen` → `openWindowContainer(EDITOR_VOLUME, tag)` **成功后** `closeWindowContainer("GridSpaceMainWindow")` (先开后关, 避免 App 无窗口被系统回收) |
| 标题条 "项目" | 打开 `ProjectSheet` (下表) |
| 切换项目 (Sheet 内点击另一项目) | 当前编辑器: `Closed` 事件立即保存 → `openWindowContainer(EDITOR_VOLUME, 新 tag)` → `closeWindowContainer(EDITOR_VOLUME, 旧 tag)` (同样先开后关); `EditorSession` 记录新 id |
| 新建项目 (Sheet 内) | 创建 "未命名 k" (网格尺寸取 Sheet 段控值) → 同 "切换项目" |
| 关闭编辑器 (系统关闭钮 / 导出脚本) | `EditorScreen` `onDispose`: `AppGraph.editorSession.closed(id)`; 若 session 为空 → 用合成期捕获的 `navigator` 调 `openWindowContainer("GridSpaceMainWindow")`; 入口列表按 `updatedAt` 刷新 (v1.0 §G.2) |
| 删除项目 | 只能在入口窗 (无编辑器时) 或 ProjectSheet 的非当前项目上; 当前项目行无删除钮 |

`ProjectSheet` (`Sheet`, title "项目" / "Projects" `title` 28, leadingAction 系统关闭钮, 内容宽 720, 行间 16, 最大高 = 1600 − 64 (文档), 超出滚动, 标题与底部固定):

| 行 | 组件 | 尺寸 | 说明 |
| --- | --- | --- | --- |
| 1 | `PrimaryButton` "+ 新建空白项目" / "+ New blank project" | 720 × 80, 图标 28 | 创建并切换 |
| 2 | `Text` "网格尺寸" `caption` 16 + `SegmentControl` 4 项 16 / 24 / 32 / 48 | 项 172 × 64, 总 720 × 72 | 默认 = 入口记忆值 |
| 3 | `Text` "最近项目" `caption` 16 labelSecondary | — | 上距 24 |
| 4.. | `ListItem` × ≤ 20 | 最小高 88, 行间 4; leading 色块 56 圆角 12; 主文字 `label` 20; 次文字 `caption` 16 "128 块 · 2 小时前"; trailing: 当前项目 → `Text` "正在编辑" / "Editing" `captionMedium` 16 Mint 文字 (固定宽 96); 其它 → `IconButton` 删除 72 / 图标 28 | 6 行可见 (6×88 + 5×4 = 548), 多于 6 行滚动 | 点击非当前行 → 切换; 删除 → `AlertDialog` (W2) |
| 空态 | `Text` "还没有其它项目" / "No other projects yet" `body` 18 labelSecondary | 行高 88 居中 | — |

内容高 (6 行) = 80 + 16 + 72 + 24 + 22 + 12 + 548 = 774 (+ Sheet 自身标题 / 内边距), < 1536。

#### N.6.B 方案 B (兜底): 入口窗缩为 560 × 720 "项目抽屉"

| 项 | 值 |
| --- | --- |
| 入口窗 | Manifest `defaultsize` 560x720; `windowConstraints(min 560 × 640, max 800 × 1000)`; 标题 `TitleBar` 同 §N.5 |
| 状态 `Idle` (无编辑器) | 单列, 内边距 24, 内容宽 512, 行间 12: `PrimaryButton` 新建 512 × 80 → 网格尺寸 `SegmentControl` 4 项 122 × 64 (总 512 × 72) → "开始之前" 3 步 (行 56) → "最近项目" `caption` → 列表行 88 (剩余高 ≈ 270, 约 3 行, 可滚动) |
| 状态 `Editing(name)` (编辑器已开) | 顶部块 (高 128, 内边距 24): `Text` "正在编辑" / "Editing" `caption` 16 labelSecondary + `Text` 项目名 `label` 20 (固定宽 320, 省略) + 右侧 `SecondaryButton` "返回" / "Back" 72 高 宽 120 (关闭 W2: `closeWindowContainer(EDITOR_VOLUME, tag)`); 下方 "最近项目" 列表 (点击另一项目 = 先开后关切换, 同 A); 3 步与新建按钮隐藏; 新建改为列表首行 `ListItem` "+ 新建空白项目" 88 高 |
| 位置 | 系统把 W1 与 W2 都放在正前方; 应用**无法**移动窗口, 入口窗内 `Editing` 态顶部加一行 `caption` 16 labelSecondary "可拖动标题栏把本窗移到一侧" / "Drag the caption bar to move this window aside" (固定宽 512, 1 行) |
| 标题条 "项目" 钮 | 不显示 (抽屉即项目列表); 标题条总宽 918 |
| 关闭编辑器 | `EditorSession.closed` → 入口回到 `Idle`, 列表刷新 |

### N.7 文本示意图 (正视, 替换 v1.0 §C.5; 方案 A, 选择模式有选区时)

```
                      ┌────────────────────────────────────────────────────────────────┐
                      │ [▤ 项目]   庭院小亭            ● 已自动保存    [中|EN]   [↑ 导出] │  TitleAugment 1098×96, 顶边外 32
                      └────────────────────────────────────────────────────────────────┘
 ┌──────────────────┐ ╔══════════════════════════════════════════════════════════════════╗ ┌──────────────────┐
 │ 已选 5 块     取消 │ ║                        (200 dp 安全边)                           ║ │ ◯   ●   ◯        │ 3 槽 88 (当前槽白环)
 │ ┌────┐┌────┐┌────┐│ ║        ┌──────────────────────────────────────────┐            ║ │ [柔和|鲜艳|大地]  │ 预设 388×72
 │ │旋转││复制││删除││ ║        │              模型 (N³ 格)                 │            ║ │ ▢  ▢  ▢  ▢       │
 │ └────┘└────┘└────┘│ ║        │     ▣▣▣▣▣ ← 已选 (白框, 变暗)             │            ║ │ ▢  ▢  ▢  ▢       │ 色板 4×6, 88
 │ 网格移动          │ ║        │          ◫◫◫◫◫ ← 移动预览 (薄荷幽灵)      │  ┌─────────────┐ ▢  ▢  ▢  ▢       │
 │ X [−]  2  [+]     │ ║        │          ░░░░░ ← 落点格高亮 (底座上)      │  │ΔX +2·ΔY 0·ΔZ −1│ ▢  ▢  ▢  ▢       │ TargetLabel 440×120
 │ Y [−]  0  [+]     │ ║        │ ▒▒▒▒▒▒▒▒▒▒▒ 底座 N×N ▒▒▒▒▒▒▒▒▒▒▒         │  │松手移动 5 块 │ ▢  ▢  ▢  ▢       │
 │ Z [−] −1  [+]     │ ║        └──────────────────────────────────────────┘  └─────────────┘ ▢  ▢  ▢  ▢       │
 │ 拖动已选方块可整体移动│ ║                       ┌──────────────────┐                      ║ │ [≡ 编辑颜色]      │ 388×72 (Popup 锚点)
 └──────────────────┘ ║                       │ [单格] [长方体]   │  BrushAugment 328×72 ║ └──────────────────┘
 SelectionAugment     ╚══════════════════════════════════════════════════════════════════╝ ColorRailAugment
 436×572, 左边外 32    ┌──────────────────────────────────────────────────────────────────┐ 436×916, 右边外 32
 anchor (0,yN,0.75)   │ [+ 添加] [◇ 擦除] [◆ 上色] [↖ 选择] │ ↶  ↷ │ ⌖ │  Toolbar (系统, ≈1010×112) │ anchor (1,0.5,0.75)
                      └──────────────────────────────────────────────────────────────────┘
```

(笔刷行只在 添加 / 擦除 模式显示; 图中为示意同时画出。)

---

## O. 吸附与直接操控 R4 (覆盖 v1.0 §C.1 "幽灵方块" 行追加, §E.2 "选择" 行, §E.4 操作语义表追加, §F.2 追加)

### O.1 底座落点格高亮 `FootprintHighlight` (新增 3D 实体, `VoxelScene.kt`)

| 项 | 值 |
| --- | --- |
| 实体 | 1 个复用的薄盒实例 (与幽灵同一单位盒 mesh 的缩放实例, 遵守 ANR 教训), 名 `Footprint`; 无碰撞体, 无 Interactable |
| 尺寸 / 位置 | 1.12 × 0.06 × 1.12 格; 中心 = (cell.x + 0.5, 0.03, cell.z + 0.5) 即贴在底座顶面 (y = 0) 上方 0.03 格, 不与底座 z-fighting; 四周各外扩 0.06 格使轮廓越出方块投影, **从上方看是一圈边, 从正前看有 0.06 格厚的亮边** |
| 颜色 | 添加 · 可放置: `Mint` @ 55% `UnlitMaterial`(TRANSPARENT); 添加 · 占用: `Coral` @ 55%; 长方体: 同色, 尺寸 = 盒 XZ 包围 + 0.06 外扩; 选区拖动预览: `SelectionWhite` @ 35%, 尺寸 = 目标格集合的 XZ 包围 |
| 竖向引导柱 `DropGuide` | 当目标格 y > 0 时另加 1 个薄盒实例 0.10 × y × 0.10 格, 从底座顶面到目标格底面, 中心 (cell.x + 0.5, y/2, cell.z + 0.5); 颜色与落点格同色 @ 30%; y = 0 时隐藏 (`HIDDEN_SCALE`) |
| 出现 | 与幽灵 / 盒预览 / 拖动预览同生同灭 (捏合保持期间, 松手后与标签同步 200 ms 淡出 = 直接隐藏, 不做透明动画); 单击不显示 |
| 超出网格 | 隐藏 (目标无效) |
| 线宽换算 | 0.06 格 = N=16 时 4.5 dp, N=48 时 1.5 dp; N ≥ 32 时外扩改为 `max(0.06 格, 3 dp 等效)` 保证可见 (同 `FEEDBACK_WIRE_DP` 的做法) |
| 颜色纪律 | 薄荷 = 可放置, 珊瑚 = 占用, 白 = 选区; 都是半透明, 不与用户方块混淆 (v1.0 §F 末段不变) |

### O.2 选择模式 · 拖动整体移动 `SelectionDragMove` (新增; 覆盖 v1.0 §E.2 "选择" 行的捏合保持列)

#### O.2.1 触发与分流 (`VoxelSceneView.kt` 的 `detectSpatialDragGesture` 回调内分流, 不新增 `pointerInput`)

| 条件 (drag start) | 路径 |
| --- | --- |
| `mode == SELECT` 且 `targetEntity` 对应的格 ∈ `selection` | **移动路径** (本节) → `SelectionDragStarted(cell)` |
| `mode == SELECT` 且命中方块 ∉ `selection` | v1.0 §E.2 选择行不变: 按下白框预览, 松手把该格**加入**选区 (`PreviewStarted(SelectCell)` … `Committed`) |
| `mode == SELECT` 且命中底座 / 空白 | 相机轨道 (v1.0 §E.5) |
| 其它模式 | v1.0 不变 |
| 单击 (`detectSpatialTapGesture`) | 任何情况下 v1.0 不变: 选择模式单击 = 切换该格选中态。**拖动零位移松手 ≠ 单击**: 不切换选中态, 不入历史 |

#### O.2.2 拖动量 → 格位移 (视平面映射; 位于 `CameraMath.kt`, 新函数 `dragToDelta`)

应用只能拿到 `dragAmount: Offset3D` (视图局部像素) 与设备位姿 (AGENTS "Tap positions, device-pose rays"); 移动**只用视平面位移**, 不用位姿射线 (射线给的是命中格, 不是位移, 且眼手模式下手的朝向 ≠ 视线)。

| 步骤 | 规则 |
| --- | --- |
| 1 轴选择 (按下时按当前 yaw / pitch 计算一次, 整个拖动不变) | `H1` = 屏幕 x 方向在模型空间的投影 `viewDirToLocal(1,0,0)` 的水平分量 (X 或 Z) 中绝对值最大的轴, 带符号 (已有 `dominantAxis`); `H2` = 另一条水平轴, 符号 = 屏幕 y 向上方向投影到该轴的符号 (屏幕向上 = 远离观察者) |
| 2 像素 → 格 | `cellPx = cellDp × density × zoom` (同 `ViewMapper.offsetTarget`); `u = dx / cellPx`, `v = −dy / cellPx` (屏幕 y 向下为正, 取反) |
| 3 死区 | `|u| < 0.5 且 |v| < 0.5` → Δ = (0,0,0), 不锁定平面 |
| 4 平面锁定 (第一次越过死区时判定一次, 之后不变) | `|v| ≥ 1.73 × |u|` (与水平夹角 ≥ 60°, "主要是竖直") → `LIFT`: u → H1, v → **Y**; 否则 → `GROUND`: u → H1, v → H2 |
| 5 步进 | `Δ(H1) = round(u)`, `Δ(第二轴) = round(v)` (round 半进位 = 0.5 格死区后每整格一步; 回拖按同一阈值, 不加额外滞回) |
| 6 越界夹取 | 以选区包围盒整体判断: 对每轴把 Δ 夹到使 `bbox.min + Δ ≥ 0` 且 `bbox.max + Δ ≤ N − 1` 的范围; 夹取后的 Δ 才用于预览与提交 |
| 7 变化通知 | Δ 变化时视图发 `SelectionDragMoved(delta: Cell)` (ViewModel 只收格位移, 不收像素, 便于单测与 demo 脚本) |

`detectSpatialDragGesture` 的 `dragAmount` 若是增量 (非累计), 视图自行累计 (R3 已有 `dragTotal`)。

#### O.2.3 预览 (`VoxelScene.kt`; 状态 `Preview.MoveSelection(delta, targets, overwrite)`)

| 元素 | 视觉 |
| --- | --- |
| 原位已选方块 | 保持白色线框 (2 dp 等效), 颜色与 `BaseInk` 按 50% 混合 (走与上色预览相同的 PBR 混色路径 `PAINT_PREVIEW_WEIGHT` 的机制, 权重 0.5) → "变暗" |
| 目标格幽灵 | 每个目标格 1 个薄荷 `Mint` @ 45% 实例 (复用幽灵的单位盒, 分帧 ≤ 64 个 / 帧) + 白色线框 2 dp; Δ = 0 时不显示幽灵 |
| 落点高亮 | `FootprintHighlight` 白 @ 35%, 尺寸 = 目标格 XZ 包围 (§O.1) |
| 将被覆盖的非选区方块 | `Coral` 线框 5 dp 等效 (`FEEDBACK_WIRE_DP`), 不填充; 数量 k 进入标签 |
| 标签 `TargetLabelAugment` | 锚点 = 目标格包围盒的 max 角 (`LabelPlacement` 规则不变)。行 1: `label_move_delta` = "ΔX +2 · ΔY 0 · ΔZ −1" (符号: 正数 "+", 零 "0", 负数 U+2212 "−"); 行 2: Δ = 0 → "拖动以整体移动" / "Drag to move all"; Δ ≠ 0, k = 0 → "松手移动 N 块" / "Release to move N cubes"; k > 0 → "松手移动 · 覆盖 k 块" / "Release to move · overwrites k" |
| 选区面板步进值 | 拖动中**实时**显示 `translation + Δ` (松手提交后即为新累计值; 取消则回退) |

#### O.2.4 提交 / 取消 / 冲突 (覆盖 v1.0 §E.4 操作语义表, 追加一行)

| 操作 | 规则 | 冲突处理 | 历史 |
| --- | --- | --- | --- |
| 拖动移动 (松手, Δ ≠ 0) | 选区整体平移 Δ (`SelectionOps.translate(selection, Δ)`); **新位置成为选区**; `translation += Δ` | 越界 → 已在 §O.2.2 步骤 6 夹取, 不会发生; 被非选区方块占用 → 覆盖 (被覆盖块进入该步历史, 同 X/Y/Z ±) | **1 步** (事件 `SelectionMoved(delta)`) |
| 拖动移动 (松手, Δ = 0) | 无变化, 预览消失 | — | 0 |
| 拖动取消 (跟踪丢失 / `PreviewCancelled`) | 无变化, 预览消失, 步进值回退 | — | 0 |
| 拖动期间点击面板步进 | 不可能同时 (单手); 若发生, 步进事件被忽略直到拖动结束 | — | — |
| 撤销 / 重做 | 同 v1.0 §E.6: 清空选区 | — | — |

### O.3 不变项 (明确不改)

- 旋转 90° / 复制 / 删除 / X Y Z ± 步进 / 取消: v1.0 §E.4 原样保留 (步进按钮是手柄 / 眼手不便拖动时的精确替代)。
- 相机: v1.0 §E.5 原样; 两手缩放不变。
- 添加模式幽灵吸附 (v1.0 §E.1 / L.1): 不改规则, 只追加 §O.1 的落点高亮。

---

## P. 多模态输入 R4 (覆盖 v1.0 §E.1 首条, §E.7, §J "手柄" 行)

### P.1 统一入口

PICO OS 6 在 眼手 (gaze+pinch) / 直接手势 (poke, directPinch; 手靠近对象时系统自动切换) / 手势射线 / 手柄射线 / 手柄近场 之间自动切换 (switch-interaction-modes 文档)。应用**不按输入源分支**:

- 3D: 全部走 `detectSpatialTapGesture` (单击) / `detectSpatialDragGesture` (捏合保持 / 扳机保持) / `detectSpatialScaleGesture` (双手) 三个 `pointerInput` 作用域 (v1.0 §L.1.8); 回调内不读事件源类型, 代码注释写明 "gaze+pinch / directPinch / poke / 手柄射线 / 手柄近场 走同一路径, 行为一致" (K4.20)。
- 2D: SpatialUI 组件自带 hover / press; 自绘可点容器 (`ColorSlot`, 色板格, 列表行) 必须加 `Modifier.spatialHoverEffect()` (默认样式) + `clickable`。
- 3D 悬停: 每个 `VoxelEntity` 带 `HoverEffectComponent` (需 `CollisionComponent` + `InteractableComponent`); 底座、幽灵、落点高亮、`OrbitCatcher` **不带**。

### P.2 元素 × 输入方式

缩写: G = 眼手 gaze+pinch; D = 直接捏合 directPinch (手靠近); K = 直接触碰 poke (指尖); R = 手柄射线 + 扳机; C = 手柄近场 (poke / 扳机)。"系统 hover" = `spatialHoverEffect` 或组件内置; "3D hover" = `HoverEffectComponent`。

| 元素 | G | D | K | R | C | 反馈 (悬停 / 按下) |
| --- | --- | --- | --- | --- | --- | --- |
| 模式 `ToggleButton` ×4 | 注视 + 捏合 = 选中 | 捏合 = 选中 | 触碰 = 选中 | 射线 + 扳机 | 触碰 / 扳机 | 内置 hover (容器变色) / press; 选中 = Mint 容器 |
| 撤销 / 重做 / 回正 `IconButton` | 同上 | 同上 | 同上 | 同上 | 同上 | 内置; 禁用 = 系统 alpha |
| 笔刷 `SegmentItem` ×2 | 同上 | 同上 | 同上 | 同上 | 同上 | 内置 (无禁用态) |
| 标题: 项目 / 导出 `Button`, 中/EN `SegmentItem` | 同上 | 同上 | 同上 | 同上 | 同上 | 内置 |
| 颜色栏: 色槽 `Box` | 捏合 = 成为当前槽 | 同 | 同 | 同 | 同 | `spatialHoverEffect` / 系统 press; 当前槽白环 |
| 颜色栏: 预设 `SegmentItem`, 色板 `Box`, 编辑颜色 `Button` | 捏合 = 选 / 选色 / 打开 | 同 | 同 | 同 | 同 | 内置 / `spatialHoverEffect`; 匹配格白环 |
| 取色编辑: 滑杆 `Slider` | 注视拇指 + 捏合保持拖动; 或注视轨道任意点 + 捏合跳到该值 | 捏住拇指拖 | 触碰轨道跳值; 触碰拖动 (poke 文档: 平面内拖动可行) | 射线 + 扳机保持拖动 | 同 D / K | 内置 (拇指 hover 52, press 52) |
| 取色编辑: 从方块取色 / 完成 / 关闭 | 同按钮 | 同 | 同 | 同 | 同 | 内置 |
| 选区面板: 旋转 / 复制 / 删除 瓦片, 步进 ±, 取消 | 同按钮 | 同 | 同 | 同 | 同 | 内置; 删除 Coral; 越界步进 100 ms 禁用 |
| 入口 / ProjectSheet: 新建 / 网格尺寸 / 列表行 / 删除 / 中/EN | 同按钮 | 同 | 同 | 同 | 同 | 列表行 `spatialHoverEffect`; 删除悬停 Coral |
| 导出 Sheet: 段控 / 文件名 `TextField` / 导出到本机 | 同按钮; `TextField` 捏合 = 聚焦, 系统键盘 | 同 | 同 | 同 | 同 | 内置 |
| 3D 方块 `VoxelEntity` 单击 | 注视方块 + 捏合 = 模式动作 (添加相邻格 / 擦除 / 上色 / 切换选中) | 手靠近方块捏合 = 同 | 指尖触碰方块表面 = 同 (系统把 poke 作 tap 送入 `detectSpatialTapGesture`) | 射线命中 + 扳机 = 同 | 手柄贴近 = 同 | 3D hover (系统高亮) |
| 3D 方块 捏合保持 | 注视起点 + 捏合保持 → 幽灵 / 线框 / 预览; 移动手 → `dragAmount` 视平面位移 (v1.0 §L.1.4 退化路径) 或位姿射线 (主路径, 若 `validates` 通过) | 捏住方块移动: 同一 `detectSpatialDragGesture`; 位姿射线此时来自手, 通常 `validates` 通过 → 按射线 | poke 不适合空间拖动 (motion-based 文档); 若系统仍发 drag, 走同一路径 | 射线拖动: 位姿 = 手柄, 主路径 | 同 D | 预览 + 标签 + 落点高亮 (§O.1) |
| 3D 已选方块 捏合保持 (选择模式) | §O.2 移动路径: 视平面位移 → Δ | 同 (direct manipulation: 手捏住方块拖, 视平面映射仍成立) | 同上 poke 备注 | 同 | 同 | 变暗 + 幽灵 + 白落点 + Δ 标签 |
| 底座 `BasePlate` 单击 / 保持 (添加) | 注视底座格 + 捏合 = 放块 / 幽灵 | 同 | 同 | 同 | 同 | 无 hover; 落点高亮 |
| 空白 `OrbitCatcher` 捏合保持 | 旋转相机 | 同 | 同 (若送 drag) | 同 | 同 | 无 |
| 双手捏合 / 双扳机 | `detectSpatialScaleGesture` 缩放 | 同 | — | 同 | 同 | 无 |

### P.3 模拟器无法验证 → 真机清单 [H]

模拟器不能注入任何空间手势 (AGENTS); 以上表全部只能代码审查 + 真机。真机必验 (写入 K4 的 [H] 项):

1. 眼手: 注视 72 dp 目标 + 捏合命中率 (模式钮、色槽、色板 88、步进 72、滑杆拇指 48 / 热区 72)。
2. 直接捏合: 手靠近颜色栏 / 选区面板时系统切到 direct, `spatialHoverEffect` 仍高亮; 捏住已选方块拖动 → Δ 预览 (§O.2)。
3. poke: 指尖点模式钮 / 色板 / 方块; 滑杆触碰跳值。
4. 手柄射线 + 扳机: 放块、拖动移动、滑杆拖动; 手柄近场 poke 模式钮。
5. 位姿射线主路径 (`ViewMapper.rayHit` + `validates`) 在眼手模式下是否常被拒绝而退化到视平面 (记录比例)。
6. 两手缩放; 回正。
7. Augment 自动面向用户: 侧卡 z = 0.75 在绕到 Volume 侧面时的可读性。

---

## Q. 双语与字体 R4 (覆盖 v1.0 §I.2 "字体" 规则, §K.45; 删除 AGENTS "Chinese-first / FORCE_CHINESE")

### Q.1 语言解析 (`ui/theme/Localization.kt`)

| 优先级 | 来源 | 值 | 说明 |
| --- | --- | --- | --- |
| 1 (最高) | 全局设置 `gridspace_lang` (`Settings.Global.getString`) | `zh` / `en` | **仅供模拟器 / QA 脚本**: `adb shell settings put global gridspace_lang en`; 用完 `settings delete`; 在 `AppLocale` 合成时读一次, 每次语言切换事件后再读一次 |
| 2 | 持久化用户选择 `SharedPreferences("gridspace", MODE_PRIVATE).getString("language")` | `zh` / `en` / 无 | 由 中/EN 段控写入 (`LanguageSelected`); 无 = 跟随系统 |
| 3 | 系统 `Locale.getDefault().language` | `zh*` → ZH; 其它 → EN | |

- 删除 `BuildFlags.FORCE_CHINESE`。
- `AppGraph.language: MutableStateFlow<AppLanguage>`; `AppLocale` 收集它并 `CompositionLocalProvider(LocalAppResources …)`; 两个窗口 (W1 / W2) 都包在 `AppLocale` 里, 任一处切换立即同时生效。
- ViewModel 的 `Context.appString` 改为读 `AppGraph.language.value`, 不再调用 `AppLanguage.default()`。
- 切换后: Augment 内容固定宽, 文字只在固定框内换字, 系统不重建窗口; 字体族随语言切换 (§Q.2) 会触发一次全量重组, 可接受 (一次性, 用户触发)。

### Q.2 字体 (`ui/theme/GridTypography.kt` 新增 `GridTypographyTheme`)

| 项 | 值 |
| --- | --- |
| 文件 | 从 `C:\Users\Admin\Downloads\ElementSpace\app\src\main\assets\fonts\` 复制 **4 个**文件到 `app/src/main/assets/fonts/`: `NotoSans-VF.ttf` (2.0 MB), `NotoSansSC-VF.ttf` (17.8 MB), `OFL-NotoSans.txt`, `OFL-NotoSansSC.txt` (OFL 文本必须随包, 许可证要求) |
| 族选择 | ZH → `NotoSansSC-VF.ttf` (含拉丁字形, 数字 / 英文混排不回退); EN → `NotoSans-VF.ttf` (无 CJK; 英文界面不出现 CJK 字符, 见 §N.5 `entry_title`) |
| 字重注册 | 每个族注册 `FontWeight` 400 / 500 / 600 / 700 四档 `Font(path, assetManager, weight, variationSettings = FontVariation.Settings(FontVariation.weight(w)))` (与 ElementSpace `ElementTypography.kt` 相同写法); 应用自身只用 400 / 500, 600 / 700 供 SDK 组件内置样式 (Sheet / AlertDialog 标题) 使用 |
| 应用方式 | `PicoTheme(typography = base.copy(每个角色 .copy(fontFamily = family)))` 包在 `PicoTheme { }` 内, 其余 token 不动; 文件缺失时回退系统字体 (不崩溃) |
| 变量轴 | 只用 `wght`; 不用 `wdth` (Noto Sans VF 无 wdth 轴; typesetting 文档的字宽建议不适用) |
| APK | +≈ 20 MB (ElementSpace 同样做法); 可接受 |
| 行高 | §M.1 行高按 Noto 的 1.25–1.3 倍设定, 两族一致 |

### Q.3 字串与英文宽度

- 所有新字串同时写入 `res/values/strings.xml` (英文) 与 `res/values-zh/strings.xml`; 键集必须一致 (K4.26); 删除 `slot_hint_1` / `slot_hint_2`。
- 新增键 (英文 / 中文):

| 键 | en | zh |
| --- | --- | --- |
| `entry_title` (改) | GridSpace | 格间 GridSpace |
| `projects` | Projects | 项目 |
| `lang_zh` / `lang_en` | 中 / EN | 中 / EN (两语言相同) |
| `edit_colour` | Edit colour | 编辑颜色 |
| `edit_colour_slot` | Edit colour · Slot %1$d | 编辑颜色 · 槽 %1$d |
| `label_move_delta` | ΔX %1$s · ΔY %2$s · ΔZ %3$s | 同 |
| `label_move_hint` | Drag to move all | 拖动以整体移动 |
| `label_move_release` | Release to move %1$d cubes | 松手移动 %1$d 块 |
| `label_move_overwrite` | Release to move · overwrites %1$d | 松手移动 · 覆盖 %1$d 块 |
| `selection_drag_hint` | Drag a selected cube to move all | 拖动已选方块可整体移动 |
| `editing_now` | Editing | 正在编辑 |
| `back_to_projects` (B) | Back | 返回 |
| `drag_window_hint` (B) | Drag the caption bar to move this window aside | 可拖动标题栏把本窗移到一侧 |
| `no_other_projects` (A) | No other projects yet | 还没有其它项目 |

- 英文更长, 每个控件给固定 / 最大宽 (20 sp Noto Sans Medium 约 11 dp/字母, 16 sp 约 9 dp):

| 控件 | 英文最长串 | 估宽 (dp) | 容器 | 结论 |
| --- | --- | --- | --- | --- |
| 模式钮 | "Select" 20 sp | 66 + 图标 28 + 10 + 内边距 48 = 152 | 最小宽 160 | 不换行 |
| 笔刷段控项 | "Voxel" 18 sp | 50 | 160 | OK |
| 标题 项目 / 导出 | "Projects" 20 sp | 88 + 24 + 8 + 40 = 160 | 固定 160 | 刚好, `maxLines 1`; 若溢出改 "Files"? 否: 保持 "Projects", 内边距改 16 |
| 保存状态 | "Auto-saved" / "Save failed" 16 sp | ≤ 100 | 固定 120 | OK |
| 选区瓦片 | "Rotate 90°" / "Duplicate" 16 sp | ≤ 88 | 120 | OK |
| 取消 | "Cancel" 20 sp | 66 + 32 | 最小宽 96 | OK |
| 颜色栏编辑钮 | "Edit colour" 20 sp | 120 + 24 + 8 + 40 | 388 | OK |
| 预设页签 | "Vivid" / "Earth" 16 sp | ≤ 50 | 124 | OK |
| 取色编辑 从方块取色 | "Pick from cube" 20 sp | 150 + 24 + 8 + 40 = 222 | 宽 220 | 内边距 16 → 214 OK |
| 滑杆标签 | "Light" 16 sp | 45 | 72 | OK |
| 目标标签 行 1 | "✓ Aligned · release to place" 20 sp | ≈ 300 + 24 + 8 | 440 − 48 = 392 | OK |
| 目标标签 行 2 | "Release to move · overwrites 12" 16 sp | ≈ 290 | 392 | OK |
| 入口 新建 | "New blank project" 20 sp | 190 + 28 + 10 | 440 | OK |
| 入口 3 步 | "Release to drop it into a cell" 18 sp | ≈ 280 | 440 − 36 − 16 = 388 | OK |
| 列表次文字 | "128 cubes · 2 h ago" 16 sp | ≈ 170 | ≥ 400 | OK |
| 导出主钮 | "Export to this device" 20 sp | ≈ 230 | 640 | OK |
| ProjectSheet 正在编辑 | "Editing" 16 sp | 60 | 固定 96 | OK |

---

## R. R4 修订 (用户反馈 2026-10-13)

用户原话: "UI 上方中心 UI 合并进去主 UI, 或者侧面面板更紧凑显示吧, 另外方块默认白色, 然后可以选长度再摆放, 把 UX 做到极致好用"。本节决定已由 orchestrator 定案, 研发实现 R4 时以本节覆盖前文; §R.6 列出被取代的行。

### R.1 去掉顶部标题条, 系统 `Toolbar` 成为唯一主栏 (取代 §N.2 "标题条" 行, §N.4 整节, §M.3 标题条 6 行)

`Toolbar { RowScope }` 内容, 左 → 右 (内边距 左右 20 / 上下 16, 总高 112, 分隔 = 16 + `VerticalDivider` 1 × 40 + 16):

| 段 | 组件 | 尺寸 (dp) | 行为 |
| --- | --- | --- | --- |
| M1..M4 添加 / 擦除 / 上色 / 选择 | `ToggleButton` ×4 | 高 80, 最小宽 160, 间 12 → 676 | §M.3 不变 |
| 分隔 | | 33 | |
| 撤销 / 重做 | `IconButton` ×2 | 72, 图标 28, 间 12 → 156 | 栈空禁用 |
| 分隔 | | 33 | |
| 回正 | `IconButton` | 72 | |
| 分隔 | | 33 | |
| 项目 / 导出 | `IconButton` ×2, 图标 `ic_folder` / `ic_upload` 28 | 72 + 12 + 72 = 156 | 项目 → `ProjectSheet` (方案 A; **方案 B 省略此钮**, 总宽 −84); 导出 → `ExportSheet`; 无 tooltip, 依靠图标 + `contentDescription` (`projects` / `export`) + 系统 hover |
| 分隔 | | 33 | |
| 中/EN | `SegmentControl` 2 项 | 项 64 × 64, `contentPadding` 4, `itemSpace` 4 → **144 × 72**, 文字 `label` 20 | `LanguageSelected` (§Q.1) |

总宽 = 40 + 676 + 33 + 156 + 33 + 72 + 33 + 156 + 33 + 144 = **1376** (< 1600; 方案 B 1292)。`Toolbar` 由系统置于 Volume 底部居中, 不加 offset (v1.0 §L.3)。

项目名与保存状态移入颜色栏顶部固定 48 dp 表头 (§R.2 行 0); `TitleAugmentContent` / `TitleBar.kt` 中的标题条代码删除 (`TargetLabel` 保留)。

### R.2 侧面面板紧凑化 (取代 §M.3 "色槽 / 取色 ×4 / 选区面板 ×3" 行, §N.3.1 表, §N.2 两行尺寸, §M.3 高度校核表)

#### 颜色栏 `ColorRailAugment` — 宽 400 (内边距 24, 内容宽 352, 行间 12)

| 行 | 组件 | 尺寸 (dp) | 说明 |
| --- | --- | --- | --- |
| 0 表头 (新) | `Text` 项目名 `label` 20 **固定宽 200** (省略) + 间 8 + 圆点 10 + 间 8 + `Text` 保存状态 `caption` 16 **固定宽 120** | 高 48, 行宽 346 ≤ 352 | 点色: 灰 / 薄荷 / 珊瑚 (v1.0 §F.2); 名与状态以 `State` 传入, 行高固定 |
| 1 色槽 | 3 × `ColorSlot`: 热区 80, 圆 64, 当前槽白环 3 dp (外径 70), 间 12 | 264 × 80, 左对齐 | 点击 = 当前槽; 无长按 |
| 2 预设页签 | `SegmentControl` 3 项 柔和 / 鲜艳 / 大地 | 项 112 × 64, 总 352 × 72 | `captionMedium` 16 |
| 3 色板 | 4 列 × 6 行 `Box` 80 × 80, 圆角 18, 间 10 | 350 × 530 | 顺序 `row * 4 + col`; 匹配环 3 dp (近白色时改 `BaseInk` @ 60%, 见 §R.3) |
| 4 编辑颜色 | `SecondaryButton` leading `ic_sliders` 24 | 352 × 72 | Popup 锚点 (§N.3.3 不变) |

总高 = 24 + 48 + 12 + 80 + 12 + 72 + 12 + 530 + 12 + 72 + 24 = **898**; 卡片 **400 × 898**; anchor / alignment / offset 同 §N.2 (1, 0.5, 0.75) `CenterLeft` (32, 0, 0)。

#### 选区面板 `SelectionAugment` — 宽 400 (内边距 24, 内容宽 352)

| 行 | 尺寸 |
| --- | --- |
| 行 1 "已选 N 块" `label` 20 + 取消 无背景 `Button` 72 高 最小宽 96 | 高 72 |
| 行 2 (上距 12) 瓦片 旋转 / 复制 / 删除 `Button` **112 × 88**, 圆角 20, 图标 28 + `captionMedium` 16, 瓦片间 8 (3 × 112 + 16 = 352) | 高 88 |
| 行 3 (上距 20) "网格移动" `caption` 16 | 22 |
| 行 4..6 (上距 12, 行间 12) 轴标 32 + 12 + `IconButton` 72 + 12 + 值 72 (`body` 18) + 12 + `IconButton` 72 = 284 | 3 × 72 |
| 行 7 (上距 12) "拖动已选方块可整体移动" `caption` 16 | 22 |

总高 = 24 + 72 + 12 + 88 + 20 + 22 + 12 + 216 + 24 + 12 + 22 + 24 = **548**; 卡片 **400 × 548**; anchor (0, yN, 0.75) `CenterRight` (−32, 0, 0) 不变。

#### 窗口级附件清单 (替换 §N.2 表中的三行)

| 附件 | anchor | alignment | offset | 尺寸 |
| --- | --- | --- | --- | --- |
| ~~标题条 `TitleAugment`~~ | 删除 | | | |
| 笔刷行 `BrushAugment` | (0.5, 1, 0.75) | `BottomCenter` | (0, −96, 0) | **905 × 72** (§R.4, 固定尺寸) |
| 选区面板 `SelectionAugment` | (0, yN, 0.75) | `CenterRight` | (−32, 0, 0) | 400 × 548 |
| 颜色栏 `ColorRailAugment` | (1, 0.5, 0.75) | `CenterLeft` | (32, 0, 0) | 400 × 898 |

#### 文本示意图 (替换 §N.7; 添加模式 · 长条笔刷, 同时示意选区面板)

```
 ┌────────────────┐ ╔══════════════════════════════════════════════════════════════╗ ┌────────────────────┐
 │ 已选 5 块   取消 │ ║                      (200 dp 安全边)                          ║ │ 庭院小亭  ● 已自动保存│ 表头 48
 │ [旋转][复制][删除]│ ║        ┌────────────────────────────────────────┐            ║ │ ◯  ●  ◯            │ 3 槽 80
 │ 网格移动        │ ║        │             模型 (N³ 格)                │            ║ │ [柔和|鲜艳|大地]    │ 352×72
 │ X [−] 0 [+]    │ ║        │     ▣▣▣▣▣ ← 已选 (白框)                 │            ║ │ ▢ ▢ ▢ ▢            │
 │ Y [−] 0 [+]    │ ║        │        ◫◫◫◫ ← 长条幽灵 (Y 轴, L=4)       │ ┌────────────┐ ▢ ▢ ▢ ▢            │ 色板 4×6, 80
 │ Z [−] 0 [+]    │ ║        │        ░ ← 落点格高亮                    │ │1 × 4 × 1   │ ▢ ▢ ▢ ▢            │
 │ 拖动已选方块可整体移动│ ║      │ ▒▒▒▒▒▒▒▒▒ 底座 N×N ▒▒▒▒▒▒▒▒▒          │ │松手放置     │ ▢ ▢ ▢ ▢            │ TargetLabel 440×120
 └────────────────┘ ║        └────────────────────────────────────────┘ └────────────┘ ▢ ▢ ▢ ▢            │
 SelectionAugment   ║   ┌────────────────────────────────────────────────────────┐    ║ │ ▢ ▢ ▢ ▢            │
 400×548, 左外 32    ║   │ [单格][长条][长方体] │ 长度 [−] 3 [+] │ 轴 [X|Y|Z]      │    ║ │ [≡ 编辑颜色]        │ 352×72
                    ╚═══╧════════════════════════════════════════════════════════╧════╝ └────────────────────┘
                    ┌────────────────────────────────────────────────────────────────────┐ ColorRailAugment
                    │ [+添加][◇擦除][◆上色][↖选择] │ ↶ ↷ │ ⌖ │ ▤ ↑ │ [中|EN] │ Toolbar 1376×112 │ 400×898, 右外 32
                    └────────────────────────────────────────────────────────────────────┘
```

### R.3 默认白色 (取代 v1.0 §D.3 末段 "默认色槽", v1.0 K.7, §K4 中涉及默认槽色的期望)

| 项 | 值 |
| --- | --- |
| `Palette.DEFAULT_SLOTS` | S1 **#FFFFFF** (当前槽), S2 #A5EDCC, S3 #F3BC9C |
| 近白色规则 | `ColorMath.isNearWhite` 为真的槽 / 色板格 / 列表色块 → 1 dp `BaseInk` @ 30% 内描边 (v1.0 §C.2 规则, 现在默认槽即触发); 色板 "匹配当前色" 的环: 普通色 3 dp `SelectionWhite`, 近白色改 3 dp `BaseInk` @ 60% (白环在白格上不可见) |
| 幽灵 / 落点 | 仍薄荷 (系统反馈色不随用户色变, v1.0 §F 纪律) |
| 样例项目 `SampleProject` | 不变 (石灰 / 珊瑚 / 薄荷, 槽亦按样例文件) |
| 入口列表 | 新建的空白项目 `dominantColor` 为空 → 仍用 `EmptyProject` #688681 色块; 放了白块后主色 #FFFFFF 带描边 |
| JSON | 字段不变; 旧项目槽色照旧读取 |

### R.4 长条笔刷 Bar (取代 v1.0 §C.3 / §M.3 "笔刷段控" 行 / §N.2 笔刷行尺寸; 追加 v1.0 §E.3 之后为 §E.3b)

#### 笔刷行 `BrushAugment` (固定 905 × 72, 左右内边距 16, 永远整行渲染以免 Augment 重建)

| 段 | 组件 | 尺寸 (dp) | 说明 |
| --- | --- | --- | --- |
| 笔刷 | `SegmentControl` 3 项 单格 / 长条 / 长方体 (Voxel / Bar / Box) | 项 120 × 64, 总 376 × 72, `bodyMedium` 18 | 添加: 3 项可选; 擦除: 3 项可选; 上色 / 选择: 行隐藏 (v1.0 不变) |
| 分隔 | 16 + `VerticalDivider` 1 × 40 + 16 | 33 | |
| 长度 | `IconButton` − 72 · `Text` 值 `label` 20 固定宽 72 居中 · `IconButton` + 72, 间 12 | 240 | 1..16, 默认 3; 到界禁用对应钮; `contentDescription` 复用 `step_minus` / `step_plus` |
| 间 | | 16 | |
| 轴 | `SegmentControl` 3 项 X / Y / Z | 项 64 × 64, 总 208 × 72, `bodyMedium` 18 | 默认 X |

总宽 = 16 + 376 + 33 + 240 + 16 + 208 + 16 = **905** (< 1600, 居中于 Volume 底边内 96 dp)。笔刷 ≠ 长条 时, 分隔 / 长度 / 轴 三段 `alpha = 0f` 且不可点 (空间保留, 行尺寸不变); 切到长条即 `alpha = 1f`。长度与轴存在 `EditorUiState.barLength / barAxis` (每个编辑器会话记忆, 不写入 JSON); 切换模式 / 笔刷不重置。

#### 放置规则 `BarFill` (`domain/usecase/BarFill.kt`)

| 步骤 | 规则 |
| --- | --- |
| 1 目标 | 目标格 t 同单格 (添加: 相邻空格 / 底座格; 擦除: 命中方块) |
| 2 正向 | cells⁺ = { t + k·u(axis) : k = 0..L−1 }; 全在 0..N−1 内 → 用 cells⁺ |
| 3 翻转 | 否则 cells⁻ = { t − k·u : k = 0..L−1 }; 全在网格内 → 用 cells⁻ |
| 4 夹取 | 两者都越界 → 取 + / − 两方向中**在网格内的格数更多**的一侧, 截到边缘 (格数相同取 +); 至少含 t |
| 5 占用 | 添加: 已占用格跳过 (同 v1.0 §E.3.3 盒规则, 不报错); 擦除: 只删存在的方块; 结果为空集 → 不提交, 0 步 |
| 6 单击 | tap = 直接按 1–5 提交 (无预览), 与单格一致 |
| 7 捏合保持 | 起点 t 每帧随目标格更新 (与单格幽灵同源), 长条每帧重算; 松手提交 |
| 8 历史 | 整条 1 步 |

#### 预览与反馈 (追加 v1.0 §F.2 两行)

| 状态 | 视觉 | 文案 (行 1 / 行 2) |
| --- | --- | --- |
| 添加 · 长条 | 薄荷 @ 45% 单个缩放实例 1 × 1 × L (沿轴), 跳过的占用格不单独标记; 落点高亮 (§O.1) = 长条 XZ 投影 (Y 轴: 1 格; X / Z 轴: L 格) + 引导柱按起点格 | `label_box_dims` 按 X × Y × Z 格数 ("3 × 1 × 1" X 轴 / "1 × 4 × 1" Y 轴) / "松手放置" (`label_release_place`) |
| 擦除 · 长条 | 珊瑚线框 5 dp 等效, 同尺寸 | 同上 / "松手擦除" (`label_release_erase`) |
| 超出 / 无效目标 | 无幽灵 | "超出网格" (v1.0 不变) |

字串 (en / zh): `brush_bar` Bar / 长条; `bar_length` Length / 长度; `bar_axis` Axis / 轴 (两者为 `contentDescription`, 行内不显示文字标签, 长度值本身 `label` 20)。

### R.5 "极致好用" UX 守则 (验收见 K4.47–K4.55)

| # | 守则 | 实现约束 |
| --- | --- | --- |
| U1 | 任何地方无长按 | `combinedClickable` 出现 0 次 |
| U2 | 工作台任一动作 ≤ 1 步 | 模式 / 笔刷 / 长度 / 轴 / 色槽 / 色板 / 撤销 / 重做 / 回正 / 语言 全部在常驻栏上; 只有 HSL 编辑 / 导出 / 项目 需打开一层 |
| U3 | 每个可点元素有 hover | SDK 组件内置; 自绘容器加 `spatialHoverEffect` |
| U4 | 默认视角下无必需元素在视野外 | 系统 Toolbar 在 Volume 底边下, 颜色栏 / 选区 / 笔刷行 全在 Volume 边 ≤ 32 dp 内; 不依赖滚动 |
| U5 | 撤销永远一键 | 工具栏撤销钮常驻, 不进菜单 |
| U6 | 切换模式保留笔刷 / 长度 / 轴 | `brushes[mode]` + `barLength` / `barAxis` 会话级 |
| U7 | 换色一键 | 点色板即写当前槽; 点槽即切当前槽 |
| U8 | 换语言一键 | 工具栏段控, 立即生效, 持久化 |
| U9 | 预览反馈 ≤ 1 帧 | `PreviewStarted` / `PreviewMoved` 同帧更新幽灵 / 落点 / 标签 (标签 Augment 首次创建的系统黑屏除外) |

### R.6 被本节取代的前文条目

| 前文 | 处理 |
| --- | --- |
| §M.3 "标题条 高 / 项目名 / 保存状态 / 导出 / 项目 (新) / 中/EN (新)" 6 行 | 删除; 项目名 / 状态见 §R.2 行 0; 项目 / 导出 / 中/EN 见 §R.1 |
| §M.3 "色槽", "取色: 色槽行 / 预设页签 / 色板 / 按钮行", "选区面板 宽 / 瓦片 / 步进行", "笔刷段控" | 以 §R.2 / §R.4 数值为准 (槽 80/64, 色板 80, 瓦片 112 × 88, 面板 400, 笔刷项 120 × 64) |
| §M.3 高度校核表 | 标题条删除; 笔刷行 905 × 72; 选区 400 × 548; 颜色栏 400 × 898; 工具栏 ≈ 1376 × 112 |
| §N.2 标题条行, 笔刷行 / 选区 / 颜色栏尺寸 | 见 §R.2 清单 |
| §N.3.1 表 | 替换为 §R.2 颜色栏表 |
| §N.4 整节 | 删除 |
| §N.6.A "标题条 '项目'" / §N.6.B "标题条 '项目' 钮 不显示, 总宽 918" | 改为工具栏 `IconButton`; B 省略该钮, 工具栏 1292 |
| §N.7 示意图 | 替换为 §R.2 示意图 |
| §Q.3 宽度表 "标题 项目 / 导出", "保存状态 固定 120" | 项目 / 导出 改图标钮无文字; 保存状态行宽 120 不变但位于颜色栏表头 |
| K4.2 (工具栏内容), K4.5 (标题条), K4.10 / K4.34 (工作台含标题条), K4.17 ("标题条 项目") | 以 K4.38–K4.46 为准; 截图不再有顶部标题条 |
| v1.0 §D.3 默认色槽, K.7 | 以 §R.3 为准 |
| v1.0 §C.3 (2 项笔刷), K.8 | 以 §R.4 为准 (3 项) |

---

## S. R5 尺度修正 (QA R4)

依据 `docs/QA_REPORT_R4.md` §1.1 / N4-01..N4-08: 入口 Planar 窗 1.27 px/dp, 挂在 Volume 上的 Augment / Popup 只有 **0.70–0.80 px/dp** (笔刷行 z 0.75: 0.93), 因为 Volumetric 窗及其附件不动态缩放。R4 的 dp 数值全部达到, 但等效尺寸只有入口窗的 55–63 %。本节决定已由 orchestrator 定案; 研发实现 R5 时以本节覆盖 §M / §N / §R 的对应条目 (§S.9 取代表)。

### S.1 编辑器 UI 缩放 `EditorScaled`

| 项 | 值 |
| --- | --- |
| 助手 | `ui/editor/components/EditorScaled.kt`: `@Composable fun EditorScaled(scale: Float, content: @Composable () -> Unit) = CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density * scale, LocalDensity.current.fontScale), content)` |
| 常量 | `EDITOR_UI_SCALE = 1.5f` (颜色栏 / 选区面板 / 取色 Popup, 侧卡 anchor z = **0.5**); `LABEL_UI_SCALE = 1.4f` (目标标签); `BRUSH_UI_SCALE = 1.3f` (笔刷行 z = 0.75, 实测已 0.93) |
| 包裹位置 | 每个 `Augment { }` 与 `SpatialPopup { }` 的 **content 最外层**; `anchor` / `alignment` / `offset` 在包裹外, 仍是 Volume-dp, 不缩放 |
| 规格单位 | 本文所有数值仍为 **设计 dp** (= 入口窗等效); 实际 Volume-dp = 设计 dp × scale; sp 随 `Density` 同步放大 (Compose sp = density × fontScale), 字号角色不改 |
| 固定尺寸规则 | 不变 (AGENTS: 内容尺寸变化重建 Augment); 包裹后 Augment 首次创建即为放大后的尺寸 |
| 不缩放的 | 系统 `Toolbar` 内容 (假定系统按同比例渲染, 模拟器不可见, [H]); 入口窗 / Sheet (系统 Planar 窗已 1.27 px/dp) |

### S.2 各卡片设计尺寸修订与 Volume-dp 足迹

为达到 QA 阈值 (在 0.70–0.78 px/dp 下 色板 / 色槽 ≥ 92 px, 步进钮 ≥ 86 px, 笔刷项高 ≥ 86 px, 标签行 1 大写高 ≥ 20 px), 以下设计 dp 在 §R.2 / §R.4 基础上上调; 其余不变。

| 控件 | §R 值 → S 值 (设计 dp) | × scale = Volume-dp | 预期 px (用 QA 实测 px/dp) | 阈值 |
| --- | --- | --- | --- | --- |
| 色板格 | 80 → **88**, 间 10 | 132 | 88 × 1.5 × 0.70–0.75 = **92–99** | ≥ 92 ✓ |
| 色槽 热区 / 圆 | 80 / 64 → **88 / 72**, 环 3 | 132 / 108 | 热区 92–99 | ≥ 92 ✓ |
| 选区步进钮 / 值宽 | 72 / 72 → **84 / 84** | 126 | 84 × 1.5 × 0.70–0.78 = **88–98** | ≥ 86 ✓ |
| 笔刷段控项 | 120 × 64 → **120 × 72** (容器 80) | 156 × 94 | 高 72 × 1.3 × 0.93 = **87** | ≥ 86 ✓ |
| 笔刷 长度步进钮 / 轴项 | 72 / 64 → **72 / 72** (容器 80) | 94 | 87 | ≥ 86 ✓ |
| 目标标签 行 1 | `label` 20 → **`title` 28 Medium** (行高 36); 行 2 `caption` 16 → **`body` 18** | 28 × 1.4 = 39 sp | 大写高 ≈ 0.72 × 28 × 1.4 × 0.73 = **20.6** | ≥ 20 ✓ (卡 440 × 120 不变: 36 + 6 + 24 = 66 ≤ 84) |
| 取色 Popup 宽 | 640 → **560** (滑杆轨 400, 标签 72) | 840 | — | 见 §S.5 |
| 其它 (页签 112 × 64, 瓦片 112 × 88, 编辑颜色 72, 完成 80, 滑杆拇指 48 / 热区 72) | 不变 | ×1.5 | 页签高 67–72 px, 完成 84–90 px, 拇指热区 76–84 px | 无阈值 |

卡片足迹 (设计 dp → Volume-dp; 全部 ≤ 1600):

| 卡片 | 设计 dp | scale | Volume-dp | 校核 |
| --- | --- | --- | --- | --- |
| 颜色栏 (§S.3) | 430 × 1038 | 1.5 | **645 × 1557** | 高 ≤ 1600 ✓ (余 43); 居中 y 0.5 → 占 0.013..0.987; **不需缩行**。备用 (若实测 px/dp 再降而需加 scale): 页签项 64 → 60 (−4), 色板间 10 → 8 (−10) → 1024 设计 = 1536 |
| 选区面板 | 400 × 584 | 1.5 | 600 × 876 | ✓ |
| 笔刷行 | 929 × 80 | 1.3 | 1208 × 104 | ✓ 宽 ≤ 1600 |
| 目标标签 | 440 × 120 | 1.4 | 616 × 168 | ✓ |
| 取色 Popup | 560 × 624 | 1.5 | 840 × 936 | ✓ |
| 系统 Toolbar (§S.4) | ≈ 1010 × 112 | 假定 1.5 | ≈ 1515 × 168 | ≤ 1600 ✓ (假定, [H]) |

截图可见性: 模拟器截图 2160 px, Volume 右面 + 32 dp 偏移约在 1683 px; 颜色栏 645 Volume-dp × 0.72 ≈ 464 px → 右缘 ≈ 2147 px, 在画面内但余量 < 15 px。侧卡 anchor 改回 **z = 0.5** (更远 → 投影更靠内, QA `rail/montage.png` 证实 z 0.5 稳定在画面内); 若仍裁切, 研发用 `gridspace_rail` 把 offset 改 16 并记录。

### S.3 颜色栏 `ColorRailAugment` 修订 (取代 §R.2 颜色栏表与 §R.1 的 项目 / 导出 / 中|EN 位置)

宽 **430** (内边距 24, 内容宽 382, 行间 12), `EditorScaled(1.5)`:

| 行 | 组件 | 尺寸 (设计 dp) | 说明 |
| --- | --- | --- | --- |
| 1 表头 | 项目名 `label` 20 固定宽 200 + 8 + 圆点 10 + 8 + 保存状态 `caption` 16 固定宽 120 | 高 48, 行宽 346 | 不变 |
| 2 动作 (新) | `IconButton` 项目 72 (`ic_folder` 28) · 12 · `IconButton` 导出 72 (`ic_upload` 28) · 12 · `SegmentControl` 中|EN 144 × 64 (项 64 × 64) | 高 72, 行宽 312, 左对齐 | 项目 → `ProjectSheet` (方案 B 省略); 导出 → `ExportSheet`; 中|EN → `LanguageSelected`; `contentDescription` `projects` / `export` |
| 3 色槽 | 3 × 热区 88 / 圆 72 / 环 3, 间 12 | 288 × 88 | |
| 4 预设页签 | `SegmentControl` 3 项 | 项 122 × 64, 总 382 × 72 | |
| 5 色板 | 4 × 6, 88, 圆角 20, 间 10 | 382 × 578 | 近白匹配环 `BaseInk` @ 60% (§R.3) |
| 6 编辑颜色 | `SecondaryButton` | 382 × 72 | Popup 锚点 |

总高 = 24 + 48 + 12 + 72 + 12 + 88 + 12 + 72 + 12 + 578 + 12 + 72 + 24 = **1038** 设计 dp (1557 Volume-dp)。anchor (1, 0.5, **0.5**), `CenterLeft`, offset (32, 0, 0)。

选区面板: 宽 400 不变, 步进行改 32 + 12 + 84 + 12 + 84 (值) + 12 + 84 = 320; 总高 24 + 72 + 12 + 88 + 20 + 22 + 12 + 3×84 + 2×12 + 12 + 22 + 24 = **584**; anchor (0, yN, **0.5**), `CenterRight`, (−32, 0, 0); `EditorScaled(1.5)`。

笔刷行: 376 (3 × 120, 项高 72) + 33 + 240 + 16 + 232 (3 × 72 轴项) + 2 × 16 = **929 × 80**; anchor / offset 不变; `EditorScaled(1.3)`。

### S.4 系统 `Toolbar` 修订 (取代 §R.1 表)

内容 = 4 模式 (80 高, 最小宽 160, 间 12 = 676) · 分隔 33 · 撤销 / 重做 (72 ×2 + 12 = 156) · 分隔 33 · 回正 72 · 内边距 40 = **1010** 设计 dp (假定系统按 1.5 渲染 → 1515 ≤ 1600)。项目 / 导出 / 中|EN 移到颜色栏行 2 (§S.3)。

### S.5 取色 Popup `ColorEditPopup` (取代 §N.3.3 尺寸, N4-03 / N4-08)

| 项 | 值 |
| --- | --- |
| 尺寸 | 宽 560 (内边距 32; 行 3 = 标签 72 + 16 + 轨 400 = 488), 高 624 (行高不变: 72 / 88 / 248 / 80 + 间距); 内容外层 `EditorScaled(1.5)` |
| 窗口参数 | `SpatialPopup(defaultMinWidth = 560.dp, defaultMinHeight = 624.dp)`, 两值与内容完全一致, 让系统一次按最终尺寸创建, 完成钮下无黑带; 研发核对 SpatialPopup 的 dp 是否按包裹内密度换算 (目标: 渲染窗 = 840 × 936 Volume-dp, 与内容一致), 不一致则把参数改为 840 / 936 |
| 位置 | 锚在 "编辑颜色" 按钮; 垂直 `VerticalPlacement.above(16.dp)`; 水平改为 **右缘对齐锚点右缘** (`HorizontalPlacement` 的 end / 右对齐变体, 名称在 design-6.1.9 `classes.jar` 核对; 若无, 用 `center()` + 向左 140 dp 的 offset), 使 Popup 右缘 = 颜色栏右缘, 不出画面 (居中会超出 2160 px 截图 ≈ 100 px) |
| 覆盖关系 | Popup 936 Volume-dp 高, 覆盖色板与页签下半; 表头 / 动作行 / 色槽 (栏顶 256 设计 dp = 384 Volume-dp) 保持可见 |
| 十六进制 | `small` 14 → **`caption` 16** labelSecondary, 固定宽 112 (N4-08); `small` 角色在应用内不再使用 (保留定义) |

### S.6 目标标签避让 (取代 AGENTS "Target-label Augment placement" 的选边规则, N4-02)

| 规则 | 值 |
| --- | --- |
| 基本 | R3 `LabelPlacement` (上 1.2 格, 侧 1.0 格, 前 0.8 格; 按投影 x 符号选边) 不变 |
| 右上限 | 投影锚点 x (归一化 Volume 坐标) **> 0.78** → 强制 `CenterRight` 向左伸 (offset −12), 不论半区 |
| 左上限 | x **< 0.22 且 `showSelectionPanel`** → 强制 `CenterLeft` 向右伸 (offset +12) |
| 尺寸 | 卡 440 × 120 设计 dp, `EditorScaled(1.4)` → 616 × 168 Volume-dp; 行 1 `title` 28, 行 2 `body` 18 (§S.2) |
| 验收 | 卡片包围盒与颜色栏 / 选区面板包围盒无交集, 目标格不被盖 (N-01 不回退) |

### S.7 Sheet 宽 (取代 §N.6.A 表 "720 / 172", §M.3 "导出 Sheet 内容宽 640 / 格式段控 320", N4-04)

SDK `Sheet` 内容宽固定 (实测 ≈ 410 dp, 1.19–1.31 px/dp)。`ProjectSheet` / `ExportSheet` 内容 `fillMaxWidth()`, 不写固定宽; 段控项用 `weight(1f)`; 列表行 ≥ 88, 主钮 80 高, 文件名 72 高, 其余尺寸不变。规格不再给 720 / 640。

### S.8 其它修正

| 编号 | 修正 |
| --- | --- |
| N4-05 | `VoxelSceneView.onDragStart`: `mode == SELECT && cell == null` (底座或空白) → ORBIT; 只有方块路由到 选中切换 / 拖动移动 (§O.2.1 第 3 行按此实现) |
| N4-07 | 选区面板步进值用与标签相同的 `signed()`: 负数 U+2212 "−1", 零 "0", 正数不带 "+" (面板为累计值, 非 Δ) |
| N4-06 | `newproject` 脚本追加 `PresetSelected(VIVID)` 一步并截图 |
| N4-09 | 任何重拍命令追加写入对应 `run.log` |

### S.9 被本节取代的前文条目

| 前文 | 处理 |
| --- | --- |
| §R.1 工具栏表 (1376, 含 项目 / 导出 / 中|EN) | §S.4 (1010, 仅模式 / 撤销 / 重做 / 回正) |
| §R.2 颜色栏表 (400 × 898, 色板 80, 槽 80/64) | §S.3 (430 × 1038, 色板 88, 槽 88/72, 新增行 2) |
| §R.2 选区面板 (步进 72, 高 548) | §S.3 (步进 84, 高 584) |
| §R.2 附件清单 anchor z 0.75 (侧卡) | z 0.5 |
| §R.4 笔刷行 905 × 72, 项 120 × 64, 轴 64 | 929 × 80, 项 120 × 72, 轴 72 |
| §N.3.3 Popup 640 宽 / 居中 / `defaultMinHeight` 未指定; §M.3 十六进制 `small` | §S.5 |
| §M.3 目标标签 行 1 `label` 20 / 行 2 `caption` 16 | `title` 28 / `body` 18 |
| §N.6.A ProjectSheet 720 / 项 172; §M.3 导出 640 / 320 | §S.7 |
| K4.38 (1376), K4.40 (400 × 898 / 80), K4.41 (72), K4.43 (905 × 72 / 64), K4.14 (640 × 624) | 以 K4.56–K4.68 为准 |

---

## T. R8 统一尺寸笔刷 (用户反馈 2026-10-13)

用户看 R7 截图 (`docs/screenshots/r7/en/r7-bar.png`, `zh/r7-box.png`) 的反馈: (1) 底部笔刷行被底座半透明玻璃的近边压住 (行在 z 0.75, 底座近角投影在它前面); (2) 单格 / 长条 / 长方体 是一回事, 应合并为 "先选 W × H × D 再摆放"; (3) "Bar" 右边的长度步进 + X|Y|Z 看不懂。决定已由 orchestrator 定案; 本节取代 §R.4 与 §S 中的笔刷行条目 (§T.6)。

### T.1 笔刷行 = 三个尺寸步进 (取代 §R.4 笔刷行表, §S.2 / §S.3 笔刷行行)

`BrushAugment` 内容 (`BrushRow.kt` 重写), 固定尺寸, 高 80 (上下内边距 4), 左右内边距 16, **scale 1.0** (不包 `EditorScaled`, 见 §T.3):

| 段 | 组件 | 尺寸 (dp) | 说明 |
| --- | --- | --- | --- |
| X 组 | `Text` "X" `label` 20 固定宽 28 · 8 · `IconButton` − 72 (图标 28) · 8 · `Text` 值 `label` 20 固定宽 56 居中 · 8 · `IconButton` + 72 | 252 × 72 | 值 1..min(16, N) (N ≥ 16 → 1..16); 到界禁用对应钮 (系统 alpha); `contentDescription` `step_minus` / `step_plus` |
| 分隔 "×" | `Text` "×" `caption` 16 labelSecondary 固定宽 24, 左右各 12 | 48 | 非翻译字串 `size_times` |
| Y 组 | 同 X 组 | 252 | |
| 分隔 "×" | 同上 | 48 | |
| Z 组 | 同 X 组 | 252 | |

总宽 = 16 + 252 + 48 + 252 + 48 + 252 + 16 = **884**; 行 **884 × 80**。轴字母用现有 `axis_x` / `axis_y` / `axis_z`。行的 `contentDescription` = `brush_size` ("尺寸" / "Size")。

| 项 | 值 |
| --- | --- |
| 状态 | `EditorUiState.brushSize: Cell = Cell(1, 1, 1)` (W = x, H = y, D = z), 每个编辑器会话记忆, 不写 JSON; 事件 `SizeStep(axis, delta)` |
| 显示条件 | 添加 / 擦除 显示; 上色 / 选择 隐藏 (不变), 这两种模式**强制按 1 × 1 × 1** 运算但不改写 `brushSize`; 回到添加 / 擦除 时恢复记忆值 (U6) |
| 删除 | `Brush` 枚举、`brushes[mode]`、`barLength` / `barAxis`、单格 / 长条 / 长方体 段控、长度步进、轴段控、**拖拽定盒** (v1.0 §E.3 的 A..B 拖动) 全部删除; 添加 / 擦除 的捏合保持现在只移动幽灵 (与 v1.0 单格笔刷相同), 尺寸只来自步进 |
| 无隐藏状态 | 三个值常驻可见; 改尺寸 = 1 次点击 (U2); 无长按 (U1) |

### T.2 放置语义 `SizedBrush` (取代 §R.4 `BarFill`, v1.0 §E.3 `BoxFill`; 文件 `domain/usecase/SizedBrush.kt`)

| 步骤 | 规则 |
| --- | --- |
| 1 目标 | 目标格 t 同单格 (添加: 命中面相邻空格 / 底座格; 擦除: 命中方块) |
| 2 展开 | 块 = { t + (i, j, k) : 0 ≤ i < W, 0 ≤ j < H, 0 ≤ k < D }, 沿 +X / +Y / +Z |
| 3 按轴翻转 | 对每轴独立: 若 t.a + len − 1 > N − 1 → 改为 t.a − (len − 1) .. t.a (向负方向); 若负向也 < 0 (len > N, 不可能, len ≤ 16 ≤ N) → 夹到 0 .. len − 1; 三轴互不影响 |
| 4 占用 | 添加: 已占用格跳过 (不报错); 擦除: 只删除存在的方块; 结果空集 → 不提交, 0 步 |
| 5 单击 | tap = 按 1–4 直接提交, 无预览 |
| 6 捏合保持 | 起点 t 每帧随目标格更新 (v1.0 §L.1.4), 块每帧重算; 松手提交; 跟踪丢失 → 不提交 |
| 7 历史 | 整块 1 步 |
| 8 大块 | W × H × D > 2000 时分帧建实体 (≤ 500 / 帧), 标签追加 "较大, 稍候" (v1.0 §E.3.6 / L.2 不变); 上限 16³ = 4096 |

预览与反馈 (取代 §R.4 "预览与反馈" 表, v1.0 §F.2 长方体行):

| 状态 | 视觉 | 文案 行 1 / 行 2 |
| --- | --- | --- |
| 添加 · 1 × 1 × 1 | 薄荷幽灵 1 格 + 落点高亮 + 引导柱 (§O.1) | `label_aligned` "✓ 已对齐 · 松手放置" / `label_cell` "X7 · Y5 · Z0" (v1.0 不变) |
| 添加 · 其它尺寸 | 薄荷 @ 45% 单个缩放实例 W × H × D (翻转后的实际包围, 占用格不单独标记) + 落点高亮 = 块的 XZ 投影 (每个列下方都有) + 0.06 外扩 + 引导柱按 t | `label_size_dims` "3 × 2 × 1" (W × H × D, 翻转后仍按 W H D 报) / `label_release_place` "松手放置" |
| 添加 · 整块被占用 | 珊瑚线框 W × H × D + 珊瑚落点 | `label_occupied` / `label_occupied_sub` |
| 擦除 · 任意尺寸 | 珊瑚线框 5 dp 等效 W × H × D | 1 × 1 × 1: `label_release_erase` 单行 (不变); 否则 `label_size_dims` / `label_release_erase` |
| 超出网格 | 无幽灵 | `label_outside` / `label_grid_dims` (不变) |

`Preview.BoxAdd / BoxErase` 改名 `Preview.BlockAdd(region, color) / BlockErase(region)` (`BoxRegion` 保留)。

### T.3 笔刷行摆位 (取代 §N.2 / §R.2 / §S.3 笔刷行 anchor, AGENTS "Brush-row Augment placement (R2)")

| 项 | 值 |
| --- | --- |
| anchor | `NormalizedPoint3D(0.5f, 1f, 0.97f)` (底面, 距前面 3 %) |
| alignment / offset | `BottomCenter`, `DpOffset3D(0, −24, 0)` |
| scale | **1.0** (去掉 `EditorScaled(BRUSH_UI_SCALE)`; `BRUSH_UI_SCALE` 常量删除): 前平面在模拟器约 **1.6 px/Volume-dp**, 设计 dp 直接可用 |
| 预期 px | 行 884 × 80 → **≈ 1414 × 128 px**, 居中于 Volume (x ≈ 373..1787 px); 步进钮 72 → **≈ 115 px** (≥ 92 ✓); 值 / 轴字母 20 sp → ≈ 32 px 字号 |
| 遮挡校核 | 底座 1200 dp, yaw 0.60 / pitch 0.40 时近角在 Volume 归一化 z ≈ 0.98, y ≈ 0.93 (x ≈ 0.6); 行占 z 0.97, y 0.935..0.985 → 近角尖端理论上仍可能露出 ≤ 16 dp 到行前, Volume 在 z = 1 裁切底座。验收以截图为准 (K4.73); 若不通过, 备选 anchor z = **1.0** (`NormalizedPoint3D.BottomFront`), offset 不变, 用 `gridspace_brush 0.5,1,1,0,-24,0` 实测后写入 DEV_REPORT |
| 下缘 | 行下缘 = 底边上 24 dp; 若截图中下缘出画 (2158 px), 改 offset −64 (同样经 `gridspace_brush` 验证) |

### T.4 字串 (取代 §R.4 字串行)

删除键 (两语言): `brush_voxel`, `brush_box`, `brush_bar`, `bar_length`, `bar_axis`。`label_box_dims` 改名 `label_size_dims` (值不变 "%1$d × %2$d × %3$d")。新增:

| 键 | en | zh |
| --- | --- | --- |
| `brush_size` | Size | 尺寸 |
| `size_times` | × | × (不翻译, `translatable="false"`) |

轴字母复用 `axis_x` / `axis_y` / `axis_z` ("X" / "Y" / "Z", 两语言相同); 步进钮复用 `step_minus` / `step_plus`。

标签文案表 (R8 全部):

| 情形 | zh 行 1 / 行 2 | en 行 1 / 行 2 |
| --- | --- | --- |
| 添加 1 × 1 × 1 | ✓ 已对齐 · 松手放置 / X7 · Y5 · Z0 | ✓ Aligned · release to place / X7 · Y5 · Z0 |
| 添加 W × H × D | 3 × 2 × 1 / 松手放置 | 3 × 2 × 1 / Release to place |
| 添加 占用 | 格已占用 / 换一个格子 | Cell occupied / Try another cell |
| 擦除 1 × 1 × 1 | 松手擦除 / — | Release to erase / — |
| 擦除 W × H × D | 3 × 2 × 1 / 松手擦除 | 3 × 2 × 1 / Release to erase |
| 较大 | 3 × 2 × 1 / 松手放置 · 较大, 稍候 | 3 × 2 × 1 / Release to place · Large, please wait |

### T.5 示意 (替换 §R.2 示意图底行)

```
 ║   ┌──────────────────────────────────────────────────────────────┐   ║
 ║   │ X [−] 3 [+]   ×   Y [−] 2 [+]   ×   Z [−] 1 [+]               │   ║  BrushAugment 884×80, z 0.97, 底边内 24
 ╚═══╧══════════════════════════════════════════════════════════════╧═══╝
```

### T.6 被本节取代的前文条目

| 前文 | 处理 |
| --- | --- |
| §R.4 整节 (长条笔刷, 905 × 72 行, `BarFill`, 字串) | 删除, 以 §T.1–T.4 为准 |
| §S.2 "笔刷段控项 120 × 72", "长度步进钮 / 轴项 72"; §S.2 足迹表 "笔刷行 929 × 80 ×1.3"; §S.3 末段 "笔刷行 929 × 80 … EditorScaled(1.3)"; §S.1 `BRUSH_UI_SCALE`; §S.9 笔刷行行 | 行 884 × 80, scale 1.0, z 0.97 |
| §N.2 / §R.2 附件清单 笔刷行 anchor (0.5, 1, 0.75) offset −96 | §T.3 |
| §M.3 "笔刷段控" 行; v1.0 §C.3 (段控), §E.3 (拖拽定盒), §F.2 长方体行; v1.0 K.8 / K.16 / K.17 | 以 §T 为准; 拖拽定盒不再存在 |
| §O.1 "长方体: 同色, 尺寸 = 盒 XZ 包围" | 读作 "W × H × D 块的 XZ 包围" |
| §P.2 "笔刷 `SegmentItem` ×2" 行 | 改为 "尺寸步进 `IconButton` ×6" (行为同按钮) |
| K4.8 中 "笔刷行 (单格 / 长方体)"; K4.33 walk 步序 `bar` / `box`; K4.43–K4.46; K4.52 (U6 长条记忆); K4.57 "笔刷段控项高 ≥ 86"; K4.58 "笔刷行 1208 × 104" | 以 K4.69–K4.76 为准 |

### T.7 R9 修订: 笔刷行三组分隔 (取代 §T.1 的行布局与总宽; 用户反馈 r8-size.png "字母、数字、按钮连成一串")

| 段 | 组件 | 尺寸 (设计 dp) | 说明 |
| --- | --- | --- | --- |
| 组 (每轴一个) | `Text` 轴标 `label` 20 M 固定宽 88 起始对齐 · 12 · `IconButton` − 72 · `Text` 值 `label` 20 M 固定宽 56 居中 · `IconButton` + 72 | 300 × 72 | 轴标 zh "宽 X" / "高 Y" / "深 Z", en "W · X" / "H · Y" / "D · Z" (新键 `size_axis_x/y/z`); 删除 `size_times` 与 "×" 分隔文本 |
| 组间 | 24 · `VerticalDivider` 1 × 48 · 24 | 49 | |
| 行 | 左右内边距 32 | **1062 × 80** | 32 + 3 × 300 + 2 × 49 + 32; scale 1.0, anchor / offset 同 §T.3 (研发实测 offset −320) |
| 状态 | − 在 1 时禁用, + 在 min(16, N) 时禁用 (系统 alpha); 值 `labelPrimary`; 内容固定尺寸, 按钮带 hover | | 不变 |
| 预期 px | 1.6–1.8 px/Volume-dp → 行 ≈ 1700–1920 px, 居中后在 2160 画面内 (余 ≥ 120 px); 若裁切, 轴标 88 → 80、间距 24 → 20 | | 验收: r9 `workbench` / `size` 两帧, 三组肉眼可分、无裁切、在底座前 |

### T.8 R10 面板开关 (用户反馈: 尺寸行与颜色栏应由用户从底部工具栏打开; 取代 §R.1 / §S.4 工具栏表与 §S.3 行 2 "动作行" 的 项目 / 导出)

| 项 | 值 |
| --- | --- |
| 工具栏 (系统 `Toolbar`, 样式不变) | `[添加][擦除][上色][选择] │ ↶ ↷ │ ⌖ │ [尺寸] [颜色] │ [项目] [导出]`; 尺寸 / 颜色 = `ToggleIconButton` 72 (图标 `ic_cube` / `ic_palette`, checked = 面板已显示), 项目 / 导出 = 原 `IconButton` 72 (从颜色栏动作行移回); 总宽 ≈ 676 + 33 + 152 + 33 + 72 + 33 + 152 + 33 + 152 + 40 = **1376** 设计 dp (< 1600) |
| 状态 | `EditorUiState.showSizeRow` / `showColorRail`, 默认 **都为 false**; 会话内记忆 (`AppGraph.panels`, 不写盘); 事件 `ToggleSizeRow` / `ToggleColorRail` |
| 显示规则 | 笔刷行 Augment: `showSizeRow && mode ∈ {添加, 擦除}`; 颜色栏 Augment: `showColorRail`; 颜色栏关闭时取色卡 / 取色条随之关闭 (`picker = null`); 选区面板仍自动 (选择模式有选区) |
| 颜色栏 | 表头 (名 + 状态) 不变; 动作行只剩 中|EN; 换色唯一路径 = 打开颜色栏 |
| 字串 | `toggle_size` 尺寸 / Size, `toggle_colour` 颜色 / Colour (`contentDescription`) |
| 演示 | walk 在需要的步骤前显式切换 (size 步前开尺寸, vivid 前开颜色); 新脚本 `panels`: workbench (双关) → size → rail → dragmove → edit |

### T.9 R11 坐标轴标注 (用户反馈: 尺寸行的 宽 X / 高 Y / 深 Z 与选区 ΔX·ΔY·ΔZ 需要可见的方向参照)

`VoxelScene` 内的 3D 内容, 无碰撞体、不可交互、所有模式常驻; 几何在 `AxisGuides` (纯数据, 单测), 由 `SceneParts.axisGuides` 作为**一个实例化实体** (共享单位盒, 不新建网格) 随底座重建:

| 件 | 几何 (格单位, 原点 = 底座顶面中心) |
| --- | --- |
| 形式 | **近边中点小坐标架** (R11 最终): 原点在底座近边 (+z 边) 中点外侧 0.6 格, 模型 (0, 0.04, +N/2 + 0.6)。协调者的右角外侧原点 (N + 0.6, ·, N + 0.6) 实测不可见: Volume 把离中心 ≈ 830–890 dp 以外的内容裁掉 (底座角 848 dp 仍可见, 角外坐标架 ≥ 896 dp 整体消失), 故移到边中点 (645 dp) 并缩短 |
| +X 杆 | 从原点沿 +x 1.2 格, 粗 0.08, 浮于底座顶面上 0.04 |
| +Z 杆 | 从原点沿 +z 1.2 格, 同粗 (尖端 ≈ 790 dp, 在裁切半径内) |
| +Y 杆 | **无** (竖杆与字母挡住模型) |
| 位置理由 | 整个坐标架在网格外 (z > N/2): 方块永远挡不住它, 它也永远不压模型; 之前沿整边 / 近边的杆在默认视点下出画, 沿后边的杆被亭子挡住、字母投影在屋顶上 |
| 箭头 | 每杆 + 端三个递缩方块 0.35 / 0.25 / 0.15 连续堆叠; 尖端 = 原点 + 1.95 格, 字母 = 原点 + 2.45 格 |
| 颜色 | 全部一色 `GridColors.AxisGuide` #AAB9B8, 不透明 (85 % 时 0.08 格的杆在底座玻璃上几乎不可辨; 网格线色提亮, 不用 Mint / Coral) |
| 不相交 | 所有盒都在 z ≥ N/2 且 y ≥ 0.04, 尖端半径 ≤ 10.6 格 (单测 `AxisGuidesTest`) |
| 字母 | X / Z (`axis_x` / `axis_z`) 在尖端外 0.5 格。**实现 = 两个 `Augment` 小片** (56 × 56 设计 dp, 前面平面 scale 1.0, `AugmentContentAlignment.Center`, 锚点 = 尖端点按 `LabelAnchor.projectToFront` 投影到 z 0.92 面并夹到 x [0.03, 0.97], y [0.03, **0.80**] (面的下部在模拟器画面外: 笔刷行在底边上 320 dp 已是画面最后一条, 画面下缘 ≈ y 0.86; +Z 圆片的自然位置 ≈ 0.85 被切半, 夹到 0.80 后完整可见, 正好贴在箭头尖端上方), 随 yaw / pitch 每帧重算, 与目标标签同一套投影): 轴色圆片 (#AAB9B8 @ 85 %) 上 `label` 20 M `BaseInk` 字母。SDK 路径已验证存在但在模拟器上不可用: `attachmentPanelComponent(size = panelSize(56.dp, 56.dp))` + `LookAtComponent(setViewerAsTarget, NEGATIVE_Z)` 挂在尖端实体上能建出 112 × 112 px 的附着窗 (logcat `isViewAttachment=true`), 位置正确, 但窗内只渲染一块空白浅色方片, Compose 内容 (圆片 + 字母) 不出现 → 按本节备选用 Augment |

---

## K4. 验收清单 R4 (追加在 v1.0 §K 之后; v1.0 K.1–K.45 仍有效, 与本节冲突时以本节为准)

标注: [E] 模拟器截图 / 点击可验; [C] 读代码可验; [D] 需 `gridspace_demo` 脚本; [H] 仅真机。

**尺寸 / 文字 (§M)**

1. [C] `GridTypography.kt` 只有 7 个角色: title 28 M / label 20 M / body 18 R / bodyMedium 18 M / caption 16 R / captionMedium 16 M / small 14 M; 无 `smallMedium`; 无任何 < 14 sp。
2. [E] 工作台截图中模式钮高 80 (在 2160 px 截图上以标题条 96 为参照目测比例), 文字 20 sp; 撤销 / 重做 / 回正 72; 工具栏内容约 1010 × 112, 位于 Volume 底部居中 (模拟器截图区看不到底部时, 由 [C] `EditorToolbar.kt` 尺寸常量代验 + 真机 [H] 补拍)。
3. [C] `FontWeight.Bold` / `SemiBold` / `W600` / `W700` 在 `app/src/main` 出现 0 次。
4. [C] 所有可点目标 ≥ 72: `IconButtonDefaults.iconButtonSize(72)`, `ButtonDefaults.buttonSize(height ≥ 72)`, `PrimaryButton` 80, 色槽 88, 色板 88, `SegmentItem` 高 64 在 72 容器内, `sliderSpec(thumbAreaSize = 72)`; grep 不到 56 / 48 / 40 dp 的可点尺寸 (`DefaultCloseIconButton` 等系统默认除外)。
5. [E] 标题条 1098 × 96: 项目 160 · 项目名 360 (28 sp) · 保存状态 138 · 中/EN 152 · 导出 160; 项目名超长显示省略号。
6. [E/D] 目标标签卡 440 × 120, 行 1 20 sp + 图标 24, 行 2 16 sp (`r4-*-ghost.png`)。
7. [E/D] 选区面板 436 宽: 瓦片 120 × 96 ×3, 步进钮 72, 值宽 72, 末行提示 "拖动已选方块可整体移动" (`r4-*-dragmove.png`)。
8. [E] 入口: 新建 440 × 80, 网格尺寸 440 × 72, 列表行 ≥ 88, 色块 56, 删除 72 (`r4-*-entry.png`)。
9. [E/D] 导出 Sheet: 内容宽 640, 文件名 72 高, 主钮 80 (`r4-*-export.png`)。

**空间排布 (§N)**

10. [E] 工作台: 上 标题条 / 右 颜色栏 / 下 笔刷行, 四者同时可见, 互不重叠; 颜色栏在 Volume 右边外 32 dp, 436 × 916, 含 3 槽 (当前槽白环) + 柔和|鲜艳|大地 + 4 × 6 色板 88 + "编辑颜色" (`r4-*-workbench.png`)。
11. [E/D] 选择模式有选区时左侧选区面板与右侧颜色栏同时显示 (`r4-*-dragmove.png`)。
12. [C] 工具栏内无色槽; `EditorToolbarContent` 只含 4 模式 + 撤销 / 重做 + 回正; `SlotLongPressed` 事件与 `combinedClickable` 已删除。
13. [D] 点击色板格 → 当前槽色块立即变色, 不入历史 (`r4-*-rail.png` 为切到 "鲜艳" 页后的截图, 页签高亮 + 24 色与 v1.0 §D.3 一致)。
14. [D] 点击 "编辑颜色" → `ColorEditPopup` 640 × 624 出现在颜色栏下部上方 16 dp, 覆盖色板, 颜色栏顶部 3 槽仍可见; 含 标题 "编辑颜色 · 槽 n" / 88 预览圆 + 十六进制 / 3 滑杆 (拇指 48, 轨 480) / 从方块取色 72 / 完成 80 (`r4-*-edit.png`)。
15. [D] 从方块取色: 面板收起为 88 高提示条; 点方块后颜色进入当前槽, 面板恢复, 色板匹配环更新 (walk 脚本步 `eyedrop`)。
16. [C] 颜色栏、选区面板、标题条的 Augment 内容尺寸固定 (固定宽 / 高 modifier), 变化值以 `State` 传入; `gridspace_rail` / `gridspace_selpanel` 摆位设置可用且默认值为 §N.2。
17. [E] 方案 A: 新建 / 打开项目后入口窗关闭, 只剩 Volume (`r4-*-workbench.png` 中无入口窗); 标题条 "项目" 打开 `ProjectSheet` 720 宽: 新建 80 / 网格尺寸 72 / 列表行 88 / 当前项目标 "正在编辑" (`r4-*-projects.png`); 关闭编辑器后入口窗重新出现 (export 脚本结束后截 `r4-*-entry-after.png`)。— 或 — 方案 B: 入口窗 560 × 720; 打开编辑器后入口内容切换为 "正在编辑 名称 · 返回" + 列表 + 拖窗提示 (`r4-*-drawer.png`); "返回" 关闭 Volume 并回到 Idle 态; 标题条无 "项目" 钮。DEV_REPORT 必须写明探针结果与所选方案。
18. [H] 方案 A: 系统关闭钮关闭 Volume 后入口窗重开且列表刷新; 方案 B: 用户拖动抽屉到一侧后再开新项目, 抽屉位置保持。

**吸附与直接操控 (§O)**

19. [D] 添加模式幽灵在 (x, y>0, z) 时: 底座对应格显示薄荷落点高亮 1.12 格 (从上方看是一圈边) + 竖向引导柱; 占用时落点与柱变珊瑚; 松手后与幽灵一起消失 (`r4-*-ghost.png`, `r4-*-occupied.png`; walk 的 ghost 目标改为 (3, 2, 5) 以便看到引导柱)。
20. [C] `VoxelSceneView.kt` drag 回调注释写明 "gaze+pinch / directPinch / poke / 手柄射线 / 手柄近场 走同一路径"; 选择模式分流: 命中已选格 → `SelectionDragStarted`; 非已选格 → v1.0 选择预览; 底座 / 空白 → 轨道。
21. [D] 选区拖动预览: walk 选 5 块后发 `SelectionDragStarted` + `SelectionDragMoved(Cell(2, 0, −1))`: 原块变暗带白框, 目标格薄荷幽灵 + 白框, 底座白色落点包围, 标签 "ΔX +2 · ΔY 0 · ΔZ −1 / 松手移动 5 块", 步进值显示 2 / 0 / −1 (`r4-*-dragmove.png`)。
22. [D] 松手 (`SelectionDragEnded`): 5 块落到新位置并保持选中; 历史 +1; 撤销一次全部回原位且选区清空; 导出 vox 逐格验证新坐标。
23. [C] 单测 `CameraMath.dragToDelta`: (a) |u|,|v| < 0.5 → Δ 0; (b) yaw 0.60 时屏幕 x → X 轴 (或 Z, 按 `dominantAxis` 结果写死期望); (c) v = 2.0, u = 0.3 → LIFT, Δy = 2; (d) v = 1.0, u = 1.0 → GROUND, 两水平轴各 1; (e) 平面锁定后不随后续比值改变; (f) 越界夹取: 选区贴 x = N−1 时 Δx 夹为 0。
24. [C] 单测 `SelectionOps.translate` 覆盖: 目标格含非选区方块时被覆盖并进入同一历史步; Δ = 0 不产生历史。
25. [H] 真机: 眼手 / 直接捏合 / 手柄 三种方式拖动已选方块, 每格一步的手感 (0.5 格死区), 竖直拖动抬升 (LIFT) 是否自然。

**双语与字体 (§Q)**

26. [C] `values` / `values-zh` 键集一致 (计数相等, 无单边键); 无 `slot_hint_1/2`; 新增 §Q.3 的键齐全; Kotlin 内无硬编码用户串。
27. [C] `assets/fonts/` 含 `NotoSans-VF.ttf`, `NotoSansSC-VF.ttf`, `OFL-NotoSans.txt`, `OFL-NotoSansSC.txt`; `GridTypographyTheme` 按语言选族, 文件缺失时回退系统字体; `BuildFlags.FORCE_CHINESE` 已删除。
28. [E] `settings put global gridspace_lang en` 后冷启动: 入口标题 "GridSpace", 按钮 "New blank project", 英文字形为 Noto Sans (与系统 PICO Sans 对照: 小写 g 双层、数字 1 带脚); `gridspace_lang zh` 后为 "格间 GridSpace" 与 Noto Sans SC。
29. [D] 中/EN 段控: walk 脚本步 `lang` 发 `LanguageSelected(EN)` → 标题条、颜色栏、选区面板、笔刷行、工具栏文字同帧切换为英文且卡片尺寸不变 (`docs/screenshots/r4/zh/r4-lang-toggle.png` 为切换后一帧); 再切回中文。
30. [C] 切换写入 `SharedPreferences` "language"; 冷启动 (无 `gridspace_lang`) 读到该值优先于系统语言。
31. [E] 英文下所有 §Q.3 表中的控件单行不省略 (逐张英文截图目检: workbench / dragmove / edit / export / entry / projects)。

**构建 / 脚本 / 截图 (简报 §3.6)**

32. [C] `./gradlew :app:assembleDebug :app:testDebugUnitTest --offline -q` EXIT 0; 单测 ≥ R3 的 35 + K4.23 / K4.24 新增; dex 校验 `SpatialApplication` refs > 0。
33. [D] `walk` 脚本 R4 步序 (每步 `mark`): ghost(含落点) → occupied → box → erase → paint → selection → dragmove → dragmove-commit → rotate → duplicate → stepx → delete → vivid → edit → eyedrop → edit-close → lang (EN 一帧) → lang-back → undo ×3 (burst) → redo ×3 → reset → export → exported → (A) projects → done; 全程 logcat 无 `AndroidRuntime` / `FATAL` / ANR, `/data/anr/` 无新 trace。
34. [E/D] 截图两套, 路径 `docs/screenshots/r4/zh/` 与 `docs/screenshots/r4/en/` (分别在 `gridspace_lang zh` / `en` 下跑一遍 walk), 每套至少:

| 文件 | 中文 (zh) 内容 | 英文 (en) 内容 |
| --- | --- | --- |
| `r4-entry.png` | 入口: 格间 GridSpace · 新建空白项目 · 网格尺寸 · 最近项目 · 中/EN | GridSpace · New blank project · Grid size · Recent projects · 中/EN |
| `r4-workbench.png` | 工作台: 标题条 (项目 / 已自动保存 / 中/EN / 导出) + 模型 + 颜色栏 (柔和) + 笔刷行 (单格 / 长方体) | Projects / Auto-saved / Export + Soft + Voxel / Box |
| `r4-ghost.png` | 添加幽灵 + 薄荷落点高亮 + 引导柱 + 标签 "✓ 已对齐 · 松手放置 / X3 · Y2 · Z5" | "✓ Aligned · release to place / X3 · Y2 · Z5" |
| `r4-occupied.png` | 珊瑚线框 + 珊瑚落点 + "格已占用 / 换一个格子" | "Cell occupied / Try another cell" |
| `r4-dragmove.png` | 选区拖动预览: 变暗原块 + 幽灵 + 白落点 + "ΔX +2 · ΔY 0 · ΔZ −1 / 松手移动 5 块" + 选区面板 (步进 2 / 0 / −1) | "ΔX +2 · ΔY 0 · ΔZ −1 / Release to move 5 cubes" + panel |
| `r4-rail.png` | 颜色栏切到 "鲜艳", 页签高亮, 色板 24 色 | "Vivid" |
| `r4-edit.png` | 取色编辑面板 (编辑颜色 · 槽 1 / 滑杆 / 从方块取色 / 完成) 覆盖色板, 顶部 3 槽可见 | "Edit colour · Slot 1 / Hue Sat. Light / Pick from cube / Done" |
| `r4-export.png` + `r4-exported.png` | 导出 Sheet 空闲态 + 成功态 | "Export / File name / Export to this device" + "Exported" |
| `r4-projects.png` (A) / `r4-drawer.png` (B) | 项目 Sheet / 项目抽屉 Editing 态 | "Projects / Editing" / "Editing · Back" |
| `r4-lang-toggle.png` (仅 zh 套) | 中文工作台切到 EN 后一帧, 卡片尺寸不变 | — |

35. [E] 每张截图用 `docs/scripts/cap.sh` 的黑块重试规则, 新建窗口 (Augment / Popup / Sheet) 出现后 ≥ 1.5 s 再拍 (AGENTS)。
36. [C] 颜色纪律 (v1.0 §I.1 / K.40) 扩展: `Coral` 新增用途仅 "占用落点高亮" 与 "拖动覆盖线框"; `Mint` 新增用途仅 "可放置落点高亮 / 引导柱" 与 "正在编辑 文字"; `SelectionWhite` 新增 "拖动落点包围"; 其它颜色角色未改。
37. [C] 无新增依赖 (`gradle/libs.versions.toml` 与 `app/build.gradle.kts` 未变); 字体从 assets 读取, 不用 `androidx.compose.ui.text.googlefonts`。

**§R 修订 (2026-10-13 用户反馈)**

38. [C] 无 `TitleAugment` / `TitleAugmentContent`; `EditorToolbarContent` 顺序 = 4 模式 · 分隔 · 撤销 重做 · 分隔 · 回正 · 分隔 · 项目 导出 (`IconButton` 72, `ic_folder` / `ic_upload`, contentDescription `projects` / `export`) · 分隔 · 中/EN (`SegmentControl` 144 × 72); 尺寸常量合计 1376 (方案 B 1292)。
39. [E/H] 工作台截图顶部无任何卡片; 项目名 + 保存点 位于颜色栏表头 48 dp (名固定宽 200, 状态固定宽 120) (`r4-*-workbench.png`); 工具栏含 ▤ ↑ 与 中|EN (模拟器底部不可见时 [C] 代验, 真机补拍)。
40. [E] 颜色栏 400 × 898: 表头 48 / 3 槽 80 (圆 64, 当前槽 3 dp 环) / 页签 352 × 72 / 色板 4 × 6 × 80 间 10 / 编辑颜色 352 × 72; 内边距 24, 行间 12。
41. [E/D] 选区面板 400 × 548: 瓦片 112 × 88 ×3 (间 8), 步进钮 72, 末行提示 (`r4-*-dragmove.png`)。
42. [C] `Palette.DEFAULT_SLOTS` = [#FFFFFF, #A5EDCC, #F3BC9C], `activeSlot = 0`; 近白色槽 / 色板 / 列表色块有 1 dp `BaseInk` @ 30% 描边; 近白匹配环为 `BaseInk` @ 60%。[E] 入口新建项目 → 工作台颜色栏第 1 槽白色带描边 + 白环, 鲜艳页 #ffffff 格显示深色匹配环 (`r4-*-newproject.png`, walk 前用 `sample` 以外的新建流程或 demo 步 `newproject`)。v1.0 K.7 期望改为本条。
43. [E/D] 笔刷行 905 × 72: 单格 | 长条 | 长方体 (120 × 64); 选 长条 后同一行出现 长度 [−] 3 [+] (72 / 值宽 72 / 72) 与 轴 X|Y|Z (64 × 64); 选 单格 / 长方体 时该两段不可见但行尺寸不变 (前后两帧截图对比卡片尺寸一致)。
44. [D] walk 步 `bar`: 添加 + 长条, 轴 Y, 长度 4, `PreviewStarted(Target(12, 0, 2))` → 薄荷 1 × 1 × 4 幽灵竖条 + 底座 (12, 2) 落点高亮 + 标签 "1 × 4 × 1 / 松手放置", 笔刷行显示 长条 高亮 · 4 · Y (`r4-*-bar.png`); `Committed` → 方块 +4 (若样例在 (12, 0..3, 2) 已有方块则按跳过规则减少, 研发对照 `SampleProject.kt` 写明期望), 历史 +1, 撤销一次全部消失; 导出 vox 逐格核对。
45. [C] 单测 `BarFillTest`: (a) 正向全在网格内 → L 格; (b) 正向越界、反向可行 → 翻转 (t − k·u); (c) 两向都越界 → 取格数多的一侧并截边, 至少含 t; (d) 添加时占用格被跳过, 全占用 → 空集; (e) 擦除只含存在的方块; (f) L = 1 等价单格; (g) 轴 X / Y / Z 各一例。
46. [C] 字串 `brush_bar` (Bar / 长条), `bar_length` (Length / 长度), `bar_axis` (Axis / 轴) 两语言齐全; 长度 / 轴 只作 contentDescription。
47. [C] U1: `combinedClickable` 与任何 `onLongClick` 出现 0 次。
48. [C] U2: 从工作台到 模式 / 笔刷 / 长度 / 轴 / 当前槽 / 色板色 / 撤销 / 重做 / 回正 / 语言 各为 1 次点击 (逐项对照常驻栏); 二层入口只有 编辑颜色 / 导出 / 项目。
49. [C] U3: 每个 `clickable` / `onClick` 的自绘容器 (色槽, 色板格, 列表行) 带 `spatialHoverEffect`; SDK 组件不再额外包裹。
50. [E/H] U4: 默认视角 (yaw 0.60 / pitch 0.40 / zoom 1) 下 颜色栏 / 选区面板 / 笔刷行 全部在截图内完整可见; 工具栏在真机默认距离下完整可见 (真机)。
51. [C] U5: 撤销 / 重做 `IconButton` 常驻工具栏, 任何模态 (Sheet) 外都可点。
52. [D] U6: walk 中 添加→长条 (长度 4, 轴 Y) → 切 擦除 → 切回 添加: 笔刷仍为长条, 长度 4, 轴 Y。
53. [D] U7: 点色板格后下一次放块即为该色 (walk: `SwatchPicked` 后放 1 块, 导出核对颜色)。
54. [D] U8: `LanguageSelected(EN)` 一帧后全部文字切换 (K4.29), 冷启动保留。
55. [C] U9: `PreviewStarted` / `PreviewMoved` 处理路径内无 `delay` / 动画等待; 幽灵、落点、标签在同一次状态更新中设置。

**§S R5 尺度修正 (QA R4)** — 截图路径 `docs/screenshots/r5/{zh,en}/r5-<name>.png`, 两套各含: entry, workbench, rail, dragmove, edit, bar, projects, export, entry-after, newproject, ghost, occupied。

56. [C] `EditorScaled.kt` 存在; `EDITOR_UI_SCALE = 1.5f` / `LABEL_UI_SCALE = 1.4f` / `BRUSH_UI_SCALE = 1.3f`; 颜色栏 / 选区面板 / 目标标签 / 笔刷行 四个 `Augment` 与 `ColorEditPopup` 的 content 最外层各有一次 `EditorScaled(...)`; `anchor` / `alignment` / `offset` 在包裹外; `GridSizes` 等常量仍为 §S.2 / §S.3 的设计 dp。
57. [E] 像素阈值 (`python -P docs/scripts/measure_r5.py`, 由 `measure_r4.py` 改区域, 对 zh 与 en 各跑一次, 每项打印 px): 色板格 ≥ **92** (`r5-rail.png` 鲜艳页饱和格); 色槽热区 ≥ **92** (`r5-workbench.png` 2× 裁剪); 选区步进钮 ≥ **86** (`r5-dragmove.png`); 笔刷段控项高 ≥ **86** (`r5-bar.png`); 标签行 1 大写字高 ≥ **20** (`r5-ghost.png` 差分裁剪, 量 "X" / "A" 高度)。任一不达 → N5 P1。
58. [E] 足迹: `r5-workbench.png` 中颜色栏完整在画面内 (右缘 ≤ 2160 px, 下缘可见), 高度 ≈ 1557 Volume-dp 不超出 Volume 上下边 (以底座 / 系统底板为参照); 选区面板 600 × 876、笔刷行 1208 × 104、标签 616 × 168 均完整; [C] 常量合计: 颜色栏 430 × 1038, 选区 400 × 584, 笔刷 929 × 80, 标签 440 × 120, Popup 560 × 624。
59. [C] `EditorToolbarContent` = 4 模式 · 分隔 · 撤销 重做 · 分隔 · 回正 (合计 1010); 颜色栏行 2 = 项目 72 · 导出 72 · 中|EN 144 × 64 (`r5-rail.png` 可见 "▤ ↑ 中|EN" 一行, 位于表头下、色槽上); [H] 真机确认系统 Toolbar 完整可见且模式钮触达 ≥ 72 等效 dp。
60. [E/D] 标签避让: `r5-dragmove.png` (锚点 x > 0.78) 与 walk `box` 步的标签卡包围盒 (差分 bbox) 与颜色栏包围盒无交集, 卡片向左伸; `r5-ghost.png` / `r5-occupied.png` 目标格 + 落点 + 标签同时完整 (N-01 不回退); [C] `LabelPlacement` 含 0.78 / 0.22 两个阈值与 `showSelectionPanel` 条件。
61. [E] `r5-edit.png`: Popup 右缘 ≤ 2160 px, 颜色栏区域近黑占比 0.00, "完成 / Done" 四周为玻璃; 十六进制 `caption` 16 在 2× 裁剪可辨 ("#FF3B30" 六位可读); 栏顶 表头 / 动作行 / 色槽 可见。
62. [C/E] `ProjectSheet.kt` / `ExportSheet.kt` 无 `width(720/640.dp)`, 段控项 `weight(1f)`; `r5-projects.png` 4 个网格尺寸项等宽、新建钮 80 高、列表行 ≥ 88; `r5-export.png` 文件名 72、主钮 80; 研发报告申报 "SDK 固定宽"。
63. [C] `onDragStart`: `SELECT && cell == null → ORBIT`; 单测 `HitResolver` / 视图分流: 选择模式 `Base` 命中不产生 `Preview.SelectCell`; walk 不变。
64. [D] `r5-dragmove.png` 选区面板 Z 值显示 "−1" (U+2212), 与标签 "ΔZ −1" 同字形; [C] `SelectionPanel` 用 `signed()`。
65. [D] `gridspace_demo newproject` 末尾切 "鲜艳" 并截 `r5-newproject.png`: 槽 1 白 + 描边 + 白环, #ffffff 格 `BaseInk` @ 60% 深色环可见。
66. [C] `r5-run.log` (zh / en) 每张截图一条 cap.sh 记录, 重拍命令追加其后; 无 "no clean frame" 而文件存在的矛盾。
67. [E] 两套 12 张截图齐全 (本段开头列表), 每张标记后 ≥ 1.5 s, 文字语言与 `gridspace_lang` 一致; `r5-entry-after.png` 入口重开 (方案 A)。
68. [C] §S.9 取代项全部按新值实现: grep 不到 1376 / 898 / 905 / 640 (Popup) / 720 (Sheet) 的尺寸常量; `GridType.small` 无调用点。

**§T R8 统一尺寸笔刷** — 截图 `docs/screenshots/r8/{zh,en}/r8-<name>.png`; walk 步序: ghost → occupied → **size** → erase → paint → selection → dragmove → … (去掉 `bar` / `box`)。

69. [C] `BrushRow.kt` 只含 X / Y / Z 三组步进 (`IconButton` 72 ×6, 值 `label` 20 宽 56, 轴字母宽 28, "×" `caption` 16 宽 24), 行常量 884 × 80; `Brush` 枚举、`BarFill`、`BoxFill`、`brushes` / `barLength` / `barAxis`、拖拽定盒代码路径 (A..B) 全部删除; `brushSize` 默认 `Cell(1,1,1)`, 范围 1..min(16, N)。
70. [C] 单测 `SizedBrushTest`: (a) 1 × 1 × 1 = 单格; (b) 3 × 2 × 1 在 (1,0,1) → 6 格 (1..3, 0..1, 1); (c) X 越界翻转: W = 4 在 x = 14 (N 16) → x 11..14; (d) Y 翻转、Z 翻转各一例, 三轴独立 (X 翻转不影响 Y); (e) 两向都不够时夹到 0..len−1 (用 N = 16, len = 16, t = 8); (f) 添加跳过占用格, 全占用 → 空集; (g) 擦除只含存在的方块; (h) > 2000 格触发 `largeFill`。
71. [D] walk 步 `size`: 添加模式, `SizeStep` 至 3 × 2 × 1 (行显示 3 · 2 · 1), `PreviewStarted(Target(1, 0, 1))` → 薄荷 3 × 2 × 1 幽灵块 + 底座 (1..3, 1) 落点高亮 + 标签 "3 × 2 × 1 / 松手放置" (`r8-*-size.png`); `Committed` → 方块 +6 (若 (1..3, 0..1, 1) 有样例方块则按跳过规则减少, 研发对照 `SampleProject.kt` 写明), 历史 +1, 撤销一次全部消失; 导出 `.vox` 逐格核对 6 个坐标与当前槽颜色。
72. [E] `r8-*-size.png` / `r8-*-workbench.png`: 笔刷行 ≈ 1414 × 128 px, 居中, 下缘在画面内 (y < 2158 px), 步进钮 ≥ 92 px (`measure_r8.py` 量 − / + 圆钮直径)。
73. [E] 遮挡: 同两张截图中笔刷行玻璃上方**没有**底座网格线 / 底座半透明层 (在行包围盒内对底座灰蓝 `BaseInk`@70% 与网格线色做像素统计, 占比 0.00); 若 z 0.97 不达, 用 z 1.0 复拍并在 DEV_REPORT 记录 (§T.3)。
74. [C] 字串: `brush_voxel` / `brush_box` / `brush_bar` / `bar_length` / `bar_axis` 两语言均已删除; `label_size_dims`, `brush_size`, `size_times`(translatable=false) 存在; 键集一致。
75. [D] U6 / U2: walk 中 添加 3 × 2 × 1 → 切 上色 (行隐藏, 上色按 1 × 1 × 1) → 切回 添加: 行显示 3 · 2 · 1; 改任一轴 = 1 次点击; 无隐藏状态 (三值常显)。
76. [C/E] U1–U9 复核: `combinedClickable` 0; 步进钮 `IconButton` 自带 hover; 添加 / 擦除捏合保持只移动幽灵 (无 A..B 拖拽); 标签文案按 §T.4 表 (`r8-*-size.png` zh / en 各一)。
