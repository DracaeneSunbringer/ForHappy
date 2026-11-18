# 07 · 尽欢（ForHappy）v0.2.0 分阶段重构实施计划

> 本计划依据已评审定稿的 [`docs/redesign/` v1.1 设计方案](README.md) 制定，并已对照现有 Kotlin 源码、`AndroidManifest.xml`、`gradle/libs.versions.toml` 及单元测试完成逐项代码审计修订（v1.2）。  
> 重构采取 **「核心底座 100% 复用并增量补齐 + UI 层按模块分阶段重构替换」** 策略，确保每个阶段结束时工程均可独立编译、通过单元测试并上真机验收。

---

## 一、重构架构边界：保留什么 / 扩展什么 / 替换什么

### 1.1 直接保留并持续复用的 `core/` 底座
以下模块经过多轮线上迭代与真机验证，架构清晰且稳定性高，重构期间**不推倒重写**，仅按代码审计结果与新交互需求做精准增量扩展：

| 模块 | 现有文件（`app/src/main/kotlin/com/jinhuan/forhappy/...`） | 复用定位与增量改动点 |
|---|---|---|
| Rust ECH 网络引擎 | `core/network/rust/RustNetManager.kt`, `RustNetBridge.kt` | 原封保留 DoH + TLS 1.3 ECH + Edge IP 优选；UI 仅订阅状态 Flow 并接入设置 › 网络诊断页；并为原生视频播放器提供本地代理 URL |
| HTTP 调度与离线容器缓存 | `core/network/cookie/PersistentCookieJar.kt`, `CookieSyncBridge.kt`, `RequestSchedulerInterceptor.kt`, `WebAssetCacheManager.kt`, `StickerFallbackInterceptor.kt` | 原封保留；为第三方红包/外部页面与新版 `WebShell` 提供共享登录态及离线资源加速 |
| 应用多镜像竞速更新 | `core/update/AppUpdateManager.kt`（含 `AppUpdateController`） | 保留 30 镜像 4 路并发与 `Range 206` 断点续传；UI 解耦为启动静默检测 → 非阻断 Snackbar + 设置子页 |
| 1s 静默已看追踪 | `core/seen/SeenTracker.kt`, `SeenDwellObserver.kt` | 保留防抖本地队列与 `UTC+8` 统计；Phase 2 扩展 `ObserveMemeSeenDwell`，接入 `onGloballyPositioned` 视口裁剪可见面积占比（`≥ 60%`）、列表滚动速率（`≤ 1500dp/s`）与模糊态门控 |
| 群聊持久化与富文本 | `core/chat/ChatSessionController.kt`, `ChatRichTextParser.kt`, `core/db/ForHappyDatabase.kt` | 保留 Room 分段缓存、楼层防断层合并；将私信列表（`DmThread`）从 `ChatScreen` 局部状态上收至 `ChatSessionController`，统一输出 `totalUnread = groupUnread + dmUnread` 与 `hasUnreadMention` 供底栏和伪装壳消费，并补充书房摘录卡解析与草稿/群别名存储 |
| 鱼鱼谷时空与共存保护 | `core/clock/IsleClockRules.kt`, `ServerClock.kt`, `core/network/pet/PetCoexistenceCoordinator.kt` | 保留动态时钟校准与 `arcade-standby` 保护检测；供「发现」Tab 摘要卡与街机入口使用 |
| 动态顶栏模块扫描 | `core/dynamic/TopbarModuleScanner.kt` | 从 `DEFAULT_BUILT_IN_MODULES` 移除 `/puzzles`、清理 `KNOWN_ROUTE_META` 并加入 7 个已下线路由黑名单（`/puzzles`、`/tools`、`/farm`、`/manor`、`/huancai`、`/music`、`/nsfw`）；同步更新 `app/src/test/.../TopbarModuleScannerTest.kt`（断言 7 路由被过滤，改用 `/arcade` 等验证新模块发现） |

### 1.2 需要增量补齐的网络接口与端侧能力（`ForHappyApiClient.kt` / `Models.kt` / 本地存储）
新设计覆盖了网站更完整的互动细节，需在对应阶段补齐以下数据能力（对照网页 `app.js`、`redpacket.js`、`guess-room.js`、`dm.js`、`me-wallet.js`）：
- **图廊 / 帖子 / 发帖**：`byDay` 日历索引与指定日期定位参数、`anchors` 插入/删除/列表、`maskTap` 本地偏好、收藏夹创建/重命名/删除（`POST /api/folders`、`PATCH/DELETE /api/folders/:id`）、帖子编辑（`PATCH /api/memes/:id`）与敏感一键切换（`POST /api/memes/:id/nsfw`）、多层解密答题进度/领奖接口、端侧局部打码与原图保留上传、社交链接剪贴板识别解析。
- **群聊 / 私信 / 猜词 / 红包**：群组历史消息 `around` 上下文锚定、群聊图片转帖（`POST /api/chat/messages/:id/to-post`）、入群申请与管理员审批、私信成员搜索（`GET /api/dm/users`）与新建会话、私信编辑（`PATCH`）/ 撤回（`DELETE`）、猜词屋三模式独立草稿、`POST /api/chat/messages/:mid/guess-room` 发送 `!`/`?`/`@欢欢 线索`、猜词开局/等候队列/流局投票/猜中打赏、发红包与九宫格端侧重采样防作弊马赛克合成（对应网站 `makePacketClue`）。
- **个人 / 发现 / 装扮**：摸鱼日历本机考勤薪资设置 + 前台计时折算 + 今日日报 3 版式数据、装扮工作室接口（保存 `deco` / 气泡 / 展示柜 / 洁癖屏蔽开关）、邀请码生成/撤销与记录、D 币钱包流水过滤、财富榜与红包站密钥（`/api/me/hb-key`）。

### 1.3 逐步替换或退场的旧 UI 代码
以下现有视图文件在新方案中将被新版页面完整取代（在各阶段验收通过后即可清理）：
- `ui/navigation/ForHappyShell.kt`（旧 5-Tab 壳 → 新 4-Tab 壳）
- `ui/wall/WallScreen.kt`、`MemeDetailModal.kt`
- `ui/chat/ChatScreen.kt`、`GuessRoomSheet.kt`
- `ui/hub/HubScreen.kt`（连同其中的旧版解谜列表一同移除）、`ui/pets/PetsScreen.kt`（重复的三合一原生表单页退场，统一收归「发现」Tab 摘要 + `WebShell`）
- `ui/me/MeScreen.kt`
- `ui/disguise/FeishuDisguiseScreen.kt`、`LinuxDoDisguiseScreen.kt`、`BossDocScreen.kt`

---

## 二、分阶段重构路线图（Phase 1 ～ Phase 6）

> 每个阶段均遵循 **「数据/状态层 → UI 组件与页面 → 单元测试 + 编译 + 真机验证」** 闭环；重构期间所有页面均逐步切换，避免半成品长期不可编译。

### Phase 1：设计系统 Token、全局防窥状态底座与 4 Tab 导航壳
- **对应设计文档**：[01 信息架构](01-information-architecture.md)、[03 防窥 §2–4](03-privacy-state-machine.md#2-状态模型)、[04 全局手势](04-gestures.md#2-全局手势)、[05 设计系统](05-design-system.md)
- **核心改动**：
  1. **重建 `Theme.kt` 与 Design Tokens**：落地 6 套色板（暖纸松绿、深松暗夜、飞书浅/深、LINUX DO 浅/深）及老板键云文档浅/深色板，补充字阶（`display` 到 `micro` 含等宽数字 `tnum`）、`4dp` 间距/圆角 Token、对比度校验与 Material Symbols Rounded 图标映射规范。
  2. **升级防窥与硬件盲操底座（`Models.kt`, `MainActivity.kt`, `PrivacyTriggerHost.kt`, `BossDocScreen.kt`）**：
     - 状态模型落地 `NORMAL` / `DISGUISE(feishu|linuxdo)` / `BOSS` + `bossReturnTo` + `isBoss` 本地持久化恢复（解决现有仅存内存导致强杀重开失效的问题）；
     - **老板键暗门先行闭环**：同步修改 `BossDocScreen.kt`，将现有 `BackHandler { onExit() }` 改为吞掉单次系统返回、取消左上角 `‹` 直接退出、`2s` 长按文档标题无水波纹暗门退出，避免 Phase 1～5 过渡期间触发老板键露馅或无法退出；
     - **落实音量键透传修正**：首次按下音量上/下键**零延迟正常透传**调节系统音量，仅在 `350–420ms` 窗口内捕获第二次按下并消费触发；
     - **落实 `FLAG_SECURE` 全程保持**：只要开启隐私防截屏，无论进入伪装还是老板键**绝不动态摘挂 Window Flag**；
     - **回前台防窥一次性豁免门控**：新增带超时安全兜底的 `suppressNextBackgroundStealth` 标记，在拉起系统 Photo Picker、文件选择器、系统分享、OAuth 或未知来源 APK 安装器前置位，`onStart()` 消费复位，防止选图/授权返回误触发模糊或老板键；
     - **多任务卡片标题与图标同步**：在 `MainActivity` 启动及切换 `AppIconAlias` 时调用 `setTaskDescription(ActivityManager.TaskDescription(...))` 同步当前别名标题与图标资源，避免多任务视图残留默认应用名；
     - 注册轻晃传感器在进入 `WebShell` 游戏（如鱼鱼谷）时自动静默停用。
  3. **深链与热启动 Intent 管道（`AndroidManifest.xml`, `MainActivity.kt`）**：完善 `ACTION_VIEW` 深链路径匹配，在 `MainActivity` 新增 `incomingIntentFlow` 同时承接 `onCreate(intent)` 与覆写的 `onNewIntent(intent: Intent)`（适配 `singleTask`）。
  4. **清理 `TopbarModuleScanner.kt` 并更新单测**：移除 `/puzzles`、补充 7 路由黑名单，同步修改 `TopbarModuleScannerTest.kt` 验证过滤行为与有效新路由扫描。
  5. **搭建新版 4-Tab `ForHappyShell`**：`图廊 / 消息 / 发现 / 我的`（56dp + nav inset 固定底栏、48dp 单行顶栏、Z0–Z5 层级管理）；过渡期挂载现有 `WallScreen / ChatScreen / HubScreen / MeScreen` 保证工程随时可编译，并将私信未读数初步纳入 `ChatSessionController` 以点亮「消息」Tab 总未读角标。
- **验证标准**：
  - 单元测试覆盖 `PrivacyStealthConfig` 状态切换、`bossReturnTo`、音量键窗口、`suppressNextBackgroundStealth`、`TopbarModuleScannerTest` 新断言；
  - `./gradlew assembleDebug testDebugUnitTest` 通过；真机验证 4-Tab 切换、冷启动老板键恢复、单次返回不退出、标题长按 2s 退出、零延迟音量键手感与最近任务标题跟随。

---

### Phase 2：图廊首页、浏览设置 Sheet、搜索、沉浸看图舞台与帖子详情
- **对应设计文档**：[02a 场景 A](02a-scenario-wall.md)、[02b 场景 B](02b-scenario-post-detail.md)、[04 §3.1–3.2](04-gestures.md#31-图廊)
- **核心改动**：
  1. **极简图廊首页（`02a §1`）**：48dp 单行可收起顶栏（左侧视图摘要胶囊 + 右侧最多 `🔍` `🔔`）、顶栏下 32dp 可关闭单行公告横条、`byDay` 按自然日吸顶分组头（点击拉起「跳到哪天」日历 Sheet）与平滑 `wallPos` 位置恢复。
  2. **干净通透的图廊卡片与三视图**：
     - 双列瀑布流 / 单列大卡 / **沉浸竖翻（`VerticalPager` 替代网页 `hz`，支持右侧竖排拇指操作区与双击红心）**；
     - 严格执行卡片禁止项（**零「已看/✓」角标**），保留长图 `长图 ▤`、轮播 `1/4`、`LIVE`、视频时长、`♡` 直赞、一句话助推胶囊（≤2）与贴纸胶囊（≤3）。
  3. **浏览设置 Bottom Sheet（`02a §2`）与全屏搜索（`02a §3`）**：排序、只看未看、体裁、敏感四档、`maskTap`（原地揭开 / 打开大图）、视图、按日分组开关、我的锚点横滑栏、以及搜索页「帖子 / 标签（含专题图集入口）/ 发图人多选筛选」。
  4. **静默 1s 已看双门控 & 模糊自动复原闭环（`02a §6–7`）**：扩展 `SeenDwellObserver.kt` 中的 `ObserveMemeSeenDwell`，接入 `onGloballyPositioned` 卡片与视口裁剪区交集比例 `>= 0.6f` 以及列表滑动速率 `<= 1500dp/s`，结合图已载入且非模糊才计时 `1000ms` 静默上报；已揭开模糊卡片滑出视口（可见 <50%）、跳页或退后台自动盖回。
  5. **卡片长按快捷菜单、帖子详情 L1 页与零依赖原生视频/实况舞台（`02b`）**：
     - 共享元素入场（280ms）与底部固定拇指互动栏（`说点什么…` / `♡` / `☺` / `「」` / `⋯`）；
     - 严格分层呈现 **四层互动**（赞组成长按气泡 + 贴纸 + `≤12` 显示单位一句话助推面板含 13 预设词及参与名单 + 楼中楼评论支持 `@` / 引用 / 贴纸 / 配图 / AI 小伙伴标志）；
     - **零新增外部依赖播放方案**（不引入 `media3-exoplayer`）：采用 Android 原生 `MediaPlayer + TextureView`（`PlaybackParams().setSpeed()` 支持 `0.5x–2.0x` 倍速、`setVolume` 切换静音；`LIVE` 实况图长按播放动态流），播放源统一经由 `RustNet` 本地代理端口或 `OkHttp` 缓存文件喂入以免绕过加速直连失败；
     - 支持 `打码版 ⇄ 原图` 警戒描边胶囊切换、超长图专用进度条舞台；
     - 内嵌 **多层解密挑战展开卡**（逐层校验、答错轻抖、通关领 D 币/兑换码/隐藏内容）与「更多操作」（多收藏夹行内新建、转发到群聊、编辑帖子、切 NSFW、删除、插锚点）。
- **验证标准**：
  - 单元测试覆盖 `maskTap` / 自动复模糊状态机、视口交集比例与快速滚动阈值判断、收藏夹 & 解密状态流转；
  - `./gradlew assembleDebug testDebugUnitTest` + 真机验证瀑布流/单列/沉浸竖翻滚动流畅度、原生 `MediaPlayer` 倍速静音、实况图长按播放、blur 滑出自动恢复及四层互动全部闭环。

---

### Phase 3：发帖工作台、多图合并/单发托盘、全屏局部打码画板与系统分享
- **对应设计文档**：[02d 场景 D](02d-scenario-compose.md)、[01 §1.3](01-information-architecture.md#13-发帖入口三路)、[04 §3.4](04-gestures.md#34-发帖--打码画板)
- **核心改动**：
  1. **防露馅系统分享与三路发帖入口打通（`AndroidManifest.xml`, `MainActivity.kt`）**：
     - 图廊右下随滚动隐藏/复现 FAB（单击拉起相册、长按出 `图文 / 文字长帖 / 链接分享`）；
     - 在 `AndroidManifest.xml` 中将 `ACTION_SEND` 与 `ACTION_SEND_MULTIPLE`（`image/*`、`video/*`、`text/plain`）声明在 `AliasJinhuan`、`AliasFeishu`、`AliasDocs` 三个 `<activity-alias>` 上（避免系统分享列表泄露「尽欢」真名），由 `MainActivity.incomingIntentFlow` 统一承接分发为 `发到图廊 / 发到群聊 / 发私信`（伪装态下只展示飞书风格的「发送给会话」选择器）；
     - 打通群聊图片 `转为图廊帖子` 预填；拉起相册前触发 `suppressNextBackgroundStealth` 豁免。
  2. **发帖工作台（`02d §1`）**：
     - 剪贴板社交链接自动识别横条；
     - 多图横滑托盘（支持 400ms 长按拖拽调序、`≤20MB` 校验与自动压缩体积预估）；
     - 发布策略分段：`合并一帖` / `每张单发` / `自定义`（每张下方独立 `合 / 单` 开关），发布按钮实时计算 `发布 N 帖`；
     - 文字长帖 Markdown 键盘快捷栏；多层解密谜题编辑器 Sheet；敏感开关与关闭 AI 评论开关；
     - **批量上传与断点补发状态机**：已成功项冻结标为 `已发 No.x`，仅允许重试失败项；离开自动提示保留完整媒体/文案草稿。
  3. **全屏局部打码画板（`02d §2`）**：
     - 黑底画板**左右预留 24dp 边缘安全区**且底部 200dp 内注册 `setSystemGestureExclusionRects`；
     - 支持 `框选`（支持选中后调角与删除）与 `涂抹`（可调粗细）× `马赛克 / 高斯模糊 / 纯黑涂抹` + 撤销/重做 + 双指缩放；
     - **每张独立的「同时保留原图」开关**与自动关联 `#打码` 标签；通过底部 ‹ › 切换托盘多图。
- **验证标准**：
  - 单元测试覆盖批量拆分/自定义分组算法（`合并 + 单发` 混合发帖计算）与打码区域合成逻辑；
  - `./gradlew assembleDebug testDebugUnitTest` + 真机验证切换伪装别名后系统分享面板图标名称变化、相册返回不误触发模糊、边缘不误触返回的手指画板打码及多图补发。

---

### Phase 4：统一「消息」Tab、零跳动群聊/私信、同屏欢欢猜词屋与 3A 全能红包
- **对应设计文档**：[02c 场景 C](02c-scenario-chat.md)、[04 §3.3](04-gestures.md#33-消息--群聊--私信)
- **核心改动**：
  1. **统一「消息」Tab 与状态全收口（`02c §1`, `ChatSessionController.kt`）**：
     - 彻底将私信列表（`DmThread`）、新私信成员搜索（`GET /api/dm/users`）与群聊 `@我` 汇总收口至 `ChatSessionController`，向底栏与伪装壳输出准确的 `totalUnread = groupUnread + dmUnread` 与 `hasUnreadMention`；
     - 列表顶部横滑筛选胶囊（`全部 / 群聊 / 私信 / @我 N / 未读`）、默认群自动置顶、底部「可加入的群 / 申请中」、顶栏 `🔍` 全局搜会话与成员、`✎` 开新私信（**禁用列表左右滑动作，改用长按菜单**）。
  2. **群聊与私信工程级视口锚定（红线 6）**：
     - 基于 `WindowInsetsAnimationCompat` 实现底部输入栏与 `reverseLayout` 消息流逐帧跟随键盘，在底部时新消息不被遮挡，看历史时不拽回底部；
     - 上拉加载更早历史使用稳定楼层 key + 偏移锁定；`around` 上下文定格跳楼（通知/搜索/引用触发）带 `1.2s` 闪光高亮 + 右下 `↓ N 回到最新` 浮标 + 离开期间新增消息顶部提示条；
     - **消息交互**：距右缘 `≥32dp` 左滑消息跟手引用回复、长按弹出表情回应条 + 操作 Sheet（助推 / `+1` / 引用 / 复制 / 保存 / 转图廊 / 编辑 / 撤回）、行内社区表情固定 `1.4em` 占位、书房摘录卡跳转；私信增加 `UTC+8` 日期分割线与末尾 `已读` 状态字样。
  3. **同屏挤压式「欢欢猜词屋」（`02c §4`）**：
     - 群聊顶栏下 **36dp 状态条**（倒计时 `<30s` 变警示色）→ 下拉为可调高 `160dp–50%` 屏高的**挤压式情报板**（`高相关 / 是 / 否 / 其他` 四 Tab）；弹键盘自动折叠为状态条，收起键盘恢复；
     - **输入栏 `[聊天 | 猜 | 问]` 三模式**各自独立持久草稿，`💡要线索` 走猜词专区独立接口（已核实不刷屏主群，无冗余群折叠逻辑）；
     - 全屏猜词完整面板（`本局（含出题人答案密匣长按 1s + 追加 ≤60 线索 + 投票流局）/ 队列 / 历史复盘 / 战绩榜`）及猜中打赏 D 币流程。
  4. **方案 3A 全能红包 & 头衔抽奖（`02c §5`）**：
     - **发红包单页分组表单**：`D 币 / 原生 LDC` 原生配置门槛（直接开 / 复读口令 / 6 种解谜游戏 / **九宫格端侧重采样真马赛克猜图生成器**）+ 定时/专属/口令/CDK + 封面，`原生 LDC` 扣款走无壳 WebView（`credit.linux.do`）授权回落；`HB / Fish` 引导至内置加速 WebView 平台发包并带回群聊；
     - **拆红包居中弹窗 & 结果 Sheet**：支持直接 `開` 金币动效、一键复读、6 种解谜玩法交互 UI、马赛克放大猜图、未抢到小猫插画、`手气最佳` 榜与群红包归档；第三方转发红包（`p.external`）原生弹窗展示快照、提供 `一键复制口令` 并跳内置 WebView 领取；头衔抽奖系统卡一键报名。
- **验证标准**：
  - 单元测试覆盖 `ChatSessionController` 群+私信未读聚合与 `@我` 计算、猜词三草稿隔离、九宫格不可逆马赛克 Bitmap 合成、群消息分段去重衔接、3A 红包解析；
  - `./gradlew assembleDebug testDebugUnitTest` + 真机实测底栏含私信总未读角标、软键盘跟随动画、长列表上拉不跳动、左滑不触发系统返回、同屏猜词与发/拆红包全流程。

---

### Phase 5：「发现」Tab（摸鱼看板 + 无壳 `WebShell` + 书房协作 + 鞭打）与「我的」枢纽 / 通知 / 分层设置
- **对应设计文档**：[02e 场景 E](02e-scenario-me-notifs-settings.md)、[02f 发现与 WebView](02f-discover-and-webview.md)、[04 §3.5](04-gestures.md#35-发现--webview-容器)
- **核心改动**：
  1. **「发现」Tab 与原生工具（`02f`）**：
     - 顶部 **摸鱼看板卡**（下班进度条 + 今日带薪赚取 + 纪念日倒计时 + 今日摸鱼高手未看帖进度一键直跳图廊未看流；可选内置 48dp 静态小站宠待机互动，取消全站悬浮桌宠）；
     - **可编辑「常用」8 格宫格**（鱼鱼谷、站宠、书房、52B 抽卡、街机厅含 `arcade-standby` 冲突保护弹窗、我的创作、鞭打、动态扫描新模块 `NEW`）+ 长按「发现」Tab 快捷环；
     - **原生摸鱼日历 L1 页**（本机考勤薪资设置、3 版式摸鱼日报生成/保存/发群、可选图廊顶栏收益小字）；
     - **原生鞭打路线图 L1 页**（分类/状态筛选、赞/踩投票、站长回复高亮、评论、提交新建议拿 D 币）。
  2. **升级无壳 `WebShell` 与书房原生协作工具条（`02f §4`）**：
     - 注入 CSS 隐藏网页自带顶栏消除「双顶栏」，悬浮半透明胶囊工具条 3s 淡出、顶部下滑唤回；画布类页面底部 200dp 申请边缘返回排除以兼容海岛连划种植；切后台或触发老板键立即静音暂停 WebView；
     - **书房阅读器底部叠加原生协作条**：`💬 群聊`（半屏原生群聊抽屉，保持阅读位置）、`✂ 摘录分享`（接收网页选中段落 ≤1600 字生成原生摘录卡并选群发送）。
  3. **紧凑型「我的」枢纽 & 全屏通知中心 & 分层设置（`02e`）**：
     - `≤260dp` 个人头（支持受限警示条 + 编辑资料）+ 4 格资产行：**D 币钱包**（流水 8 组筛选 / 财富榜 / 红包站密钥）、**52B 集卡图鉴**（联动 WebView 抽卡）、**装扮工作室**（上半屏卡片/群名牌/聊天气泡/回应框 4 处实时预览 + 称号/渐变昵称/6 格徽章柜/气泡皮肤/洁癖屏蔽开关）、**邀请码管理**；
     - 吸顶 **6 类我的内容流**（收藏夹胶囊 CRUD + 喜欢/回应/评论/发布/最近看过，排序与分类内搜索）；
     - **全屏通知中心（9 分类 Tab + 未读筛选 + 置顶公告 + 16 项提醒偏好 `PUT /api/notifs/prefs`）**，全站顶栏铃铛统一呼出；
     - **二级分层设置中心**：把「防窥与摸鱼（含 **10 秒练习模式**）」「网络加速与内核诊断」「关于与更新（启动一次性不阻断 Snackbar + 镜像测速/自定义源收归二级页，拉起 APK 安装前触发 `suppressNextBackgroundStealth`）」「存储与缓存」「AI 小伙伴」「账号与安全」「外观」「管理后台（按权限）」清晰归位。
- **验证标准**：
  - 单元测试覆盖摸鱼计时与薪资计算、装扮状态序列化；
  - `./gradlew assembleDebug testDebugUnitTest` + 真机验证书房摘录分享进群、鱼鱼谷画布连划不冲突、`我的` 首屏高度比例与通知跳转回群聊上下文。

---

### Phase 6：零死角三层防窥壳重构与全量回归验收
- **对应设计文档**：[03 防窥状态机与伪装方案](03-privacy-state-machine.md)、[05 §2.2–2.4 & §7.1](05-design-system.md#22-飞书伪装态)、[06 覆盖矩阵](06-coverage-matrix.md)
- **核心改动**：
  1. **新版飞书 IM 伪装壳（`03 §5.1–5.3`）**：
     - 落地 **`消息 / 通讯录 / 我的` 3-Tab 真功能底栏**（`通讯录` 可真搜成员发起私信，绝对没有假「云文档」Tab）；
     - 一张列表混排 12 轮换图廊工作群 + 本地工作化别名真实群聊 + 真实私信，提供「成员显示为同事姓名」开关与群名备注编辑；
     - **工作群完全复用原生聊天发帖/评论机制**：拆除旧式禁言横幅，底部挂标准飞书输入栏（选图+配文发帖，含飞书皮肤打码画板与合发/单发；左滑或长按「回复」挂引用即评论；长按浮层点表情即点赞与贴纸回应；`☺` 面板含快捷表态助推），点击 `💬 N 条回复 ›` 进入飞书话题（Thread）详情；
     - 真实群聊内的猜词状态条与情报板伪装为飞书「群待办 / 投票表格」，红包隐藏外部品牌仅保留飞书红包样式，全局使用飞书 Toast/断网条措辞与 1.5dp 线性图标，彻底禁绝 `尽欢 / ECH / ms / 松绿 / 摸鱼 / 动漫贴纸原图`。
  2. **新版 LINUX DO 论坛长帖伪装壳（`03 §5.4–5.6`）**：
     - Discourse 主题盖楼流（一帖一楼）、分类色块与标签、长帖底部 `回复主题（盖新楼）` 发帖与单楼 `↩ 回复` 评论、右下角 `12 / 480` 胶囊展开位于右缘 24dp 之外的竖向楼层时间线、右上 `💬` 拉出 Discourse 风格无气泡频道/私信抽屉。
  3. **完全版云文档老板键（`03 §7`）**：
     - 在 Phase 1 暗门与防单返基础上补全完整长文档、右侧目录抽屉、`‹` 假「知识库」多文档列表页、动态近期更新时间、可勾选本地待办与飞书风格模拟评论/分享 Toast，**界面绝无任何退出文字或按钮**。
  4. **全面露馅自检与真机回归**：逐条勾选 [03 §5.7 露馅自检清单](03-privacy-state-machine.md#57-露馅自检清单研发验收) 与 [06 功能覆盖矩阵](06-coverage-matrix.md) 全部条目，并补充单元测试与 UI 状态机测试。
- **验证标准**：
  - 全部单元测试通过；Pixel 真机走查「正常 ↔ 飞书 ↔ L 站 ↔ 云文档老板键」全链路、`0ms` 瞬切无键盘/音频残余、工作群发图/评论闭环、多任务卡片标题一致性、以及全量功能无回归。
