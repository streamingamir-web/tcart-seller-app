package ir.tcart.seller.data

data class PlanInfo(
    val key: String,
    val titleFa: String,
    val description: String,
    val recommended: Boolean,
    val features: List<Pair<String, Boolean>>,
)

data class DurationOption(val months: Int, val label: String, val discount: Int)

/** همان متادیتای پلن‌های سایت (website/src/data/plans.ts). */
object Plans {
    val hidden = setOf("platform_max")
    private val minDuration = mapOf("enterprise" to 3, "platform" to 3, "platform_max" to 3)
    val order = listOf("starter", "business", "enterprise", "platform", "platform_max")

    /** تخفیف از سرور می‌آید؛ فقط مکس ۱۰٪ — نوار مدت دیگر تخفیف سراسری نشان نمی‌دهد */
    val durations = listOf(
        DurationOption(1, "۱ ماهه", 0),
        DurationOption(3, "۳ ماهه", 0),
        DurationOption(6, "۶ ماهه", 0),
    )

    const val DEFAULT_MONTHS = 3

    private const val SUPPORT = "پشتیبانی اختصاصی"

    private val meta: Map<String, PlanInfo> = listOf(
        PlanInfo(
            "starter", "استارتر",
            "فروشنده اینستاگرامی یا تازه‌کار — بدون نیاز به API", false,
            listOf(
                "۱ کارت بانکی" to true,
                "تا ۱۰۰ پرداخت موفق در ماه" to true,
                "تأیید خودکار از SMS بانکی" to true,
                "لینک خرید برای هر محصول" to true,
                "تحویل خودکار لینک/فایل" to true,
                "پنل وب و اپ موبایل" to true,
                "پشتیبانی تلگرام" to true,
                "API، Webhook و Callback" to false,
            ),
        ),
        PlanInfo(
            "business", "بیزنس",
            "فروشگاه آنلاین، ربات‌های تلگرامی، اینستاگرام، لینک خرید و API", true,
            listOf(
                "۳ کارت بانکی" to true,
                "تا ۵۰۰ پرداخت موفق در ماه" to true,
                "همه امکانات استارتر" to true,
                "API فاکتور + کلید اختصاصی" to true,
                "افزونه وردپرس اختصاصی" to true,
                "Webhook SMS + Callback امن" to true,
                "اتصال ربات/سایت خارجی" to true,
                "راه‌اندازی رایگان ربات تلگرام" to true,
                SUPPORT to false,
            ),
        ),
        PlanInfo(
            "enterprise", "مکس",
            "فروش پرترافیک — تراکنش و کارت نامحدود", false,
            listOf(
                "کارت بانکی نامحدود" to true,
                "تراکنش نامحدود در ماه" to true,
                "همه امکانات بیزنس" to true,
                "API + Webhook بدون محدودیت عملی" to true,
                "افزونه وردپرس اختصاصی" to true,
                "اولویت در پردازش SMS" to true,
                "مناسب چند کانال فروش همزمان" to true,
                "فقط اشتراک ۳ یا ۶ ماهه" to true,
                SUPPORT to true,
            ),
        ),
        PlanInfo(
            "platform", "پلتفرم",
            "مارکت‌پلیس و SaaS چندفروشنده — یک اشتراک، چند فروشنده زیرمجموعه", false,
            listOf(
                "تا ۵۰ فروشنده (وندور) با پنل جدا" to true,
                "۳ کارت بانکی (مالک پلتفرم)" to true,
                "۳ کارت بانکی برای هر فروشنده" to true,
                "تا ۵٬۰۰۰ پرداخت موفق در ماه (جمع کل)" to true,
                "کارت و SMS جدا برای هر فروشنده" to true,
                "Webhook SMS + Callback + رویداد وندور" to true,
                "افزونه وردپرس اختصاصی" to true,
                "فقط اشتراک ۳ یا ۶ ماهه" to true,
            ),
        ),
        PlanInfo(
            "platform_max", "پلتفرم مکس",
            "مارکت‌پلیس پرمقیاس — وندور و تراکنش بالا، کارت نامحدود", false,
            listOf(
                "تا ۵۰۰ فروشنده (وندور) با پنل جدا" to true,
                "کارت بانکی نامحدود (مالک پلتفرم)" to true,
                "تراکنش نامحدود در ماه (جمع کل)" to true,
                "فقط اشتراک ۳ یا ۶ ماهه" to true,
            ),
        ),
    ).associateBy { it.key }

    fun info(key: String): PlanInfo? = meta[key]

    fun titleFa(key: String, fallback: String = ""): String =
        meta[key]?.titleFa ?: fallback.ifBlank { key.ifBlank { "—" } }

    fun locked(tier: String, months: Int): Boolean = minDuration[tier]?.let { months < it } == true

    fun rank(key: String): Int = order.indexOf(key)

    fun limitLabel(limit: Int): String = if (limit == 0) "نامحدود" else Fmt.num(limit)

    fun features(info: PlanInfo, months: Int): List<Pair<String, Boolean>> =
        info.features.map { (text, ok) -> if (text == SUPPORT && info.key != "enterprise") text to (months >= 3) else text to ok }
}
