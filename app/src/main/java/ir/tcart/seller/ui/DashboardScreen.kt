package ir.tcart.seller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.tcart.seller.data.Account
import ir.tcart.seller.data.Fmt
import ir.tcart.seller.data.Plans
import kotlin.math.roundToInt

private data class Step(val title: String, val hint: String, val done: Boolean, val dest: Dest)

@Composable
internal fun DashboardScreen(vm: SellerViewModel, acc: Account) {
    val st = vm.stats
    val period = vm.period

    PageHeader(
        kicker = "نمای کلی حساب",
        title = "سلام، ${acc.displayName}",
        subtitle = "همه‌چیز فروشت یک‌جا؛ محصول بساز، لینک را بفرست و واریزها را زنده ببین.",
    )

    // دسترسی سریع
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickChip("محصول جدید", Icons.Rounded.Add) {
            vm.newProductRequest = true
            vm.go(Dest.Products)
        }
        QuickChip("کارت جدید", Icons.Rounded.CreditCard) {
            vm.newCardRequest = true
            vm.go(Dest.Cards)
        }
        QuickChip("فروش‌ها", Icons.Rounded.Payments) { vm.go(Dest.Orders) }
        QuickChip("پیامک بانک", Icons.Rounded.Sms) { vm.go(Dest.Sms) }
    }

    // وضعیت اشتراک
    SubscriptionBanner(vm, acc)

    // راه‌اندازی
    val steps = buildList {
        if (acc.phone.isBlank()) {
            add(Step("شماره موبایل را ثبت کن", "برای بازیابی رمز و اعلان‌ها", false, Dest.More))
        }
        if (acc.subscriptionEnabled) {
            add(Step("اشتراک را فعال کن", "بدون اشتراک ساخت محصول و کارت قفل است", acc.subscriptionActive, Dest.Subscription))
        }
        add(Step("کارت بانکی را ثبت کن", "پول مستقیم به همین کارت می‌نشیند", acc.hasCard, Dest.Cards))
        add(Step("پیامک بانک را وصل کن", "سرشمارهٔ بانک را انتخاب و ارسال خودکار را روشن کن", vm.listening && vm.selectedBanks.isNotEmpty(), Dest.Sms))
        add(Step("اولین محصول را بساز", "لینک خرید برای مشتری آماده می‌شود", vm.products.isNotEmpty(), Dest.Products))
    }
    val doneCount = steps.count { it.done }
    if (doneCount < steps.size) {
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("راه‌اندازی فروش", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${Fmt.num(doneCount)} از ${Fmt.num(steps.size)} مرحله انجام شده", color = Muted, fontSize = 12.sp)
                }
                SitePill("${Fmt.num((doneCount * 100) / steps.size)}٪", Cyan)
            }
            Spacer(Modifier.height(10.dp))
            Meter(doneCount.toFloat() / steps.size.toFloat())
            Spacer(Modifier.height(8.dp))
            steps.forEach { step -> StepRow(step) { vm.go(step.dest) } }
        }
    }

    if (acc.locked) {
        LockedGate(vm, "دیدن آمار و فروش")
        NoticesCard(acc)
        return
    }

    // درآمد
    HeroCard {
        Text("درآمد این ماه", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            Fmt.toman(st?.revenueMonth ?: 0L),
            color = Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val prev = period.previousRevenue30
            val rev = period.revenue30
            when {
                prev > 0 -> {
                    val pct = ((rev - prev) * 100.0 / prev).roundToInt()
                    val up = pct >= 0
                    SitePill("${if (up) "▲" else "▼"} ${Fmt.num(kotlin.math.abs(pct))}٪", if (up) Mint else Danger)
                    Text("نسبت به ۳۰ روز قبل", color = Muted, fontSize = 11.sp)
                }
                rev > 0 -> SitePill("شروع خوب ✨", Mint)
                else -> Text("هنوز فروشی ثبت نشده", color = Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("درآمد کل: ${Fmt.toman(st?.revenueAll ?: 0L)}", color = Muted, fontSize = 12.sp)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile("امروز", Fmt.toman(period.todayRevenue), "${Fmt.num(period.todayPaid)} فروش", Mint, Modifier.weight(1f))
        MetricTile("در انتظار تأیید", Fmt.num(st?.pending ?: 0), "سفارش باز", Amber, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile("فروش موفق", Fmt.num(st?.paid ?: 0), "${Fmt.num(st?.paidThisMonth ?: 0)} مورد این ماه", Cyan, Modifier.weight(1f))
        MetricTile("محصولات", Fmt.num(vm.products.size), "لینک خرید فعال", Violet, Modifier.weight(1f))
    }

    // نمودار
    GlassCard {
        SectionTitle("فروش ۳۰ روز اخیر")
        Spacer(Modifier.height(10.dp))
        RevenueChart(vm.series)
        Spacer(Modifier.height(14.dp))
        StatusMixBar(st?.paid ?: 0, st?.pending ?: 0, st?.total ?: 0)
    }

    // مصرف پلن
    if (acc.subscriptionActive && acc.planTier.isNotBlank()) {
        GlassCard(onClick = { vm.go(Dest.Subscription) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("مصرف پلن ${Plans.titleFa(acc.planTier)}", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${Fmt.num(acc.paidThisMonth)} از ${Plans.limitLabel(acc.planLimit)} فروش این ماه",
                        color = Muted,
                        fontSize = 12.sp,
                    )
                }
                SitePill("مدیریت اشتراک", Cyan)
            }
            if (acc.planLimit > 0) {
                Spacer(Modifier.height(10.dp))
                Meter(
                    acc.paidThisMonth.toFloat() / acc.planLimit.toFloat(),
                    if (acc.paidThisMonth * 100 >= acc.planLimit * 85) Amber else Mint,
                )
            }
        }
    }

    NoticesCard(acc)

    // آخرین سفارش‌ها
    GlassCard {
        SectionTitle("آخرین سفارش‌ها") {
            SitePill("همه", Cyan) { vm.go(Dest.Orders) }
        }
        Spacer(Modifier.height(6.dp))
        if (vm.orders.isEmpty()) {
            Text("هنوز سفارشی نداری. لینک خرید محصولت را برای مشتری بفرست.", color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
        } else {
            vm.orders.take(6).forEach { order -> OrderMiniRow(order) }
        }
    }
}

@Composable
private fun QuickChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Cyan.copy(alpha = 0.08f))
            .border(1.dp, Cyan.copy(alpha = 0.26f), RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StepRow(step: Step, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (step.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            null,
            tint = if (step.done) Mint else Muted,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                step.title,
                color = if (step.done) Muted else Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            if (!step.done) Text(step.hint, color = Muted, fontSize = 11.sp)
        }
        if (!step.done) SitePill("برو", Cyan)
    }
}

@Composable
internal fun SubscriptionBanner(vm: SellerViewModel, acc: Account) {
    if (!acc.subscriptionEnabled) return
    val days = acc.daysLeft
    when {
        acc.subscriptionActive && days != null && days <= 7 -> {
            GlassCard(onClick = { vm.go(Dest.Subscription) }) {
                Text(
                    "اشتراکت ${if (days <= 0) "امروز" else "${Fmt.num(days)} روز دیگر"} تمام می‌شود",
                    color = Amber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(4.dp))
                Text("قبل از تمام شدن تمدید کن تا لینک‌های خرید قطع نشوند.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                SmallGradientButton("تمدید اشتراک") { vm.go(Dest.Subscription) }
            }
        }
        !acc.subscriptionActive && acc.expiresAt.isNotBlank() -> {
            GlassCard(onClick = { vm.go(Dest.Subscription) }) {
                Text(
                    "اشتراک ${Plans.titleFa(acc.planTier)} منقضی شده",
                    color = Danger,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(4.dp))
                Text("برای ادامه فروش اشتراک را تمدید کن.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                SmallGradientButton("تمدید اشتراک") { vm.go(Dest.Subscription) }
            }
        }
    }
}

@Composable
internal fun NoticesCard(acc: Account) {
    if (acc.notices.isEmpty()) return
    GlassCard {
        SectionTitle("پیام‌های تیکارت")
        acc.notices.take(3).forEach { n ->
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 6.dp).size(7.dp).clip(CircleShape).background(Cyan))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(n.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    if (n.message.isNotBlank()) Text(n.message, color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
internal fun OrderMiniRow(order: ir.tcart.seller.data.Order) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(order.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (order.timeMs > 0) Fmt.dateTime(order.timeMs) else "—",
                color = Muted,
                fontSize = 11.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Fmt.toman(order.uniqueToman), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            SitePill(statusLabel(order.status), statusColor(order.status))
        }
    }
}

@Composable
internal fun statusColor(status: String): Color = when (status) {
    "paid" -> Mint
    "pending" -> Amber
    "cancelled" -> Danger
    else -> Muted
}

internal fun statusLabel(status: String): String = when (status) {
    "pending" -> "در انتظار پرداخت"
    "paid" -> "پرداخت شد"
    "expired" -> "منقضی"
    "cancelled" -> "لغو شد"
    else -> status
}
