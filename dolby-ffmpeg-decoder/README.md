# Dolby FFmpeg Decoder

该模块基于 AndroidX Media3 `1.10.1` 的官方 `decoder_ffmpeg` 扩展源码，内置仅面向
`arm64-v8a` 的 FFmpeg `6.0` LGPL 构建。构建配置只显式启用 `eac3`；FFmpeg 会同时启用
其依赖的 `ac3` 解码器，不包含视频解码器和其他音频解码器。

预编译 JNI 库使用 Android NDK r26b、API 26 构建，并启用 16 KiB ELF 页面对齐。
AndroidX Media 源码遵循 Apache License 2.0，FFmpeg 静态库与最终 JNI 库遵循
LGPL 2.1 or later；完整许可证文本位于 `licenses/`。

复现原生库时，应从 AndroidX Media `1.10.1` 的 `libraries/decoder_ffmpeg` 目录和
FFmpeg `release/6.0` 源码开始，仅向官方 `build_ffmpeg.sh` 传入 `eac3` 解码器。
