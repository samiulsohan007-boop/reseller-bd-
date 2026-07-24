package com.example.ui.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun detectPlatformFromUrl(url: String): String {
    val l = url.lowercase().trim()
    return when {
        l.contains("facebook.com") || l.contains("fb.com") || l.contains("fb.watch") || l.contains("m.facebook") -> "FACEBOOK"
        l.contains("youtube.com") || l.contains("youtu.be") -> "YOUTUBE"
        l.contains("tiktok.com") -> "TIKTOK"
        l.contains("t.me") || l.contains("telegram.org") || l.contains("telegram.me") -> "TELEGRAM"
        l.contains("wa.me") || l.contains("whatsapp.com") || l.contains("chat.whatsapp.com") -> "WHATSAPP"
        l.contains("instagram.com") || l.contains("instagr.am") -> "INSTAGRAM"
        l.contains("twitter.com") || l.contains("x.com") -> "TWITTER"
        l.contains("linkedin.com") -> "LINKEDIN"
        l.contains("threads.net") -> "THREADS"
        else -> "WEBSITE"
    }
}

fun getPlatformBrandColor(url: String, platform: String = ""): Color {
    val p = if (platform.isNotBlank() && platform != "AUTO") platform.uppercase() else detectPlatformFromUrl(url)
    return when (p) {
        "FACEBOOK" -> Color(0xFF1877F2)
        "YOUTUBE" -> Color(0xFFFF0000)
        "TIKTOK" -> Color(0xFF111827)
        "TELEGRAM" -> Color(0xFF0088CC)
        "WHATSAPP" -> Color(0xFF25D366)
        "INSTAGRAM" -> Color(0xFFE1306C)
        "TWITTER" -> Color(0xFF1DA1F2)
        "LINKEDIN" -> Color(0xFF0A66C2)
        "THREADS" -> Color(0xFF000000)
        else -> Color(0xFF2563EB)
    }
}

fun getPlatformDisplayName(url: String, platform: String = ""): String {
    val p = if (platform.isNotBlank() && platform != "AUTO") platform.uppercase() else detectPlatformFromUrl(url)
    return when (p) {
        "FACEBOOK" -> "ফেসবুক (Facebook)"
        "YOUTUBE" -> "ইউটিউব (YouTube)"
        "TIKTOK" -> "টিকটক (TikTok)"
        "TELEGRAM" -> "টেলিগ্রাম (Telegram)"
        "WHATSAPP" -> "হোয়াটসঅ্যাপ (WhatsApp)"
        "INSTAGRAM" -> "ইনস্টাগ্রাম (Instagram)"
        "TWITTER" -> "এক্স / টুইটার (X/Twitter)"
        "LINKEDIN" -> "লিঙ্কডইন (LinkedIn)"
        "THREADS" -> "থ্রেডস (Threads)"
        else -> "ওয়েবসাইট / চ্যানেল (Website)"
    }
}

@Composable
fun SocialPlatformLogo(
    url: String = "",
    platform: String = "",
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val detected = remember(url, platform) {
        if (platform.isNotBlank() && platform != "AUTO") platform.uppercase()
        else detectPlatformFromUrl(url)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (detected) {
            "FACEBOOK" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Color(0xFF1877F2))
                    val w = this.size.width
                    val h = this.size.height
                    val p = Path().apply {
                        moveTo(w * 0.62f, h * 0.95f)
                        lineTo(w * 0.62f, h * 0.58f)
                        lineTo(w * 0.75f, h * 0.58f)
                        lineTo(w * 0.77f, h * 0.42f)
                        lineTo(w * 0.62f, h * 0.42f)
                        lineTo(w * 0.62f, h * 0.32f)
                        cubicTo(w * 0.62f, h * 0.27f, w * 0.65f, h * 0.23f, w * 0.72f, h * 0.23f)
                        lineTo(w * 0.78f, h * 0.23f)
                        lineTo(w * 0.78f, h * 0.08f)
                        cubicTo(w * 0.71f, h * 0.07f, w * 0.63f, h * 0.06f, w * 0.55f, h * 0.06f)
                        cubicTo(w * 0.38f, h * 0.06f, w * 0.27f, h * 0.16f, w * 0.27f, h * 0.35f)
                        lineTo(w * 0.27f, h * 0.42f)
                        lineTo(w * 0.15f, h * 0.42f)
                        lineTo(w * 0.15f, h * 0.58f)
                        lineTo(w * 0.27f, h * 0.58f)
                        lineTo(w * 0.27f, h * 0.95f)
                        close()
                    }
                    drawPath(p, color = Color.White)
                }
            }
            "YOUTUBE" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    drawRoundRect(
                        color = Color(0xFFFF0000),
                        cornerRadius = CornerRadius(w * 0.25f, h * 0.25f)
                    )
                    val p = Path().apply {
                        moveTo(w * 0.40f, h * 0.30f)
                        lineTo(w * 0.72f, h * 0.50f)
                        lineTo(w * 0.40f, h * 0.70f)
                        close()
                    }
                    drawPath(p, color = Color.White)
                }
            }
            "TIKTOK" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Color(0xFF111827))
                    val w = this.size.width
                    val h = this.size.height
                    val notePath = Path().apply {
                        moveTo(w * 0.52f, h * 0.20f)
                        cubicTo(w * 0.56f, h * 0.35f, w * 0.68f, h * 0.42f, w * 0.80f, h * 0.43f)
                        lineTo(w * 0.80f, h * 0.56f)
                        cubicTo(w * 0.71f, h * 0.56f, w * 0.62f, h * 0.52f, w * 0.56f, h * 0.46f)
                        lineTo(w * 0.56f, h * 0.68f)
                        cubicTo(w * 0.56f, h * 0.82f, w * 0.44f, h * 0.90f, w * 0.33f, h * 0.88f)
                        cubicTo(w * 0.22f, h * 0.86f, w * 0.15f, h * 0.76f, w * 0.16f, h * 0.65f)
                        cubicTo(w * 0.17f, h * 0.52f, w * 0.28f, h * 0.44f, w * 0.41f, h * 0.46f)
                        lineTo(w * 0.41f, h * 0.59f)
                        cubicTo(w * 0.35f, h * 0.58f, w * 0.30f, h * 0.62f, w * 0.29f, h * 0.67f)
                        cubicTo(w * 0.28f, h * 0.72f, w * 0.32f, h * 0.77f, w * 0.37f, h * 0.77f)
                        cubicTo(w * 0.43f, h * 0.77f, w * 0.47f, h * 0.73f, w * 0.47f, h * 0.67f)
                        lineTo(w * 0.47f, h * 0.20f)
                        close()
                    }
                    drawPath(notePath, color = Color(0xFFFE2C55))
                    drawPath(notePath, color = Color(0xFF00F2FE), blendMode = BlendMode.Screen)
                    drawPath(notePath, color = Color.White)
                }
            }
            "TELEGRAM" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Color(0xFF229ED9))
                    val w = this.size.width
                    val h = this.size.height
                    val p = Path().apply {
                        moveTo(w * 0.20f, h * 0.50f)
                        lineTo(w * 0.78f, h * 0.25f)
                        lineTo(w * 0.68f, h * 0.75f)
                        lineTo(w * 0.48f, h * 0.62f)
                        lineTo(w * 0.40f, h * 0.70f)
                        lineTo(w * 0.40f, h * 0.58f)
                        lineTo(w * 0.65f, h * 0.38f)
                        close()
                    }
                    drawPath(p, color = Color.White)
                }
            }
            "WHATSAPP" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Color(0xFF25D366))
                    val w = this.size.width
                    val h = this.size.height
                    val bubble = Path().apply {
                        addOval(Rect(w * 0.18f, h * 0.18f, w * 0.82f, h * 0.82f))
                        moveTo(w * 0.22f, h * 0.78f)
                        lineTo(w * 0.15f, h * 0.88f)
                        lineTo(w * 0.28f, h * 0.80f)
                    }
                    drawPath(bubble, color = Color.White)
                    drawCircle(color = Color(0xFF25D366), radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.5f))
                    val phone = Path().apply {
                        moveTo(w * 0.42f, h * 0.38f)
                        cubicTo(w * 0.45f, h * 0.38f, w * 0.48f, h * 0.42f, w * 0.48f, h * 0.46f)
                        lineTo(w * 0.48f, h * 0.52f)
                        cubicTo(w * 0.52f, h * 0.56f, w * 0.56f, h * 0.60f, w * 0.60f, h * 0.60f)
                        lineTo(w * 0.66f, h * 0.60f)
                        cubicTo(w * 0.70f, h * 0.60f, w * 0.72f, h * 0.64f, w * 0.70f, h * 0.68f)
                        lineTo(w * 0.62f, h * 0.74f)
                        cubicTo(w * 0.40f, h * 0.74f, w * 0.32f, h * 0.52f, w * 0.32f, h * 0.46f)
                        close()
                    }
                    drawPath(phone, color = Color.White)
                }
            }
            "INSTAGRAM" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
                    )
                    drawRoundRect(
                        brush = gradient,
                        cornerRadius = CornerRadius(w * 0.28f, h * 0.28f)
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.25f, h * 0.25f),
                        size = Size(w * 0.50f, h * 0.50f),
                        cornerRadius = CornerRadius(w * 0.15f, h * 0.15f),
                        style = Stroke(width = w * 0.08f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.12f,
                        center = Offset(w * 0.50f, h * 0.50f),
                        style = Stroke(width = w * 0.07f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.04f,
                        center = Offset(w * 0.62f, h * 0.38f)
                    )
                }
            }
            "TWITTER" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Color(0xFF000000))
                    val w = this.size.width
                    val h = this.size.height
                    val xPath = Path().apply {
                        moveTo(w * 0.28f, h * 0.28f)
                        lineTo(w * 0.46f, h * 0.52f)
                        lineTo(w * 0.28f, h * 0.72f)
                        lineTo(w * 0.35f, h * 0.72f)
                        lineTo(w * 0.50f, h * 0.55f)
                        lineTo(w * 0.65f, h * 0.72f)
                        lineTo(w * 0.72f, h * 0.72f)
                        lineTo(w * 0.54f, h * 0.48f)
                        lineTo(w * 0.72f, h * 0.28f)
                        lineTo(w * 0.65f, h * 0.28f)
                        lineTo(w * 0.50f, h * 0.45f)
                        lineTo(w * 0.35f, h * 0.28f)
                        close()
                    }
                    drawPath(xPath, color = Color.White)
                }
            }
            "LINKEDIN" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    drawRoundRect(
                        color = Color(0xFF0A66C2),
                        cornerRadius = CornerRadius(w * 0.20f, h * 0.20f)
                    )
                    drawRect(color = Color.White, topLeft = Offset(w * 0.25f, h * 0.40f), size = Size(w * 0.10f, h * 0.35f))
                    drawCircle(color = Color.White, radius = w * 0.06f, center = Offset(w * 0.30f, h * 0.30f))
                    val inPath = Path().apply {
                        moveTo(w * 0.45f, h * 0.40f)
                        lineTo(w * 0.55f, h * 0.40f)
                        lineTo(w * 0.55f, h * 0.46f)
                        cubicTo(w * 0.58f, h * 0.41f, w * 0.64f, h * 0.39f, w * 0.70f, h * 0.42f)
                        cubicTo(w * 0.75f, h * 0.45f, w * 0.75f, h * 0.52f, w * 0.75f, h * 0.58f)
                        lineTo(w * 0.75f, h * 0.75f)
                        lineTo(w * 0.65f, h * 0.75f)
                        lineTo(w * 0.65f, h * 0.60f)
                        cubicTo(w * 0.65f, h * 0.54f, w * 0.62f, h * 0.50f, w * 0.57f, h * 0.50f)
                        cubicTo(w * 0.52f, h * 0.50f, w * 0.48f, h * 0.54f, w * 0.48f, h * 0.60f)
                        lineTo(w * 0.48f, h * 0.75f)
                        lineTo(w * 0.45f, h * 0.75f)
                        close()
                    }
                    drawPath(inPath, color = Color.White)
                }
            }
            "THREADS" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Color(0xFF000000))
                    val w = this.size.width
                    val h = this.size.height
                    drawCircle(color = Color.White, radius = w * 0.22f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = w * 0.08f))
                }
            }
            else -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val gradient = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF0284C7)))
                    drawCircle(brush = gradient)
                    drawCircle(color = Color.White, radius = w * 0.30f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = w * 0.06f))
                    drawLine(color = Color.White, start = Offset(w * 0.20f, h * 0.50f), end = Offset(w * 0.80f, h * 0.50f), strokeWidth = w * 0.06f)
                    drawOval(color = Color.White, topLeft = Offset(w * 0.35f, h * 0.20f), size = Size(w * 0.30f, h * 0.60f), style = Stroke(width = w * 0.05f))
                }
            }
        }
    }
}
