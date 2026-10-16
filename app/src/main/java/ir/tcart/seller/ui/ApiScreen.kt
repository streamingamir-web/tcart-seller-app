package ir.tcart.seller.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import ir.tcart.seller.data.Account
import ir.tcart.seller.data.TcartApi
import ir.tcart.seller.data.Vendor

@Composable
internal fun ApiScreen(vm: SellerViewModel, acc: Account) {
    val context = LocalContext.current
    var confirmRegen by remember { mutableStateOf(false) }

    // ارسال فایل افزونه بعد از دانلود
    val plugin = vm.pluginFile
    LaunchedEffect(plugin) {
        if (plugin != null) {
            runCatching {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", plugin)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "ذخیره یا ارسال افزونهٔ ووکامرس"))
            }
            vm.consumePlugin()
        }
    }

    PageHeader(
        kicker = "اتصال به سایت و ربات",
        title = "API و افزونه",
        subtitle = "کلیدها و آدرس‌هایی که برای وصل کردن سایت، ربات یا فروشگاه به تیکارت لازم است.",
    )

    if (acc.locked) {
        LockedGate(vm, "استفاده از API")
        return
    }
    if (!acc.hasApiPlan) {
        EmptyState(
            icon = Icons.Rounded.Key,
            title = "API در پلن بیزنس و بالاتر است",
            text = "با پلن بیزنس، مکس یا پلتفرم می‌توانی از سایت و ربات خودت فاکتور بسازی، وب‌هوک بگیری و افزونهٔ ووکامرس را نصب کنی.",
            actionLabel = "مشاهدهٔ پلن‌ها",
            onAction = { vm.go(Dest.Subscription) },
        )
        return
    }
    if (!acc.hasCard) {
        Banner("کلیدها بعد از ثبت اولین کارت بانکی ساخته می‌شوند.", Tone.Warn)
        OutlineButton("ثبت کارت بانکی", color = Amber) {
            vm.newCardRequest = true
            vm.go(Dest.Cards)
        }
        return
    }

    GlassCard {
        SectionTitle("کلیدها و آدرس‌ها")
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CopyField("آدرس پایهٔ API", "${TcartApi.ROOT}/api/v1")
            CopyField("کلید API", acc.apiKey)
            CopyField("آدرس وب‌هوک پیامک", acc.webhookUrl)
            CopyField("کلید وب‌هوک پیامک (Secret)", acc.smsSecret)
            if (acc.callbackSecret.isNotBlank()) CopyField("کلید امضای Callback", acc.callbackSecret)
            if (acc.callbackUrl.isNotBlank()) CopyField("آدرس Callback پیش‌فرض", acc.callbackUrl)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "کلید را در هدر Authorization: Bearer … یا X-API-Key بفرست. ساخت فاکتور و محصول فقط با اشتراک فعال و کارت ثبت‌شده کار می‌کند.",
            color = Muted,
            fontSize = 11.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineButton("مستندات", icon = Icons.Rounded.OpenInNew) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${TcartApi.ROOT}/docs"))) }
            }
            OutlineButton(
                "ساخت کلید جدید",
                color = Danger,
                icon = Icons.Rounded.Refresh,
                enabled = !vm.isBusy("regen"),
            ) { confirmRegen = true }
        }
    }

    GlassCard {
        SectionTitle("افزونهٔ ووکامرس")
        Spacer(Modifier.height(4.dp))
        Text(
            "فایل ZIP افزونهٔ اختصاصی تیکارت را دانلود کن و در وردپرس نصب کن. بعد از دانلود می‌توانی آن را در فایل‌ها ذخیره یا برای خودت ارسال کنی.",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(12.dp))
        GradientButton("دانلود افزونه", loading = vm.isBusy("plugin")) { vm.downloadPlugin() }
    }

    if (confirmRegen) {
        ConfirmDialog(
            title = "ساخت کلید API جدید؟",
            text = "کلید فعلی از کار می‌افتد و هر سایت یا رباتی که از آن استفاده می‌کند باید کلید جدید را بگیرد.",
            confirmLabel = "بله، کلید جدید",
            danger = true,
            onConfirm = {
                confirmRegen = false
                vm.regenerateKey()
            },
            onDismiss = { confirmRegen = false },
        )
    }
}

@Composable
internal fun VendorsScreen(vm: SellerViewModel, acc: Account) {
    var externalId by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var hook by remember(acc.platformWebhook) { mutableStateOf(acc.platformWebhook) }
    var deactivate by remember { mutableStateOf<Vendor?>(null) }

    PageHeader(
        kicker = "پلتفرم چندفروشنده",
        title = "فروشندگان زیرمجموعه",
        subtitle = "برای هر فروشندهٔ مارکت‌پلیس خودت لینک ثبت‌نام و پنل جدا بساز.",
        accent = Violet,
    )

    if (!acc.isPlatform) {
        EmptyState(
            icon = Icons.Rounded.Groups,
            title = "این بخش مخصوص پلن پلتفرم است",
            text = "با پلن پلتفرم می‌توانی چند فروشنده را زیر یک اشتراک مدیریت کنی؛ هر فروشنده کارت و پیامک جدا دارد.",
            actionLabel = "مشاهدهٔ پلن‌ها",
            onAction = { vm.go(Dest.Subscription) },
        )
        return
    }
    if (acc.locked) {
        LockedGate(vm, "مدیریت فروشندگان")
        return
    }

    val data = vm.vendors
    GlassCard {
        SectionTitle("فروشندهٔ جدید") {
            if (data != null) SitePill("${ir.tcart.seller.data.Fmt.num(data.count)} از ${ir.tcart.seller.data.Fmt.num(data.max.coerceAtLeast(acc.maxVendors))}", Violet)
        }
        Spacer(Modifier.height(10.dp))
        SiteField(externalId, { externalId = it }, "شناسهٔ یکتا در سیستم تو", ltr = true, placeholder = "shop-1024")
        Spacer(Modifier.height(10.dp))
        SiteField(name, { name = it }, "نام فروشگاه")
        Spacer(Modifier.height(12.dp))
        SmallGradientButton("ساخت فروشنده", enabled = !vm.isBusy("vendor")) {
            vm.createVendor(externalId, name) {
                externalId = ""
                name = ""
            }
        }
    }

    if (data == null) {
        Text("در حال دریافت فهرست…", color = Muted, fontSize = 13.sp)
    } else if (data.vendors.isEmpty()) {
        Text("هنوز فروشنده‌ای نساخته‌ای.", color = Muted, fontSize = 13.sp)
    } else {
        data.vendors.forEach { v ->
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(v.name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(v.externalId, color = Muted, fontSize = 11.sp)
                    }
                    SitePill(if (v.active) "فعال" else "غیرفعال", if (v.active) Mint else Muted)
                    Spacer(Modifier.width(6.dp))
                    SitePill(if (v.onboarded) "کارت ثبت شد" else "منتظر ثبت کارت", if (v.onboarded) Cyan else Amber)
                }
                if (v.contactName.isNotBlank() || v.contactPhone.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        listOf(v.contactName, v.contactPhone, v.contactEmail).filter { it.isNotBlank() }.joinToString(" · "),
                        color = Muted,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!v.onboarded && v.onboardingUrl.isNotBlank()) CopyField("لینک ثبت‌نام و ثبت کارت", v.onboardingUrl)
                    if (v.panelUrl.isNotBlank()) CopyField("لینک پنل فروشنده", v.panelUrl)
                    if (v.active) {
                        OutlineButton("غیرفعال کردن", color = Danger, enabled = !vm.isBusy("vendor:${v.id}")) { deactivate = v }
                    }
                }
            }
        }
    }

    GlassCard {
        SectionTitle("وب‌هوک رویدادهای فروشنده")
        Spacer(Modifier.height(4.dp))
        Text(
            "هر وقت فروشنده‌ای ثبت‌نام کند یا کارتش را ثبت کند، تیکارت به این آدرس خبر می‌دهد.",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(10.dp))
        SiteField(hook, { hook = it }, "آدرس وب‌هوک", ltr = true, placeholder = "https://example.com/tcart/vendors")
        Spacer(Modifier.height(12.dp))
        SmallGradientButton("ذخیره", enabled = !vm.isBusy("webhook")) { vm.savePlatformWebhook(hook) }
    }

    deactivate?.let { v ->
        ConfirmDialog(
            title = "غیرفعال کردن ${v.name}؟",
            text = "این فروشنده دیگر نمی‌تواند فروش جدید بگیرد.",
            confirmLabel = "غیرفعال کن",
            danger = true,
            onConfirm = {
                deactivate = null
                vm.deactivateVendor(v.id)
            },
            onDismiss = { deactivate = null },
        )
    }
}
