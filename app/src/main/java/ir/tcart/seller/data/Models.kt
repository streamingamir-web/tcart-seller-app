package ir.tcart.seller.data

import ir.tcart.seller.sms.SmsText

class ApiException(message: String, val code: Int = 0) : Exception(message)

data class Notice(
    val id: Long,
    val title: String,
    val message: String,
    val createdAt: String,
)

data class Account(
    val token: String,
    val name: String,
    val phone: String,
    val email: String,
    val hasTelegram: Boolean,
    val hasCard: Boolean,
    val cardFormatted: String,
    val cardHolder: String,
    val bankName: String,
    val shebaFormatted: String,
    val apiKey: String,
    val smsSecret: String,
    val callbackSecret: String,
    val callbackUrl: String,
    val webhookUrl: String,
    val subscriptionEnabled: Boolean,
    val subscriptionActive: Boolean,
    val planTier: String,
    val expiresAt: String,
    val daysLeft: Int?,
    val paidThisMonth: Int,
    val planLimit: Int,
    val rotateEnabled: Boolean,
    val isPlatform: Boolean,
    val maxVendors: Int,
    val platformWebhook: String,
    val notices: List<Notice>,
) {
    /** سایت: اشتراک روشن است ولی فعال نیست → کارت/محصول/فروش قفل است. */
    val locked: Boolean get() = subscriptionEnabled && !subscriptionActive
    val hasApiPlan: Boolean get() = planTier in API_TIERS
    val displayName: String get() = name.ifBlank { "فروشنده" }
}

val API_TIERS = setOf("business", "enterprise", "platform", "platform_max")

data class AppConfig(
    val botUsername: String,
    val orderExpiryMinutes: Int,
)

data class Order(
    val id: Long,
    val tracking: String,
    val title: String,
    val status: String,
    val source: String,
    val buyerName: String,
    val baseToman: Long,
    val uniqueToman: Long,
    val expiresAt: String,
    val paidAt: String,
    val createdAt: String,
    val buyerUrl: String,
    val timeMs: Long,
) {
    val canAct: Boolean get() = status == "pending" || status == "expired"
}

data class SellerStats(
    val total: Int,
    val paid: Int,
    val paidThisMonth: Int,
    val pending: Int,
    val expired: Int,
    val cancelled: Int,
    val revenueAll: Long,
    val revenueMonth: Long,
    val planTier: String,
    val planTitle: String,
    val planLimit: Int,
    val subscriptionActive: Boolean,
    val testPaid: Int,
)

data class OrdersData(val orders: List<Order>, val stats: SellerStats)

data class Product(
    val id: Long,
    val name: String,
    val description: String,
    val price: Long,
    val buyPath: String,
    val deliveryType: String,
    val deliveryContent: String,
    val deliveryInstructions: String,
    val deliveryFileName: String,
    val hasFile: Boolean,
    val maxPurchases: Int?,
    val paidCount: Int,
    val reserved: Int,
    val slotsRemaining: Int?,
    val salesEnabled: Boolean,
    val paymentExpiry: Int?,
    val effectiveExpiry: Int?,
    val soldOut: Boolean,
    val linksTotal: Int,
    val linksAvailable: Int,
    val linksAssigned: Int,
    val linksShared: Boolean,
)

data class ProductInput(
    val name: String,
    val priceToman: Long,
    val description: String,
    val deliveryType: String,
    val deliveryContent: String,
    val deliveryInstructions: String,
    val maxPurchases: Int?,
    val salesEnabled: Boolean,
    val paymentExpiryMinutes: Int?,
)

class PickedFile(val name: String, val mime: String, val bytes: ByteArray)

data class BankCard(
    val id: Long,
    val number: String,
    val numberFormatted: String,
    val holder: String,
    val bank: String,
    val sheba: String,
    val shebaFormatted: String,
    val nickname: String,
    val paused: Boolean,
    val maxTx: Int?,
    val usedTx: Int,
    val primary: Boolean,
)

data class PaymentSettings(
    val defaultExpiry: Int?,
    val showSheba: Boolean,
    val effectiveExpiry: Int,
    val systemExpiry: Int,
    val shebaExpiry: Int,
)

data class CardsData(
    val cards: List<BankCard>,
    val maxCards: Int,
    val canAdd: Boolean,
    val rotate: Boolean,
    val settings: PaymentSettings,
)

data class PlanTier(
    val key: String,
    val title: String,
    val monthly: Long,
    val limit: Int,
)

data class PlanOffer(
    val tier: String,
    val title: String,
    val months: Int,
    val total: Long,
    val discount: Int,
)

data class PlansData(val tiers: List<PlanTier>, val offers: List<PlanOffer>)

data class SubPayment(
    val id: Long,
    val tracking: String,
    val tier: String,
    val months: Int,
    val amount: Long,
    val expiresAtMs: Long,
)

data class PlatformCard(
    val number: String,
    val numberFormatted: String,
    val holder: String,
    val bank: String,
    val shebaFormatted: String,
)

data class Invoice(val payment: SubPayment, val card: PlatformCard)

data class Vendor(
    val id: Long,
    val externalId: String,
    val name: String,
    val onboarded: Boolean,
    val hasAccount: Boolean,
    val contactName: String,
    val contactPhone: String,
    val contactEmail: String,
    val onboardingUrl: String,
    val panelUrl: String,
    val active: Boolean,
)

data class VendorsData(val vendors: List<Vendor>, val count: Int, val max: Int)

data class DayPoint(val label: String, val revenue: Long, val paid: Int)

data class SmsPostResult(
    val http: Int,
    val ok: Boolean,
    val orderId: Long?,
    val subscriptionPaymentId: Long?,
    val ignored: Boolean,
    val error: String,
)

/** ابزارهای قالب‌بندی فارسی (ارقام، تومان، تاریخ شمسی). */
object Fmt {
    private val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun amount(n: Long): String = SmsText.faAmount(n)

    fun toman(n: Long): String = SmsText.faAmount(n) + " تومان"

    fun num(n: Int): String = SmsText.faAmount(n.toLong())

    fun fa(raw: String): String = raw.map { ch -> if (ch in '0'..'9') persian[ch - '0'] else ch }.joinToString("")

    /** هر رقم فارسی/عربی را لاتین می‌کند تا ورودی کاربر قابل‌خواندن برای سرور باشد. */
    fun en(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            val p = persian.indexOf(ch)
            when {
                p >= 0 -> sb.append('0' + p)
                ch in '٠'..'٩' -> sb.append('0' + (ch - '٠'))
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun digitsOnly(raw: String): String = en(raw).filter { it in '0'..'9' }

    fun minutes(total: Int): String {
        if (total < 60) return "${num(total)} دقیقه"
        val h = total / 60
        val m = total % 60
        return if (m > 0) "${num(h)} ساعت و ${num(m)} دقیقه" else "${num(h)} ساعت"
    }

    fun isoMillis(raw: String): Long {
        if (raw.isBlank()) return 0L
        val hasZone = raw.endsWith("Z") || Regex("[+-]\\d{2}:\\d{2}$").containsMatchIn(raw)
        val text = if (hasZone) raw else "${raw}Z"
        return runCatching { java.time.OffsetDateTime.parse(text).toInstant().toEpochMilli() }.getOrDefault(0L)
    }

    private fun jalali(ms: Long, pattern: String): String {
        if (ms <= 0L) return "—"
        return runCatching {
            val fmt = android.icu.text.SimpleDateFormat(pattern, android.icu.util.ULocale("fa_IR@calendar=persian"))
            fmt.format(java.util.Date(ms))
        }.getOrDefault("—")
    }

    fun dateTime(ms: Long): String = jalali(ms, "d MMMM، HH:mm")

    fun date(ms: Long): String = jalali(ms, "d MMMM yyyy")

    fun dayLabel(ms: Long): String = jalali(ms, "d MMM")
}
