package com.android.purebilibili.feature.login

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.ui.LoadingAnimation
import com.android.purebilibili.core.ui.SuccessAnimation
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*

@Immutable
internal data class LoginPalette(
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val bgOverlayLeft: Color,
    val bgOverlayRight: Color,
    val orbBlue: Color,
    val orbPurple: Color,
    val orbMint: Color,
    val panelFill: Color,
    val panelStroke: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val segmentTrack: Color,
    val segmentSelected: Color,
    val segmentBorder: Color,
    val segmentSelectedText: Color,
    val segmentUnselectedText: Color,
    val inputFill: Color,
    val inputStroke: Color,
    val inputText: Color,
    val inputPlaceholder: Color,
    val inputIcon: Color,
    val buttonFill: Color,
    val buttonDisabled: Color,
    val buttonText: Color,
    val buttonGradientStart: Color,
    val buttonGradientEnd: Color,
    val link: Color,
    val error: Color,
    val qrShell: Color,
    val closeBg: Color,
    val closeFg: Color,
    val accentStart: Color,
    val accentEnd: Color,
    val accentGlow: Color,
    val success: Color,
    val qrContent: Color,
)

internal fun resolveLoginPalette(
    colorScheme: ColorScheme,
    darkTheme: Boolean
): LoginPalette {
    val accentScrimAlpha = if (darkTheme) 0.28f else 0.16f
    val subtleStrokeAlpha = if (darkTheme) 0.18f else 0.34f
    val disabledAlpha = 0.42f
    val overlayAlpha = if (darkTheme) 0.46f else 0.58f

    return LoginPalette(
        bgTop = if (darkTheme) colorScheme.background else colorScheme.surfaceContainer,
        bgMid = colorScheme.background,
        bgBottom = if (darkTheme) colorScheme.surface else colorScheme.primaryContainer.copy(alpha = 0.34f),
        bgOverlayLeft = colorScheme.primaryContainer.copy(alpha = overlayAlpha),
        bgOverlayRight = Color.Transparent,
        orbBlue = colorScheme.primary.copy(alpha = if (darkTheme) 0.34f else 0.28f),
        orbPurple = colorScheme.tertiary.copy(alpha = if (darkTheme) 0.28f else 0.24f),
        orbMint = colorScheme.secondary.copy(alpha = if (darkTheme) 0.24f else 0.22f),
        panelFill = colorScheme.surface,
        panelStroke = colorScheme.outlineVariant.copy(alpha = subtleStrokeAlpha),
        primaryText = colorScheme.onSurface,
        secondaryText = colorScheme.onSurfaceVariant,
        tertiaryText = colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
        segmentTrack = colorScheme.surfaceVariant.copy(alpha = if (darkTheme) 0.52f else 0.64f),
        segmentSelected = colorScheme.surface.copy(alpha = if (darkTheme) 0.68f else 0.96f),
        segmentBorder = colorScheme.outlineVariant.copy(alpha = subtleStrokeAlpha),
        segmentSelectedText = colorScheme.onSurface,
        segmentUnselectedText = colorScheme.onSurfaceVariant,
        inputFill = colorScheme.surfaceVariant,
        inputStroke = colorScheme.outlineVariant.copy(alpha = subtleStrokeAlpha),
        inputText = colorScheme.onSurface,
        inputPlaceholder = colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        inputIcon = colorScheme.onSurfaceVariant.copy(alpha = 0.86f),
        buttonFill = colorScheme.primary,
        buttonDisabled = colorScheme.primary.copy(alpha = disabledAlpha),
        buttonText = colorScheme.onPrimary,
        buttonGradientStart = colorScheme.primary,
        buttonGradientEnd = colorScheme.tertiary,
        link = colorScheme.primary,
        error = colorScheme.error,
        qrShell = colorScheme.surface.copy(alpha = if (darkTheme) 0.82f else 0.96f),
        closeBg = colorScheme.surfaceVariant.copy(alpha = if (darkTheme) 0.58f else 0.72f),
        closeFg = colorScheme.onSurface,
        accentStart = colorScheme.primary,
        accentEnd = colorScheme.tertiary,
        accentGlow = colorScheme.secondary.copy(alpha = accentScrimAlpha),
        success = colorScheme.secondary,
        qrContent = colorScheme.onPrimary,
    )
}

@Composable
private fun rememberLoginPalette(): LoginPalette {
    val dark = isSystemInDarkTheme()
    val cs = MaterialTheme.colorScheme

    return remember(dark, cs) {
        resolveLoginPalette(cs, dark)
    }
}

@Composable
fun LoginBackground() {
    val palette = rememberLoginPalette()
    val infiniteTransition = rememberInfiniteTransition(label = "login_bg")
    val drift by infiniteTransition.animateFloat(
        initialValue = -16f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(7200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_drift"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.bgTop, palette.bgMid, palette.bgBottom)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-120).dp)
                .size(width = 880.dp, height = 260.dp)
                .graphicsLayer(rotationZ = -7.5f)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            palette.accentStart.copy(alpha = 0.28f),
                            Color.Transparent,
                            palette.accentEnd.copy(alpha = 0.24f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            palette.bgOverlayLeft,
                            palette.bgOverlayRight
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .offset(x = (-132 + drift).dp, y = (-92).dp)
                .size(300.dp)
                .blur(100.dp)
                .background(palette.orbBlue, CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 102.dp, y = 124.dp)
                .size(320.dp)
                .blur(115.dp)
                .background(palette.orbPurple, CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-76 - drift).dp, y = 180.dp)
                .size(220.dp)
                .blur(95.dp)
                .background(palette.orbMint, CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 68.dp, y = (-34).dp)
                .size(180.dp)
                .blur(72.dp)
                .background(palette.accentGlow, CircleShape)
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(34.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val palette = rememberLoginPalette()

    Box(
        modifier = modifier
            .clip(shape)
            .background(palette.panelFill)
            .border(1.dp, palette.panelStroke, shape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            palette.primaryText.copy(alpha = if (isSystemInDarkTheme()) 0.05f else 0.35f),
                            Color.Transparent,
                        )
                    )
                )
        )
        content()
    }
}

@Composable
fun BrandingHeader(isSmall: Boolean = false) {
    val context = LocalContext.current
    val palette = rememberLoginPalette()
    val logoSize = if (isSmall) 58.dp else 82.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = palette.segmentTrack,
            border = BorderStroke(1.dp, palette.segmentBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = CupertinoIcons.Filled.Star,
                    contentDescription = null,
                    tint = palette.buttonGradientStart,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "高画质登录",
                    color = palette.secondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
            modifier = Modifier.size(logoSize),
            shape = RoundedCornerShape(if (isSmall) 18.dp else 26.dp),
            color = palette.segmentSelected,
            border = BorderStroke(1.dp, palette.segmentBorder)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(com.android.purebilibili.R.mipmap.ic_launcher_3d)
                    .crossfade(true)
                    .build(),
                contentDescription = "BiliPai",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "BiliPai 登录",
            color = palette.primaryText,
            fontSize = if (isSmall) 24.sp else 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "安全登录，继续你的观看进度",
            color = palette.secondaryText,
            fontSize = 13.sp
        )
    }
}

@Composable
fun LoginMethodTabs(
    selectedMethod: LoginMethod,
    onMethodChange: (LoginMethod) -> Unit
) {
    val palette = rememberLoginPalette()
    val methods = remember {
        resolveAvailableLoginMethods().map { method ->
            Triple(method, "扫码登录", CupertinoIcons.Filled.Camera)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.segmentTrack)
            .border(1.dp, palette.segmentBorder, RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            methods.forEach { (method, title, icon) ->
                val selected = method == selectedMethod
                val bgBrush = if (selected) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            palette.accentStart.copy(alpha = if (isSystemInDarkTheme()) 0.24f else 0.20f),
                            palette.accentEnd.copy(alpha = if (isSystemInDarkTheme()) 0.26f else 0.16f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
                val fg = if (selected) palette.segmentSelectedText else palette.segmentUnselectedText

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgBrush)
                        .border(
                            width = if (selected) 1.2.dp else 0.dp,
                            color = if (selected) palette.segmentBorder else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onMethodChange(method) }
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = fg,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun QrCodeLoginContent(
    state: LoginState,
    onRefresh: () -> Unit
) {
    val palette = rememberLoginPalette()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "打开哔哩哔哩 App 扫码",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = resolveQrLoginReason(),
            color = palette.secondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LoginPill("推荐", palette)
            LoginPill("4K/HDR", palette)
            LoginPill("快速登录", palette)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(262.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.qrShell)
                .border(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            palette.accentStart.copy(alpha = 0.42f),
                            palette.accentEnd.copy(alpha = 0.42f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    is LoginState.Loading -> LoadingAnimation(size = 46.dp)
                    is LoginState.QrCode -> {
                        Image(
                            bitmap = state.bitmap.asImageBitmap(),
                            contentDescription = "登录二维码",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is LoginState.Scanned -> {
                        Image(
                            bitmap = state.bitmap.asImageBitmap(),
                            contentDescription = "登录二维码",
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.18f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = CupertinoIcons.Filled.Phone,
                                contentDescription = null,
                                tint = palette.success,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "已扫码，请在手机确认",
                                color = palette.qrContent,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    is LoginState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = CupertinoIcons.Filled.ExclamationmarkCircle,
                                contentDescription = null,
                                tint = palette.error,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "二维码加载失败",
                                color = palette.qrContent,
                                fontSize = 13.sp
                            )
                            TextButton(onClick = onRefresh) {
                                Text(text = "重试", color = palette.link)
                            }
                        }
                    }
                    is LoginState.Success -> SuccessAnimation(size = 62.dp)
                    else -> LoadingAnimation(size = 46.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = palette.segmentTrack,
            border = BorderStroke(1.dp, palette.segmentBorder),
            modifier = Modifier.clickable(onClick = onRefresh)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "↻", color = palette.link, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "刷新二维码", color = palette.link, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PhoneLoginContent(
    state: LoginState,
    viewModel: LoginViewModel
) {
    val activity = LocalActivity.current
    val palette = rememberLoginPalette()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var smsCode by rememberSaveable { mutableStateOf("") }
    var selectedRegionCid by rememberSaveable { mutableStateOf(86) }
    var captchaManager by remember { mutableStateOf<CaptchaManager?>(null) }
    var regionMenuExpanded by remember { mutableStateOf(false) }
    val phoneRegions = remember { resolveSupportedPhoneRegions() }
    val selectedRegion = remember(selectedRegionCid, phoneRegions) {
        phoneRegions.firstOrNull { it.cid == selectedRegionCid } ?: phoneRegions.first()
    }
    val phoneEligible = isPhoneEligibleForCaptcha(phoneNumber, selectedRegion)

    DisposableEffect(Unit) {
        onDispose { captchaManager?.destroy() }
    }

    val captchaReady = state is LoginState.CaptchaReady
    val captchaData = (state as? LoginState.CaptchaReady)?.captchaData
    val showCodeInput = state is LoginState.SmsSent || smsCode.isNotEmpty()

    LaunchedEffect(selectedRegionCid) {
        if (phoneNumber.length > selectedRegion.maxDigits) {
            phoneNumber = phoneNumber.take(selectedRegion.maxDigits)
        }
    }

    LaunchedEffect(captchaReady, captchaData, phoneNumber, selectedRegionCid) {
        if (captchaReady && captchaData != null && activity != null && phoneEligible) {
            captchaManager = CaptchaManager(activity)
            captchaManager?.startCaptcha(
                gt = captchaData.geetest?.gt ?: "",
                challenge = captchaData.geetest?.challenge ?: "",
                onSuccess = { validate, seccode, challenge ->
                    viewModel.saveCaptchaResult(validate, seccode, challenge)
                    viewModel.sendSmsCode(
                        phone = phoneNumber,
                        countryCode = selectedRegion.cid
                    )
                },
                onFailed = { error ->
                    android.util.Log.e("PhoneLogin", "Captcha failed: $error")
                }
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "手机号验证登录",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "验证码有效期 5 分钟，同手机号 60 秒内不可重复发送",
            color = palette.secondaryText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.92f)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.segmentTrack,
            border = BorderStroke(1.dp, palette.segmentBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = CupertinoIcons.Filled.Star,
                    contentDescription = null,
                    tint = palette.buttonGradientStart,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "提示：仅扫码登录可解锁更高画质（4K/HDR/1080P60）",
                    fontSize = 12.sp,
                    color = palette.tertiaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountryCodeSelector(
                region = selectedRegion,
                onClick = { regionMenuExpanded = true },
                modifier = Modifier.width(152.dp)
            )
            ModernTextField(
                value = phoneNumber,
                onValueChange = { value ->
                    if (value.length <= selectedRegion.maxDigits && value.all { it.isDigit() }) {
                        phoneNumber = value
                    }
                },
                placeholder = "${selectedRegion.dialingCode} ${selectedRegion.name}手机号",
                icon = CupertinoIcons.Filled.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f)
            )
        }

        DropdownMenu(
            expanded = regionMenuExpanded,
            onDismissRequest = { regionMenuExpanded = false }
        ) {
            phoneRegions.forEach { region ->
                DropdownMenuItem(
                    text = { Text("${region.dialingCode} ${region.name}") },
                    onClick = {
                        selectedRegionCid = region.cid
                        regionMenuExpanded = false
                    }
                )
            }
        }

        if (phoneNumber.isNotBlank() && !phoneEligible) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "号码长度需为 ${selectedRegion.minDigits}-${selectedRegion.maxDigits} 位",
                color = palette.tertiaryText,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(0.96f)
            )
        }

        if (showCodeInput) {
            Spacer(modifier = Modifier.height(12.dp))
            ModernTextField(
                value = smsCode,
                onValueChange = { value ->
                    if (value.length <= 6 && value.all { it.isDigit() }) {
                        smsCode = value
                    }
                },
                placeholder = "6 位短信验证码",
                icon = CupertinoIcons.Filled.Lock,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.segmentTrack,
            border = BorderStroke(1.dp, palette.segmentBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = CupertinoIcons.Filled.Shield,
                    contentDescription = null,
                    tint = palette.buttonGradientStart,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "先完成安全验证，再发送短信验证码",
                    fontSize = 12.sp,
                    color = palette.tertiaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (showCodeInput) {
            ModernButton(
                text = "立即登录",
                onClick = { smsCode.toIntOrNull()?.let(viewModel::loginBySms) },
                enabled = smsCode.length == 6,
                isLoading = state is LoginState.Loading
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    viewModel.getCaptcha()
                },
                enabled = phoneEligible && state !is LoginState.Loading
            ) {
                Text(text = "重新获取验证码", color = palette.link, fontSize = 13.sp)
            }
        } else {
            ModernButton(
                text = "获取验证码",
                onClick = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    viewModel.getCaptcha()
                },
                enabled = phoneEligible,
                isLoading = state is LoginState.Loading
            )
        }

        if (state is LoginState.Error) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.msg,
                color = palette.error,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }
    }
}

@Composable
private fun CountryCodeSelector(
    region: PhoneRegion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberLoginPalette()
    Row(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.inputFill)
            .border(1.dp, palette.inputStroke, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = region.dialingCode,
            color = palette.inputText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = region.name,
            color = palette.inputPlaceholder,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "▾",
            color = palette.inputIcon,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    val palette = rememberLoginPalette()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        singleLine = true,
        textStyle = TextStyle(
            color = palette.inputText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        ),
        cursorBrush = SolidColor(palette.buttonFill),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.inputFill)
                    .border(1.dp, palette.inputStroke, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = palette.segmentTrack
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = palette.inputIcon,
                            modifier = Modifier
                                .size(26.dp)
                                .padding(5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = palette.inputPlaceholder,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            }
        }
    )
}

@Composable
fun ModernButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val palette = rememberLoginPalette()
    val isEnabled = enabled && !isLoading
    val shape = RoundedCornerShape(14.dp)
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            palette.buttonGradientStart,
            palette.buttonGradientEnd
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(if (isEnabled) gradientBrush else SolidColor(palette.buttonDisabled))
            .border(1.dp, palette.panelStroke, shape)
            .clickable(enabled = isEnabled) { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = palette.buttonText,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = palette.buttonText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TopBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberLoginPalette()

    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(palette.closeBg)
                .border(1.dp, palette.segmentBorder, CircleShape)
                .wrapContentHeight(Alignment.CenterVertically)
        ) {
            Text(
                text = "‹",
                color = palette.closeFg,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LoginPill(text: String, palette: LoginPalette) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = palette.segmentTrack,
        border = BorderStroke(1.dp, palette.segmentBorder)
    ) {
        Text(
            text = text,
            color = palette.tertiaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
