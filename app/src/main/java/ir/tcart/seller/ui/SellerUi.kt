package ir.tcart.seller.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.tcart.seller.R
import ir.tcart.seller.data.OrderRow
import ir.tcart.seller.data.ProductRow
import ir.tcart.seller.sms.SmsText

import androidx.compose.runtime.staticCompositionLocalOf

private class Palette(
    val ink: Color,
    val muted: Color,
    val blue: Color,
    val blueDeep: Color,
    val page: Color,
    val card: Color,
    val line: Color,
    val mint: Color,
)

private val DarkPalette = Palette(
    ink = Color(0xFFF8FAFC),
    muted = Color(0xFF94A3B8),
    blue = Color(0xFF60A5FA),
    blueDeep = Color(0xFF2563EB),
    page = Color(0xFF070B14),
    card = Color(0xFF111827),
    line = Color(0xFF1E293B),
    mint = Color(0xFF34D399),
)

private val LightPalette = Palette(
    ink = Color(0xFF0F172A),
    muted = Color(0xFF64748B),
    blue = Color(0xFF2563EB),
    blueDeep = Color(0xFF1D4ED8),
    page = Color(0xFFF4F7FB),
    card = Color.White,
    line = Color(0xFFE2E8F0),
    mint = Color(0xFF059669),
)

private val LocalPalette = staticCompositionLocalOf { LightPalette }

private val Ink: Color @Composable get() = LocalPalette.current.ink
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Blue: Color @Composable get() = LocalPalette.current.blue
private val BlueDeep: Color @Composable get() = LocalPalette.current.blueDeep
private val Page: Color @Composable get() = LocalPalette.current.page
private val Card: Color @Composable get() = LocalPalette.current.card
private val Line: Color @Composable get() = LocalPalette.current.line
private val Mint: Color @Composable get() = LocalPalette.current.mint

@Composable
fun SellerApp(vm: SellerViewModel = viewModel()) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val pal = if (vm.light) LightPalette else DarkPalette
        MaterialTheme(
            colorScheme = if (vm.light) {
                androidx.compose.material3.lightColorScheme(primary = pal.blue, background = pal.page, surface = pal.card)
            } else {
                darkColorScheme(primary = pal.blue, background = pal.page, surface = pal.card)
            },
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalPalette provides pal) {
                Box(Modifier.fillMaxSize().background(Page)) {
                    when (vm.stage) {
                        Stage.Splash -> Splash()
                        Stage.Welcome -> Welcome(vm)
                        Stage.Permissions -> Permissions(vm)
                        Stage.Login -> LoginScreen(vm)
                        Stage.Panel -> Panel(vm)
                    }
                    vm.celebrate?.let { PayPop(it.amount) { vm.celebrate = null } }
                }
            }
        }
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(180.dp)
                .background(Brush.radialGradient(listOf(Blue.copy(alpha = 0.35f), Color.Transparent))),
        )
        Image(painterResource(R.drawable.ic_mark), null, Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)))
    }
}

@Composable
private fun Welcome(vm: SellerViewModel) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1f))
        Image(painterResource(R.drawable.ic_mark), null, Modifier.size(108.dp).clip(RoundedCornerShape(28.dp)))
        Spacer(Modifier.height(28.dp))
        Text("به تیکارت خوش آمدید", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "پنل فروشنده، روی گوشی. درآمد، سفارش و تأیید پیامک بانک.",
            color = Muted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.weight(1f))
        Pill("شروع") { vm.startIntro() }
    }
}

@Composable
private fun Permissions(vm: SellerViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.reloadSims()
        vm.nextPerm()
    }
    val pages = listOf(
        PermPage("اعلان‌ها", "وقتی واریز تأیید شود، مبلغ را همان لحظه می‌بینی.", Icons.Rounded.Notifications),
        PermPage("دریافت پیامک", "پیامک بانک همین گوشی به تیکارت می‌رود تا سفارش خودکار تأیید شود.", Icons.Rounded.Sms),
        PermPage("اجرا بعد از روشن شدن", "در تنظیمات برنامه، اجرای خودکار را روشن کن تا بعد از ری‌استارت قطع نشود.", Icons.Rounded.Home),
        PermPage("اجرا در پشت صحنه", "اجازه بده گوشی تیکارت را در پس‌زمینه نبندد.", Icons.Rounded.Person),
    )
    val page = pages[vm.permStep.coerceIn(0, 3)]
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .height(6.dp)
                        .width(if (i == vm.permStep) 22.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (i <= vm.permStep) Blue else Line),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(120.dp).clip(CircleShape).background(Blue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(page.icon, null, tint = Blue, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(page.title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(page.body, color = Muted, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 24.sp)
        Spacer(Modifier.weight(1f))
        Pill("دادن مجوز") {
            when (vm.permStep) {
                0 -> if (vm.notifGranted()) vm.nextPerm() else launcher.launch(vm.permissions())
                1 -> if (vm.smsGranted()) vm.nextPerm() else launcher.launch(vm.permissions())
                2 -> {
                    vm.openAppSettings(context)
                    vm.nextPerm()
                }
                else -> {
                    vm.askBattery(context)
                    vm.nextPerm()
                }
            }
        }
        TextButton(onClick = { vm.nextPerm() }) { Text("بعداً", color = Muted) }
    }
}

private data class PermPage(val title: String, val body: String, val icon: ImageVector)

@Composable
private fun LoginScreen(vm: SellerViewModel) {
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val field = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Ink,
        unfocusedTextColor = Ink,
        focusedBorderColor = Blue,
        unfocusedBorderColor = Line,
        focusedLabelColor = Blue,
        unfocusedLabelColor = Muted,
        cursorColor = Blue,
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Image(painterResource(R.drawable.ic_mark), null, Modifier.size(84.dp).clip(RoundedCornerShape(22.dp)))
        Spacer(Modifier.height(16.dp))
        Text("ورود به تیکارت", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("همان حساب پنل سایت", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("شماره موبایل") }, singleLine = true, colors = field, shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("رمز عبور") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = field, shape = RoundedCornerShape(16.dp))
        vm.error?.let { Text(it, color = Color(0xFFFCA5A5), modifier = Modifier.padding(top = 12.dp), fontSize = 13.sp) }
        Spacer(Modifier.height(20.dp))
        Pill(if (vm.busy) "در حال ورود…" else "ورود", enabled = !vm.busy) { vm.login(phone, password) }
    }
}

@Composable
private fun Panel(vm: SellerViewModel) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (vm.tab) {
                0 -> HomeTab(vm)
                1 -> OrdersTab(vm)
                2 -> ProductsTab(vm)
                else -> AccountTab(vm)
            }
        }
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF0B1220)).navigationBarsPadding().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NavItem("خانه", Icons.Rounded.Home, vm.tab == 0) { vm.tab = 0 }
            NavItem("فروش", Icons.Rounded.ReceiptLong, vm.tab == 1) { vm.tab = 1 }
            NavItem("محصول", Icons.Rounded.Inventory2, vm.tab == 2) { vm.tab = 2 }
            NavItem("حساب", Icons.Rounded.Person, vm.tab == 3) { vm.tab = 3 }
        }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, on: Boolean, click: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = click).padding(8.dp)) {
        Icon(icon, null, tint = if (on) Blue else Muted, modifier = Modifier.size(22.dp))
        Text(label, color = if (on) Blue else Muted, fontSize = 11.sp)
    }
}

@Composable
private fun HomeTab(vm: SellerViewModel) {
    val stats = vm.stats
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_mark), null, Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("تیکارت", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(vm.name.ifBlank { "فروشنده" }, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (vm.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Blue)
        }
        Spacer(Modifier.height(16.dp))
        if (!vm.listening) {
            SurfaceCard {
                Text("پیامک بانک هنوز وصل نیست", color = Ink, fontWeight = FontWeight.Bold)
                Text("از تب حساب، سرشمارهٔ بانک را انتخاب کن.", color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric("این ماه", stats?.revenueMonth ?: 0, Modifier.weight(1f))
            Metric("کل فروش", stats?.revenueAll ?: 0, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric("در انتظار", (stats?.pendingCount ?: 0).toLong(), Modifier.weight(1f), money = false)
            Metric("کیف پول", vm.wallet, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Text("۱۴ روز اخیر", color = Ink, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        SurfaceCard { LineChart(vm.series) }
        Spacer(Modifier.height(16.dp))
        Text("آخرین فروش‌ها", color = Ink, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (vm.orders.isEmpty()) Text("هنوز سفارشی نیست.", color = Muted) else vm.orders.take(6).forEach { OrderCard(it) }
        vm.error?.let { Text(it, color = Color(0xFFFCA5A5), modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp) }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OrdersTab(vm: SellerViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("فروش‌ها", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("${SmsText.faDigits((vm.stats?.paidCount ?: 0).toLong())} تأییدشده", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        if (vm.orders.isEmpty()) Text("سفارشی برای نمایش نیست.", color = Muted) else vm.orders.forEach { OrderCard(it) }
    }
}

@Composable
private fun ProductsTab(vm: SellerViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("محصولات", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (vm.products.isEmpty()) Text("محصولی ثبت نشده.", color = Muted) else vm.products.forEach { ProductCard(it) }
    }
}

@Composable
private fun AccountTab(vm: SellerViewModel) {
    val context = LocalContext.current
    val acc = vm.account
    LaunchedEffect(Unit) {
        vm.refreshSenders()
        vm.loadPlans()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("حساب", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.toggleTheme() }) {
                Text(if (vm.light) "حالت تیره" else "حالت روشن", color = Blue)
            }
        }
        Spacer(Modifier.height(12.dp))
        SurfaceCard {
            Text(if (acc?.subActive == true) "اشتراک فعال" else "اشتراک غیرفعال", color = if (acc?.subActive == true) Mint else Muted, fontWeight = FontWeight.Bold)
            Text(
                buildString {
                    if (!acc?.planTitle.isNullOrBlank()) append(acc?.planTitle)
                    if ((acc?.daysLeft ?: 0) > 0) append(" · ${SmsText.faDigits((acc?.daysLeft ?: 0).toLong())} روز")
                    if ((acc?.planLimit ?: 0) > 0) append(" · ${SmsText.faDigits((acc?.paidThisMonth ?: 0).toLong())} از ${SmsText.faDigits((acc?.planLimit ?: 0).toLong())} سفارش")
                }.ifBlank { "برای فروش، یک پلن بخر" },
                color = Muted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(8.dp))
            vm.offers.forEach { offer ->
                TextButton(onClick = { vm.buy(offer.tier, offer.months) }, enabled = !vm.busy) {
                    Text(
                        "${offer.title} · ${SmsText.faDigits(offer.months.toLong())} ماه · ${SmsText.faAmount(offer.price)} تومان",
                        color = Blue,
                    )
                }
            }
            vm.invoice?.let { inv ->
                Text("مبلغ دقیق واریز", color = Muted, fontSize = 12.sp)
                Text("${SmsText.faAmount(inv.amount)} تومان", color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("${inv.bank} · ${inv.holder}", color = Muted, fontSize = 13.sp)
                Text(inv.card, color = Ink, fontWeight = FontWeight.SemiBold)
                Text("همین مبلغ را کارت‌به‌کارت کن. با پیامک بانک، اشتراک خودش فعال می‌شود.", color = Muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        SurfaceCard {
            Text("سرشمارهٔ بانک", color = Ink, fontWeight = FontWeight.Bold)
            Text("شمارهٔ خودت نیست. شماره‌ای که بانک از آن برایت پیامک می‌فرستد.", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            if (vm.senders.isEmpty()) Text("هنوز پیامکی در گوشی پیدا نشد.", color = Muted, fontSize = 13.sp)
            vm.senders.forEach { sender ->
                val on = sender.key in vm.selectedBanks
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (on) Blue.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { vm.toggleBank(sender.key) }
                        .padding(8.dp),
                ) {
                    Text(sender.address, color = if (on) Blue else Ink, fontWeight = FontWeight.SemiBold)
                    if (sender.preview.isNotBlank()) Text(sender.preview, color = Muted, fontSize = 12.sp)
                }
            }
            OutlinedTextField(
                vm.manualSender,
                { vm.manualSender = it },
                Modifier.fillMaxWidth(),
                label = { Text("افزودن سرشماره") },
                singleLine = true,
            )
            TextButton(onClick = { vm.addManualSender() }) { Text("اضافه کن", color = Blue) }
            if (vm.listening) TextButton(onClick = { vm.pause() }) { Text("قطع گوش دادن", color = Muted) }
            else TextButton(onClick = { vm.startListening() }) { Text("این سرشماره‌ها وصل شوند", color = Blue) }
        }
        Spacer(Modifier.height(10.dp))
        SurfaceCard {
            Text("کارت", color = Muted, fontSize = 12.sp)
            Text(if (acc?.hasCard == true) acc.cardMasked.ifBlank { "ثبت شده" } else "هنوز در سایت ثبت نشده", color = Ink, fontWeight = FontWeight.SemiBold)
            if (!acc?.bankName.isNullOrBlank()) Text("${acc?.bankName} · ${acc?.holder.orEmpty()}", color = Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        SurfaceCard {
            Text("کلید API", color = Muted, fontSize = 12.sp)
            Text(
                acc?.apiKey?.let { if (it.length > 12) it.take(8) + "…" + it.takeLast(4) else it }.orEmpty().ifBlank { "بعد از ثبت کارت ساخته می‌شود" },
                color = Ink,
                fontWeight = FontWeight.SemiBold,
            )
            if (!acc?.apiKey.isNullOrBlank()) {
                TextButton(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("api", acc?.apiKey))
                    vm.copied = true
                }) { Text(if (vm.copied) "کپی شد" else "کپی کلید", color = Blue) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Pill("نمایش اعلان نمونه") { vm.previewNotification() }
        TextButton(onClick = { vm.loadDash() }, modifier = Modifier.fillMaxWidth()) { Text("تازه‌سازی", color = Blue) }
        TextButton(onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth()) { Text("خروج", color = Color(0xFFFCA5A5)) }
        vm.error?.let { Text(it, color = Color(0xFFFCA5A5), fontSize = 13.sp) }
    }
}

@Composable
private fun Metric(label: String, value: Long, modifier: Modifier, money: Boolean = true) {
    SurfaceCard(modifier) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(
            if (money) "${SmsText.faAmount(value)} تومان" else SmsText.faDigits(value),
            color = Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun OrderCard(order: OrderRow) {
    SurfaceCard(Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(order.title, color = Ink, fontWeight = FontWeight.SemiBold)
                Text(statusFa(order.status), color = if (order.status == "paid") Mint else Muted, fontSize = 12.sp)
            }
            Text("${SmsText.faAmount(order.amount)} تومان", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProductCard(product: ProductRow) {
    SurfaceCard(Modifier.padding(bottom = 8.dp)) {
        Text(product.name, color = Ink, fontWeight = FontWeight.SemiBold)
        Text("${SmsText.faAmount(product.price)} تومان · ${SmsText.faDigits(product.paidCount.toLong())} فروش", color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun LineChart(points: List<DayPoint>) {
    if (points.isEmpty()) {
        Text("داده‌ای برای نمودار نیست", color = Muted, fontSize = 13.sp)
        return
    }
    val lineColor = Blue
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val max = (points.maxOf { it.revenue }).coerceAtLeast(1L).toFloat()
        val step = size.width / (points.size - 1).coerceAtLeast(1)
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = i * step
            val y = size.height - (p.revenue.toFloat() / max) * (size.height - 12f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
        val last = points.last()
        val x = (points.size - 1) * step
        val y = size.height - (last.revenue.toFloat() / max) * (size.height - 12f)
        drawCircle(Color.White, 7f, Offset(x, y))
        drawCircle(lineColor, 4f, Offset(x, y))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(points.first().label, color = Muted, fontSize = 11.sp)
        Text(points.last().label, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun SurfaceCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Card)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) { content() }
}

@Composable
private fun Pill(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BlueDeep, contentColor = Color.White),
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun PayPop(amount: Long?, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xCC070B14)).clickable { onDone() }, contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(28.dp).clip(RoundedCornerShape(28.dp)).background(Card).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.ic_mark), null, Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)))
            Spacer(Modifier.height(12.dp))
            Text("پرداخت تأیید شد", color = Mint, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                if (amount != null && amount > 0) "مبلغ ${SmsText.faAmount(amount)} تومان" else "واریز ثبت شد",
                color = Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun statusFa(status: String) = when (status) {
    "paid" -> "تأیید شد"
    "pending" -> "در انتظار"
    "expired" -> "منقضی"
    "cancelled" -> "لغو"
    else -> status
}
