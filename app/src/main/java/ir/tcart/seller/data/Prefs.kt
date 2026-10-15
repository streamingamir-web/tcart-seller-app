package ir.tcart.seller.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PayEvent(
    val at: Long,
    val title: String,
    val detail: String,
    val kind: String,
    val amount: Long,
)

class Prefs(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("tcart_seller", Context.MODE_PRIVATE)

    var token: String
        get() = p.getString(KEY_TOKEN, "").orEmpty()
        set(value) = p.edit().putString(KEY_TOKEN, value).apply()

    var sellerName: String
        get() = p.getString(KEY_NAME, "").orEmpty()
        set(value) = p.edit().putString(KEY_NAME, value).apply()

    var webhookUrl: String
        get() = p.getString(KEY_URL, "").orEmpty()
        set(value) = p.edit().putString(KEY_URL, value).apply()

    var secret: String
        get() = p.getString(KEY_SECRET, "").orEmpty()
        set(value) = p.edit().putString(KEY_SECRET, value).apply()

    var listening: Boolean
        get() = p.getBoolean(KEY_ON, false)
        set(value) = p.edit().putBoolean(KEY_ON, value).apply()

    /** -1 یعنی خط مشخص نشده و هر خط این گوشی قبول است. */
    var subscriptionId: Int
        get() = p.getInt(KEY_SUB, UNSET)
        set(value) = p.edit().putInt(KEY_SUB, value).apply()

    var lineLabel: String
        get() = p.getString(KEY_LINE, "").orEmpty()
        set(value) = p.edit().putString(KEY_LINE, value).apply()

    fun history(): List<PayEvent> {
        val raw = p.getString(KEY_HISTORY, "[]").orEmpty()
        val arr = try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    PayEvent(
                        at = o.optLong("at"),
                        title = o.optString("title"),
                        detail = o.optString("detail"),
                        kind = o.optString("kind"),
                        amount = o.optLong("amount"),
                    ),
                )
            }
        }
    }

    fun pushHistory(event: PayEvent) {
        val next = JSONArray()
        fun put(item: PayEvent) {
            next.put(
                JSONObject()
                    .put("at", item.at)
                    .put("title", item.title)
                    .put("detail", item.detail)
                    .put("kind", item.kind)
                    .put("amount", item.amount),
            )
        }
        put(event)
        history().take(19).forEach { put(it) }
        p.edit().putString(KEY_HISTORY, next.toString()).apply()
    }

    var seenIntro: Boolean
        get() = p.getBoolean(KEY_INTRO, false)
        set(value) = p.edit().putBoolean(KEY_INTRO, value).apply()

    var lightTheme: Boolean
        get() = p.getBoolean(KEY_LIGHT, false)
        set(value) = p.edit().putBoolean(KEY_LIGHT, value).apply()

    fun bankSenders(): Set<String> = p.getStringSet(KEY_BANKS, emptySet())?.toSet().orEmpty()

    fun setBankSenders(values: Set<String>) {
        p.edit().putStringSet(KEY_BANKS, values.toSet()).apply()
    }

    fun knownPending(): Set<String> = p.getStringSet(KEY_PENDING, emptySet())?.toSet().orEmpty()

    fun setKnownPending(ids: Set<String>) {
        p.edit().putStringSet(KEY_PENDING, ids.toSet()).apply()
    }

    var pendingPrimed: Boolean
        get() = p.getBoolean(KEY_PRIMED, false)
        set(value) = p.edit().putBoolean(KEY_PRIMED, value).apply()

    fun clearSession() {
        val intro = seenIntro
        val light = lightTheme
        val banks = bankSenders()
        p.edit().clear().apply()
        seenIntro = intro
        lightTheme = light
        setBankSenders(banks)
    }

    companion object {
        const val UNSET = -1
        private const val KEY_TOKEN = "token"
        private const val KEY_NAME = "name"
        private const val KEY_URL = "url"
        private const val KEY_SECRET = "secret"
        private const val KEY_ON = "on"
        private const val KEY_SUB = "sub"
        private const val KEY_LINE = "line"
        private const val KEY_HISTORY = "history"
        private const val KEY_INTRO = "intro"
        private const val KEY_LIGHT = "light"
        private const val KEY_BANKS = "banks"
        private const val KEY_PENDING = "pending_ids"
        private const val KEY_PRIMED = "pending_primed"
    }
}
