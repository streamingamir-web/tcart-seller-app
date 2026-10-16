package ir.tcart.seller.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.tcart.seller.data.Account
import ir.tcart.seller.data.Fmt
import ir.tcart.seller.data.Order
import ir.tcart.seller.data.TcartApi

private fun sourceLabel(source: String): String = when (source) {
    "web", "web_link", "buy_page" -> "لینک وب"
    "telegram", "bot" -> "تلگرام"
    "api" -> "API"
    "woocommerce" -> "ووکامرس"
    "" -> ""
    else -> source
}

@Composable
internal fun OrdersScreen(vm: SellerViewModel, acc: Account) {
    PageHeader(
        kicker = "مدیریت تراکنش‌ها",
        title = "فروش‌ها",
        subtitle = "همهٔ سفارش‌ها؛ واریزها خودکار تأیید می‌شوند و سفارش مشکوک را دستی بررسی می‌کنی.",
    )
    if (acc.locked) {
        LockedGate(vm, "دیدن فروش‌ها")
        return
    }

    var filter by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }
    var confirmFor by remember { mutableStateOf<Order?>(null) }
    var rejectFor by remember { mutableStateOf<Order?>(null) }
    val st = vm.stats
    val orders = vm.orders

    // جستجو
    GlassCard {
        Row(verticalAlignment = Alignment.Top) {
            SiteField(
                query,
                { query = it; if (it.isBlank()) vm.clearLookup() },
                "کد پیگیری یا شمارهٔ سفارش",
                modifier = Modifier.weight(1f),
                ltr = true,
                keyboard = KeyboardType.Text,
            )
            Spacer(Modifier.width(8.dp))
            SmallGradientButton("جستجو", Modifier.height(56.dp), enabled = !vm.isBusy("lookup")) { vm.lookup(query) }
        }
    }
    vm.lookupResult?.let { found ->
        SectionTitle("نتیجهٔ جستجو") { SitePill("پاک کردن", Muted) { vm.clearLookup(); query = "" } }
        OrderCard(vm, found, { confirmFor = it }, { rejectFor = it })
    }

    if (st != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("درآمد این ماه", Fmt.toman(st.revenueMonth), "${Fmt.num(st.paidThisMonth)} فروش", Mint, Modifier.weight(1f))
            MetricTile("درآمد کل", Fmt.toman(st.revenueAll), "${Fmt.num(st.paid)} فروش موفق", Cyan, Modifier.weight(1f))
        }
        if (acc.planLimit > 0 && acc.subscriptionActive) {
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("سقف فروش ماهانهٔ پلن", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${Fmt.num(acc.paidThisMonth)} / ${Fmt.num(acc.planLimit)}", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(8.dp))
                Meter(
                    acc.paidThisMonth.toFloat() / acc.planLimit.toFloat(),
                    if (acc.paidThisMonth * 100 >= acc.planLimit * 85) Amber else Mint,
                )
            }
        }
    }

    val pending = orders.filter { it.status == "pending" }
    if (pending.isNotEmpty() && filter == "all") {
        GlassCard {
            Text("منتظر تأیید تو (${Fmt.num(pending.size)})", color = Amber, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "اگر پول را در حساب دیدی ولی پیامک نرسید، همین‌جا دستی تأیید کن.",
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 20.sp,
            )
        }
        pending.forEach { OrderCard(vm, it, { o -> confirmFor = o }, { o -> rejectFor = o }) }
    }

    // فیلترها
    val filters = listOf(
        "all" to "همه",
        "pending" to "در انتظار",
        "paid" to "پرداخت‌شده",
        "expired" to "منقضی",
        "cancelled" to "لغو",
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        filters.forEach { (key, label) ->
            val count = if (key == "all") orders.size else orders.count { it.status == key }
            FilterPill("$label ${Fmt.num(count)}", filter == key) { filter = key }
        }
    }

    val shown = orders.filter {
        when (filter) {
            "all" -> it.status != "pending"
            else -> it.status == filter
        }
    }
    if (orders.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Payments,
            title = "هنوز فروشی ثبت نشده",
            text = "لینک خرید یکی از محصولاتت را برای مشتری بفرست؛ سفارش‌ها همین‌جا زنده نمایش داده می‌شوند.",
            actionLabel = "رفتن به محصولات",
            onAction = { vm.go(Dest.Products) },
        )
    } else if (shown.isEmpty()) {
        Text("سفارشی در این دسته نیست.", color = Muted, fontSize = 13.sp)
    } else {
        shown.forEach { OrderCard(vm, it, { o -> confirmFor = o }, { o -> rejectFor = o }) }
    }

    confirmFor?.let { o ->
        ConfirmDialog(
            title = "تأیید دستی پرداخت",
            text = "مطمئنی مبلغ ${Fmt.toman(o.uniqueToman)} به حسابت واریز شده؟ با تأیید، تحویل محصول به خریدار انجام می‌شود.",
            confirmLabel = "بله، تأیید کن",
            onConfirm = {
                confirmFor = null
                vm.confirm(o)
            },
            onDismiss = { confirmFor = null },
        )
    }
    rejectFor?.let { o ->
        ConfirmDialog(
            title = "رد سفارش",
            text = "سفارش «${o.title}» رد می‌شود و خریدار دیگر نمی‌تواند برایش پرداخت کند.",
            confirmLabel = "رد سفارش",
            danger = true,
            onConfirm = {
                rejectFor = null
                vm.reject(o)
            },
            onDismiss = { rejectFor = null },
        )
    }
}

@Composable
internal fun OrderCard(vm: SellerViewModel, order: Order, onConfirm: (Order) -> Unit, onReject: (Order) -> Unit) {
    val context = LocalContext.current
    val busy = vm.isBusy("order:${order.id}")
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(order.title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    buildList {
                        if (order.timeMs > 0) add(Fmt.dateTime(order.timeMs))
                        val src = sourceLabel(order.source)
                        if (src.isNotBlank()) add(src)
                        if (order.buyerName.isNotBlank()) add(order.buyerName)
                    }.joinToString(" · "),
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            SitePill(statusLabel(order.status), statusColor(order.status))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Fmt.toman(order.uniqueToman), color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            if (order.baseToman > 0 && order.baseToman != order.uniqueToman) {
                Text("قیمت پایه ${Fmt.toman(order.baseToman)}", color = Muted, fontSize = 11.sp)
            }
        }
        if (order.tracking.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            CopyField("کد پیگیری", order.tracking)
        }
        if (order.status == "pending" && order.expiresAt.isNotBlank()) {
            val exp = Fmt.isoMillis(order.expiresAt)
            if (exp > 0) {
                Spacer(Modifier.height(8.dp))
                Text("مهلت پرداخت تا ${Fmt.dateTime(exp)}", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (order.canAct || order.buyerUrl.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (order.canAct) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallGradientButton("تأیید دستی", Modifier.weight(1f), enabled = !busy) { onConfirm(order) }
                        if (order.status == "pending") {
                            OutlineButton("رد", Modifier.weight(1f), color = Danger, enabled = !busy) { onReject(order) }
                        }
                    }
                }
                if (order.buyerUrl.isNotBlank()) {
                    OutlineButton("صفحهٔ فاکتور خریدار", icon = Icons.Rounded.OpenInNew) {
                        val url = if (order.buyerUrl.startsWith("http")) order.buyerUrl else "${TcartApi.ROOT}${order.buyerUrl}"
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    }
                }
            }
        }
    }
}
