# Vendored upstream

This module contains the Android library sources from
[`bytedance/DanmakuRenderEngine`](https://github.com/bytedance/DanmakuRenderEngine), pinned at
commit `577d3f4e170283a5bdb198af3ca924b8c32d1ab4`.

The upstream source is Apache-2.0 licensed. Its original per-file copyright headers and the
top-level [`LICENSE`](LICENSE) are retained. BiliPai-specific adapter, cursor, reverse-track and
mask changes live in this module so the app layer depends only on
`com.android.purebilibili.danmaku.engine` contracts.
