# Bilibili 直播 API 调研与 BiliPai 优化建议

整理时间：2026-04-28

资料来源：`/Users/yiyang/Desktop/bilibili-API-collect`。本文件只基于本地 API 文档归纳，未进行线上接口探测。

## 相关文档范围

### 核心直播文档

`docs/live/` 下共有 19 份直播文档，均与直播功能直接相关：

| 文档 | 主题 | 对 BiliPai 的价值 |
| --- | --- | --- |
| `docs/live/info.md` | 房间基础信息、短号转真实房间号、主播信息、批量状态、`getRoomPlayInfo` | 播放入口、房间状态、竖屏判断、画质/线路选择 |
| `docs/live/live_stream.md` | 旧版 `Room/playUrl` 直播流 | 画质描述与旧接口兜底 |
| `docs/live/message_stream.md` | WebSocket 信息流、包格式、所有下行 `cmd` | 实时弹幕、SC、礼物、房间状态、重连与播放 URL 刷新 |
| `docs/live/danmaku.md` | 弹幕配置、历史弹幕、弹幕样式、发送弹幕 | 弹幕发送能力、登录态能力降级、历史预加载 |
| `docs/live/emoticons.md` | 直播间表情包 | 弹幕表情渲染和输入面板 |
| `docs/live/follow_up_live.md` | 关注 UP 直播情况 | 关注直播页、首页直播提醒 |
| `docs/live/recommend.md` | 直播推荐 | 热门直播/推荐列表 |
| `docs/live/live_area.md` | 直播分区 | 分区页和筛选 |
| `docs/live/report.md` | Web 端观看心跳 | 观看时长、推荐质量、减少异常断流风险 |
| `docs/live/gift.md` | 礼物面板、盲盒概率 | 礼物展示、送礼消息语义 |
| `docs/live/guard.md` | 大航海、粉丝团成员 | 用户身份、粉丝团/舰长标识增强 |
| `docs/live/user.md` | 粉丝勋章、佩戴勋章、贡献榜、观看时长、用户卡片 | 用户面板、勋章、贡献榜、观看时长 |
| `docs/live/redpocket.md` | 人气红包 | 红包提示与状态 |
| `docs/live/live_vote.md` | 直播投票 | 投票面板与主播侧状态 |
| `docs/live/live_replay.md` | 直播回放、切片、高光 | 后续支持回放/高光入口 |
| `docs/live/manage.md` | 开播、关播、房间信息、公告、直播姬版本 | 主播管理能力，普通客户端低优先级 |
| `docs/live/silent_user_manage.md` | 禁言、查询禁言列表、解除禁言 | 房管/主播管理能力 |
| `docs/live/live_bill.md` | 礼物流水 | 主播收益/礼物流水，普通客户端低优先级 |
| `docs/live/live_data.md` | 场次数据、直播表现 | 主播数据看板，普通客户端低优先级 |

### 交叉文档

| 文档 | 相关点 |
| --- | --- |
| `docs/dynamic/content.md` | `w_live_users` 可获取正在直播的已关注者，适合动态页或首页轻量提醒 |
| `docs/comment/readme.md` | 评论区类型 `8` 为直播活动，`10` 为直播公告 |
| `docs/opus/features.md` | 图文/动态卡片中 `LINK_CARD_TYPE_LIVE` 的直播卡片模型 |
| `docs/broadcast/readme.md`、`docs/broadcast/video_room.md` | 主站广播协议参考；直播已由 `docs/live/message_stream.md` 覆盖，主要作协议对照 |
| `grpc_api/bilibili/live/app/room/v1/room.proto` | `GetStudioList`，返回多主播/工作室列表 |
| `grpc_api/bilibili/live/general/interfaces/v1/interfaces.proto` | `GetOnlineRank`，返回在线榜、粉丝勋章和大航海等级 |

## 核心接口按功能分组

### 播放和房间状态

| 能力 | 接口 | 要点 |
| --- | --- | --- |
| 短号/真实房间号、直播状态 | `GET /room/v1/Room/room_init?id=` | 返回 `room_id`、`short_id`、`uid`、`live_status`、`live_time`、`is_portrait` |
| 基础房间信息 | `GET /room/v1/Room/get_info?room_id=` | 返回标题、在线数、关注数、分区、开播状态 |
| Web 房间基础信息 | `GET /xlive/web-room/v1/index/getRoomBaseInfo` | 可批量拿房间基础状态 |
| 播放流主接口 | `GET /xlive/web-room/v2/index/getRoomPlayInfo` | 支持 `protocol=0,1`、`format=0,1,2`、`codec=0,1`、`qn`、`only_audio=1` |
| 旧播放流接口 | `GET /room/v1/Room/playUrl?cid=&qn=&platform=` | 返回 `durl` 和 `quality_description`，适合作为兜底和画质说明来源 |
| 批量直播状态 | `GET/POST /room/v1/Room/get_status_info_by_uids` | 适合列表页批量刷新关注 UP 是否开播 |

清晰度代码：`30000` 杜比、`20000` 4K、`10000` 原画、`400` 蓝光、`250` 超清、`150` 高清、`80` 流畅。当前项目默认请求 `10000`，但 API 默认是 `150`；UI 应以接口返回的 `g_qn_desc`/`accept_qn` 为准。

播放流选择建议：

- 优先 `http_hls`，格式优先 `fmp4/ts`，再回退 `http_stream/flv`。
- 同一 `codec` 下保留所有 `url_info` 线路，播放失败时切下一个 host，不必重新请求接口。
- HEVC (`codec=1`) 应受设备解码能力和用户设置控制；低端设备默认 AVC 更稳。
- `only_audio=1` 可用于后台纯音频/锁屏省流模式。

### 实时信息流

入口：`GET /xlive/web-room/v1/index/getDanmuInfo`

文档注明 2025-05-26 起强制 Wbi 签名，2025-06-27 起要求 Cookie 中 `buvid3` 非空。未认证可连接，但会触发隐私降级，部分弹幕/进场消息的用户 mid 为 `0`、用户名被打码。

返回字段：

- `token`：WebSocket 认证 key。
- `host_list`：可用节点，含 `host`、`port`、`ws_port`、`wss_port`。
- 建议连接：`wss://{host}:{wss_port}/sub`。

协议要点：

| 偏移 | 含义 |
| --- | --- |
| `0..3` | 包总长度，大端 uint32 |
| `4..5` | 头长度，一般 16 |
| `6..7` | 协议版本，`0` JSON，`1` 心跳/认证，`2` zlib，`3` brotli |
| `8..11` | 操作码，`2` 心跳，`3` 心跳回复，`5` 普通消息，`7` 认证，`8` 认证回复 |
| `12..15` | sequence |

认证包字段：`uid`、`roomid`、`protover`、`platform=web`、`type=2`、`key`。心跳约 30 秒一次；服务器心跳回复 body 前 4 字节是人气值。

对 BiliPai 重要的 `cmd`：

| 类别 | `cmd` | 用途 |
| --- | --- | --- |
| 聊天 | `DANMU_MSG`、`DM_INTERACTION`、`INTERACT_WORD`、`INTERACT_WORD_V2`、`RECALL_DANMU_MSG` | 弹幕、回复、进场/关注、撤回 |
| 醒目留言 | `SUPER_CHAT_MESSAGE`、`SUPER_CHAT_MESSAGE_JPN`、`SUPER_CHAT_MESSAGE_DELETE`、`SUPER_CHAT_ENTRANCE` | SC 列表、删除同步、入口状态 |
| 礼物/舰长 | `SEND_GIFT`、`COMBO_SEND`、`SPECIAL_GIFT`、`GUARD_BUY`、`USER_TOAST_MSG` | 礼物条、连击、上舰提示 |
| 房间状态 | `LIVE`、`PREPARING`、`ROOM_CHANGE`、`ROOM_REAL_TIME_MESSAGE_UPDATE`、`PLAYURL_RELOAD`、`CHANGE_ROOM_INFO` | 开播/下播、标题/封面变化、播放源刷新 |
| 在线与排行 | `WATCHED_CHANGE`、`ONLINE_RANK_COUNT`、`ONLINE_RANK_V2`、`ONLINE_RANK_V3`、`POPULAR_RANK_CHANGED`、`HOT_RANK_CHANGED` | 看过人数、高能榜、人气/热门榜 |
| 红包/活动 | `POPULARITY_RED_POCKET_START`、`POPULARITY_RED_POCKET_NEW`、`POPULARITY_RED_POCKET_WINNER_LIST`、`ANCHOR_LOT_*`、`LIVE_MULTI_VIEW_NEW_INFO` | 红包、天选、多视角 |
| 管理/安全 | `WARNING`、`CUT_OFF`、`CUT_OFF_V2`、`ROOM_SILENT_ON`、`ROOM_SILENT_OFF`、`ROOM_BLOCK_MSG`、`ROOM_ADMINS` | 警告、切断、禁言、房管变化 |

### 弹幕、表情和互动

| 能力 | 接口 | 备注 |
| --- | --- | --- |
| 可发送弹幕配置 | `GET /xlive/web-room/v1/dM/GetDMConfigByGroup` | 登录后可获得更多颜色/模式；未登录仅白色滚动 |
| 历史弹幕 | `GET /xlive/web-room/v1/dM/gethistory?roomid=` | 可作为进入直播间首屏预填充 |
| 设置弹幕样式 | `POST /xlive/web-room/v1/dM/AjaxSetConfig` | 需要 `SESSDATA`/CSRF |
| 发送弹幕 | `POST /msg/send` | 字段含 `roomid`、`msg`、`color`、`fontsize`、`mode`、`rnd`、`csrf` |
| 表情包 | `GET /xlive/web-ucenter/v2/emoticon/GetEmoticons?platform=pc&room_id=` | 用于文本中的 `[热]` 等表情替换，也可做输入面板 |
| 点赞上报 | 文档未收录，但项目已用 `/xlive/web-ucenter/v1/like/like_report_v3` | 保留静默失败策略 |
| 屏蔽用户 | 项目用 `/liveact/shield_user` | 文档中另有房管禁言 API，语义不同 |

### 列表、分区、关注和搜索

| 能力 | 接口 | 备注 |
| --- | --- | --- |
| 推荐列表 | `GET /xlive/web-interface/v1/webMain/getMoreRecList` | 可补齐首页推荐流 |
| 分区列表 | `GET /room/v1/Area/getList` | 两级分区：父分区和子分区 |
| 分区房间 | 文档外项目已用 `/room/v3/area/getRoomList` 和 `/xlive/web-interface/v1/second/getList` | 当前实现已有兜底 |
| 关注 UP 直播情况 | `GET /xlive/web-ucenter/user/following` | `hit_ab=true` 时可拿完整正在直播列表；`page_size` 文档写有效值 1-10 |
| PC 正在直播关注列表 | `GET /xlive/web-ucenter/v1/xfetter/GetWebList` | `hit_ab` 会影响 `rooms/list/count` 字段 |
| 动态正在直播关注者 | `GET /dynamic_svr/v1/dynamic_svr/w_live_users` | 更轻量，适合动态页角标 |
| 搜索直播间 | 项目已用主站搜索 `search_type=live_room` | 与直播文档无直接对应 |

### 用户、榜单、粉丝团和管理

| 能力 | 接口 | 备注 |
| --- | --- | --- |
| 粉丝勋章列表 | `GET /xlive/app-ucenter/v1/user/GetMyMedals` | 可展示/切换佩戴勋章 |
| 佩戴勋章 | `POST /xlive/web-room/v1/fansMedal/wear` | 需要登录 |
| 贡献榜 | `GET /xlive/general-interface/v1/rank/getOnlineGoldRank` | 文档有普通贡献榜，项目另用 `queryContributionRank` |
| 观看时长 | `GET /xlive/general-interface/v1/guard/GuardActive` | 可显示自己在房间的观看时长 |
| 用户直播间卡片 | `GET /xlive/app-ucenter/v2/card/user` | 点击弹幕用户名后的用户面板 |
| 大航海列表 | `GET /xlive/app-room/v2/guardTab/topListNew` | 需要 `roomid` 和主播 `ruid` |
| 粉丝团成员 | `GET /xlive/general-interface/v1/rank/getFansMembersRank` | 需要主播 `ruid` |
| 禁言管理 | `POST /xlive/web-ucenter/v1/banned/AddSilentUser`、`GetSilentUserList`、`del_room_block_user` | 仅房管/主播有意义 |

### 回放、活动和主播侧能力

普通观看端可先低优先级处理：

- 回放/切片：`docs/live/live_replay.md` 覆盖回放列表、切片流、高光保存、发布、下载流程。
- 红包：`getLotteryInfoWeb` 可判断红包状态；实时流有 `POPULARITY_RED_POCKET_*`。
- 投票：`votePanel`、`voteHistory`、`createVote`、`terminateVote`；观看端优先只做投票展示。
- 主播管理：开通直播间、更新房间信息、开始/关闭直播、公告、直播姬版本。
- 主播数据/流水：场次数据、直播表现、礼物流水。

## BiliPai 当前覆盖情况

已覆盖：

- 列表与分区：`getLiveList`、`getLiveAreaList`、`getLiveSecondAreaList`、关注直播 `following`。
- 房间加载：`room_init`、`getRoomInfo`、`getInfoByRoom`、`getH5InfoByRoom` 多路 fallback。
- 播放流：主用 `getRoomPlayInfo`，旧 `playUrl` 作为画质描述和兜底；已有多候选 URL 重载逻辑。
- 实时弹幕：`getDanmuInfo` + Wbi 版本、WebSocket、zlib/brotli 解码、30 秒心跳、失败重连。
- 聊天渲染：`DANMU_MSG`、`SUPER_CHAT_MESSAGE`、`WATCHED_CHANGE`、`ONLINE_RANK_COUNT`、`ROOM_CHANGE`。
- 互动：发送弹幕、点赞、表情映射、历史弹幕预加载、SC 预加载、贡献榜、屏蔽用户。

主要缺口：

- 实时 `cmd` 覆盖偏少，礼物、上舰、进场、下播、播放链接刷新、禁言/房管、红包、撤回等消息未进入 UI 状态。
- `getDanmuInfo` 的 Wbi 和 `buvid3` 要求应作为强约束：未登录或缺少 `buvid3` 时要明确进入匿名降级状态。
- 播放源选择还可以更细：按设备能力选择 AVC/HEVC，按 `accept_qn` 校正请求画质，按 `url_info.stream_ttl` 或 `PLAYURL_RELOAD` 主动刷新。
- 关注直播页当前为了在线人数逐个请求 `get_info`，容易产生 N+1 请求；可改用列表字段、批量状态接口或 `GetWebList`/`w_live_users`。
- 用户面板能力不足：点击弹幕用户可补 `card/user`、粉丝勋章、观看时长、屏蔽/举报/禁言入口区分。
- 观看心跳 `webHeartBeat` 未接入；这可能影响观看时长、推荐与部分房间统计。

## 优化路线

### P0：稳定播放与连接

1. `getDanmuInfo` 统一走 Wbi 签名参数，确保 `web_location`、`wts`、`w_rid` 完整；缺 `buvid3` 时在 UI/日志里标记匿名降级。
2. 处理 `PLAYURL_RELOAD`、`PREPARING`、`LIVE`、`CUT_OFF`、`WARNING`：
   - `PLAYURL_RELOAD`：无感刷新当前画质播放源。
   - `PREPARING`：切到“主播暂未开播/已下播”状态并停止无效重试。
   - `LIVE`：从未开播状态自动重拉播放流。
   - `CUT_OFF/WARNING`：展示房间被切断或警告，而不是泛化成播放错误。
3. 播放候选排序策略化：HLS 优先，AVC/HEVC 根据设备能力，保留同画质所有 CDN host，失败只切线不重建全状态。
4. 接入 `webHeartBeat`，按返回 `next_interval` 调度；进入后台、PIP、音频模式时保持一致策略。

### P1：提升直播间信息完整度

1. 扩展实时消息：
   - `SEND_GIFT`/`COMBO_SEND`/`GUARD_BUY`：礼物与上舰提示。
   - `INTERACT_WORD(_V2)`：进场、关注、分享等轻量系统消息。
   - `SUPER_CHAT_MESSAGE_DELETE`：删除已显示 SC。
   - `RECALL_DANMU_MSG`：撤回聊天区对应弹幕。
   - `ROOM_REAL_TIME_MESSAGE_UPDATE`、`CHANGE_ROOM_INFO`：同步标题、封面、关注数等。
2. 进入直播间后加载 `GetDMConfigByGroup`，发送面板根据可用颜色/模式动态展示，未登录只显示白色滚动。
3. 弹幕 `extra` 中的 `emots` 优先于全局表情映射，减少表情缺图。
4. 高能榜优先使用当前 `queryContributionRank`，同时评估 GRPC `GetOnlineRank` 作为移动端模型补充。

### P2：列表和发现页效率

1. 关注直播页避免逐房间 `get_info`：优先使用 `following` 的 `watched_show/text_small/popularity`，需要批量刷新时用 `get_status_info_by_uids`。
2. 首页直播推荐可接入 `getMoreRecList`；分区页继续保留 `second/getList` 和 `room/v3/area/getRoomList` 双路径。
3. 动态页直播提醒用 `w_live_users`，比完整关注直播页更轻。
4. 列表卡片优先使用 `keyframe`，其次 `user_cover/room_cover/cover/face`，并对竖屏直播用 `is_portrait` 调整封面比例。

### P3：扩展体验

1. 红包和天选：REST 拉初始状态，实时 `POPULARITY_RED_POCKET_*`、`ANCHOR_LOT_*` 更新 UI。
2. 用户面板：接入 `card/user`、`GuardActive`、粉丝勋章信息；普通用户显示屏蔽/举报，房管再显示禁言。
3. 回放入口：主播或房间支持时展示 `GetOtherSliceList`/高光片段。
4. 多视角：监听 `LIVE_MULTI_VIEW_NEW_INFO`，后续可做视角切换入口。

## 建议落点

| 文件/模块 | 建议 |
| --- | --- |
| `app/src/main/java/com/android/purebilibili/data/repository/LiveRepository.kt` | 增加心跳、DM 配置、用户卡片、红包、礼物/舰长数据模型；把播放候选排序抽成 policy |
| `app/src/main/java/com/android/purebilibili/core/network/socket/LiveDanmakuClient.kt` | 支持 host fallback、认证失败分类、按服务端能力选择 `protover=3` 或 `2` |
| `app/src/main/java/com/android/purebilibili/feature/live/LivePlayerViewModel.kt` | 扩展 `cmd` 分发，处理 `PLAYURL_RELOAD/PREPARING/LIVE/CUT_OFF`，避免所有消息落到聊天流 |
| `app/src/main/java/com/android/purebilibili/feature/live/components/LiveChatSection.kt` | 支持礼物、进场、撤回、SC 删除、红包等不同消息类型的轻量渲染 |
| `app/src/main/java/com/android/purebilibili/feature/live/LiveFollowingScreen.kt` | 去掉逐房间在线数请求，改用列表字段或批量接口 |
| `app/src/test/java/com/android/purebilibili/feature/live/` | 增加播放候选排序、`cmd` 分发、下播/刷新/切断状态、关注列表批量策略测试 |

## 后续实现顺序建议

1. 先做 `cmd` 分发 policy 和单元测试，不直接扩大 UI 复杂度。
2. 再接 `PLAYURL_RELOAD/PREPARING/LIVE/CUT_OFF`，优先解决播放稳定性。
3. 然后优化关注直播页 N+1 请求。
4. 最后补礼物、上舰、红包、用户面板等体验增强。
