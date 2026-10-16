package ir.tcart.seller.sms

import android.content.Context
import android.provider.Telephony

data class BankSender(
    val address: String,
    val key: String,
    val preview: String,
    val count: Int,
)

object SmsInbox {
    fun recentSenders(context: Context): List<BankSender> {
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
            null,
            null,
            "${Telephony.Sms.DATE} DESC",
        ) ?: return emptyList()
        val grouped = linkedMapOf<String, BankSender>()
        cursor.use {
            var seen = 0
            val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            while (it.moveToNext() && seen < 500 && grouped.size < 40) {
                seen++
                val address = if (addressIdx >= 0) it.getString(addressIdx).orEmpty().trim() else ""
                if (address.isBlank()) continue
                val key = normalizeSender(address)
                if (key.isBlank()) continue
                val body = if (bodyIdx >= 0) it.getString(bodyIdx).orEmpty().replace("\n", " ").take(48) else ""
                val current = grouped[key]
                if (current == null) {
                    grouped[key] = BankSender(address, key, body, 1)
                } else {
                    grouped[key] = current.copy(count = current.count + 1)
                }
            }
        }
        return grouped.values.toList()
    }

    fun normalizeSender(raw: String): String {
        val compact = raw.trim().uppercase().replace(" ", "")
        val digits = compact.filter { it.isDigit() }
        return if (digits.length >= 4) digits else compact
    }

    fun matches(address: String, selected: Set<String>): Boolean {
        if (selected.isEmpty()) return false
        val key = normalizeSender(address)
        return selected.any { chosen ->
            val other = normalizeSender(chosen)
            key == other || (key.length >= 4 && other.length >= 4 && (key.endsWith(other) || other.endsWith(key)))
        }
    }
}
