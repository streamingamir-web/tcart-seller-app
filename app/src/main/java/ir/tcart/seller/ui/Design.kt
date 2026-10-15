package ir.tcart.seller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.tcart.seller.R
import ir.tcart.seller.data.DayPoint
import ir.tcart.seller.data.Fmt
import kotlin.math.roundToInt

// ───────────────────────── فونت و تایپوگرافی ─────────────────────────

internal val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
)

internal val SiteTypography = Typography(
    displayLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.ExtraBold),
    displayMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.ExtraBold),
    displaySmall = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.ExtraBold),
    headlineMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium),
)

// ───────────────────────── پالت رنگ ─────────────────────────

internal class Palette(
    val ink: Color,
    val muted: Color,
    val cyan: Color,
    val blueDeep: Color,
    val page: Color,
    val cardTop: Color,
    val cardBottom: Color,
    val heroTop: Color,
    val heroBottom: Color,
    val line: Color,
    val heroLine: Color,
    val mint: Color,
    val amber: Color,
)

internal val DarkStudioPalette = Palette(
    ink = Color(0xFFF8FAFC),
    muted = Color(0xFF94A3B8),
    cyan = Color(0xFF22D3EE),
    blueDeep = Color(0xFF2563EB),
    page = Color(0xFF050A12),
    cardTop = Color(0xFF10182A),
    cardBottom = Color(0xFF090D18),
    heroTop = Color(0xFF0F2038),
    heroBottom = Color(0xFF080D1A),
    line = Color(0xFF1E2D45),
    heroLine = Color(0xFF1E4E68),
    mint = Color(0xFF34D399),
    amber = Color(0xFFFBBF24),
)

internal val LightStudioPalette = Palette(
    ink = Color(0xFF0C1A2E),
    muted = Color(0xFF475569),
    cyan = Color(0xFF0EA5E9),
    blueDeep = Color(0xFF2563EB),
    page = Color(0xFFF4F7FB),
    cardTop = Color(0xFFFFFFFF),
    cardBottom = Color(0xFFF8FBFF),
    heroTop = Color(0xFFEFF6FF),
    heroBottom = Color(0xFFFFFFFF),
    line = Color(0xFFD8E6FA),
    heroLine = Color(0xFF93C5FD),
    mint = Color(0xFF059669),
    amber = Color(0xFFD97706),
)

internal val LocalPalette = staticCompositionLocalOf { DarkStudioPalette }

/** پیام کوتاه (تأیید/خطا) به‌صورت سراسری؛ از روی ViewModel مقداردهی می‌شود. */
internal val LocalNotify = staticCompositionLocalOf<(String, Boolean) -> Unit> { { _, _ -> } }

internal val Ink: Color @Composable get() = LocalPalette.current.ink
internal val Muted: Color @Composable get() = LocalPalette.current.muted
internal val Cyan: Color @Composable get() = LocalPalette.current.cyan
internal val BlueDeep: Color @Composable get() = LocalPalette.current.blueDeep
internal val Page: Color @Composable get() = LocalPalette.current.page
internal val CardTop: Color @Composable get() = LocalPalette.current.cardTop
internal val CardBottom: Color @Composable get() = LocalPalette.current.cardBottom
internal val HeroTop: Color @Composable get() = LocalPalette.current.heroTop
internal val HeroBottom: Color @Composable get() = LocalPalette.current.heroBottom
internal val Line: Color @Composable get() = LocalPalette.current.line
internal val HeroLine: Color @Composable get() = LocalPalette.current.heroLine
internal val Mint: Color @Composable get() = LocalPalette.current.mint
internal val Amber: Color @Composable get() = LocalPalette.current.amber

internal val Danger = Color(0xFFF43F5E)
internal val Violet = Color(0xFF8B5CF6)

internal val BrandColors = listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF2563EB))

internal val ProductGradients = listOf(
    listOf(Color(0xFF0EA5E9), Color(0xFF2563EB)),
    listOf(Color(0xFF10B981), Color(0xFF0D9488)),
    listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)),
    listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
    listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
)

// ───────────────────────── زمینه و لوگو ─────────────────────────

@Composable
internal fun AuroraBackdrop(content: @Composable () -> Unit) {
    val pal = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(pal.page)
            .background(
                Brush.radialGradient(
                    colors = listOf(pal.cyan.copy(alpha = 0.14f), pal.mint.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(900f, 80f),
                    radius = 1100f,
                ),
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(pal.blueDeep.copy(alpha = 0.11f), Color.Transparent),
                    center = Offset(120f, 1600f),
                    radius = 950f,
                ),
            ),
    ) {
        content()
    }
}

@Composable
internal fun BrandLogo(size: Int = 84) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size((size + 46).dp)
                .background(
                    Brush.radialGradient(
                        listOf(Mint.copy(alpha = 0.30f), Cyan.copy(alpha = 0.18f), Color.Transparent),
                    ),
                ),
        )
        Image(
            painter = painterResource(R.drawable.tcart_logo),
            contentDescription = "تیکارت",
            modifier = Modifier
                .size(size.dp)
                .shadow(20.dp, RoundedCornerShape((size / 4).dp), spotColor = Mint)
                .clip(RoundedCornerShape((size / 4).dp))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape((size / 4).dp)),
        )
    }
}

// ───────────────────────── کارت‌ها و پیل‌ها ─────────────────────────

@Composable
internal fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = Cyan.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
            .border(1.dp, Line, RoundedCornerShape(22.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
    ) { content() }
}

@Composable
internal fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Cyan.copy(alpha = 0.24f))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(HeroTop, HeroBottom)))
            .border(1.dp, HeroLine, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) { content() }
}

@Composable
internal fun SitePill(text: String, color: Color, onClick: (() -> Unit)? = null) {
    val mod = Modifier
        .clip(RoundedCornerShape(99.dp))
        .background(color.copy(alpha = 0.14f))
        .border(1.dp, color.copy(alpha = 0.36f), RoundedCornerShape(99.dp))
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(horizontal = 10.dp, vertical = 4.dp)
    Box(mod) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
internal fun IconPill(icon: ImageVector, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    val resolved = if (tint == Color.Unspecified) Ink else tint
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardTop)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = resolved, modifier = Modifier.size(18.dp))
    }
}

// ───────────────────────── دکمه‌ها ─────────────────────────

@Composable
internal fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    val active = enabled && !loading
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(if (active) 12.dp else 0.dp, RoundedCornerShape(99.dp), spotColor = Cyan.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(99.dp))
            .background(
                if (active) Brush.horizontalGradient(BrandColors)
                else Brush.horizontalGradient(listOf(Line, Line)),
            )
            .clickable(enabled = active, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(
                text,
                color = if (enabled) Color.White else Muted,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
internal fun SmallGradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(
                if (enabled) Brush.horizontalGradient(BrandColors) else Brush.horizontalGradient(listOf(Line, Line)),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) Color.White else Muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
internal fun OutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val base = if (color == Color.Unspecified) Cyan else color
    val tint = if (enabled) base else Muted
    Row(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.34f), RoundedCornerShape(99.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = tint, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

// ───────────────────────── فیلدها ─────────────────────────

@Composable
internal fun siteFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    disabledTextColor = Muted,
    focusedBorderColor = Cyan,
    unfocusedBorderColor = Line,
    focusedLabelColor = Cyan,
    unfocusedLabelColor = Muted,
    cursorColor = Cyan,
    focusedContainerColor = CardTop,
    unfocusedContainerColor = CardBottom,
    disabledContainerColor = CardBottom,
    disabledBorderColor = Line,
    disabledLabelColor = Muted,
)

@Composable
internal fun SiteField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    ltr: Boolean = false,
    password: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    placeholder: String? = null,
    helper: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
) {
    val ph: (@Composable () -> Unit)? = if (placeholder != null) {
        { Text(placeholder, color = Muted.copy(alpha = 0.6f), fontSize = 13.sp) }
    } else {
        null
    }
    val style = if (ltr) {
        TextStyle(fontFamily = Vazirmatn, fontSize = 15.sp, textDirection = TextDirection.Ltr, textAlign = TextAlign.Left)
    } else {
        TextStyle(fontFamily = Vazirmatn, fontSize = 15.sp)
    }
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text(label, fontSize = 13.sp) },
            placeholder = ph,
            singleLine = singleLine,
            minLines = minLines,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = imeAction),
            textStyle = style,
            colors = siteFieldColors(),
            shape = RoundedCornerShape(16.dp),
        )
        if (helper != null) {
            Text(
                helper,
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 4.dp),
            )
        }
    }
}

@Composable
internal fun SwitchRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Mint,
                uncheckedThumbColor = Muted,
                uncheckedTrackColor = Line,
                uncheckedBorderColor = Line,
            ),
        )
    }
}

@Composable
internal fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(
                if (selected) Brush.horizontalGradient(listOf(Cyan.copy(alpha = 0.22f), Mint.copy(alpha = 0.16f)))
                else Brush.horizontalGradient(listOf(CardTop, CardTop)),
            )
            .border(1.dp, if (selected) Cyan.copy(alpha = 0.5f) else Line, RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            color = if (selected) Ink else Muted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

// ───────────────────────── اجزای عمومی ─────────────────────────

internal enum class Tone { Error, Success, Info, Warn }

@Composable
internal fun Banner(text: String, tone: Tone = Tone.Info, modifier: Modifier = Modifier) {
    val color = when (tone) {
        Tone.Error -> Danger
        Tone.Success -> Mint
        Tone.Info -> Cyan
        Tone.Warn -> Amber
    }
    Text(
        text,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.32f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        color = color,
        fontSize = 12.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
internal fun PageHeader(
    kicker: String,
    title: String,
    subtitle: String? = null,
    accent: Color = Color.Unspecified,
    trailing: (@Composable () -> Unit)? = null,
) {
    val accentColor = if (accent == Color.Unspecified) Cyan else accent
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(kicker, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
internal fun SectionTitle(text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        if (trailing != null) trailing()
    }
}

@Composable
internal fun EmptyState(
    icon: ImageVector,
    title: String,
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    GlassCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Mint.copy(alpha = 0.18f), Cyan.copy(alpha = 0.12f))))
                    .border(1.dp, Mint.copy(alpha = 0.32f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Mint, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(text, color = Muted, fontSize = 12.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                GradientButton(actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
internal fun MetricTile(title: String, value: String, sub: String, accent: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
        }
        Spacer(Modifier.height(8.dp))
        Text(value, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Text(sub, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun Meter(fraction: Float, color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    val f = fraction.coerceIn(0f, 1f)
    val bar = if (color == Color.Unspecified) Mint else color
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Line),
    ) {
        if (f > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(f)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Brush.horizontalGradient(listOf(bar, Cyan))),
            )
        }
    }
}

/** ردیف «برچسب + مقدار» با دکمه کپی. */
@Composable
internal fun CopyField(label: String, value: String, ltr: Boolean = true, mono: Boolean = false) {
    val clipboard = LocalClipboardManager.current
    val notify = LocalNotify.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Page.copy(alpha = 0.55f))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .clickable(enabled = value.isNotBlank()) {
                clipboard.setText(AnnotatedString(value))
                notify("$label کپی شد", false)
            }
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ContentCopy, null, tint = Cyan, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.height(3.dp))
        CompositionLocalProvider(
            LocalLayoutDirection provides if (ltr) LayoutDirection.Ltr else LayoutDirection.Rtl,
        ) {
            Text(
                value.ifBlank { "—" },
                color = Ink,
                fontSize = if (mono) 13.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (ltr) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardTop,
        titleContentColor = Ink,
        textContentColor = Muted,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp) },
        text = { Text(text, fontSize = 13.sp, lineHeight = 22.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (danger) Danger else Cyan, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = Muted, fontWeight = FontWeight.Bold) }
        },
    )
}

// ───────────────────────── نمودارها ─────────────────────────

@Composable
internal fun RevenueChart(points: List<DayPoint>) {
    if (points.isEmpty()) return
    val lineCol = Cyan
    val mintCol = Mint
    val gridCol = Line
    var selected by remember(points) { mutableIntStateOf(points.lastIndex) }
    val current = points[selected.coerceIn(0, points.lastIndex)]

    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(current.label, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(Fmt.toman(current.revenue), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
            SitePill("${Fmt.num(current.paid)} فروش", if (current.paid > 0) Mint else Muted)
        }
        Spacer(Modifier.height(8.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .pointerInput(points) {
                        detectTapGestures { off ->
                            val stepX = if (points.size > 1) size.width.toFloat() / (points.size - 1) else 1f
                            selected = (off.x / stepX).roundToInt().coerceIn(0, points.lastIndex)
                        }
                    }
                    .pointerInput(points) {
                        detectHorizontalDragGestures { change, _ ->
                            val stepX = if (points.size > 1) size.width.toFloat() / (points.size - 1) else 1f
                            selected = (change.position.x / stepX).roundToInt().coerceIn(0, points.lastIndex)
                        }
                    },
            ) {
                val max = (points.maxOfOrNull { it.revenue } ?: 0L).coerceAtLeast(1L)
                val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width
                val topPad = 10.dp.toPx()
                val h = size.height - topPad - 6.dp.toPx()

                for (g in 0..2) {
                    val gy = topPad + (h / 2f) * g
                    drawLine(gridCol.copy(alpha = 0.45f), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx())
                }

                val coords = points.mapIndexed { idx, p ->
                    Offset(idx * stepX, topPad + h - (p.revenue.toFloat() / max.toFloat()) * h)
                }
                val stroke = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 0 until coords.lastIndex) {
                        val c0 = coords[i]
                        val c1 = coords[i + 1]
                        val midX = (c0.x + c1.x) / 2f
                        cubicTo(midX, c0.y, midX, c1.y, c1.x, c1.y)
                    }
                }
                val fill = Path().apply {
                    addPath(stroke)
                    lineTo(coords.last().x, topPad + h)
                    lineTo(coords.first().x, topPad + h)
                    close()
                }
                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineCol.copy(alpha = 0.36f), mintCol.copy(alpha = 0.10f), Color.Transparent),
                        startY = 0f,
                        endY = topPad + h,
                    ),
                )
                drawPath(
                    path = stroke,
                    brush = Brush.horizontalGradient(listOf(mintCol, lineCol)),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
                val sel = selected.coerceIn(0, coords.lastIndex)
                val pt = coords[sel]
                drawLine(
                    lineCol.copy(alpha = 0.5f),
                    Offset(pt.x, topPad),
                    Offset(pt.x, topPad + h),
                    strokeWidth = 1.dp.toPx(),
                )
                drawCircle(lineCol.copy(alpha = 0.28f), radius = 9.dp.toPx(), center = pt)
                drawCircle(mintCol, radius = 4.5.dp.toPx(), center = pt)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0, points.size / 3, (points.size * 2) / 3, points.lastIndex).distinct().forEach { i ->
                    Text(points[i].label, color = Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
internal fun StatusMixBar(paid: Int, pending: Int, total: Int) {
    val safeTotal = total.coerceAtLeast(paid + pending).coerceAtLeast(1)
    val other = (safeTotal - paid - pending).coerceAtLeast(0)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ترکیب وضعیت سفارش‌ها", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("${Fmt.num(safeTotal)} فاکتور", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Line),
        ) {
            if (paid > 0) Box(Modifier.weight(paid.toFloat()).fillMaxHeight().background(Mint))
            if (pending > 0) Box(Modifier.weight(pending.toFloat()).fillMaxHeight().background(Amber))
            if (other > 0) Box(Modifier.weight(other.toFloat()).fillMaxHeight().background(Muted.copy(alpha = 0.35f)))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(Mint, "پرداخت‌شده ${Fmt.num(paid)}")
            LegendDot(Amber, "در انتظار ${Fmt.num(pending)}")
            LegendDot(Muted, "سایر ${Fmt.num(other)}")
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text, color = Muted, fontSize = 10.sp)
    }
}
