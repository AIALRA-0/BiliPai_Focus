# 防止 JNI 方法及其调用目标被混淆或移除。
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep, includedescriptorclasses class androidx.media3.decoder.ffmpeg.FfmpegAudioDecoder {
    private java.nio.ByteBuffer growOutputBuffer(androidx.media3.decoder.SimpleDecoderOutputBuffer, int);
}
