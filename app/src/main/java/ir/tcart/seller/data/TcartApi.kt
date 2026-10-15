package ir.tcart.seller.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SellerSession(
    val token: String,
    val name: String,
    val webhookUrl: String,
    val secret: String,
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
    /** همان سرور فعلی پنل. اگر دامنه عوض شد فقط همین خط را عوض کن. */
    const val ROOT = "https://tcartt.com"

    private val json = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    fun login(phone: String, password: String): SellerSession {
        val body = JSONObject()
            .put("phone", phone.trim())
            .put("password", password)
            .toString()
            .toRequestBody(json)
        val request = Request.Builder()
            .url("$ROOT/api/web/auth/login")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val payload = parse(text)
            if (!response.isSuccessful || payload.optBoolean("ok") != true) {
                throw IllegalStateException(payload.optString("error").ifBlank { "ورود انجام نشد" })
            }
            if (payload.optString("account_type") != "seller") {
                throw IllegalStateException("این اپ فقط برای حساب فروشنده است")
            }
            val token = payload.optString("token")
            if (token.isBlank()) throw IllegalStateException("توکن ورود نیامد")
            val user = payload.optJSONObject("user")
            return loadSession(token, user?.optString("full_name").orEmpty())
        }
    }

    fun loadSession(token: String, fallbackName: String = ""): SellerSession {
        val request = Request.Builder()
            .url("$ROOT/api/web/auth/me")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val payload = parse(response.body?.string().orEmpty())
            if (response.code == 401) throw IllegalStateException("نشست تمام شده. دوباره وارد شو")
            if (!response.isSuccessful || payload.optBoolean("ok") != true) {
                throw IllegalStateException(payload.optString("error").ifBlank { "حساب خوانده نشد" })
            }
            val user = payload.optJSONObject("user")
            val profile = payload.optJSONObject("profile")
            val name = user?.optString("full_name").orEmpty().ifBlank { fallbackName }
            return SellerSession(
                token = token,
                name = name,
                webhookUrl = profile?.optString("webhook_sms_url").orEmpty(),
                secret = profile?.optString("sms_webhook_secret").orEmpty(),
            )
        }
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
            val order = payload.optLong("order_id", 0L).takeIf { it > 0 }
            val sub = payload.optLong("subscription_payment_id", 0L).takeIf { it > 0 }
            return SmsPostResult(
                http = response.code,
                ok = payload.optBoolean("ok"),
                orderId = order,
                subscriptionPaymentId = sub,
                ignored = payload.optBoolean("ignored"),
                error = payload.optString("error"),
            )
        }
    }

    private fun parse(text: String): JSONObject {
        if (text.isBlank()) return JSONObject()
        return try {
            JSONObject(text)
        } catch (_: Exception) {
            JSONObject()
        }
    }
}
