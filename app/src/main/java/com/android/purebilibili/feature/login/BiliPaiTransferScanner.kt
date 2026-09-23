package com.android.purebilibili.feature.login

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

/** CameraX scanner used by both halves of the direct QR transfer handshake. */
@Composable
fun BiliPaiTransferScanner(
    onCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCode by rememberUpdatedState(onCode)
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!granted) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Text("需要摄像头权限才能扫描 BiliPai 传输二维码", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    DisposableEffect(lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            var lastCode: String? = null
            var lastDeliveredAt = 0L
            analysis.setAnalyzer(executor) { image ->
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - lastDeliveredAt >= 700L) {
                    val bytes = image.toNv21()
                    if (bytes != null) {
                        BiliPaiQrDecoder.decode(bytes, image.width, image.height, image.imageInfo.rotationDegrees)?.let {
                            if (it != lastCode || now - lastDeliveredAt >= 700L) {
                                lastCode = it
                                lastDeliveredAt = now
                                currentOnCode(it)
                            }
                        }
                    }
                }
                image.close()
            }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }
        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier.fillMaxSize())
}

private fun androidx.camera.core.ImageProxy.toNv21(): ByteArray? {
    val yPlane = planes.getOrNull(0) ?: return null
    val uPlane = planes.getOrNull(1) ?: return null
    val vPlane = planes.getOrNull(2) ?: return null
    val output = ByteArray(width * height * 3 / 2)
    var offset = 0
    val yBuffer = yPlane.buffer
    for (row in 0 until height) {
        yBuffer.position(row * yPlane.rowStride)
        var col = 0
        while (col < width && yBuffer.hasRemaining()) {
            output[offset++] = yBuffer.get()
            col++
        }
    }
    val chromaHeight = height / 2
    val chromaWidth = width / 2
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer
    for (row in 0 until chromaHeight) {
        for (col in 0 until chromaWidth) {
            val uIndex = row * uPlane.rowStride + col * uPlane.pixelStride
            val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
            if (uIndex >= uBuffer.limit() || vIndex >= vBuffer.limit() || offset + 1 >= output.size) return null
            output[offset++] = vBuffer.get(vIndex)
            output[offset++] = uBuffer.get(uIndex)
        }
    }
    return output
}
