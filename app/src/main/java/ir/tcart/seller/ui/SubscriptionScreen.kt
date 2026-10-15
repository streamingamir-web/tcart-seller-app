package ir.tcart.seller.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.tcart.seller.data.Account
import ir.tcart.seller.data.Fmt
import ir.tcart.seller.data.Invoice
import ir.tcart.seller.data.PlanOffer
import ir.tcart.seller.data.PlanTier
import ir.tcart.seller.data.Plans
import kotlinx.coroutines.delay

@Composable
internal fun SubscriptionScreen(vm: SellerViewModel, acc: Account) {
    val invoice = vm.invoice
    if (invoice != null) {
        InvoiceView(vm, invoice)
        return
    }

    val active = acc.subscriptionActive
    var months by rememberSaveable { mutableStateOf(Plans.DEFAULT_MONTHS) }
    val data = vm.plans

    PageHeader(
        kicker = "اشتراک تیکارت",
        title = if (active) "اشتراک و ارتقا" else "پلن فروش خودت را انتخاب کن",
        subtitle = if (active) "تمدید یعنی همان پلن با اضافه شدن مدت؛ ارتقا یعنی پلن بالاتر با حفظ مدت باقی‌مانده."
        else "برای ساخت محصول و لینک خرید اول اشتراک لازم است. پرداخت اشتراک هم کارت‌به‌کارت است و خودکار تأیید می‌شود.",
    )

    NoticesCard(acc)

    if (active && acc.planTier.isNotBlank()) {
        HeroCard {
            Text("اشتراک فعال", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(Plans.titleFa(acc.planTier), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            val exp = Fmt.isoMillis(acc.expiresAt)
            if (exp > 0) {
                Text(
                    "تا ${Fmt.date(exp)}${acc.daysLeft?.let { " · ${Fmt.num(it)} روز مانده" } ?: ""}",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("مصرف این ماه", color = Muted, fontSize = 12.sp)
                Text(
                    "${Fmt.num(acc.paidThisMonth)} / ${Plans.limitLabel(acc.planLimit)} فروش",
                    color = Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (acc.planLimit > 0) {
                Spacer(Modifier.height(8.dp))
                Meter(acc.paidThisMonth.toFloat() / acc.planLimit.toFloat())
            }
        }
    } else if (!active && acc.planTier.isNotBlank() && acc.expiresAt.isNotBlank()) {
        Banner("اشتراک ${Plans.titleFa(acc.planTier)} منقضی شده؛ برای ادامهٔ فروش تمدید کن.", Tone.Warn)
    }

    // مدت
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Plans.durations.forEach { d ->
            FilterPill(
                if (d.discount > 0) "${d.label} · ${Fmt.num(d.discount)}٪ تخفیف" else d.label,
                months == d.months,
            ) { months = d.months }
        }
    }

    if (data == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = Cyan, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("در حال دریافت پلن‌ها…", color = Muted, fontSize = 13.sp)
        }
        OutlineButton("تلاش دوباره") { vm.loadPlans() }
        return
    }

    val tiers = data.tiers
        .filter { it.key !in Plans.hidden }
        .sortedBy { Plans.rank(it.key).let { r -> if (r < 0) 99 else r } }

    tiers.forEach { tier ->
        PlanCard(vm, acc, tier, data.offers.firstOrNull { it.tier == tier.key && it.months == months }, months)
    }
}

@Composable
private fun PlanCard(vm: SellerViewModel, acc: Account, tier: PlanTier, offer: PlanOffer?, months: Int) {
    val info = Plans.info(tier.key)
    val total = offer?.total ?: (tier.monthly * months)
    val discount = offer?.discount ?: 0
    val perMonth = if (months > 0) total / months else total
    val isCurrent = acc.subscriptionActive && acc.planTier == tier.key
    val locked = Plans.locked(tier.key, months)
    val higher = acc.subscriptionActive && Plans.rank(tier.key) > Plans.rank(acc.planTier)
    val label = when {
        isCurrent -> "تمدید همین پلن"
        higher -> "ارتقا به ${Plans.titleFa(tier.key, tier.title)}"
        acc.subscriptionActive -> "انتخاب این پلن"
        else -> "خرید اشتراک"
    }
    val content: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                info?.titleFa ?: tier.title,
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            if (info?.recommended == true) SitePill("پیشنهادی", Cyan)
            if (isCurrent) {
                Spacer(Modifier.width(6.dp))
                SitePill("پلن فعلی", Mint)
            }
        }
        if (info != null) {
            Spacer(Modifier.height(4.dp))
            Text(info.description, color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Fmt.toman(total), color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(8.dp))
            if (discount > 0) SitePill("${Fmt.num(discount)}٪ تخفیف", Mint)
        }
        Text(
            "برای ${Fmt.num(months)} ماه · ماهی ${Fmt.toman(perMonth)} · ${if (tier.limit == 0) "فروش نامحدود" else "تا ${Fmt.num(tier.limit)} فروش در ماه"}",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 20.sp,
        )
        if (info != null) {
            Spacer(Modifier.height(10.dp))
            Plans.features(info, months).forEach { (text, included) ->
                Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                    Text(if (included) "✓" else "✕", color = if (included) Mint else Muted, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(8.dp))
                    Text(text, color = if (included) Ink else Muted, fontSize = 12.sp, lineHeight = 19.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (locked) {
            Banner("این پلن فقط به‌صورت ۳ یا ۶ ماهه فروخته می‌شود.", Tone.Warn)
        } else {
            GradientButton(label, loading = vm.isBusy("buy:${tier.key}")) { vm.buy(tier.key, months) }
        }
    }
    if (info?.recommended == true || isCurrent) HeroCard { content() } else GlassCard { content() }
}

private fun formatClock(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return Fmt.fa(String.format("%02d:%02d", m, s))
}

@Composable
private fun InvoiceView(vm: SellerViewModel, invoice: Invoice) {
    BackHandler { vm.closeInvoice() }
    val pay = invoice.payment
    val card = invoice.card

    if (vm.invoiceActivated) {
        HeroCard {
            Text("اشتراک فعال شد ✓", color = Mint, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "پرداختت تأیید شد و پلن ${Plans.titleFa(pay.tier)} فعال است. حالا می‌توانی کارت بانکی را ثبت کنی و محصول بسازی.",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(14.dp))
            GradientButton("ادامه") {
                vm.closeInvoice()
                vm.go(Dest.Home)
            }
        }
        return
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val remaining = if (pay.expiresAtMs > 0) ((pay.expiresAtMs - now) / 1000).coerceAtLeast(0) else -1L
    val expired = remaining == 0L

    PageHeader(
        kicker = "پرداخت اشتراک",
        title = "${Plans.titleFa(pay.tier)} · ${Fmt.num(pay.months)} ماهه",
        subtitle = "مبلغ را دقیقاً با همین رقم به کارت زیر واریز کن؛ تأیید خودکار است.",
    )

    HeroCard {
        Text("مبلغ قابل پرداخت", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(Fmt.toman(pay.amount), color = Ink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        CopyField("مبلغ دقیق (تومان)", pay.amount.toString())
        if (remaining >= 0) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (expired) "مهلت فاکتور تمام شد" else "زمان باقی‌مانده",
                    color = if (expired) Danger else Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatClock(remaining),
                    color = if (expired) Danger else if (remaining < 120) Amber else Mint,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }

    GlassCard {
        SectionTitle("کارت مقصد")
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CopyField("شمارهٔ کارت", card.number.ifBlank { card.numberFormatted })
            if (card.holder.isNotBlank()) CopyField("به نام", card.holder, ltr = false)
            if (card.bank.isNotBlank()) CopyField("بانک", card.bank, ltr = false)
            if (card.shebaFormatted.isNotBlank()) CopyField("شمارهٔ شبا", card.shebaFormatted)
        }
    }

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = Cyan, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "بعد از واریز، همین‌جا خودکار بررسی می‌کنیم و اشتراک فعال می‌شود. لازم نیست کاری بکنی.",
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Start,
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlineButton("لغو فاکتور", Modifier.weight(1f), color = Danger, enabled = !vm.isBusy("cancel")) { vm.cancelCheckout() }
        OutlineButton("بستن و برگشت", Modifier.weight(1f), color = Muted) { vm.closeInvoice() }
    }
}
