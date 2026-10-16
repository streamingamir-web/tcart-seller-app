package ir.tcart.seller.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.tcart.seller.R
import ir.tcart.seller.data.Fmt
@Composable
fun SellerApp(vm: SellerViewModel = viewModel()) {
    val pal = if (vm.light) LightStudioPalette else DarkStudioPalette
    val view = LocalView.current
    val activity = LocalContext.current as? Activity
    SideEffect {
        val window = activity?.window
        if (window != null) {
            window.statusBarColor = pal.page.toArgb()
            window.navigationBarColor = pal.page.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = vm.light
            controller.isAppearanceLightNavigationBars = vm.light
        }
    }
    val notify = remember(vm) { { text: String, error: Boolean -> vm.say(text, error) } }

    MaterialTheme(
        colorScheme = if (vm.light) {
            lightColorScheme(primary = pal.blueDeep, background = pal.page, surface = pal.cardTop)
        } else {
            darkColorScheme(primary = pal.cyan, background = pal.page, surface = pal.cardTop)
        },
        typography = SiteTypography,
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalPalette provides pal,
            LocalNotify provides notify,
        ) {
            BackHandler(enabled = vm.stage == Stage.Panel && (vm.dest != Dest.Home)) { vm.back() }
            BackHandler(enabled = vm.stage == Stage.Auth && vm.authMode != AuthMode.Login) {
                vm.switchAuth(AuthMode.Login)
            }
            AuroraBackdrop {
                when (vm.stage) {
                    Stage.Splash -> Splash()
                    Stage.Welcome -> Welcome(vm)
                    Stage.Permissions -> Permissions(vm)
                    Stage.Auth -> AuthScreen(vm)
                    Stage.Panel -> PanelShell(vm)
                }
                vm.flash?.let { FlashBar(it) }
                vm.celebrate?.let { PayPop(it.amount, it.status, it.detail) { vm.celebrate = null } }
            }
        }
    }
}

@Composable
private fun FlashBar(flash: Flash) {
    val color = if (flash.error) Danger else Mint
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            flash.text,
            color = Ink,
            fontSize = 13.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(18.dp), spotColor = color)
                .clip(RoundedCornerShape(18.dp))
                .background(CardTop)
                .border(1.5.dp, color.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun PanelShell(vm: SellerViewModel) {
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        TopBar(vm)
        Box(Modifier.weight(1f)) {
            val acc = vm.account
            if (acc == null) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = Cyan, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("در حال دریافت اطلاعات حساب…", color = Muted, fontSize = 13.sp)
                }
            } else {
                key(vm.dest) {
                    val scroll = rememberScrollState()
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        when (vm.dest) {
                            Dest.Home -> DashboardScreen(vm, acc)
                            Dest.Products -> ProductsScreen(vm, acc)
                            Dest.Orders -> OrdersScreen(vm, acc)
                            Dest.Cards -> CardsScreen(vm, acc)
                            Dest.More -> MoreScreen(vm, acc)
                            Dest.Sms -> SmsScreen(vm, acc)
                            Dest.Subscription -> SubscriptionScreen(vm, acc)
                            Dest.Api -> ApiScreen(vm, acc)
                            Dest.Vendors -> VendorsScreen(vm, acc)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
        BottomDock(vm)
    }
}

private fun titleOf(dest: Dest): String = when (dest) {
    Dest.Home -> "داشبورد"
    Dest.Products -> "محصولات"
    Dest.Orders -> "فروش‌ها"
    Dest.Cards -> "کارت‌های بانکی"
    Dest.More -> "بیشتر"
    Dest.Sms -> "پیامک بانک"
    Dest.Subscription -> "اشتراک"
    Dest.Api -> "API و افزونه"
    Dest.Vendors -> "فروشندگان پلتفرم"
}

@Composable
private fun TopBar(vm: SellerViewModel) {
    val sub = !vm.isRoot(vm.dest)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(CardTop.copy(alpha = 0.95f), CardBottom.copy(alpha = 0.9f))))
            .border(0.5.dp, Line)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (sub) {
            IconPill(Icons.AutoMirrored.Rounded.ArrowBack) { vm.back() }
        } else {
            Image(
                painter = painterResource(R.drawable.tcart_logo),
                contentDescription = "تیکارت",
                modifier = Modifier
                    .size(38.dp)
                    .shadow(10.dp, RoundedCornerShape(12.dp), spotColor = Mint)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("پنل فروشنده", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(titleOf(vm.dest), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(99.dp))
                .background((if (vm.listening) Mint else Amber).copy(alpha = 0.14f))
                .border(1.dp, (if (vm.listening) Mint else Amber).copy(alpha = 0.36f), RoundedCornerShape(99.dp))
                .clickable { vm.go(Dest.Sms) }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (vm.listening) Mint else Amber))
            Spacer(Modifier.width(5.dp))
            Text(
                if (vm.listening) "پیامک وصل" else "پیامک خاموش",
                color = if (vm.listening) Mint else Amber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(6.dp))
        IconPill(if (vm.light) Icons.Rounded.DarkMode else Icons.Rounded.LightMode) { vm.toggleTheme() }
        Spacer(Modifier.width(6.dp))
        IconPill(Icons.Rounded.Refresh) { vm.refreshAll() }
    }
}

private data class DockItem(val dest: Dest, val label: String, val icon: ImageVector)

@Composable
private fun BottomDock(vm: SellerViewModel) {
    val items = listOf(
        DockItem(Dest.Home, "خانه", Icons.Rounded.Home),
        DockItem(Dest.Products, "محصولات", Icons.Rounded.Inventory2),
        DockItem(Dest.Orders, "فروش‌ها", Icons.Rounded.Payments),
        DockItem(Dest.Cards, "کارت‌ها", Icons.Rounded.CreditCard),
        DockItem(Dest.More, "بیشتر", Icons.Rounded.MoreHoriz),
    )
    val root = vm.activeRoot()
    val pending = vm.stats?.pending ?: 0
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(CardTop.copy(alpha = 0.97f), CardBottom)))
            .border(0.5.dp, Line)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { item ->
            val active = root == item.dest
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (active) Brush.linearGradient(listOf(BlueDeep.copy(alpha = 0.14f), Cyan.copy(alpha = 0.10f)))
                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                    )
                    .border(1.dp, if (active) Cyan.copy(alpha = 0.28f) else Color.Transparent, RoundedCornerShape(16.dp))
                    .clickable { vm.go(item.dest) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    Icon(item.icon, null, tint = if (active) Accent else Muted, modifier = Modifier.size(22.dp))
                    if (item.dest == Dest.Orders && pending > 0) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Amber),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    item.label,
                    color = if (active) Ink else Muted,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

/** وقتی اشتراک فعال نیست، صفحه‌های محصول/فروش/کارت قفل‌اند؛ مثل سایت. */
@Composable
internal fun LockedGate(vm: SellerViewModel, what: String) {
    EmptyState(
        icon = Icons.Rounded.Lock,
        title = "برای $what اشتراک لازم است",
        text = "اشتراک تیکارت را فعال کن؛ بعد محصول، کارت بانکی و پیامک را وصل می‌کنی و فروش شروع می‌شود. پرداخت اشتراک هم از همین اپ و کارت‌به‌کارت است.",
        actionLabel = "انتخاب اشتراک",
        onAction = { vm.go(Dest.Subscription) },
    )
}

@Composable
internal fun Splash() {
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
                .background(Brush.verticalGradient(listOf(Color(0xFF0D2228), Color(0xFF091322), Color(0xFF060B14))))
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
                if (amount != null && amount > 0) "+${Fmt.toman(amount)}" else "پرداخت جدید",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (detail.isBlank() || detail == "نمونه") "مبلغ با موفقیت به حسابت نشست" else "$detail · به حسابت نشست",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
