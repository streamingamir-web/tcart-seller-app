package ir.tcart.seller.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.tcart.seller.data.PayEvent
import ir.tcart.seller.sms.SmsText
import java.util.Calendar

private val Ink = Color(0xFF0F172A)
private val Muted = Color(0xFF64748B)
private val Blue = Color(0xFF1D4ED8)
private val BlueSoft = Color(0xFFEFF6FF)
private val Mint = Color(0xFF059669)
private val MintSoft = Color(0xFFECFDF5)
private val Page = Color(0xFFF4F7FB)
private val Line = Color(0xFFE2E8F0)

@Composable
fun SellerApp(vm: SellerViewModel = viewModel()) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = Blue,
                onPrimary = Color.White,
                background = Page,
                surface = Color.White,
            ),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Page),
            ) {
                when (vm.stage) {
                    Stage.Boot -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Blue)
                    }
                    Stage.Login -> LoginScreen(vm)
                    Stage.NeedCard -> NeedCardScreen(vm)
                    Stage.Setup -> SetupScreen(vm)
                    Stage.Home -> HomeScreen(vm)
                }
                vm.celebrate?.let { Celebrate(it.amount, it.status, it.detail) { vm.celebrate = null } }
            }
        }
    }
}

@Composable
private fun LoginScreen(vm: SellerViewModel) {
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("تیکارت", color = Blue, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("اپ فروشنده", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "همان شماره و رمزی که با آن وارد پنل سایت می‌شوی.",
            color = Muted,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("شماره موبایل") },
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("رمز عبور") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        vm.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color(0xFFB91C1C), fontSize = 14.sp)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { vm.login(phone, password) },
            enabled = !vm.busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
        ) {
            Text(if (vm.busy) "در حال ورود…" else "ورود", fontSize = 16.sp)
        }
    }
}

@Composable
private fun NeedCardScreen(vm: SellerViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("اول کارت را در سایت ثبت کن", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "اپ وقتی کار می‌کند که در پنل سایت، کارت بانکی ثبت شده باشد. همان‌جا کلید پیامک ساخته می‌شود. بعد برگرد و دوباره چک کن.",
            color = Muted,
            fontSize = 16.sp,
            lineHeight = 26.sp,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { vm.restore() },
            enabled = !vm.busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
        ) { Text("دوباره چک کن") }
        TextButton(onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth()) { Text("خروج") }
    }
}

@Composable
private fun SetupScreen(vm: SellerViewModel) {
    val context = LocalContext.current
    var granted by rememberSaveable { mutableStateOf(vm.smsGranted()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = vm.smsGranted()
        if (granted) vm.reloadSims()
    }
    LaunchedEffect(vm.stage) {
        granted = vm.smsGranted()
        if (granted) vm.reloadSims()
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("یک بار تنظیم کن", color = Blue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text("بعدش دیگر با پیامک دستی کار نداری", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "اجازه می‌دهی پیامک این گوشی خوانده شود. رمز یکبارمصرف فرستاده نمی‌شود. فقط پیام همین خطی که انتخاب می‌کنی به تیکارت می‌رود تا واریز مشتری خودکار تأیید شود.",
            color = Muted,
            fontSize = 15.sp,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { launcher.launch(vm.permissions()) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
        ) { Text(if (granted) "دسترسی داده شده" else "اجازه پیامک و اعلان") }
        Spacer(Modifier.height(10.dp))
        TextButton(
            onClick = { vm.askBattery(context) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("اجازه بده گوشی اپ را در پس‌زمینه نبندد") }
        if (granted) {
            Spacer(Modifier.height(8.dp))
            Text("کدام خط، پیامک بانک را می‌گیرد؟", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            vm.sims.forEach { sim ->
                val on = sim.id == vm.picked
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) BlueSoft else Color.White)
                        .border(1.dp, if (on) Blue else Line, RoundedCornerShape(16.dp))
                        .clickable {
                            granted = true
                            vm.pick(sim.id)
                        }
                        .padding(16.dp),
                ) {
                    Text(sim.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(sim.detail, color = Muted, fontSize = 13.sp)
                }
            }
            Button(
                onClick = {
                    granted = vm.smsGranted()
                    if (granted) vm.startListening() else launcher.launch(vm.permissions())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint),
            ) { Text("همین خط وصل شود") }
        }
        vm.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color(0xFFB91C1C), fontSize = 14.sp)
        }
    }
}

@Composable
private fun HomeScreen(vm: SellerViewModel) {
    LaunchedEffect(Unit) { vm.refreshHistory() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(vm.name.ifBlank { "فروشنده" }, color = Muted, fontSize = 13.sp)
                Text("وصل است", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(MintSoft)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("فعال", color = Mint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.dp, Line, RoundedCornerShape(18.dp))
                .padding(16.dp),
        ) {
            Text("خط انتخاب‌شده", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(vm.line.ifBlank { "همین گوشی" }, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "یک نوار کوچک «تیکارت وصل است» بالای صفحه می‌ماند تا گوشی اپ را نبندد. اعلان قشنگ فقط وقتی می‌آید که پول تأیید شود.",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { vm.previewNotification() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
        ) { Text("نمایش یک اعلان نمونه") }
        Spacer(Modifier.height(22.dp))
        Text("آخرین پیام‌ها", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        if (vm.history.isEmpty()) {
            Text("هنوز واریزی از این خط نرسیده.", color = Muted, fontSize = 14.sp)
        } else {
            vm.history.forEach { event -> HistoryRow(event) }
        }
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = { vm.pause() }, modifier = Modifier.fillMaxWidth()) { Text("عوض کردن خط") }
        TextButton(onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth()) { Text("خروج از حساب") }
    }
}

@Composable
private fun HistoryRow(event: PayEvent) {
    val tone = if (event.kind == "paid") Mint else Muted
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(event.title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(event.detail, color = Muted, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (event.amount > 0) {
                Text("${SmsText.faAmount(event.amount)} تومان", color = tone, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Text(clock(event.at), color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun Celebrate(amount: Long?, status: String, detail: String, onDone: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pop",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC0F172A))
            .clickable { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(28.dp)
                .scale(scale)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Mint, modifier = Modifier.height(54.dp).width(54.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                if (amount != null && amount > 0) "${SmsText.faAmount(amount)} تومان" else "پرداخت جدید",
                color = Ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(status, color = Mint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(detail, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

private fun clock(at: Long): String {
    if (at <= 0) return ""
    val cal = Calendar.getInstance().apply { timeInMillis = at }
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = cal.get(Calendar.MINUTE)
    return "%02d:%02d".format(h, m).map { ch ->
        if (ch.isDigit()) SmsText.faDigits(ch.digitToInt().toLong()) else ch.toString()
    }.joinToString("")
}
