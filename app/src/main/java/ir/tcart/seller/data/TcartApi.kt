package ir.tcart.seller.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Account(
    val token: String,
    val name: String,
    val phone: String,
    val webhookUrl: String,
    val secret: String,
    val apiKey: String,
    val hasCard: Boolean,
    val cardMasked: String,
    val bankName: String,
    val holder: String,
    val planTitle: String,
    val subActive: Boolean,
    val daysLeft: Int,
    val paidThisMonth: Int,
    val planLimit: Int,
)

data class OrderRow(
    val id: Long,
    val title: String,
    val status: String,
    val amount: Long,
    val at: Long,
)

data class ProductRow(
    val id: Long,
    val name: String,
    val price: Long,
    val paidCount: Int,
    val enabled: Boolean,
)

data class OrderStats(
    val revenueMonth: Long,
    val revenueAll: Long,
    val paidCount: Int,
    val pendingCount: Int,
    val paidThisMonth: Int,
)

data class PlanOffer(
    val tier: String,
    val title: String,
    val months: Int,
    val price: Long,
    val discount: Int,
)

data class Invoice(
    val tier: String,
    val months: Int,
    val amount: Long,
    val card: String,
    val holder: String,
    val bank: String,
)

data class Dash(
    val account: Account,
    val orders: List<OrderRow>,
    val products: List<ProductRow>,
    val stats: OrderStats,
    val wallet: Long,
)

data class SmsPostResult(
    val http: Int,
    val ok: Boolean,
    val orderId: Long?,
    val subscriptionPaymentId: Long?,
    val ignored: Boolean,
    val error: String,
)

object TcartApi {
    const val ROOT = "https://tcartt.com"

    private val json = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun login(phone: String, password: String): Account {
        val body = JSONObject().put("phone", phone.trim()).put("password", password)
        val payload = call("POST", "/api/web/auth/login", body = body)
        if (payload.optString("account_type") != "seller") {
            throw IllegalStateException("این اپ فقط برای حساب فروشنده است")
        }
        val token = payload.optString("token")
        if (token.isBlank()) throw IllegalStateException("توکن ورود نیامد")
        return account(token)
    }

    fun account(token: String): Account = parseAccount(token, call("GET", "/api/web/auth/me", token))

    fun dashboard(token: String): Dash {
        val me = call("GET", "/api/web/auth/me", token)
        val ordersJson = call("GET", "/api/web/seller/orders", token)
        val productsJson = call("GET", "/api/web/seller/products", token)
        val wallet = runCatching {
            call("GET", "/api/web/seller/wallet", token)
                .optJSONObject("wallet")
                ?.optLong("balance_toman") ?: 0L
        }.getOrDefault(0L)
        val statsObj = ordersJson.optJSONObject("stats")
        val stats = OrderStats(
            revenueMonth = statsObj?.optLong("revenue_this_month_toman") ?: 0L,
            revenueAll = statsObj?.optLong("revenue_all_time_toman") ?: 0L,
            paidCount = statsObj?.optInt("paid_count") ?: 0,
            pendingCount = statsObj?.optInt("pending_count") ?: 0,
            paidThisMonth = statsObj?.optInt("paid_this_month") ?: 0,
        )
        return Dash(
            account = parseAccount(token, me),
            orders = ordersJson.optJSONArray("orders").toOrders(),
            products = productsJson.optJSONArray("products").toProducts(),
            stats = stats,
            wallet = wallet,
        )
    }

    fun plans(): List<PlanOffer> {
        val payload = call("GET", "/api/web/plans")
        val arr = payload.optJSONArray("offers") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    PlanOffer(
                        tier = o.optString("tier_key"),
                        title = o.optString("tier_title").ifBlank { o.optString("tier_key") },
                        months = o.optInt("duration_months"),
                        price = o.optLong("total_price_toman"),
                        discount = o.optInt("discount_percent"),
                    ),
                )
            }
        }
    }

    fun checkout(token: String, tier: String, months: Int): Invoice {
        val payload = call(
            "POST",
            "/api/web/seller/subscription/checkout",
            token,
            JSONObject().put("tier_key", tier).put("duration_months", months),
        )
        val payment = payload.optJSONObject("payment") ?: JSONObject()
        val card = payload.optJSONObject("platform_card") ?: JSONObject()
        return Invoice(
            tier = payment.optString("plan_tier").ifBlank { tier },
            months = payment.optInt("duration_months", months),
            amount = payment.optLong("unique_amount_toman"),
            card = card.optString("number_formatted").ifBlank { card.optString("number") },
            holder = card.optString("holder"),
            bank = card.optString("bank_name"),
        )
    }

    fun postSms(webhookUrl: String, secret: String, sms: String): SmsPostResult {
        val body = JSONObject().put("sms", sms).toString().toRequestBody(json)
        val request = Request.Builder()
            .url(webhookUrl)
            .header("X-Webhook-Secret", secret)
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val payload = parse(response.body?.string().orEmpty())
            return SmsPostResult(
                http = response.code,
                ok = payload.optBoolean("ok"),
                orderId = payload.optLong("order_id").takeIf { it > 0 },
                subscriptionPaymentId = payload.optLong("subscription_payment_id").takeIf { it > 0 },
                ignored = payload.optBoolean("ignored"),
                error = payload.optString("error"),
            )
        }
    }

    private fun parseAccount(token: String, payload: JSONObject): Account {
        val user = payload.optJSONObject("user")
        val profile = payload.optJSONObject("profile")
        return Account(
            token = token,
            name = user?.optString("full_name").orEmpty(),
            phone = user?.optString("phone").orEmpty(),
            webhookUrl = profile?.optString("webhook_sms_url").orEmpty(),
            secret = profile?.optString("sms_webhook_secret").orEmpty(),
            apiKey = profile?.optString("api_key").orEmpty(),
            hasCard = profile?.optBoolean("has_card") == true,
            cardMasked = profile?.optString("card_number_formatted").orEmpty(),
            bankName = profile?.optString("bank_name").orEmpty(),
            holder = profile?.optString("card_holder").orEmpty(),
            planTitle = profile?.optString("plan_tier").orEmpty(),
            subActive = profile?.optBoolean("subscription_active") == true,
            daysLeft = profile?.optInt("subscription_days_left") ?: 0,
            paidThisMonth = profile?.optInt("paid_orders_this_month") ?: 0,
            planLimit = profile?.optInt("plan_limit") ?: 0,
        )
    }

    private fun call(method: String, path: String, token: String? = null, body: JSONObject? = null): JSONObject {
        val builder = Request.Builder().url("$ROOT$path")
        if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
        if (method == "POST") {
            builder.post((body ?: JSONObject()).toString().toRequestBody(json))
        } else {
            builder.get()
        }
        client.newCall(builder.build()).execute().use { response ->
            val payload = parse(response.body?.string().orEmpty())
            if (response.code == 401) throw IllegalStateException("نشست تمام شده. دوباره وارد شو")
            if (!response.isSuccessful || (payload.has("ok") && !payload.optBoolean("ok"))) {
                throw IllegalStateException(payload.optString("error").ifBlank { "خطا در ارتباط با سرور" })
            }
            return payload
        }
    }

    private fun parse(text: String): JSONObject =
        if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrDefault(JSONObject())
}

private fun JSONArray?.toOrders(): List<OrderRow> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            add(
                OrderRow(
                    id = o.optLong("id"),
                    title = o.optString("title").ifBlank { "سفارش" },
                    status = o.optString("status"),
                    amount = o.optLong("unique_amount_toman"),
                    at = isoMillis(o.optString("paid_at").ifBlank { o.optString("created_at") }),
                ),
            )
        }
    }
}

private fun JSONArray?.toProducts(): List<ProductRow> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            add(
                ProductRow(
                    id = o.optLong("id"),
                    name = o.optString("name"),
                    price = o.optLong("price_toman"),
                    paidCount = o.optInt("paid_count"),
                    enabled = o.optBoolean("sales_enabled", true),
                ),
            )
        }
    }
}

private fun isoMillis(raw: String): Long {
    if (raw.isBlank()) return 0L
    val text = if (raw.endsWith("Z") || raw.contains("+")) raw else "${raw}Z"
    return runCatching { java.time.Instant.parse(text).toEpochMilli() }.getOrDefault(0L)
}
