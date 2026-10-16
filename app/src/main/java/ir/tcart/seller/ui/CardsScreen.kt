package ir.tcart.seller.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.tcart.seller.data.Account
import ir.tcart.seller.data.BankCard
import ir.tcart.seller.data.CardsData
import ir.tcart.seller.data.Fmt

private val BinBanks = listOf(
    "603799" to "ملی", "170019" to "ملی", "606373" to "مهر", "603770" to "کشاورزی",
    "589210" to "سپه", "627353" to "تجارت", "585983" to "تجارت", "627412" to "اقتصاد نوین",
    "627648" to "گردشگری", "502229" to "پاسارگاد", "639347" to "پاسارگاد", "621986" to "سامان",
    "627381" to "انصار", "622106" to "پارسیان", "627884" to "پارسیان", "639599" to "سینا",
    "639346" to "سینا", "627961" to "پست بانک", "610433" to "ملت", "991975" to "ملت",
    "627488" to "کارآفرین", "502806" to "شهر", "504172" to "رسالت", "636214" to "آینده",
    "505785" to "ایران زمین", "627760" to "پست بانک", "628023" to "مسکن", "589463" to "رفاه",
)

private val BankSuggestions = listOf("ملت", "ملی", "پاسارگاد", "اقتصاد نوین", "سامان", "پارسیان", "صادرات", "تجارت")

private fun bankFromCard(number: String): String? {
    val digits = Fmt.digitsOnly(number)
    if (digits.length < 6) return null
    val bin = digits.take(6)
    return BinBanks.firstOrNull { bin.startsWith(it.first) }?.second
}

@Composable
internal fun CardsScreen(vm: SellerViewModel, acc: Account) {
    if (acc.locked) {
        PageHeader("مقصد واریز", "کارت‌های بانکی", "کارتی که مشتری به آن واریز می‌کند")
        LockedGate(vm, "ثبت کارت بانکی")
        return
    }

    val data = vm.cards
    var formOpen by rememberSaveable { mutableStateOf(false) }
    var deleteFor by remember { mutableStateOf<BankCard?>(null) }

    LaunchedEffect(Unit) { if (vm.cards == null) vm.loadCards() }
    LaunchedEffect(vm.newCardRequest) {
        if (vm.newCardRequest) {
            vm.newCardRequest = false
            formOpen = true
        }
    }

    if (formOpen) {
        BackHandler { formOpen = false }
        AddCardForm(vm, data?.rotate == true) { formOpen = false }
        return
    }

    PageHeader(
        kicker = "مقصد واریز",
        title = "کارت‌های بانکی",
        subtitle = if (data == null) "در حال دریافت…" else
            "حداکثر ${if (data.maxCards == 0) "نامحدود" else Fmt.num(data.maxCards)} کارت · " +
                if (data.rotate) "چرخش فعال است" else "همهٔ فاکتورها روی کارت اصلی می‌روند",
        trailing = {
            if (data != null && data.canAdd) SmallGradientButton("کارت جدید") { formOpen = true }
        },
    )

    if (data == null) {
        Text("در حال دریافت کارت‌ها…", color = Muted, fontSize = 13.sp)
        return
    }

    if (data.cards.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.CreditCard,
            title = "هنوز کارتی نداری",
            text = "کارتی که می‌خواهی پول فروش به آن واریز شود را ثبت کن. بعد از آن کلید پیامک ساخته می‌شود و می‌توانی محصول بسازی.",
            actionLabel = "افزودن کارت",
            onAction = { formOpen = true },
        )
    }

    PaymentSettingsCard(vm, data)

    GlassCard {
        SwitchRow(
            title = "چرخش خودکار کارت‌ها",
            subtitle = "پیش‌فرض خاموش است. فقط اگر خودت بخواهی روشنش کن.",
            checked = data.rotate,
            enabled = !vm.isBusy("rotation"),
        ) { vm.setRotation(it) }
        Spacer(Modifier.height(10.dp))
        Text(
            "خاموش: مشتری همیشه کارت اصلی (ستاره‌دار) را می‌بیند.\n" +
                "روشن: هر فاکتور جدید نوبتی به یکی از کارت‌های فعال می‌رود و با رسیدن به سقف تراکنش، کارت بعدی استفاده می‌شود. اگر همهٔ کارت‌ها پر یا متوقف باشند، فروش قطع نمی‌شود و کارت اصلی استفاده می‌شود.",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 21.sp,
        )
    }

    data.cards.forEachIndexed { index, card ->
        CardBlock(vm, card, index, data, onDelete = { deleteFor = card })
    }

    if (data.cards.isNotEmpty()) {
        Banner("برای دریافت خودکار پیامک بانک، از بخش «پیامک بانک» سرشمارهٔ بانکت را انتخاب کن.", Tone.Info)
        OutlineButton("رفتن به پیامک بانک") { vm.go(Dest.Sms) }
    }

    deleteFor?.let { card ->
        ConfirmDialog(
            title = "حذف این کارت؟",
            text = "کارت ${Fmt.fa(card.numberFormatted)} از حساب پاک می‌شود.",
            confirmLabel = "حذف کارت",
            danger = true,
            onConfirm = {
                deleteFor = null
                vm.deleteCard(card.id)
            },
            onDismiss = { deleteFor = null },
        )
    }
}

@Composable
private fun PaymentSettingsCard(vm: SellerViewModel, data: CardsData) {
    val s = data.settings
    var showSheba by remember(s.showSheba) { mutableStateOf(s.showSheba) }
    var minutes by remember(s.defaultExpiry) { mutableStateOf(s.defaultExpiry?.toString().orEmpty()) }
    GlassCard {
        SectionTitle("تنظیمات پرداخت مشتری")
        Spacer(Modifier.height(10.dp))
        SwitchRow(
            title = "نمایش شبا روی فاکتور",
            subtitle = "اگر روشن باشد، شمارهٔ شبا کنار کارت به خریدار نشان داده می‌شود (مهلت پیش‌فرض ${Fmt.minutes(s.shebaExpiry)}).",
            checked = showSheba,
        ) { showSheba = it }
        Spacer(Modifier.height(12.dp))
        SiteField(
            minutes,
            { minutes = Fmt.digitsOnly(it) },
            "مهلت پیش‌فرض واریز (دقیقه)",
            ltr = true,
            keyboard = KeyboardType.Number,
            placeholder = "خودکار: ${Fmt.num(s.effectiveExpiry)}",
            helper = "بین ۵ تا ۱۴۴۰ دقیقه. خالی = پیش‌فرض سیستم (${Fmt.minutes(s.systemExpiry)}). روی هر محصول هم می‌توانی مهلت جدا بگذاری.",
        )
        Spacer(Modifier.height(12.dp))
        SmallGradientButton("ذخیرهٔ تنظیمات", enabled = !vm.isBusy("pay")) { vm.savePaySettings(showSheba, minutes) }
    }
}

@Composable
private fun CardBlock(vm: SellerViewModel, card: BankCard, index: Int, data: CardsData, onDelete: () -> Unit) {
    var editing by remember(card.id) { mutableStateOf(false) }
    var sheba by remember(card.id, card.sheba) { mutableStateOf(card.sheba) }
    var cap by remember(card.id, card.maxTx) { mutableStateOf(card.maxTx?.toString().orEmpty()) }
    val busy = vm.isBusy("card:${card.id}")
    val colors = ProductGradients[(index + 2) % ProductGradients.size]
    val shape = RoundedCornerShape(24.dp)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(14.dp, shape, spotColor = colors.first().copy(alpha = 0.5f))
                .clip(shape)
                .background(Brush.linearGradient(colors))
                .padding(18.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        card.bank.ifBlank { "کارت بانکی" },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (card.primary) CardBadge("★ کارت اصلی")
                    if (card.paused) {
                        Spacer(Modifier.width(6.dp))
                        CardBadge("متوقف")
                    }
                }
                Spacer(Modifier.height(18.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        Fmt.fa(card.numberFormatted),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Left,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(card.holder, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (card.nickname.isNotBlank()) {
                            Text(card.nickname, color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
                        }
                    }
                    if (data.rotate) {
                        Text(
                            if (card.maxTx != null) "${Fmt.num(card.usedTx)} / ${Fmt.num(card.maxTx)} تراکنش"
                            else "${Fmt.num(card.usedTx)} تراکنش",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        GlassCard {
            if (card.sheba.isNotBlank()) {
                CopyField("شمارهٔ شبا", card.shebaFormatted)
                Spacer(Modifier.height(10.dp))
            }
            CopyField("شمارهٔ کارت", card.number.ifBlank { Fmt.digitsOnly(card.numberFormatted) })
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!card.primary) {
                    OutlineButton("کارت اصلی شود", color = Mint, enabled = !busy) { vm.makePrimary(card.id) }
                }
                if (data.rotate) {
                    OutlineButton(
                        if (card.paused) "فعال‌سازی" else "توقف",
                        color = if (card.paused) Mint else Amber,
                        enabled = !busy,
                    ) { vm.setPaused(card.id, !card.paused) }
                }
                OutlineButton(if (editing) "بستن ویرایش" else "ویرایش شبا و سقف", color = Cyan) { editing = !editing }
                if (data.cards.size > 1) {
                    OutlineButton("حذف", color = Danger, enabled = !busy, onClick = onDelete)
                }
            }
            if (editing) {
                Spacer(Modifier.height(12.dp))
                SiteField(sheba, { sheba = it.uppercase() }, "شمارهٔ شبا", ltr = true, placeholder = "IR120170000000123456789012")
                if (data.rotate) {
                    Spacer(Modifier.height(10.dp))
                    SiteField(
                        cap,
                        { cap = Fmt.digitsOnly(it).take(7) },
                        "سقف تراکنش این کارت",
                        ltr = true,
                        keyboard = KeyboardType.Number,
                        placeholder = "خالی = بدون سقف",
                        helper = "وقتی این تعداد فاکتور باز یا پرداخت‌شده رسید، کارت بعدی استفاده می‌شود.",
                    )
                }
                Spacer(Modifier.height(12.dp))
                SmallGradientButton("ذخیره", enabled = !busy) {
                    vm.saveShebaAndCap(card.id, sheba, cap, data.rotate)
                    editing = false
                }
            }
        }
    }
}

@Composable
private fun CardBadge(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

@Composable
private fun AddCardForm(vm: SellerViewModel, rotation: Boolean, onClose: () -> Unit) {
    var nickname by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }
    var holder by rememberSaveable { mutableStateOf("") }
    var bank by rememberSaveable { mutableStateOf("") }
    var sheba by rememberSaveable { mutableStateOf("") }
    var cap by rememberSaveable { mutableStateOf("") }

    PageHeader(
        kicker = "کارت جدید",
        title = "ثبت کارت بانکی",
        subtitle = "پول فروش مستقیم به همین کارت واریز می‌شود.",
        trailing = { OutlineButton("بستن", color = Muted, onClick = onClose) },
    )
    GlassCard {
        SiteField(nickname, { nickname = it.take(64) }, "نام برای خودت (اختیاری)", placeholder = "مثلاً کارت اینستا")
        Spacer(Modifier.height(12.dp))
        SiteField(
            number,
            {
                val next = Fmt.digitsOnly(it).take(16)
                number = next
                val detected = bankFromCard(next)
                if (detected != null && bank.isBlank()) bank = detected
            },
            "شمارهٔ کارت (۱۶ رقم)",
            ltr = true,
            keyboard = KeyboardType.Number,
            placeholder = "6037xxxxxxxxxxxx",
            helper = "${Fmt.num(number.length)} از ۱۶ رقم",
        )
        Spacer(Modifier.height(12.dp))
        SiteField(holder, { holder = it }, "نام صاحب کارت")
        Spacer(Modifier.height(12.dp))
        SiteField(bank, { bank = it }, "بانک", placeholder = "مثلاً ملت")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BankSuggestions.forEach { b -> FilterPill(b, bank == b) { bank = b } }
        }
        Spacer(Modifier.height(12.dp))
        SiteField(
            sheba,
            { sheba = it.uppercase() },
            "شمارهٔ شبا (اختیاری)",
            ltr = true,
            placeholder = "IR120170000000123456789012",
            helper = "اگر وارد کنی، روی فاکتور خریدار کنار کارت دیده می‌شود.",
        )
        if (rotation) {
            Spacer(Modifier.height(12.dp))
            SiteField(
                cap,
                { cap = Fmt.digitsOnly(it).take(7) },
                "سقف تراکنش این کارت (اختیاری)",
                ltr = true,
                keyboard = KeyboardType.Number,
                placeholder = "خالی = بدون سقف",
            )
        }
        Spacer(Modifier.height(16.dp))
        GradientButton("ذخیره و ثبت کارت", loading = vm.isBusy("card")) {
            vm.addCard(
                number = number,
                holder = holder,
                bank = bank,
                sheba = sheba,
                nickname = nickname,
                maxTx = Fmt.digitsOnly(cap).toIntOrNull()?.takeIf { it > 0 },
                onDone = onClose,
            )
        }
    }
}
