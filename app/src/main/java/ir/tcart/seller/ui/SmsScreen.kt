package ir.tcart.seller.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.tcart.seller.data.Account
import ir.tcart.seller.data.Fmt

@Composable
internal fun SmsScreen(vm: SellerViewModel, acc: Account) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.refreshSenders()
    }
    LaunchedEffect(Unit) {
        if (vm.smsGranted()) vm.refreshSenders()
    }

    PageHeader(
        kicker = "اتصال مستقیم پیامک",
        title = "پیامک بانک",
        subtitle = "سرشماره‌ای که بانک با آن پیامک واریز می‌فرستد را انتخاب کن (شمارهٔ خودت نیست). بقیهٔ کارها خودکار است.",
        accent = Mint,
    )

    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background((if (vm.listening) Mint else Amber).copy(alpha = 0.2f))
                    .border(1.dp, if (vm.listening) Mint else Amber, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Sms, null, tint = if (vm.listening) Mint else Amber, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (vm.listening) "موتور پیامک فعال است" else "موتور پیامک خاموش است",
                    color = Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
                Text(
                    if (vm.listening) "در حال گوش دادن به ${Fmt.num(vm.selectedBanks.size)} سرشماره"
                    else "سرشمارهٔ بانک را تیک بزن و ارسال خودکار را روشن کن",
                    color = if (vm.listening) Mint else Muted,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (!acc.hasCard) {
            Banner("اول در بخش «کارت‌ها» یک کارت بانکی ثبت کن؛ با ثبت کارت، کلید اتصال پیامک ساخته می‌شود.", Tone.Warn)
            Spacer(Modifier.height(10.dp))
            OutlineButton("ثبت کارت بانکی", color = Amber) {
                vm.newCardRequest = true
                vm.go(Dest.Cards)
            }
        } else if (vm.listening) {
            OutlineButton("توقف ارسال خودکار", Modifier.fillMaxWidth(), color = Danger) { vm.pauseListening() }
        } else {
            GradientButton("روشن کردن ارسال خودکار پیامک") { vm.startListening() }
        }
    }

    // تنها دکمهٔ تست اعلان
    OutlineButton("تست اعلان", Modifier.fillMaxWidth(), icon = Icons.Rounded.Notifications) { vm.previewNotification() }

    GlassCard {
        SectionTitle("سرشمارهٔ بانک‌ها")
        Spacer(Modifier.height(4.dp))
        Text(
            "پیامک‌های گوشی را بررسی می‌کنیم و فرستنده‌ها را اینجا می‌آوریم (مثل 98200012 یا Melli). فقط پیامک همین سرشماره‌ها به تیکارت می‌رود و رمز پویا همیشه فیلتر می‌شود.",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            SiteField(
                vm.manualSender,
                { vm.manualSender = it },
                "افزودن دستی سرشماره",
                modifier = Modifier.weight(1f),
                ltr = true,
            )
            Spacer(Modifier.width(8.dp))
            SmallGradientButton("افزودن", Modifier.height(56.dp)) { vm.addManualSender() }
        }
        Spacer(Modifier.height(12.dp))
        if (!vm.smsGranted()) {
            GradientButton("دادن دسترسی خواندن پیامک") { launcher.launch(vm.permissions()) }
        } else if (vm.senders.isEmpty()) {
            OutlineButton("خواندن سرشماره‌های گوشی", Modifier.fillMaxWidth()) { vm.refreshSenders() }
        } else {
            vm.senders.forEach { sender ->
                val selected = sender.key in vm.selectedBanks
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) Brush.horizontalGradient(listOf(Mint.copy(alpha = 0.18f), Cyan.copy(alpha = 0.12f)))
                            else Brush.horizontalGradient(listOf(Page.copy(alpha = 0.65f), Page.copy(alpha = 0.65f))),
                        )
                        .border(1.dp, if (selected) Mint else Line, RoundedCornerShape(16.dp))
                        .clickable { vm.toggleBank(sender.key) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(sender.address, color = if (selected) Mint else Ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("${Fmt.num(sender.count)} پیامک", color = Muted, fontSize = 11.sp)
                        }
                        if (sender.preview.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(sender.preview, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    SitePill(if (selected) "✓ انتخاب شد" else "انتخاب", if (selected) Mint else Cyan)
                }
            }
        }
    }

    GlassCard {
        SectionTitle("پایداری در پس‌زمینه")
        Spacer(Modifier.height(4.dp))
        Text(
            "برای اینکه بعد از قفل شدن گوشی هم پیامک‌ها برسند، باتری را برای تیکارت «بدون محدودیت» کن.",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(10.dp))
        OutlineButton("تنظیم باتری") { vm.askBattery(context) }
    }

    if (vm.history.isNotEmpty()) {
        GlassCard {
            SectionTitle("آخرین پیامک‌های بررسی‌شده")
            Spacer(Modifier.height(6.dp))
            vm.history.take(8).forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = if (item.kind == "paid") Mint else Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(item.detail, color = Muted, fontSize = 11.sp)
                    }
                    if (item.amount > 0) {
                        Text(Fmt.toman(item.amount), color = Mint, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
