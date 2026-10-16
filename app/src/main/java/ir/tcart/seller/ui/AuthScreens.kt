package ir.tcart.seller.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.tcart.seller.data.Fmt

@Composable
internal fun Welcome(vm: SellerViewModel) {
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
            "محصول بساز، لینک خرید را بفرست و تمام. پرداخت‌ها از پیامک بانک خودت خودکار تأیید می‌شوند و فوری بهت خبر می‌دهیم.",
            color = Muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 25.sp,
        )
        Spacer(Modifier.weight(1f))
        GradientButton("شروع کنیم") { vm.startIntro() }
    }
}

private data class PermPage(val title: String, val body: String, val icon: ImageVector)

@Composable
internal fun Permissions(vm: SellerViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.nextPerm()
    }
    val pages = listOf(
        PermPage("اعلان لحظه‌ای واریز", "وقتی مبلغی به کارتت واریز شود یا سفارشی منتظر تأیید باشد، همان لحظه اعلان می‌گیری.", Icons.Rounded.Notifications),
        PermPage("خواندن پیامک بانک", "فقط پیامک سرشماره‌هایی که خودت انتخاب می‌کنی برای تیکارت ارسال می‌شود؛ رمز پویا همیشه فیلتر می‌شود.", Icons.Rounded.Sms),
        PermPage("اجرای خودکار بعد از روشن شدن", "در تنظیمات گوشی اجرای خودکار تیکارت را فعال کن تا بعد از ری‌استارت هم قطع نشود.", Icons.Rounded.Storefront),
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
        GradientButton("اعطای دسترسی مرحله ${Fmt.num(vm.permStep + 1)}") {
            when (vm.permStep) {
                0, 1 -> {
                    val ok = if (vm.permStep == 0) vm.notifGranted() else vm.smsGranted()
                    if (ok) vm.nextPerm() else launcher.launch(vm.permissions())
                }
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

@Composable
internal fun AuthScreen(vm: SellerViewModel) {
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    val busy = vm.isBusy("auth")
    val mode = vm.authMode

    val (kicker, title, subtitle) = when (mode) {
        AuthMode.Login -> Triple("حساب فروشنده تیکارت", "ورود به پنل فروشنده", "با شماره موبایل (یا ایمیل) و رمز عبور حسابت وارد شو")
        AuthMode.Register -> Triple("شروع رایگان", "ساخت حساب فروشنده", "در چند ثانیه حساب بساز؛ بعد اشتراک، کارت و محصول")
        AuthMode.Forgot -> Triple("بازیابی رمز", "فراموشی رمز عبور", "شمارهٔ ثبت‌شده را بنویس تا کد بازیابی در تلگرام برایت بیاید")
        AuthMode.Reset -> Triple("رمز جدید", "تنظیم رمز جدید", "کدی که در تلگرام گرفتی را با رمز جدید وارد کن")
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(18.dp))
        BrandLogo(80)
        Spacer(Modifier.height(14.dp))
        SitePill(kicker, Cyan)
        Spacer(Modifier.height(10.dp))
        Text(title, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
        Spacer(Modifier.height(20.dp))

        HeroCard {
            when (mode) {
                AuthMode.Login -> {
                    SiteField(identifier, { identifier = it }, "شماره موبایل یا ایمیل", ltr = true, keyboard = KeyboardType.Email, placeholder = "09123456789")
                    Spacer(Modifier.height(12.dp))
                    SiteField(password, { password = it }, "رمز عبور", password = true, ltr = true, keyboard = KeyboardType.Password)
                }
                AuthMode.Register -> {
                    SiteField(name, { name = it }, "نام و نام خانوادگی (اختیاری)")
                    Spacer(Modifier.height(12.dp))
                    SiteField(identifier, { identifier = it }, "شماره موبایل", ltr = true, keyboard = KeyboardType.Phone, placeholder = "09123456789")
                    Spacer(Modifier.height(12.dp))
                    SiteField(email, { email = it }, "ایمیل (اختیاری)", ltr = true, keyboard = KeyboardType.Email)
                    Spacer(Modifier.height(12.dp))
                    SiteField(password, { password = it }, "رمز عبور (حداقل ۸ کاراکتر)", password = true, ltr = true, keyboard = KeyboardType.Password)
                    Spacer(Modifier.height(12.dp))
                    SiteField(confirm, { confirm = it }, "تکرار رمز عبور", password = true, ltr = true, keyboard = KeyboardType.Password)
                }
                AuthMode.Forgot -> {
                    SiteField(identifier, { identifier = it }, "شماره موبایل ثبت‌شده", ltr = true, keyboard = KeyboardType.Phone, placeholder = "09123456789")
                }
                AuthMode.Reset -> {
                    SiteField(identifier, { identifier = it }, "شماره موبایل", ltr = true, keyboard = KeyboardType.Phone)
                    Spacer(Modifier.height(12.dp))
                    SiteField(code, { code = it }, "کد بازیابی", ltr = true, keyboard = KeyboardType.Number)
                    Spacer(Modifier.height(12.dp))
                    SiteField(password, { password = it }, "رمز جدید (حداقل ۸ کاراکتر)", password = true, ltr = true, keyboard = KeyboardType.Password)
                    Spacer(Modifier.height(12.dp))
                    SiteField(confirm, { confirm = it }, "تکرار رمز جدید", password = true, ltr = true, keyboard = KeyboardType.Password)
                }
            }
            vm.authInfo?.let {
                Spacer(Modifier.height(12.dp))
                Banner(it, Tone.Success)
            }
            vm.authError?.let {
                Spacer(Modifier.height(12.dp))
                Banner(it, Tone.Error)
            }
            Spacer(Modifier.height(18.dp))
            val label = when (mode) {
                AuthMode.Login -> "ورود به پنل تیکارت"
                AuthMode.Register -> "ساخت حساب و ورود"
                AuthMode.Forgot -> "ارسال کد بازیابی"
                AuthMode.Reset -> "ثبت رمز جدید و ورود"
            }
            GradientButton(label, loading = busy) {
                when (mode) {
                    AuthMode.Login -> vm.login(identifier, password)
                    AuthMode.Register -> vm.register(name, identifier, email, password, confirm)
                    AuthMode.Forgot -> vm.forgot(identifier)
                    AuthMode.Reset -> vm.reset(identifier, code, password, confirm)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        when (mode) {
            AuthMode.Login -> {
                TextButton(onClick = { vm.switchAuth(AuthMode.Forgot) }) {
                    Text("رمزم را فراموش کرده‌ام", color = Muted, fontSize = 13.sp)
                }
                TextButton(onClick = { vm.switchAuth(AuthMode.Register) }) {
                    Text("حساب ندارم؛ ثبت‌نام می‌کنم", color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            else -> {
                TextButton(onClick = { vm.switchAuth(AuthMode.Login) }) {
                    Text("برگشت به ورود", color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}
