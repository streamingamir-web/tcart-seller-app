package ir.tcart.seller.sms

import java.text.NumberFormat
import java.util.Locale

object SmsText {
    private val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val arabic = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun isSecret(raw: String): Boolean {
        val n = normalize(raw)
        if (n.isBlank()) return true
        return listOf("رمز پویا", "رمزپویا", "رمز دوم", "رمز یکبار", "کد تایید", "کد تأیید", "کد ورود", "password")
            .any { n.contains(it) }
    }

    /** مبلغ نمایشی. تأیید واقعی را سرور انجام می‌دهد. */
    fun amountToman(raw: String): Long? {
        val n = normalize(raw)
        val beforeBalance = n.split("مانده", limit = 2).first()
        toman(beforeBalance)?.let { return it }
        rial(beforeBalance)?.let { return it / 10 }
        val deposit = Regex("""واریز[:\s]*(\d+)(?!\s*-)""").find(beforeBalance)
        if (deposit != null) return deposit.groupValues[1].toLongOrNull()
        return null
    }

    fun faDigits(value: Long): String =
        value.toString().map { ch -> persian[ch - '0'] }.joinToString("")

    fun faAmount(amount: Long): String {
        val grouped = NumberFormat.getIntegerInstance(Locale.US).format(amount)
        return grouped.map { ch ->
            when {
                ch.isDigit() -> persian[ch - '0']
                ch == ',' -> '٬'
                else -> ch
            }
        }.joinToString("")
    }

    private fun toman(text: String): Long? {
        val patterns = listOf(
            Regex("""\+\s*(\d+)\s*تومان"""),
            Regex("""واریز[:\s]*(\d+)\s*تومان"""),
            Regex("""مبلغ[:\s]*(\d+)\s*تومان"""),
            Regex("""(\d+)\s*تومان"""),
        )
        return patterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.get(1)?.toLongOrNull() }
    }

    private fun rial(text: String): Long? {
        val patterns = listOf(
            Regex("""\+\s*(\d+)\s*ریال"""),
            Regex("""واریز[:\s]*(\d+)\s*ریال"""),
            Regex("""مبلغ[:\s]*(\d+)\s*ریال"""),
            Regex("""(\d+)\s*ریال"""),
        )
        return patterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.get(1)?.toLongOrNull() }
    }

    private fun normalize(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            val p = persian.indexOf(ch)
            val a = arabic.indexOf(ch)
            when {
                p >= 0 -> sb.append('0' + p)
                a >= 0 -> sb.append('0' + a)
                ch == ',' || ch == '،' || ch == '٬' -> Unit
                ch == 'ي' -> sb.append('ی')
                ch == 'ك' -> sb.append('ک')
                ch == '\u202a' || ch == '\u202c' || ch == '\u2066' || ch == '\u2069' || ch == '\u200c' -> Unit
                else -> sb.append(ch)
            }
        }
        return sb.toString().replace("ريال", "ریال")
    }
}
