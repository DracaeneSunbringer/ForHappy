# 05 · 移动端视觉设计系统（Design System）

> 对应交接文档 **交付物 5**。正常态在现有「暖纸松绿 / 深松暗夜」基础上补齐语义层级与对比度；伪装态色板与风格**高度还原目标 App**，不做品牌化改造。

---

## 1. 设计原则

1. **内容是主角**：界面色彩低饱和，品牌色只用于「可操作 + 已选中」两类元素；梗图与表情包永远是屏幕上最鲜艳的东西。
2. **一屏一个主操作**：每屏最多一个品牌色实底按钮。
3. **克制的信息密度**：卡片只放「能直接点的」信息（胶囊、赞），说明性元数据下沉到详情。
4. **伪装即还原**：伪装态不追求好看，追求「像」——圆角、字号、间距、图标线宽都以目标 App 为准。

---

## 2. 色彩语义表

### 2.1 正常态

| Token | 用途 | 暖纸松绿（浅） | 深松暗夜（深） |
|---|---|---|---|
| `bg` | 页面背景 | `#F7F6F0` | `#14261F` |
| `surface` | 卡片、底栏、Sheet | `#FFFFFF` | `#1D3329` |
| `surfaceVariant` | 次表面：胶囊底、输入框底、分组背景 | `#EEF0E6` | `#243D31` |
| `surfaceRaised` | 浮层（菜单、对话框） | `#FFFFFF` + 阴影 | `#2A4538` |
| `outline` | 分割线、描边 | `#DCE0D2` | `#33503F` |
| `text` | 主文字 | `#172D25` | `#F2F4EC` |
| `textMuted` | 辅助文字、时间、楼层 | `#5A6F65` | `#A3B6AA` |
| `textDisabled` | 禁用 | `#9AA89F` | `#5E7468` |
| `primary` | 品牌主色：选中 Tab、主按钮、链接 | `#1F6B4F` | `#B7CE78` |
| `onPrimary` | 主色上的文字 | `#FFFFFF` | `#14261F` |
| `primaryContainer` | 一句话胶囊底、选中胶囊底 | `#DCEBD9` | `#2F4D3C` |
| `accent` | 点缀：成就、进度条高光 | `#B7CE78` | `#5CA382` |
| `coin` | D 币金额、红包相关 | `#B7791F` | `#E9B949` |
| `redpacket` | 红包封面主色 | `#D9433B` | `#E25B50` |
| `like` | 已点赞红心 | `#E0475B` | `#F06A7B` |
| `danger` | 警示、受限、删除、倒计时 <30s | `#C2410C` | `#F97316` |
| `badge` | 未读数字底 | `#E5484D` | `#F2555A` |
| `highlight` | 跳转定位闪光、命中字底 | `#FFF1B8` | `#5A4A12` |
| `scrim` | 遮罩 | `#000000` 40% | `#000000` 56% |
| `blurOverlay` | 模糊图暗化层 | `#000000` 12% | `#000000` 24% |

**对比度要求**：`text/bg` ≥ 7:1，`textMuted/bg` ≥ 4.5:1，`onPrimary/primary` ≥ 4.5:1（深色模式 `#14261F` on `#B7CE78` 计算约 9.1:1；浅色 `textMuted #5A6F65` on `#F7F6F0` 约 5.0:1）。

### 2.2 飞书伪装态

| Token | 浅色 | 深色 |
|---|---|---|
| `bg` | `#F5F6F7` | `#191919` |
| `surface` | `#FFFFFF` | `#222326` |
| `surfaceVariant`（输入框、气泡-他人） | `#F2F3F5` | `#2E2F33` |
| `bubbleSelf`（自己的气泡） | `#E1EAFF` | `#1F3A6E` |
| `outline` | `#DEE0E3` | `#373737` |
| `text` | `#1F2329` | `#EBEBEB` |
| `textMuted` | `#646A73` | `#A6A6A6` |
| `primary`（飞书蓝） | `#3370FF` | `#4C88FF` |
| `badge` | `#F54A45` | `#F76964` |
| `success`（待办勾选） | `#34C724` | `#51BA43` |

- 字体：系统默认（不使用自定义字体）；会话名 16sp Medium、摘要 14sp、时间 12sp。
- 圆角：群头像 8dp、人头像圆形、图片消息 8dp、气泡 8dp。

### 2.3 LINUX DO（Discourse）伪装态

| Token | 浅色 | 深色 |
|---|---|---|
| `bg` | `#F7F8FA` | `#1D1F21` |
| `surface` | `#FFFFFF` | `#25282A` |
| `outline` | `#E9E9E9` | `#3A3D40` |
| `text` | `#222222` | `#DDDDDD` |
| `textMuted` | `#919191` | `#999999` |
| `primary`（Discourse 蓝） | `#0088CC` | `#1A95E0` |
| `like` | `#FA6C8D` | `#FA6C8D` |
| `categoryBadge`（搞七捻三分类色块示例） | `#F7941D` | `#F7941D` |
| `highlight`（新楼层） | `#FFFFE0` | `#3B3A24` |

- 字母头像：首字母 + Discourse letter avatar 色池；楼层头像 40dp。
- 正文 15sp，行高 1.5；圆角 4dp；分隔用 1dp 线，无卡片阴影。

### 2.4 老板键云文档

| Token | 浅色 | 深色 |
|---|---|---|
| `bg` | `#FFFFFF` | `#1A1A1A` |
| `text` | `#1F2329` | `#E6E6E6` |
| `textMuted` | `#8F959E` | `#8A8A8A` |
| `tableHeader` | `#F5F6F7` | `#262626` |
| `tableBorder` | `#DEE0E3` | `#3A3A3A` |
| `calloutBg`（高亮块） | `#FFF5EB` | `#3A2C1E` |
| `link` | `#3370FF` | `#4C88FF` |

---

## 3. 字阶（正常态）

| Token | 字号 / 行高 | 字重 | 用途 |
|---|---|---|---|
| `display` | 28 / 36 | SemiBold | 钱包余额、倒计时大数字（等宽数字 `tnum`） |
| `titleL` | 20 / 28 | SemiBold | 页面标题（二级页顶栏） |
| `titleM` | 18 / 26 | SemiBold | 帖子详情标题、Sheet 标题 |
| `titleS` | 16 / 24 | Medium | 一级页顶栏标题、会话名、分组标题 |
| `body` | 15 / 22 | Regular | 聊天消息、评论、正文 |
| `bodyS` | 14 / 20 | Regular | 卡片标题、会话摘要、设置项 |
| `label` | 13 / 18 | Medium | 胶囊、按钮次要、筛选胶囊 |
| `caption` | 12 / 16 | Regular | 时间、楼层、作者、计数 |
| `micro` | 11 / 14 | Medium | 底栏标签、角标 |

- 中文字体使用系统默认（思源/HarmonyOS Sans/MiSans 由系统决定）；数字统一使用等宽数字特性（倒计时、余额、计数不跳宽）。
- 聊天字号跟随设置「聊天字号」可在 `body` 基础上 ±2sp。
- 支持系统字体缩放至 1.3×：所有容器高度用最小高度而非固定高度。

---

## 4. 间距、圆角、尺寸

### 4.1 间距（4dp 网格）

| Token | 值 | 用途 |
|---|---|---|
| `space1` | 4dp | 图标与文字、胶囊内间距 |
| `space2` | 8dp | 瀑布流列间距、胶囊之间 |
| `space3` | 12dp | 卡片内边距 |
| `space4` | 16dp | 页面左右边距、列表项内边距 |
| `space5` | 24dp | 分组之间 |
| `space6` | 32dp | 大区块之间 |

### 4.2 圆角

| Token | 值 | 用途 |
|---|---|---|
| `radiusS` | 6dp | 标签、小角标 |
| `radiusM` | 12dp | 卡片、图片、输入框 |
| `radiusL` | 16dp | 聊天气泡、FAB |
| `radiusXL` | 24dp | Bottom Sheet 顶部 |
| `radiusFull` | 999 | 胶囊、头像 |

### 4.3 关键尺寸

| 元素 | 尺寸 |
|---|---|
| 顶栏 | 48dp |
| 底栏 | 56dp + 系统 inset |
| 临时横条（公告/网络/猜词状态条） | 32–36dp |
| 最小点击热区 | 48 × 48dp（视觉可小，热区不可小） |
| 胶囊高度 | 28dp（热区扩到 40dp） |
| 头像 | 聊天 36dp / 会话列表 48dp / 个人页 64dp |
| FAB | 56dp |
| 底部互动栏 / 输入栏 | 56dp（多行输入最高 5 行） |

---

## 5. 信息密度规则

| 页面 | 规则 |
|---|---|
| 图廊卡片 | 媒体之外最多 3 行信息（标题 2 行 + 胶囊/作者 1 行组合），胶囊 ≤ 2 + 3 个 |
| 聊天 | 同组连续消息间距 2dp，不同人 12dp；徽章 ≤ 2 枚 |
| 会话列表 | 每项 72dp，两行（名称/时间、摘要/角标） |
| 设置列表 | 每项 52dp，右侧最多显示一个状态值 |
| 「我的」头部 + 资产行 | ≤ 260dp |
| Sheet | 半屏默认 ≤ 60% 屏高，内容超出时可上拉全屏，不嵌套 |

---

## 6. 动效

| 场景 | 时长 | 曲线 |
|---|---|---|
| 按压态 / 胶囊切换 | 100ms | `FastOutSlowIn` |
| 点赞心跳 | 300ms | spring（dampingRatio 0.5） |
| Sheet 弹出 / 收起 | 250ms / 200ms | `EmphasizedDecelerate` / `EmphasizedAccelerate` |
| 卡片 → 详情共享元素 | 280ms | `Emphasized` |
| 看图舞台下拉关闭 | 跟手 + 200ms 回弹 | spring |
| 模糊揭开 / 盖回 | 200ms / 150ms | linear |
| 定位闪光高亮 | 1200ms（两次脉冲） | ease-in-out |
| 红包「開」 | 600ms | 自定义翻转 |
| 顶栏收起 / 出现 | 跟随滚动 | — |
| **老板键进入 / 退出** | **0ms（无动画）** | — |
| **伪装切换** | **0ms（无动画）** | — |
| 全图模糊开关 | 150ms | linear |

- 系统「移除动画」开启时：所有装饰性动画关闭，仅保留状态变化。
- 120Hz：列表滚动、跟手拖动禁止在主线程做图片解码；动画只用 `graphicsLayer` 属性（透明度、位移、缩放）。

---

## 7. 组件清单（研发实现单元）

| 组件 | 变体 | 说明 |
|---|---|---|
| `TopBar` | 一级 / 二级 / 透明浮层（详情） | 单行 48dp，右侧 ≤ 2 图标 |
| `BottomNav` | 正常 4 Tab / 飞书 3 Tab | — |
| `InlineBanner` | 公告 / 网络 / 离开期间新消息 / 猜词状态条 | 32–36dp 单行 |
| `MemeCard` | 双列 / 单列 / 沉浸 | 含模糊状态机、已看计时（无 UI） |
| `BlurMedia` | 图片 / 视频 / GIF / 实况 | 模糊、揭开、自动盖回 |
| `QuickReplyChip` | 卡片 / 详情 / 本人 | 书名号样式 + `×N` |
| `ReactionChip` | 普通 / 本人已投 | — |
| `StickerText` | 行内表情混排 | 固定占位尺寸 |
| `MessageBubble` | 他人 / 自己 / 系统卡 / 红包 / 帖子预览 / 摘录卡 | 支持皮肤 |
| `Composer` | 评论 / 群聊三模式 / 私信 / 一句话（12 单位计数） | — |
| `JumpToLatestPill` | `↓ N` | — |
| `ActionSheet` | 卡片快捷菜单 / 消息菜单 / 更多 | — |
| `SettingRow` | 开关 / 选择 / 跳转 / 分段 | — |
| `MaskCanvas` | 框选 / 涂抹 × 马赛克 / 模糊 / 涂黑 | — |
| `RedPacketDialog` | 直接 / 复读 / 解谜×6 / 猜图 / 定时 / 口令 / CDK | — |
| `GuessIntelPanel` | 状态条 / 情报板 / 全屏 | — |
| `WebShell` | 游戏沉浸 / 书房（含协作条） | — |
| 伪装组件集 | `FeishuConversationRow` `FeishuImageMessage` `FeishuThread` `DiscoursePost` `DiscourseTimeline` `DiscourseChatDrawer` `FeishuDoc` | 与正常组件共享数据层，仅换渲染层 |

### 7.1 图标

- 正常态：Material Symbols Rounded，线宽 2dp，24dp 网格；表情包/贴纸保持原图。
- 飞书伪装：线性图标、线宽 1.5dp、更细更方正（接近飞书 UD 图标）。
- LINUX DO 伪装：Font Awesome 风格实心小图标（Discourse 默认）。
