# BiliPai JS 插件示例

`tv-live.bilipai.js` 是 BiliPai 原生 JS 插件示例，入口为 `BiliPaiPlugin`，只使用 `BiliPai.http`、`BiliPai.storage` 和 `BiliPai.log`。

旧版 `WidgetMetadata` / `Widget.http` 脚本不是 BiliPai 原生格式。需要先按本目录示例改写为 `BiliPaiPlugin` 后再导入。

## 与 pure_live_TV 等 IPTV 应用互通
`tv-live.bilipai.js` 解析的是标准 M3U/M3U Plus/TXT/JSON 直播源，与 pure_live_TV（`lib/core/iptv/parsers`）等应用吃同一份格式。最佳互通方式是**共享同一个数据源 URL**，无需桥接或移植：

- BiliPai：tv-live 插件参数 `dataSource` 填入源 URL。
- pure_live_TV：设置 → 导入 M3U 源，填入同一 URL。

自 v1.1.0 起，插件已对齐 pure_live_TV 风格的 M3U Plus 字段，确保共享源时 BiliPai 侧分类/台标/线路不劣化：

- `group-title`：优先按源分类归类（央视/卫视/体育/影视/纪录/少儿/音乐/新闻/港澳台/海外），映射不上时回退频道名猜测。可由参数 `preferGroupCategory` 关闭。
- `tvg-id`：作为频道稳定主键（同 `tvg-id` 的多 URL 合并为备用线路），与 pure_live_TV 的 `uniqueKey` 逻辑一致。
- `tvg-name` / `tvg-logo`：优先于频道名逗号后缀和图标库猜测。
- `#EXTGRP`：作为 `group-title` 的回退分组。
- `url-epg` / `x-tvg-url`：从 `#EXTM3U` 头提取 EPG 地址并存入 `BiliPai.storage`（key 为 `epgUrl:<源 URL>`），供后续 EPG 功能复用，本次不展示节目单。
- 协议：支持 `http/https/rtmp/rtmps/rtsp/udp/mms`，与 pure_live_TV `_isValidStreamUrl` 对齐。

不建议把 JS 插件移植成 Dart provider，或为 pure_live_TV 增加 JS 运行时：会引入双份代码维护负担，且 pure_live_TV 原生 IPTV（EPG / 分类 / 搜索 / WebDAV 同步）已比该插件更完整。BiliPai 插件只作为 BiliPai 宿主端的同等能力实现，两边通过标准源格式自然互通。

## `huya-live.bilipai.js` — 虎牙直播平台插件

不同于 `tv-live`（标准 IPTV 源解析），`huya-live` 是**平台直播插件**：直接调用虎牙官方接口拉房间列表，按平台 anticode 算法签名生成可播放的 FLV 流地址。作为 BiliPai 原生 JS 插件交付，复用宿主的 `BiliPaiJsPluginContentScreen`（列表浏览）和 `ExternalMediaPlayerScreen`（带 header 的 ExoPlayer 播放），不引入新 UI。

### 模块

- `loadRooms`：按 `mode`（recommend / category / search）加载房间列表。`category` 模式填 `areaId`（虎牙分区 ID），`search` 模式填 `keyword`，支持 `page` 翻页。
- `loadStream`：根据 `roomId` 解析播放地址。返回 `BiliPaiJsMediaItem`，`streams` 为多条 CDN 线路（FLV，带 `Referer`/`Origin`/`User-Agent`/可选 `Cookie` header），`childItems` 为清晰度选项（原画/高清等，通过 `ratio` 参数切换）。

### 签名实现与维护

虎牙 anticode 采用 MD5+base64（`wsSecret = md5(secretPrefix_calcUid_streamName_secretHash_wsTime)`），与 pure_live_TV `huya_site.dart` 的 `buildAntiCode` 算法一致。MD5 计算使用 WebView 原生 `crypto.subtle.digest("MD5")`（Android WebView Chromium 支持），不依赖手写 MD5 实现，避免位运算/有符号数坑。

**交叉验证**：`app/src/test/.../livesite/huya/HuyaAnticodeTest.kt` 用固定输入（`nowMillis=1700000000000`、`random=0.5`、`wsTime=65e1a2b3`、`presenterUid=777`）锁定完整输出串，该期望值由同输入下的 JS 端（Node + crypto.createHash）产出，两端逐字节一致。虎牙旋转 anticode 算法时，更新此插件 + 期望值即可，无需发新 app。

### 已知限制

- 无弹幕：`ExternalMediaPlayerScreen` 是裸播放器，未接 `LiveDanmakuOverlay`。虎牙 WebSocket 弹幕协议需后续给 JS 运行时加 WebSocket 桥。
- UX 为扁平列表 + 参数表单翻页，不是 pure_live_TV 的屏内标签切换。可接受；要增强需改 `BiliPaiJsPluginContentScreen`（核心 UI）。
- 播放地址有效性取决于虎牙 CDN 是否接受签名后的 URL，需设备实测确认。
