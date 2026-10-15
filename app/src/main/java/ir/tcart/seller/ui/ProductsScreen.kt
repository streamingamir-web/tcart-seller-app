package ir.tcart.seller.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.tcart.seller.data.Account
import ir.tcart.seller.data.Fmt
import ir.tcart.seller.data.Product
import ir.tcart.seller.data.ProductInput

private fun deliveryLabel(type: String): String = when (type) {
    "link" -> "تحویل لینک"
    "text" -> "تحویل متن/کد"
    "file" -> "تحویل فایل"
    else -> "تحویل دستی"
}

@Composable
internal fun ProductsScreen(vm: SellerViewModel, acc: Account) {
    if (acc.locked) {
        PageHeader("فروش اینترنتی", "محصولات", "لینک خرید برای هر محصول")
        LockedGate(vm, "ساخت محصول")
        return
    }

    var formOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var toDelete by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(vm.newProductRequest) {
        if (vm.newProductRequest) {
            vm.newProductRequest = false
            editingId = null
            formOpen = true
        }
    }

    if (formOpen) {
        val editing = vm.products.firstOrNull { it.id == editingId }
        BackHandler {
            formOpen = false
            editingId = null
            vm.clearPicked()
        }
        ProductForm(vm, editing) {
            formOpen = false
            editingId = null
        }
        return
    }

    PageHeader(
        kicker = "فروش اینترنتی",
        title = "محصولات",
        subtitle = "لینک خرید را برای مشتری بفرست؛ بعد از واریز، تحویل خودکار انجام می‌شود.",
        trailing = {
            SmallGradientButton("محصول جدید") {
                editingId = null
                formOpen = true
            }
        },
    )

    if (!acc.hasCard) {
        Banner("برای ساخت محصول اول باید یک کارت بانکی ثبت کنی تا پول به آن کارت واریز شود.", Tone.Warn)
        OutlineButton("ثبت کارت بانکی", color = Amber) {
            vm.newCardRequest = true
            vm.go(Dest.Cards)
        }
    }

    if (vm.products.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("محصولات فعال", Fmt.num(vm.products.size), "لینک خرید ساخته‌شده", Cyan, Modifier.weight(1f))
            MetricTile("فروش ثبت‌شده", Fmt.num(vm.products.sumOf { it.paidCount }), "روی همهٔ محصولات", Mint, Modifier.weight(1f))
        }
    }

    if (vm.products.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Inventory2,
            title = "اولین محصولت را بساز",
            text = "نام، قیمت و نوع تحویل را بنویس؛ لینک خرید وب و تلگرام همان لحظه آماده می‌شود.",
            actionLabel = "ساخت محصول",
            onAction = { formOpen = true },
        )
    } else {
        vm.products.forEachIndexed { index, product ->
            ProductCard(
                vm = vm,
                product = product,
                index = index,
                onEdit = {
                    editingId = product.id
                    formOpen = true
                },
                onDelete = { toDelete = product },
            )
        }
    }

    toDelete?.let { p ->
        ConfirmDialog(
            title = "حذف «${p.name}»؟",
            text = "اگر این محصول قبلاً سفارش داشته باشد فقط از فروش خارج می‌شود و سوابق می‌ماند.",
            confirmLabel = "حذف محصول",
            danger = true,
            onConfirm = {
                toDelete = null
                vm.deleteProduct(p)
            },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun ProductCard(vm: SellerViewModel, product: Product, index: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val gradient = ProductGradients[index % ProductGradients.size]
    val webUrl = vm.buyUrl(product)
    val tgUrl = vm.telegramUrl(product)
    val shape = RoundedCornerShape(22.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, spotColor = Cyan.copy(alpha = 0.12f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
            .border(1.dp, Line, shape),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(16.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        product.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (product.salesEnabled && !product.soldOut) "فروش فعال" else if (product.soldOut) "تمام شده" else "فروش متوقف",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(Fmt.toman(product.price), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SitePill("کد ${Fmt.num(product.id.toInt())}", Muted)
                SitePill(deliveryLabel(product.deliveryType), Cyan)
                SitePill("${Fmt.num(product.paidCount)} فروش", Mint)
                product.maxPurchases?.let { cap ->
                    val left = product.slotsRemaining
                    SitePill(
                        if (left != null) "ظرفیت ${Fmt.num(cap)} · ${Fmt.num(left)} مانده" else "ظرفیت ${Fmt.num(cap)}",
                        if (product.soldOut) Danger else Violet,
                    )
                }
                (product.paymentExpiry ?: product.effectiveExpiry)?.let { minutes ->
                    SitePill("مهلت ${Fmt.minutes(minutes)}", Amber)
                }
                if (product.deliveryType == "link" && product.linksTotal > 0) {
                    SitePill(
                        if (product.linksShared) "لینک مشترک" else "${Fmt.num(product.linksAvailable)} لینک آزاد",
                        if (!product.linksShared && product.linksAvailable == 0) Danger else Cyan,
                    )
                }
                if (product.deliveryType == "file" && product.deliveryFileName.isNotBlank()) {
                    SitePill(product.deliveryFileName, Cyan)
                }
            }

            if (product.description.isNotBlank()) {
                Text(product.description, color = Muted, fontSize = 12.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }

            CopyField("لینک خرید وب", webUrl)
            if (tgUrl.isNotBlank()) CopyField("لینک خرید تلگرام", tgUrl)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SmallGradientButton("ارسال لینک", Modifier.weight(1f)) {
                    val text = buildString {
                        append(product.name)
                        append(" — ")
                        append(Fmt.toman(product.price))
                        append("\n")
                        append(webUrl)
                        if (tgUrl.isNotBlank()) {
                            append("\nتلگرام: ")
                            append(tgUrl)
                        }
                    }
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    runCatching { context.startActivity(Intent.createChooser(send, "ارسال لینک خرید")) }
                }
                OutlineButton("باز کردن", icon = Icons.Rounded.OpenInNew) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlineButton("ویرایش", Modifier.weight(1f), icon = Icons.Rounded.Edit, onClick = onEdit)
                OutlineButton(
                    "حذف",
                    Modifier.weight(1f),
                    color = Danger,
                    icon = Icons.Rounded.Delete,
                    enabled = !vm.isBusy("product:${product.id}"),
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun DeliveryTile(label: String, hint: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Mint.copy(alpha = 0.12f) else Page.copy(alpha = 0.5f))
            .border(1.dp, if (selected) Mint.copy(alpha = 0.55f) else Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(label, color = if (selected) Ink else Muted, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(2.dp))
        Text(hint, color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun ProductForm(vm: SellerViewModel, editing: Product?, onClose: () -> Unit) {
    val key = editing?.id ?: -1L
    var name by remember(key) { mutableStateOf(editing?.name.orEmpty()) }
    var price by remember(key) { mutableStateOf(editing?.price?.toString().orEmpty()) }
    var desc by remember(key) { mutableStateOf(editing?.description.orEmpty()) }
    var type by remember(key) { mutableStateOf(editing?.deliveryType ?: "manual") }
    var text by remember(key) { mutableStateOf(editing?.takeIf { it.deliveryType == "text" }?.deliveryContent.orEmpty()) }
    var instructions by remember(key) { mutableStateOf(editing?.deliveryInstructions.orEmpty()) }
    var maxPurchases by remember(key) { mutableStateOf(editing?.maxPurchases?.toString().orEmpty()) }
    var expiry by remember(key) { mutableStateOf(editing?.paymentExpiry?.toString().orEmpty()) }
    var salesEnabled by remember(key) { mutableStateOf(editing?.salesEnabled ?: true) }
    val links = remember(key) {
        mutableStateListOf<String>().apply {
            val initial = editing?.takeIf { it.deliveryType == "link" }
                ?.deliveryContent.orEmpty()
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            addAll(if (initial.isEmpty()) listOf("") else initial)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.pickFile(uri)
    }
    val saving = vm.isBusy("product")
    val priceValue = Fmt.digitsOnly(price).toLongOrNull() ?: 0L
    val defaultExpiry = vm.cards?.settings?.effectiveExpiry ?: vm.config?.orderExpiryMinutes ?: 30

    PageHeader(
        kicker = if (editing == null) "محصول جدید" else "ویرایش محصول",
        title = if (editing == null) "ساخت محصول و لینک خرید" else editing.name,
        subtitle = "قیمت و تحویل را تعیین کن؛ لینک خرید بعد از ذخیره آماده است.",
        trailing = { OutlineButton("بستن", color = Muted) { vm.clearPicked(); onClose() } },
    )

    GlassCard {
        SiteField(name, { name = it }, "نام محصول", placeholder = "مثلاً: اشتراک یک‌ماهه")
        Spacer(Modifier.height(12.dp))
        SiteField(
            price,
            { price = Fmt.digitsOnly(it) },
            "قیمت (تومان)",
            ltr = true,
            keyboard = KeyboardType.Number,
            placeholder = "250000",
            helper = if (priceValue > 0) "= ${Fmt.toman(priceValue)}" else "حداقل ۱٬۰۰۰ تومان",
        )
        Spacer(Modifier.height(12.dp))
        SiteField(desc, { desc = it }, "توضیحات (اختیاری)", singleLine = false, minLines = 3)
    }

    GlassCard {
        Text("تحویل بعد از پرداخت", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text("خریدار بعد از تأیید پرداخت، محصول را خودکار دریافت می‌کند.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeliveryTile("دستی", "بعد از پرداخت خودت تحویل می‌دهی", type == "manual", Modifier.weight(1f)) { type = "manual" }
            DeliveryTile("لینک", "گروه تلگرام، ویدیو، دانلود و…", type == "link", Modifier.weight(1f)) { type = "link" }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeliveryTile("متن / کد", "لایسنس، رمز، دستورالعمل", type == "text", Modifier.weight(1f)) { type = "text" }
            DeliveryTile("فایل", "PDF، ZIP، ویدیو و… (تا ۲۵ مگابایت)", type == "file", Modifier.weight(1f)) { type = "file" }
        }

        if (type == "link") {
            Spacer(Modifier.height(14.dp))
            Text("لینک‌های تحویل", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            links.forEachIndexed { i, value ->
                Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SiteField(
                        value,
                        { links[i] = it },
                        "لینک ${Fmt.num(i + 1)}",
                        modifier = Modifier.weight(1f),
                        ltr = true,
                        keyboard = KeyboardType.Uri,
                        placeholder = "https://t.me/+...",
                    )
                    if (links.size > 1) {
                        Spacer(Modifier.width(8.dp))
                        IconPill(Icons.Rounded.Delete, Danger) { links.removeAt(i) }
                    }
                }
            }
            OutlineButton("افزودن لینک", icon = Icons.Rounded.Add) { links.add("") }
            Spacer(Modifier.height(8.dp))
            val filled = links.count { it.isNotBlank() }
            Text(
                if (filled > 1) "با چند لینک، هر خریدار یک لینک جدا می‌گیرد (مناسب لینک یک‌بارمصرف تلگرام)."
                else "با یک لینک، همان لینک برای همهٔ خریداران نمایش داده می‌شود.",
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 18.sp,
            )
            if (editing != null && editing.linksTotal > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    if (editing.linksShared) "لینک مشترک"
                    else "${Fmt.num(editing.linksAvailable)} آزاد، ${Fmt.num(editing.linksAssigned)} اختصاص‌یافته",
                    color = Cyan,
                    fontSize = 11.sp,
                )
            }
        }

        if (type == "text") {
            Spacer(Modifier.height(14.dp))
            SiteField(
                text,
                { text = it },
                "متن / کد تحویل",
                ltr = true,
                singleLine = false,
                minLines = 3,
                placeholder = "لایسنس، رمز عبور یا دستورالعمل…",
            )
        }

        if (type == "file") {
            Spacer(Modifier.height(14.dp))
            Text("فایل محصول", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            val picked = vm.pickedFile
            if (picked != null) {
                Banner("فایل انتخاب‌شده: ${picked.name} (${Fmt.num((picked.bytes.size / 1024).coerceAtLeast(1))} کیلوبایت) — بعد از ذخیره آپلود می‌شود.", Tone.Success)
            } else if (editing != null && editing.hasFile) {
                Banner("فایل فعلی: ${editing.deliveryFileName.ifBlank { "آپلودشده" }} — برای تعویض، فایل جدید انتخاب کن.", Tone.Info)
            } else {
                Banner("هنوز فایلی انتخاب نشده است.", Tone.Warn)
            }
            Spacer(Modifier.height(8.dp))
            OutlineButton(
                if (picked == null) "انتخاب فایل" else "انتخاب فایل دیگر",
                icon = Icons.Rounded.UploadFile,
            ) { picker.launch(arrayOf("*/*")) }
        }

        if (type != "manual") {
            Spacer(Modifier.height(14.dp))
            SiteField(
                instructions,
                { instructions = it },
                "پیام همراه تحویل (اختیاری)",
                placeholder = "مثلاً: لینک ۴۸ ساعت معتبر است",
            )
        }
    }

    GlassCard {
        SiteField(
            maxPurchases,
            { maxPurchases = Fmt.digitsOnly(it) },
            "حداکثر تعداد خرید",
            ltr = true,
            keyboard = KeyboardType.Number,
            placeholder = "خالی = نامحدود",
            helper = "سفارش‌های باز و پرداخت‌شده شمرده می‌شوند.",
        )
        Spacer(Modifier.height(12.dp))
        SiteField(
            expiry,
            { expiry = Fmt.digitsOnly(it) },
            "مهلت واریز (دقیقه)",
            ltr = true,
            keyboard = KeyboardType.Number,
            placeholder = "پیش‌فرض: ${Fmt.num(defaultExpiry)}",
            helper = "خالی بگذار تا مهلت پیش‌فرض حساب (${Fmt.minutes(defaultExpiry)}) اعمال شود.",
        )
        Spacer(Modifier.height(14.dp))
        SwitchRow("فروش فعال", "با خاموش کردن، لینک خرید موقتاً سفارش نمی‌گیرد.", salesEnabled) { salesEnabled = it }
    }

    GradientButton(if (editing == null) "ساخت محصول" else "ذخیرهٔ تغییرات", loading = saving) {
        val content = when (type) {
            "link" -> links.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
            "text" -> text
            else -> ""
        }
        val cap = Fmt.digitsOnly(maxPurchases).toIntOrNull()?.takeIf { it > 0 }
        val minutes = Fmt.digitsOnly(expiry).toIntOrNull()?.takeIf { it > 0 }
        val input = ProductInput(
            name = name.trim(),
            priceToman = priceValue,
            description = desc.trim(),
            deliveryType = type,
            deliveryContent = content,
            deliveryInstructions = if (type == "manual") "" else instructions.trim(),
            maxPurchases = cap,
            salesEnabled = salesEnabled,
            paymentExpiryMinutes = minutes,
        )
        vm.saveProduct(editing, input, if (type == "file") vm.pickedFile else null) { onClose() }
    }
}
