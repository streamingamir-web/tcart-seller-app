package ir.tcart.seller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.tcart.seller.data.Account
import ir.tcart.seller.data.Fmt
import ir.tcart.seller.data.Plans

@Composable
private fun MenuTile(icon: ImageVector, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.34f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun MoreScreen(vm: SellerViewModel, acc: Account) {
    var confirmLogout by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }

    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(BrandColors)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    acc.displayName.take(1),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(acc.displayName, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    listOf(acc.phone, acc.email).firstOrNull { it.isNotBlank() }?.let { Fmt.fa(it) } ?: "شماره‌ای ثبت نشده",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (acc.planTier.isNotBlank()) {
                SitePill(
                    Plans.titleFa(acc.planTier) + if (acc.subscriptionActive) "" else " (منقضی)",
                    if (acc.subscriptionActive) Mint else Danger,
                )
            } else if (acc.subscriptionEnabled) {
                SitePill("بدون اشتراک", Amber)
            }
            if (acc.hasTelegram) SitePill("تلگرام وصل است", Cyan)
        }
    }

    if (acc.phone.isBlank()) {
        GlassCard {
            SectionTitle("ثبت شماره موبایل")
            Spacer(Modifier.height(4.dp))
            Text("با ثبت شماره، بازیابی رمز و اتصال تلگرام ممکن می‌شود.", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            SiteField(phone, { phone = it }, "شماره موبایل", ltr = true, keyboard = KeyboardType.Phone, placeholder = "09123456789")
            Spacer(Modifier.height(10.dp))
            SmallGradientButton("ثبت شماره", enabled = !vm.isBusy("phone")) { vm.linkPhone(phone) }
        }
    }

    MenuTile(
        Icons.Rounded.Sms,
        "پیامک بانک",
        if (vm.listening) "فعال · ${Fmt.num(vm.selectedBanks.size)} سرشماره" else "خاموش؛ برای تأیید خودکار روشنش کن",
        if (vm.listening) Mint else Amber,
    ) { vm.go(Dest.Sms) }

    if (acc.subscriptionEnabled) {
        MenuTile(
            Icons.Rounded.Payments,
            "اشتراک",
            if (acc.subscriptionActive) "${Plans.titleFa(acc.planTier)} · ${acc.daysLeft?.let { Fmt.num(it) + " روز مانده" } ?: "فعال"}"
            else "خرید یا تمدید اشتراک",
            Cyan,
        ) { vm.go(Dest.Subscription) }
    }

    MenuTile(
        Icons.Rounded.Key,
        "API و افزونهٔ ووکامرس",
        if (acc.hasApiPlan) "کلید، وب‌هوک و دانلود افزونه" else "در پلن بیزنس و بالاتر",
        Violet,
    ) { vm.go(Dest.Api) }

    if (acc.isPlatform) {
        MenuTile(Icons.Rounded.Groups, "فروشندگان پلتفرم", "ساخت و مدیریت فروشندگان زیرمجموعه", Violet) { vm.go(Dest.Vendors) }
    }

    GlassCard {
        SwitchRow(
            title = "حالت روشن",
            subtitle = "ظاهر اپ را بین تیره و روشن عوض کن.",
            checked = vm.light,
        ) { vm.toggleTheme() }
    }

    if (acc.hasCard) {
        GlassCard {
            SectionTitle("کارت اصلی")
            Spacer(Modifier.height(8.dp))
            Text(
                listOf(acc.bankName, acc.cardHolder).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "—" },
                color = Muted,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(Fmt.fa(acc.cardFormatted), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
    }

    OutlineButton(
        "خروج از حساب",
        Modifier.fillMaxWidth(),
        color = Danger,
        icon = Icons.Rounded.PowerSettingsNew,
    ) { confirmLogout = true }

    Text(
        "تیکارت فروشنده · نسخهٔ ۱٫۰",
        color = Muted,
        fontSize = 11.sp,
        modifier = Modifier.fillMaxWidth(),
    )

    if (confirmLogout) {
        ConfirmDialog(
            title = "خروج از حساب؟",
            text = "ارسال خودکار پیامک روی این گوشی متوقف می‌شود. هر وقت خواستی دوباره وارد شو.",
            confirmLabel = "خروج",
            danger = true,
            onConfirm = {
                confirmLogout = false
                vm.logout()
            },
            onDismiss = { confirmLogout = false },
        )
    }
}
