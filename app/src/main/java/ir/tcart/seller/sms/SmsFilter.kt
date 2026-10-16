package ir.tcart.seller.sms

import java.text.NumberFormat
import java.util.Locale

object SmsFilter {
    private val otp = listOf(
        "رمز پویا",
        "رمزپویا",
        "رمز دوم",
        "رمزیکبار",
        "رمز یکبار",
        "کد تایید",
        "کد تأیید",
        "کد ورود",
        "یکبار رمز",
    )

    fun isSecret(text: String): Boolean {
        val n = text.replace("\u200c", "").replace("ي", "ی").replace("ك", "ک")
        return otp.any { n.contains(it) }
    }

    /** مبلغ نمایشی. تصمیم واقعی پرداخت سمت سرور است. */
    fun amountToman(text: String): Long? {
        val n = text
            .replace("\u200c", "")
            .replace("ي", "ی")
            .replace("ك", "ک")
            .replace(",", "")
            .replace("،", "")
            .replace("٬", "")
        val head = n.split("مانده").first()
        val toman = Regex("""(\d+)\s*تومان""").find(head)
        if (toman != null) return toman.groupValues[1].toLongOrNull()
        val rial = Regex("""\+\s*(\d+)\s*ریال""").find(head)
            ?: Regex("""(\d+)\s*ریال""").find(head)
        val rialValue = rial?.groupValues?.get(1)?.toLongOrNull()
        if (rialValue != null && rialValue >= 10) return rialValue / 10
        return null
    }
}

fun faAmount(toman: Long): String {
    val grouped = NumberFormat.getIntegerInstance(Locale.US).format(toman)
    val digits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val persian = grouped.map { ch ->
        when {
            ch.isDigit() -> digits[ch - '0']
            ch == ',' -> '٬'
            else -> ch
        }
    }.joinToString("")
    return "$persian تومان"
}
