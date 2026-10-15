package ir.tcart.seller.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.tcart.seller.R
import ir.tcart.seller.data.OrderRow
import ir.tcart.seller.data.ProductRow
import ir.tcart.seller.sms.SmsText

private val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
)

private val SiteTypography = Typography(
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

private class Palette(
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

private val DarkStudioPalette = Palette(
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

private val LightStudioPalette = Palette(
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

private val LocalPalette = staticCompositionLocalOf { DarkStudioPalette }

private val Ink: Color @Composable get() = LocalPalette.current.ink
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Cyan: Color @Composable get() = LocalPalette.current.cyan
private val BlueDeep: Color @Composable get() = LocalPalette.current.blueDeep
private val Page: Color @Composable get() = LocalPalette.current.page
private val CardTop: Color @Composable get() = LocalPalette.current.cardTop
private val CardBottom: Color @Composable get() = LocalPalette.current.cardBottom
private val HeroTop: Color @Composable get() = LocalPalette.current.heroTop
private val HeroBottom: Color @Composable get() = LocalPalette.current.heroBottom
private val Line: Color @Composable get() = LocalPalette.current.line
private val HeroLine: Color @Composable get() = LocalPalette.current.heroLine
private val Mint: Color @Composable get() = LocalPalette.current.mint
private val Amber: Color @Composable get() = LocalPalette.current.amber

@Composable
fun SellerApp(vm: SellerViewModel = viewModel()) {
    val pal = if (vm.light) LightStudioPalette else DarkStudioPalette
    MaterialTheme(
        colorScheme = if (vm.light) {
            lightColorScheme(primary = pal.blueDeep, background = pal.page, surface = pal.cardTop)
        } else {
            darkColorScheme(primary = pal.cyan, background = pal.page, surface = pal.cardTop)
        },
        typography = SiteTypography,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalPalette provides pal) {
            AuroraBackdrop {
                when (vm.stage) {
                    Stage.Splash -> Splash()
                    Stage.Welcome -> Welcome(vm)
                    Stage.Permissions -> Permissions(vm)
                    Stage.Login -> LoginScreen(vm)
                    Stage.Panel -> PanelShell(vm)
                }
                vm.celebrate?.let { PayPop(it.amount, it.status, it.detail) { vm.celebrate = null } }
            }
        }
    }
}

@Composable
private fun AuroraBackdrop(content: @Composable () -> Unit) {
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
private fun BrandLogo(size: Int = 84) {
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

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandLogo(96)
            Spacer(Modifier.height(18.dp))
            Text("تیکارت", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text("پنل هوشمند فروشندگان", color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Welcome(vm: SellerViewModel) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        BrandLogo(104)
        Spacer(Modifier.height(26.dp))
        SitePill("موتور خودکار تأیید واریز", Mint)
        Spacer(Modifier.height(14.dp))
        Text(
            "پنل فروشنده تیکارت",
            color = Ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "بدون نیاز به MacroDroid یا تنظیم دستی؛ فقط سرشمارهٔ بانک‌هایت را تیک بزن تا هر واریز در لحظه تأیید شود و با نوتیفیکیشن بهت خبر بدهد.",
            color = Muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 25.sp,
        )
        Spacer(Modifier.weight(1f))
        GradientButton("شروع راه‌اندازی") { vm.startIntro() }
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
        PermPage("اعلان لحظه‌ای واریز", "وقتی مبلغی به کارتت واریز شود یا سفارشی در انتظار باشد، نوتیفیکیشن فوری با لوگوی تیکارت برایت می‌آید.", Icons.Rounded.Notifications),
        PermPage("خواندن پیامک بانک", "فقط پیامک سرشماره‌های بانکی که خودت انتخاب می‌کنی برای تیکارت ارسال می‌شود؛ کدهای رمز پویا کاملاً فیلتر می‌شوند.", Icons.Rounded.Sms),
        PermPage("اجرای خودکار پس از روشن شدن", "در صفحهٔ تنظیمات، اجرای خودکار را فعال کن تا بعد از ری‌استارت گوشی هم قطع نشود.", Icons.Rounded.Vibration),
        PermPage("فعال ماندن در پس‌زمینه", "اجازه بده باتری گوشی موتور پیامک تیکارت را در پس‌زمینه متوقف نکند.", Icons.Rounded.PowerSettingsNew),
    )
    val page = pages[vm.permStep.coerceIn(0, 3)]
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .height(6.dp)
                        .width(if (i == vm.permStep) 28.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (i <= vm.permStep) Brush.horizontalGradient(listOf(Mint, Cyan))
                            else Brush.horizontalGradient(listOf(Line, Line)),
                        ),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        HeroCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(listOf(Mint.copy(alpha = 0.20f), Cyan.copy(alpha = 0.14f))))
                        .border(1.dp, Mint.copy(alpha = 0.38f), RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(page.icon, null, tint = Mint, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text(page.title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(page.body, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 24.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        GradientButton("اعطای دسترسی مرحله ${SmsText.faDigits((vm.permStep + 1).toLong())}") {
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
        TextButton(onClick = { vm.nextPerm() }) { Text("رد شدن از این مرحله", color = Muted, fontSize = 13.sp) }
    }
}

private data class PermPage(val title: String, val body: String, val icon: ImageVector)

@Composable
private fun LoginScreen(vm: SellerViewModel) {
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val field = siteFieldColors()
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        BrandLogo(88)
        Spacer(Modifier.height(18.dp))
        SitePill("حساب فروشنده تیکارت", Cyan)
        Spacer(Modifier.height(10.dp))
        Text("ورود به پنل فروشنده", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("با شماره موبایل و رمز عبور حساب تیکارت وارد شوید", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        HeroCard {
            OutlinedTextField(
                phone,
                { phone = it },
                Modifier.fillMaxWidth(),
                label = { Text("شماره موبایل (مثلاً 0912...)") },
                singleLine = true,
                colors = field,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                password,
                { password = it },
                Modifier.fillMaxWidth(),
                label = { Text("رمز عبور") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = field,
                shape = RoundedCornerShape(16.dp),
            )
            vm.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color(0xFFFB7185), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(18.dp))
            GradientButton(if (vm.busy) "در حال ورود به حساب…" else "ورود به پنل تیکارت", enabled = !vm.busy) {
                vm.login(phone, password)
            }
        }
    }
}

@Composable
private fun PanelShell(vm: SellerViewModel) {
    BackHandler(enabled = vm.screen != "main") {
        vm.openScreen("main")
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SiteTopBar(vm)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            vm.error?.let {
                GlassCard(Modifier.padding(bottom = 12.dp)) {
                    Text(it, color = Color(0xFFFB7185), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            when {
                vm.screen == "cards" -> CardsSubScreen(vm)
                vm.screen == "wallet" -> WalletSubScreen(vm)
                vm.tab == 0 -> DashboardTab(vm)
                vm.tab == 1 -> SmsBankTab(vm)
                vm.tab == 2 -> OrdersTab(vm)
                vm.tab == 3 -> ProductsAndCardsTab(vm)
                else -> AccountAndPlanTab(vm)
            }
            Spacer(Modifier.height(18.dp))
        }
        SiteBottomDock(vm)
    }
}

@Composable
private fun SiteTopBar(vm: SellerViewModel) {
    val titles = listOf("داشبورد", "پیامک بانک", "فروش‌ها", "محصول و کارت", "حساب و اشتراک")
    val currentTitle = when (vm.screen) {
        "cards" -> "کارت‌های بانکی"
        "wallet" -> "کیف پول"
        else -> titles.getOrElse(vm.tab) { "داشبورد" }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(CardTop.copy(alpha = 0.92f), CardBottom.copy(alpha = 0.82f))))
            .border(0.5.dp, Line)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.tcart_logo),
            contentDescription = "تیکارت",
            modifier = Modifier
                .size(38.dp)
                .shadow(10.dp, RoundedCornerShape(12.dp), spotColor = Mint)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("پنل فروشنده", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                vm.account?.planTitle?.takeIf { it.isNotBlank() }?.let { plan ->
                    SitePill(plan, Mint)
                }
            }
            Text(currentTitle, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(if (vm.listening) Mint.copy(alpha = 0.14f) else Amber.copy(alpha = 0.14f))
                .border(1.dp, if (vm.listening) Mint.copy(alpha = 0.36f) else Amber.copy(alpha = 0.36f), RoundedCornerShape(99.dp))
                .clickable {
                    vm.openScreen("main")
                    vm.tab = 1
                }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (vm.listening) Mint else Amber))
            Spacer(Modifier.width(5.dp))
            Text(
                if (vm.listening) "پیامک وصل" else "نصب پیامک",
                color = if (vm.listening) Mint else Amber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(6.dp))
        IconPill(if (vm.light) Icons.Rounded.DarkMode else Icons.Rounded.LightMode) { vm.toggleTheme() }
        Spacer(Modifier.width(6.dp))
        IconPill(Icons.Rounded.Refresh) { vm.loadDash() }
    }
}

@Composable
private fun IconPill(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardTop)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Ink, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SiteBottomDock(vm: SellerViewModel) {
    val items = listOf(
        Triple(0, "داشبورد", Icons.Rounded.Dashboard),
        Triple(1, "پیامک", Icons.Rounded.Sms),
        Triple(2, "فروش‌ها", Icons.Rounded.ReceiptLong),
        Triple(3, "محصولات", Icons.Rounded.Inventory2),
        Triple(4, "حساب", Icons.Rounded.AccountBalanceWallet),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(CardTop.copy(alpha = 0.96f), CardBottom)))
            .border(0.5.dp, Line)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { (index, label, icon) ->
            val active = vm.screen == "main" && vm.tab == index
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (active) Brush.linearGradient(listOf(Cyan.copy(alpha = 0.20f), Mint.copy(alpha = 0.14f)))
                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                    )
                    .border(
                        1.dp,
                        if (active) Cyan.copy(alpha = 0.42f) else Color.Transparent,
                        RoundedCornerShape(16.dp),
                    )
                    .clickable {
                        vm.openScreen("main")
                        vm.tab = index
                    }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(icon, null, tint = if (active) Cyan else Muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(2.dp))
                Text(
                    label,
                    color = if (active) Ink else Muted,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DashboardTab(vm: SellerViewModel) {
    val st = vm.stats
    Text("نمای کلی حساب", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Text(
        if (vm.name.isBlank()) "داشبورد فروشنده" else "سلام، ${vm.name}",
        color = Ink,
        fontSize = 23.sp,
        fontWeight = FontWeight.ExtraBold,
    )
    Text(
        "درآمد و فروش از دادهٔ واقعی سفارش‌ها · متصل به تیکارت",
        color = Muted,
        fontSize = 12.sp,
    )
    Spacer(Modifier.height(12.dp))

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickActionChip("اتصال پیامک بانک", Icons.Rounded.Sms) { vm.tab = 1 }
        QuickActionChip("فروش‌ها", Icons.Rounded.ReceiptLong) { vm.tab = 2 }
        QuickActionChip("کارت‌های بانکی", Icons.Rounded.CreditCard) { vm.openScreen("cards") }
        QuickActionChip("محصول جدید", Icons.Rounded.Inventory2) { vm.tab = 3 }
        QuickActionChip("کیف پول", Icons.Rounded.AccountBalanceWallet) { vm.openScreen("wallet") }
        QuickActionChip("تست نوتیف واریز", Icons.Rounded.AutoAwesome) { vm.previewNotification() }
    }

    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            title = "درآمد این ماه",
            value = "${SmsText.faAmount(st?.revenueMonth ?: 0)} ت",
            sub = "کل: ${SmsText.faAmount(st?.revenueAll ?: 0)} تومان",
            accent = Cyan,
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            title = "موجودی کیف پول",
            value = "${SmsText.faAmount(vm.wallet)} ت",
            sub = "قابل تسویه و استفاده",
            accent = Mint,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            title = "پرداخت‌های موفق",
            value = SmsText.faDigits((st?.paidCount ?: 0).toLong()),
            sub = "${SmsText.faDigits((st?.paidThisMonth ?: 0).toLong())} مورد در این ماه",
            accent = Mint,
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            title = "در انتظار تأیید",
            value = SmsText.faDigits((st?.pendingCount ?: 0).toLong()),
            sub = "فاکتور باز منتظر پیامک",
            accent = Amber,
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(14.dp))
    HeroCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("روند درآمد ۱۴ روز اخیر", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("بر اساس سفارش‌های تأییدشده", color = Muted, fontSize = 12.sp)
            }
            SitePill("همه تراکنش‌ها ←", Cyan) { vm.tab = 2 }
        }
        Spacer(Modifier.height(14.dp))
        StudioRevenueChart(vm.series)
        Spacer(Modifier.height(14.dp))
        StatusMixBar(
            paid = st?.paidCount ?: 0,
            pending = st?.pendingCount ?: 0,
            total = vm.orders.size,
        )
    }

    Spacer(Modifier.height(14.dp))
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Mint.copy(alpha = 0.22f), Cyan.copy(alpha = 0.16f))))
                    .border(1.dp, Mint.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Mint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("نوتیفیکیشن دوپامینی واریز", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("امتحان کن وقتی پول به کارتت می‌نشیند چه نوتیفی می‌آید", color = Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            SmallGradientButton("تست اعلان") { vm.previewNotification() }
        }
    }

    if (vm.orders.isNotEmpty()) {
        Spacer(Modifier.height(14.dp))
        GlassCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("آخرین تراکنش‌ها", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text(
                    "مشاهده همه",
                    color = Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { vm.tab = 2 },
                )
            }
            Spacer(Modifier.height(8.dp))
            vm.orders.take(5).forEach { row ->
                OrderItemRow(row, vm, showActions = false)
            }
        }
    }
}

@Composable
private fun QuickActionChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Cyan.copy(alpha = 0.08f))
            .border(1.dp, Cyan.copy(alpha = 0.26f), RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricTile(title: String, value: String, sub: String, accent: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(title, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
        }
        Spacer(Modifier.height(8.dp))
        Text(value, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Text(sub, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StudioRevenueChart(points: List<DayPoint>) {
    val lineCol = Cyan
    val mintCol = Mint
    val gridCol = Line
    if (points.isEmpty()) return
    Column {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val max = (points.maxOfOrNull { it.revenue } ?: 0L).coerceAtLeast(1L)
            val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width
            val h = size.height - 16.dp.toPx()
            val topPad = 10.dp.toPx()

            for (g in 0..2) {
                val gy = topPad + (h / 2f) * g
                drawLine(gridCol.copy(alpha = 0.45f), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx())
            }

            val coords = points.mapIndexed { idx, p ->
                val x = idx * stepX
                val y = topPad + h - (p.revenue.toFloat() / max.toFloat()) * h
                Offset(x, y)
            }

            val strokePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.lastIndex) {
                    val c0 = coords[i]
                    val c1 = coords[i + 1]
                    val midX = (c0.x + c1.x) / 2f
                    cubicTo(midX, c0.y, midX, c1.y, c1.x, c1.y)
                }
            }
            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(coords.last().x, topPad + h)
                lineTo(coords.first().x, topPad + h)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineCol.copy(alpha = 0.36f), mintCol.copy(alpha = 0.10f), Color.Transparent),
                    startY = 0f,
                    endY = topPad + h,
                ),
            )
            drawPath(
                path = strokePath,
                brush = Brush.horizontalGradient(listOf(mintCol, lineCol)),
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
            )
            coords.forEachIndexed { idx, pt ->
                if (points[idx].revenue > 0L || idx == coords.lastIndex) {
                    drawCircle(lineCol.copy(alpha = 0.28f), radius = 7.dp.toPx(), center = pt)
                    drawCircle(mintCol, radius = 3.8.dp.toPx(), center = pt)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val pick = listOf(0, points.size / 3, (points.size * 2) / 3, points.lastIndex).distinct()
            pick.forEach { i ->
                Text(points[i].label, color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StatusMixBar(paid: Int, pending: Int, total: Int) {
    val safeTotal = total.coerceAtLeast(paid + pending).coerceAtLeast(1)
    val other = (safeTotal - paid - pending).coerceAtLeast(0)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ترکیب وضعیت سفارش‌ها", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("${SmsText.faDigits(safeTotal.toLong())} فاکتور", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    }
}

@Composable
private fun SmsBankTab(vm: SellerViewModel) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.refreshSenders()
    }
    LaunchedEffect(Unit) {
        if (vm.smsGranted()) vm.refreshSenders()
    }
    val field = siteFieldColors()

    Text("اتصال مستقیم به درگاه پیامک", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Text("موتور خودکار پیامک بانک", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    Text(
        "سرشماره‌هایی که بانک از آن‌ها برایت پیامک واریز می‌فرستد را انتخاب کن تا هر واریز در لحظه تأیید شود.",
        color = Muted,
        fontSize = 13.sp,
        lineHeight = 22.sp,
    )
    Spacer(Modifier.height(14.dp))

    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (vm.listening) Mint.copy(alpha = 0.2f) else Amber.copy(alpha = 0.2f))
                    .border(1.dp, if (vm.listening) Mint else Amber, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Sms, null, tint = if (vm.listening) Mint else Amber, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (vm.listening) "موتور پیامک فعال و آماده است" else "موتور پیامک هنوز روشن نشده",
                    color = Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
                Text(
                    if (vm.listening) "در حال گوش دادن به ${SmsText.faDigits(vm.selectedBanks.size.toLong())} سرشمارهٔ بانک"
                    else "ابتدا از لیست پایین سرشمارهٔ بانک را تیک بزن",
                    color = if (vm.listening) Mint else Muted,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (vm.listening) {
            Button(
                onClick = { vm.pause() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
            ) { Text("توقف موقت ارسال خودکار پیامک", fontWeight = FontWeight.ExtraBold) }
        } else {
            GradientButton("فعال‌سازی ارسال خودکار پیامک‌های بانک") { vm.startListening() }
        }
    }

    Spacer(Modifier.height(12.dp))
    GlassCard {
        Text("انتخاب سرشماره‌های بانک از پیامک‌های گوشی", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "منظور شمارهٔ خودت نیست؛ شماره یا نام فرستنده‌ای است که بانک با آن پیامک واریز می‌فرستد (مثل 982000... یا Melli یا Mellat).",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                vm.manualSender,
                { vm.manualSender = it },
                Modifier.weight(1f),
                label = { Text("افزودن دستی سرشماره بانک") },
                singleLine = true,
                colors = field,
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.width(8.dp))
            SmallGradientButton("افزودن") { vm.addManualSender() }
        }
        Spacer(Modifier.height(12.dp))
        if (!vm.smsGranted()) {
            GradientButton("دادن دسترسی خواندن لیست پیامک‌ها") { launcher.launch(vm.permissions()) }
        } else if (vm.senders.isEmpty()) {
            GradientButton("خواندن سرشماره‌های پیامک گوشی") { vm.refreshSenders() }
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
                            Text("${SmsText.faDigits(sender.count.toLong())} پیامک", color = Muted, fontSize = 11.sp)
                        }
                        if (sender.preview.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(sender.preview, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    SitePill(if (selected) "✓ بانک انتخاب‌شده" else "انتخاب", if (selected) Mint else Cyan)
                }
            }
        }
    }

    if (vm.history.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        GlassCard {
            Text("آخرین پیامک‌های بررسی‌شده", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            vm.history.take(8).forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = if (item.kind == "paid") Mint else Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(item.detail, color = Muted, fontSize = 11.sp)
                    }
                    if (item.amount > 0) {
                        Text("${SmsText.faAmount(item.amount)} ت", color = Mint, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersTab(vm: SellerViewModel) {
    var filter by rememberSaveable { mutableStateOf("all") }
    val field = siteFieldColors()
    Text("مدیریت تراکنش‌ها", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Text("فروش‌ها و سفارش‌ها", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(10.dp))

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                vm.search,
                { vm.search = it },
                Modifier.weight(1f),
                label = { Text("جستجوی کد سفارش") },
                singleLine = true,
                colors = field,
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.width(8.dp))
            SmallGradientButton("بیاب") { vm.findOrder() }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("all" to "همه", "pending" to "در انتظار تأیید", "paid" to "تأیید شده", "expired" to "منقضی").forEach { (k, label) ->
                SitePill(label, if (filter == k) Mint else Cyan) { filter = k }
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    val rows = if (filter == "all") vm.orders else vm.orders.filter { it.status == filter }
    if (rows.isEmpty()) {
        GlassCard { Text("در این بخش سفارشی وجود ندارد.", color = Muted, fontSize = 13.sp) }
    } else {
        GlassCard {
            rows.forEach { row ->
                OrderItemRow(row, vm, showActions = true)
            }
        }
    }
}

@Composable
private fun OrderItemRow(row: OrderRow, vm: SellerViewModel, showActions: Boolean) {
    val isPaid = row.status == "paid"
    val isPending = row.status == "pending"
    val badgeColor = when {
        isPaid -> Mint
        isPending -> Amber
        else -> Muted
    }
    val badgeText = when (row.status) {
        "paid" -> "پرداخت موفق"
        "pending" -> "در انتظار تأیید"
        "expired" -> "منقضی"
        "cancelled" -> "لغو شده"
        else -> row.status
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Page.copy(alpha = 0.55f))
            .border(1.dp, if (isPending) Amber.copy(alpha = 0.35f) else Line, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.title, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text(
                    "سفارش #${SmsText.faDigits(row.id)}" + if (row.buyerName.isNotBlank()) " · ${row.buyerName}" else "",
                    color = Muted,
                    fontSize = 11.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${SmsText.faAmount(row.amount)} ت", color = if (isPaid) Mint else Ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                SitePill(badgeText, badgeColor)
            }
        }
        if (showActions && isPending) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.confirm(row.id) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF041018)),
                ) { Text("تأیید دستی واریز", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
                Button(
                    onClick = { vm.reject(row.id) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.18f), contentColor = Color(0xFFFB7185)),
                ) { Text("رد سفارش", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun ProductsAndCardsTab(vm: SellerViewModel) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<ProductRow?>(null) }
    var creating by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
            GradientButton(if (mode == 0) "● محصولات" else "محصولات") { mode = 0 }
        }
        Box(Modifier.weight(1f)) {
            GradientButton(if (mode == 1) "● کارت‌های بانکی" else "کارت‌های بانکی") {
                mode = 1
                vm.loadCards()
            }
        }
    }
    Spacer(Modifier.height(14.dp))

    if (mode == 1) {
        CardsSubScreen(vm, showBack = false)
        return
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("فروشگاه شما", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("محصولات و لینک خرید", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        }
        SmallGradientButton("+ محصول جدید") {
            editing = null
            creating = true
        }
    }
    Spacer(Modifier.height(12.dp))

    if (creating || editing != null) {
        ProductEditorCard(vm, editing) {
            creating = false
            editing = null
        }
        Spacer(Modifier.height(12.dp))
    }

    if (vm.products.isEmpty()) {
        GlassCard { Text("هنوز محصولی ثبت نکرده‌ای. با دکمهٔ «+ محصول جدید» اولین محصولت را بساز.", color = Muted, fontSize = 13.sp) }
    } else {
        vm.products.forEach { prod ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .shadow(14.dp, RoundedCornerShape(22.dp), spotColor = Cyan.copy(alpha = 0.18f))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
                    .border(1.dp, Line, RoundedCornerShape(22.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFF1D4ED8), Color(0xFF0EA5E9), Color(0xFF10B981))))
                        .padding(16.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(prod.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                            if (prod.description.isNotBlank()) {
                                Text(prod.description, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 1)
                            }
                        }
                        Text("${SmsText.faAmount(prod.price)} ت", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SitePill(if (prod.enabled) "فعال · ${SmsText.faDigits(prod.paidCount.toLong())} فروش" else "غیرفعال", if (prod.enabled) Mint else Muted)
                    Row {
                        TextButton(onClick = { editing = prod; creating = false }) { Text("ویرایش", color = Cyan, fontWeight = FontWeight.Bold) }
                        TextButton(onClick = { vm.removeProduct(prod.id) }) { Text("حذف", color = Color(0xFFFB7185), fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductEditorCard(vm: SellerViewModel, current: ProductRow?, onClose: () -> Unit) {
    var name by remember(current) { mutableStateOf(current?.name.orEmpty()) }
    var price by remember(current) { mutableStateOf(current?.price?.toString().orEmpty()) }
    var desc by remember(current) { mutableStateOf(current?.description.orEmpty()) }
    var content by remember(current) { mutableStateOf(current?.deliveryContent.orEmpty()) }
    var enabled by remember(current) { mutableStateOf(current?.enabled ?: true) }
    val field = siteFieldColors()

    HeroCard {
        Text(if (current == null) "ساخت محصول جدید" else "ویرایش محصول", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("نام محصول") }, singleLine = true, colors = field, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text("قیمت (تومان)") }, singleLine = true, colors = field, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(desc, { desc = it }, Modifier.fillMaxWidth(), label = { Text("توضیح کوتاه") }, colors = field, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(content, { content = it }, Modifier.fillMaxWidth(), label = { Text("محتوای تحویل خودکار پس از پرداخت") }, colors = field, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("محصول فعال باشد", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                GradientButton("ذخیره محصول") {
                    vm.saveProduct(current?.id, name, price.toLongOrNull() ?: 0L, desc, "text", content, enabled)
                    onClose()
                }
            }
            TextButton(onClick = onClose) { Text("بستن", color = Muted) }
        }
    }
}

@Composable
private fun CardsSubScreen(vm: SellerViewModel, showBack: Boolean = true) {
    LaunchedEffect(Unit) { vm.loadCards() }
    var number by rememberSaveable { mutableStateOf("") }
    var holder by rememberSaveable { mutableStateOf("") }
    var bank by rememberSaveable { mutableStateOf("") }
    var sheba by rememberSaveable { mutableStateOf("") }
    val field = siteFieldColors()

    if (showBack) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("کارت‌های بانکی و درگاه", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            SitePill("بازگشت ←", Cyan) { vm.openScreen("main") }
        }
        Spacer(Modifier.height(10.dp))
    }

    vm.cardsList.forEach { card ->
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = BlueDeep.copy(alpha = 0.28f))
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF0F766E))))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                .padding(18.dp),
        ) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(card.bank.ifBlank { "کارت بانکی تیکارت" }, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    SitePill(if (card.primary) "کارت اصلی" else if (card.paused) "متوقف" else "فعال", Mint)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    card.masked.chunked(4).joinToString("  "),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(card.holder, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (card.sheba.isNotBlank()) {
                    Text(card.sheba, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!card.primary) {
                        SitePill("انتخاب به عنوان اصلی", Cyan) { vm.makePrimary(card.id) }
                    }
                    SitePill(if (card.paused) "فعال‌سازی" else "توقف موقت", Amber) { vm.togglePause(card.id, !card.paused) }
                    SitePill("حذف", Color(0xFFFB7185)) { vm.removeCard(card.id) }
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    GlassCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("چرخش خودکار بین کارت‌ها", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("پخش شدن واریزها بین کارت‌های فعال", color = Muted, fontSize = 12.sp)
            }
            Switch(checked = vm.rotateOn, onCheckedChange = { vm.setRotate(it) })
        }
    }

    Spacer(Modifier.height(12.dp))
    HeroCard {
        Text("افزودن کارت بانکی جدید", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(number, { number = it }, Modifier.fillMaxWidth(), label = { Text("شماره ۱۶ رقمی کارت") }, singleLine = true, colors = field, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(holder, { holder = it }, Modifier.fillMaxWidth(), label = { Text("نام صاحب حساب") }, singleLine = true, colors = field, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(bank, { bank = it }, Modifier.fillMaxWidth(), label = { Text("نام بانک (مثلاً ملت)") }, singleLine = true, colors = field, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(sheba, { sheba = it }, Modifier.fillMaxWidth(), label = { Text("شماره شبا (اختیاری)") }, singleLine = true, colors = field, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(12.dp))
        GradientButton("ثبت کارت بانکی") {
            vm.addCard(number, holder, bank, sheba, "")
            number = ""
            holder = ""
            bank = ""
            sheba = ""
        }
    }
}

@Composable
private fun WalletSubScreen(vm: SellerViewModel) {
    LaunchedEffect(Unit) { vm.loadWallet() }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("کیف پول فروشنده", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        SitePill("بازگشت ←", Cyan) { vm.openScreen("main") }
    }
    Spacer(Modifier.height(12.dp))
    HeroCard {
        Text("موجودی کل کیف پول", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text("${SmsText.faAmount(vm.wallet)} تومان", color = Mint, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    }
    Spacer(Modifier.height(12.dp))
    GlassCard {
        Text("گردش‌های اخیر کیف پول", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        if (vm.walletTx.isEmpty()) {
            Text("هنوز تراکنشی در کیف پول ثبت نشده است.", color = Muted, fontSize = 13.sp)
        } else {
            vm.walletTx.forEach { tx ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tx.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${SmsText.faAmount(kotlin.math.abs(tx.signed))} ت", color = Mint, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun AccountAndPlanTab(vm: SellerViewModel) {
    val clip = LocalClipboardManager.current
    val acc = vm.account
    LaunchedEffect(Unit) {
        if (vm.offers.isEmpty()) vm.loadPlans()
    }

    Text("حساب، کلید API و اشتراک", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Text(acc?.name ?: "فروشنده تیکارت", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(12.dp))

    HeroCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("پلن فعلی: ${acc?.planTitle ?: "اشتراک فروشنده"}", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("شماره حساب: ${acc?.phone.orEmpty()}", color = Muted, fontSize = 12.sp)
            }
            SitePill(
                if (acc?.subActive == true) "${SmsText.faDigits((acc.daysLeft).toLong())} روز فعال" else "نیاز به تمدید",
                if (acc?.subActive == true) Mint else Amber,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickActionChip("کارت‌های بانکی", Icons.Rounded.CreditCard) { vm.openScreen("cards") }
            QuickActionChip("کیف پول (${SmsText.faAmount(vm.wallet)} ت)", Icons.Rounded.AccountBalanceWallet) { vm.openScreen("wallet") }
        }
    }

    Spacer(Modifier.height(12.dp))
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Key, null, tint = Cyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("کلید API سایت و افزونه ووکامرس", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Page.copy(alpha = 0.7f))
                .border(1.dp, Line, RoundedCornerShape(14.dp))
                .padding(12.dp),
        ) {
            Text(
                acc?.apiKey?.ifBlank { "برای دریافت کلید API، اشتراک فعال داشته باشید" } ?: "—",
                color = Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallGradientButton(if (vm.copied) "کپی شد ✓" else "کپی کلید API") {
                acc?.apiKey?.takeIf { it.isNotBlank() }?.let {
                    clip.setText(AnnotatedString(it))
                    vm.copied = true
                }
            }
            TextButton(onClick = { vm.regenKey() }) { Text("تولید مجدد کلید", color = Amber, fontWeight = FontWeight.Bold) }
        }
    }

    Spacer(Modifier.height(12.dp))
    GlassCard {
        Text("خرید یا تمدید مستقیم اشتراک", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        vm.invoice?.let { inv ->
            HeroCard(Modifier.padding(bottom = 12.dp)) {
                Text("فاکتور اشتراک (${SmsText.faDigits(inv.months.toLong())} ماهه)", color = Mint, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text("مبلغ دقیق واریزی: ${SmsText.faAmount(inv.amount)} تومان", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text("به شماره کارت: ${inv.card}", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("به نام ${inv.holder} (${inv.bank})", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                SitePill("کپی شماره کارت", Mint) { clip.setText(AnnotatedString(inv.card)) }
            }
        }
        vm.offers.forEach { offer ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Page.copy(alpha = 0.55f))
                    .border(1.dp, Line, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${offer.title} · ${SmsText.faDigits(offer.months.toLong())} ماهه", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text("${SmsText.faAmount(offer.price)} تومان", color = Mint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                SmallGradientButton("انتخاب پلن") { vm.buy(offer.tier, offer.months) }
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    Button(
        onClick = { vm.logout() },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.16f), contentColor = Color(0xFFFB7185)),
    ) {
        Text("خروج از حساب کاربری", fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Cyan.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
            .border(1.dp, Line, RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun HeroCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(24.dp), spotColor = Cyan.copy(alpha = 0.24f))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(HeroTop, HeroBottom)))
            .border(1.dp, HeroLine, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) { content() }
}

@Composable
private fun SitePill(text: String, color: Color, onClick: (() -> Unit)? = null) {
    val mod = Modifier
        .clip(RoundedCornerShape(99.dp))
        .background(color.copy(alpha = 0.14f))
        .border(1.dp, color.copy(alpha = 0.36f), RoundedCornerShape(99.dp))
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(horizontal = 10.dp, vertical = 4.dp)
    Box(mod) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun GradientButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(14.dp, RoundedCornerShape(99.dp), spotColor = Cyan.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(99.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF2563EB)))
                else Brush.horizontalGradient(listOf(Line, Line)),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun SmallGradientButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF2563EB))))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun siteFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedBorderColor = Cyan,
    unfocusedBorderColor = Line,
    focusedLabelColor = Cyan,
    unfocusedLabelColor = Muted,
    cursorColor = Cyan,
    focusedContainerColor = CardTop,
    unfocusedContainerColor = CardBottom,
)

@Composable
private fun PayPop(amount: Long?, status: String, detail: String, onDone: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { armed = true }
    val scale by animateFloatAsState(
        targetValue = if (armed) 1f else 0.82f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "payPop",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC040812))
            .clickable { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .scale(scale)
                .shadow(36.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFF10B981))
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0D2228), Color(0xFF091322), Color(0xFF060B14)),
                    ),
                )
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF22D3EE))),
                    RoundedCornerShape(32.dp),
                )
                .padding(horizontal = 28.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandLogo(68)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.18f))
                    .border(1.dp, Color(0xFF34D399).copy(alpha = 0.5f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    status.ifBlank { "واریز جدید تأیید شد" },
                    color = Color(0xFF6EE7B7),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (amount != null && amount > 0) "+${SmsText.faAmount(amount)} تومان" else "پرداخت جدید",
                color = Color.White,
                fontSize = 31.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (detail.isBlank() || detail == "نمونه") "تأیید خودکار توسط موتور پیامک تیکارت" else detail,
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
