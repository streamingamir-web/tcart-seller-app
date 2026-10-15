package ir.tcart.seller.ui

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Vibration
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.tcart.seller.R
import ir.tcart.seller.sms.SmsText

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
    blue = Color(0xFF38BDF8),
    blueDeep = Color(0xFF2563EB),
    page = Color(0xFF06080F),
    card = Color(0xFF0F172A),
    line = Color(0xFF1E293B),
    mint = Color(0xFF34D399),
)

private val LightPalette = Palette(
    ink = Color(0xFF0F172A),
    muted = Color(0xFF475569),
    blue = Color(0xFF2563EB),
    blueDeep = Color(0xFF1D4ED8),
    page = Color(0xFFF6F9FF),
    card = Color(0xFFFFFFFF),
    line = Color(0xFFDBEAFE),
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
    val pal = if (vm.light) LightPalette else DarkPalette
    MaterialTheme(
        colorScheme = if (vm.light) {
            lightColorScheme(primary = pal.blue, background = pal.page, surface = pal.card)
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
                vm.celebrate?.let { PayPop(it.amount, it.status, it.detail) { vm.celebrate = null } }
            }
        }
    }
}

@Composable
private fun BrandLogo(size: Int = 88) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size((size + 44).dp)
                .background(Brush.radialGradient(listOf(Blue.copy(alpha = 0.28f), Mint.copy(alpha = 0.14f), Color.Transparent))),
        )
        Image(
            painter = painterResource(R.drawable.tcart_logo),
            contentDescription = "تیکارت",
            modifier = Modifier
                .size(size.dp)
                .shadow(16.dp, RoundedCornerShape((size / 4).dp))
                .clip(RoundedCornerShape((size / 4).dp)),
        )
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { BrandLogo(96) }
}

@Composable
private fun Welcome(vm: SellerViewModel) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        BrandLogo(104)
        Spacer(Modifier.height(24.dp))
        Text("به تیکارت خوش آمدید", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text(
            "پنل کامل فروشنده، دقیقاً با ظاهر سایت + اتصال خودکار پیامک بانک روی همین گوشی.",
            color = Muted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 25.sp,
        )
        Spacer(Modifier.weight(1f))
        PrimaryPill("شروع") { vm.startIntro() }
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
        PermPage("اعلان‌های واریز", "وقتی پول به کارتت بنشیند، نوتیفیکیشن فوری با مبلغ تأییدشده می‌آید.", Icons.Rounded.Notifications),
        PermPage("دریافت پیامک بانک", "پیامک سرشماره‌های بانکی که انتخاب می‌کنی خودکار برای تیکارت ارسال می‌شود.", Icons.Rounded.Sms),
        PermPage("اجرا بعد از روشن شدن", "در تنظیمات برنامه، اجرای خودکار را روشن کن تا همیشه آماده بماند.", Icons.Rounded.Vibration),
        PermPage("فعال ماندن در پس‌زمینه", "اجازه بده گوشی تیکارت را در پس‌زمینه نبندد.", Icons.Rounded.PowerSettingsNew),
    )
    val page = pages[vm.permStep.coerceIn(0, 3)]
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .height(6.dp)
                        .width(if (i == vm.permStep) 26.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (i <= vm.permStep) BlueDeep else Line),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(116.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(listOf(BlueDeep.copy(alpha = 0.16f), Mint.copy(alpha = 0.12f))))
                .border(1.dp, Blue.copy(alpha = 0.28f), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(page.icon, null, tint = BlueDeep, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(page.title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text(page.body, color = Muted, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 25.sp)
        Spacer(Modifier.weight(1f))
        PrimaryPill("دادن مجوز") {
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
        TextButton(onClick = { vm.nextPerm() }) { Text("رد شدن", color = Muted) }
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
        focusedBorderColor = BlueDeep,
        unfocusedBorderColor = Line,
        focusedLabelColor = BlueDeep,
        unfocusedLabelColor = Muted,
        cursorColor = BlueDeep,
        focusedContainerColor = Card,
        unfocusedContainerColor = Card,
    )
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        BrandLogo(84)
        Spacer(Modifier.height(16.dp))
        Text("ورود به پنل تیکارت", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("با شماره موبایل و رمز عبور سایت وارد شوید", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        SurfaceCard {
            OutlinedTextField(
                phone,
                { phone = it },
                Modifier.fillMaxWidth(),
                label = { Text("شماره موبایل") },
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
                Text(it, color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(18.dp))
            PrimaryPill(if (vm.busy) "در حال ورود…" else "ورود به پنل", enabled = !vm.busy) {
                vm.login(phone, password)
            }
        }
    }
}

@Composable
private fun Panel(vm: SellerViewModel) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.weight(1f)) {
            if (vm.tab == 0) {
                LiveWebPanel(vm)
            } else {
                SmsBankScreen(vm)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Card)
                .border(1.dp, Line)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BottomPill(
                label = "پنل کامل سایت",
                icon = Icons.Rounded.Dashboard,
                selected = vm.tab == 0,
                modifier = Modifier.weight(1f),
            ) { vm.tab = 0 }
            BottomPill(
                label = if (vm.listening) "پیامک بانک (فعال)" else "اتصال پیامک بانک",
                icon = Icons.Rounded.Sms,
                selected = vm.tab == 1,
                badgeColor = if (vm.listening) Mint else Color(0xFFF59E0B),
                modifier = Modifier.weight(1f),
            ) { vm.tab = 1 }
        }
    }
}

@Composable
private fun BottomPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    badgeColor: Color? = null,
    onClick: () -> Unit,
) {
    val bg = if (selected) Brush.horizontalGradient(listOf(BlueDeep, Color(0xFF0EA5E9))) else Brush.horizontalGradient(listOf(Card, Card))
    val content = if (selected) Color.White else Ink
    Row(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, if (selected) Color.Transparent else Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = content, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (badgeColor != null) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(badgeColor))
        }
    }
}

/**
 * خودِ سایت `https://tcartt.com/panel` با فونت وزیرمتن، گرادیان‌ها، نمودار استرایپ و تمام صفحات واقعی پنل.
 * توکن ورود فروشنده مستقیم تزریق می‌شود تا دوباره لاگین نخواهد.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LiveWebPanel(vm: SellerViewModel) {
    val token = vm.account?.token?.takeIf { it.isNotBlank() } ?: vm.getSavedToken()
    val mode = if (vm.light) "light" else "dark"
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }

    BackHandler(enabled = webViewRef?.canGoBack() == true) {
        webViewRef?.goBack()
    }

    LaunchedEffect(mode) {
        webViewRef?.evaluateJavascript(
            "try{localStorage.setItem('tcart_color_mode_v2','$mode');document.documentElement.dataset.colorMode='$mode';document.body.dataset.colorMode='$mode';}catch(e){}",
            null,
        )
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.textZoom = 100
                    val bootJs = """
                        try {
                          if ('$token') localStorage.setItem('tcart_token', '$token');
                          localStorage.setItem('tcart_color_mode_v2', '$mode');
                          document.documentElement.dataset.colorMode = '$mode';
                        } catch (e) {}
                    """.trimIndent()
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                            view.evaluateJavascript(bootJs, null)
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            view.evaluateJavascript(bootJs, null)
                            loading = false
                        }

                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
                    }
                    loadUrl("https://tcartt.com/panel")
                    webViewRef = this
                }
            },
        )
        if (loading) {
            Box(Modifier.fillMaxSize().background(Page), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandLogo(76)
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(color = BlueDeep, strokeWidth = 3.dp)
                }
            }
        }
    }
}

@Composable
private fun SmsBankScreen(vm: SellerViewModel) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.refreshSenders()
    }
    LaunchedEffect(Unit) {
        if (vm.smsGranted()) vm.refreshSenders()
    }
    val field = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Ink,
        unfocusedTextColor = Ink,
        focusedBorderColor = BlueDeep,
        unfocusedBorderColor = Line,
        focusedLabelColor = BlueDeep,
        unfocusedLabelColor = Muted,
        cursorColor = BlueDeep,
        focusedContainerColor = Card,
        unfocusedContainerColor = Card,
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.tcart_logo),
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("اتصال خودکار پیامک بانک", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (vm.listening) "فعال · ${vm.line}" else "سرشمارهٔ بانک‌هایت را انتخاب کن",
                    color = if (vm.listening) Mint else Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TextButton(onClick = { vm.toggleTheme() }) {
                Text(if (vm.light) "تیره" else "روشن", color = BlueDeep, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(14.dp))
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("تست نوتیفیکیشن واریز", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("ببین وقتی پول می‌آید اعلان و انیمیشن چطور نمایش داده می‌شود.", color = Muted, fontSize = 12.sp)
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = { vm.previewNotification() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueDeep, contentColor = Color.White),
                ) {
                    Text("اعلان نمونه", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SurfaceCard {
            Text("سرشماره‌های پیامک بانک", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "شماره‌هایی که بانک از آن‌ها برایت پیامک واریز می‌فرستد را تیک بزن. پیامک بقیه شماره‌ها اصلاً فرستاده نمی‌شود.",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(12.dp))
            if (!vm.smsGranted()) {
                PrimaryPill("دادن دسترسی خواندن پیامک") { launcher.launch(vm.permissions()) }
                Spacer(Modifier.height(10.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    vm.manualSender,
                    { vm.manualSender = it },
                    Modifier.weight(1f),
                    label = { Text("افزودن سرشماره (مثلاً 982000...)") },
                    singleLine = true,
                    colors = field,
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { vm.addManualSender() },
                    modifier = Modifier.height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueDeep, contentColor = Color.White),
                ) { Text("افزودن", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(12.dp))
            if (vm.senders.isEmpty()) {
                Text("برای دیدن لیست فرستنده‌های اخیر، دکمهٔ خواندن پیامک‌ها را بزن.", color = Muted, fontSize = 13.sp)
                TextButton(onClick = {
                    if (vm.smsGranted()) vm.refreshSenders() else launcher.launch(vm.permissions())
                }) { Text("خواندن فرستنده‌های پیامک گوشی", color = BlueDeep, fontWeight = FontWeight.Bold) }
            } else {
                vm.senders.forEach { sender ->
                    val on = sender.key in vm.selectedBanks
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (on) BlueDeep.copy(alpha = 0.12f) else Page)
                            .border(1.dp, if (on) BlueDeep else Line, RoundedCornerShape(14.dp))
                            .clickable { vm.toggleBank(sender.key) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(sender.address, color = if (on) BlueDeep else Ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            if (sender.preview.isNotBlank()) {
                                Text(sender.preview, color = Muted, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (on) BlueDeep else Line)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                if (on) "انتخاب شد" else "انتخاب",
                                color = if (on) Color.White else Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (vm.listening) {
                Button(
                    onClick = { vm.pause() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                ) { Text("توقف ارسال خودکار پیامک", fontWeight = FontWeight.Bold) }
            } else {
                PrimaryPill("فعال‌سازی ارسال خودکار این سرشماره‌ها") { vm.startListening() }
            }
            vm.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { vm.refreshSenders() }) { Text("بروزرسانی لیست پیامک", color = BlueDeep) }
            TextButton(onClick = { vm.logout() }) { Text("خروج از حساب", color = Color(0xFFEF4444)) }
        }
    }
}

@Composable
private fun SurfaceCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = BlueDeep.copy(alpha = 0.14f))
            .clip(RoundedCornerShape(22.dp))
            .background(Card)
            .border(1.dp, Line, RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun PrimaryPill(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BlueDeep, contentColor = Color.White),
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable
private fun PayPop(amount: Long?, status: String, detail: String, onDone: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { armed = true }
    val scale by animateFloatAsState(
        targetValue = if (armed) 1f else 0.84f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "payPop",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xB306080F))
            .clickable { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .scale(scale)
                .shadow(28.dp, RoundedCornerShape(30.dp), spotColor = Mint)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF09101E))))
                .border(1.5.dp, Mint.copy(alpha = 0.45f), RoundedCornerShape(30.dp))
                .padding(horizontal = 26.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.tcart_logo),
                    contentDescription = null,
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Text("موتور تیکارت", color = Color(0xFF93C5FD), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Rounded.CheckCircle, null, tint = Mint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (amount != null && amount > 0) "${SmsText.faAmount(amount)} تومان" else "پرداخت جدید",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Mint.copy(alpha = 0.16f))
                    .border(1.dp, Mint.copy(alpha = 0.4f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
            ) {
                Text(
                    status.ifBlank { "پرداخت تأیید شد" },
                    color = Mint,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            if (detail.isNotBlank() && detail != "نمونه") {
                Spacer(Modifier.height(8.dp))
                Text(detail, color = Color(0xFF94A3B8), fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
