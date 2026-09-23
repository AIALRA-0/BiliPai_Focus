package com.android.purebilibili.core.network.socket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.brotli.dec.BrotliInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.InflaterInputStream

internal const val MAX_LIVE_DANMAKU_FRAME_BYTES = 2 * 1024 * 1024
internal const val MAX_LIVE_DANMAKU_DECOMPRESSED_BYTES = 8 * 1024 * 1024
internal const val MAX_LIVE_DANMAKU_PACKET_BODY_BYTES = 256 * 1024
internal const val MAX_LIVE_DANMAKU_PACKETS_PER_FRAME = 4096
private const val MAX_LIVE_DANMAKU_COMPRESSION_DEPTH = 2

internal fun shouldAcceptLiveDanmakuFrame(sizeBytes: Int): Boolean {
    return sizeBytes in 1..MAX_LIVE_DANMAKU_FRAME_BYTES
}

/**
 * Bilibili 直播弹幕协议解析器
 * 
 * 协议格式 (Big Endian):
 * [0-3]   Packet Length (Header + Body)
 * [4-5]   Header Length (16)
 * [6-7]   Protocol Version
 * [8-11]  Operation
 * [12-15] Sequence ID
 * [16-..] Body
 */
object DanmakuProtocol {
    // 头部长度
    const val HEAD_LENGTH = 16
    
    // 协议版本
    const val PROTO_VER_JSON = 0       // JSON 数据
    const val PROTO_VER_HEARTBEAT = 1  // 心跳/人气值 (Int32)
    const val PROTO_VER_ZLIB = 2       // Zlib 压缩
    const val PROTO_VER_BROTLI = 3     // Brotli 压缩 (目前主流)
    
    // 操作码
    const val OP_HEARTBEAT = 2         // 客户端发送心跳
    const val OP_HEARTBEAT_REPLY = 3   // 服务端回复心跳 (人气值)
    const val OP_MESSAGE = 5           // 通知消息 (弹幕、礼物等)
    const val OP_AUTH = 7              // 认证包
    const val OP_AUTH_REPLY = 8        // 认证响应
    
    /**
     * 数据包结构
     */
    data class Packet(
        val version: Int,
        val operation: Int,
        val sequence: Int = 1,
        val body: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Packet
            if (version != other.version) return false
            if (operation != other.operation) return false
            if (sequence != other.sequence) return false
            if (!body.contentEquals(other.body)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = version
            result = 31 * result + operation
            result = 31 * result + sequence
            result = 31 * result + body.contentHashCode()
            return result
        }
    }

    /**
     * 编码数据包 (Client -> Server)
     */
    fun encode(packet: Packet): ByteArray {
        val totalLength = HEAD_LENGTH + packet.body.size
        val buffer = ByteBuffer.allocate(totalLength).order(ByteOrder.BIG_ENDIAN)
        
        buffer.putInt(totalLength)      // Packet Length
        buffer.putShort(HEAD_LENGTH.toShort()) // Header Length
        buffer.putShort(packet.version.toShort()) // Protocol Version
        buffer.putInt(packet.operation) // Operation
        buffer.putInt(packet.sequence)  // Sequence ID
        
        if (packet.body.isNotEmpty()) {
            buffer.put(packet.body)
        }
        
        return buffer.array()
    }
    
    /**
     * 解码数据包 (Server -> Client)
     * 支持递归解压 (Brotli/Zlib)
     */
    suspend fun decode(data: ByteArray): List<Packet> = withContext(Dispatchers.Default) {
        if (!shouldAcceptLiveDanmakuFrame(data.size) || data.size < HEAD_LENGTH) {
            return@withContext emptyList()
        }

        buildList {
            decodeInto(data = data, compressionDepth = 0, packets = this)
        }
    }

    private fun decodeInto(
        data: ByteArray,
        compressionDepth: Int,
        packets: MutableList<Packet>
    ) {
        if (compressionDepth > MAX_LIVE_DANMAKU_COMPRESSION_DEPTH) return
        if (data.size > MAX_LIVE_DANMAKU_DECOMPRESSED_BYTES) return
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
        
        while (
            buffer.remaining() >= HEAD_LENGTH &&
            packets.size < MAX_LIVE_DANMAKU_PACKETS_PER_FRAME
        ) {
            val position = buffer.position()
            val totalLength = buffer.int
            
            // 校验包长度是否合法
            if (totalLength < HEAD_LENGTH || totalLength > buffer.capacity() - position) {
                // 数据不完整或错误，停止解析
                break
            }
            
            val headLength = buffer.short.toInt()
            val version = buffer.short.toInt()
            val operation = buffer.int
            val sequence = buffer.int

            if (headLength < HEAD_LENGTH || headLength > totalLength) {
                break
            }
            
            val bodyLength = totalLength - headLength
            val extendedHeaderLength = headLength - HEAD_LENGTH
            if (
                bodyLength < 0 ||
                extendedHeaderLength < 0 ||
                extendedHeaderLength + bodyLength > buffer.remaining()
            ) {
                break
            }
            buffer.position(buffer.position() + extendedHeaderLength)

            val maxBodyBytes = if (version == PROTO_VER_ZLIB || version == PROTO_VER_BROTLI) {
                MAX_LIVE_DANMAKU_FRAME_BYTES
            } else {
                MAX_LIVE_DANMAKU_PACKET_BODY_BYTES
            }
            if (bodyLength > maxBodyBytes) {
                buffer.position(buffer.position() + bodyLength)
                continue
            }
            val body = ByteArray(bodyLength)
            buffer.get(body)
            
            when (version) {
                PROTO_VER_JSON, PROTO_VER_HEARTBEAT -> {
                    // 普通数据，直接添加
                    packets.add(Packet(version, operation, sequence, body))
                }
                PROTO_VER_ZLIB -> {
                    // Zlib 解压后递归解析
                    try {
                        val decompressed = decompressZlib(body)
                        decodeInto(decompressed, compressionDepth + 1, packets)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                PROTO_VER_BROTLI -> {
                    // Brotli 解压后递归解析
                    try {
                        val decompressed = decompressBrotli(body)
                        decodeInto(decompressed, compressionDepth + 1, packets)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                else -> {
                    // 未知版本，保留但不处理内容
                    packets.add(Packet(version, operation, sequence, body))
                }
            }
        }
    }
    
    /**
     * Zlib 解压
     */
    private fun decompressZlib(data: ByteArray): ByteArray {
        return InflaterInputStream(ByteArrayInputStream(data)).use(::readLimitedDecompressedBytes)
    }
    
    /**
     * Brotli 解压
     */
    private fun decompressBrotli(data: ByteArray): ByteArray {
        return BrotliInputStream(ByteArrayInputStream(data)).use(::readLimitedDecompressedBytes)
    }

    private fun readLimitedDecompressedBytes(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream(16 * 1024)
        val chunk = ByteArray(8 * 1024)
        var totalBytes = 0
        while (true) {
            val readBytes = input.read(chunk)
            if (readBytes < 0) break
            totalBytes += readBytes
            if (totalBytes > MAX_LIVE_DANMAKU_DECOMPRESSED_BYTES) {
                throw IOException(
                    "Live danmaku payload exceeds $MAX_LIVE_DANMAKU_DECOMPRESSED_BYTES bytes"
                )
            }
            output.write(chunk, 0, readBytes)
        }
        return output.toByteArray()
    }
}
