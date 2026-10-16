package ir.tcart.seller.data

import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")
private fun JSONObject.lng(key: String): Long = if (isNull(key)) 0L else optLong(key, 0L)
private fun JSONObject.int(key: String): Int = if (isNull(key)) 0 else optInt(key, 0)
private fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else optInt(key, 0)
private fun JSONObject.bool(key: String, default: Boolean = false): Boolean =
    if (isNull(key)) default else optBoolean(key, default)

private inline fun <T> JSONArray?.mapObjects(block: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        val o = optJSONObject(i) ?: continue
        out.add(block(o))
    }
    return out
}

object TcartApi {
    /** لینک عمومی سایت/خرید برای مشتری — همان دامنهٔ برند */
    const val SITE = "https://tcartt.com"

    /**
     * همهٔ APIهای اپ مستقیم به VPS — مثل SMS Forwarder قدیمی.
     * از Cloudflare رد نمی‌شود؛ بدون VPN کار می‌کند.
     */
    const val ROOT = "http://155.117.127.242:8080"
    const val DIRECT_ORIGIN = ROOT
    private const val P = "/api/web"

    /** وب‌هوک پیامک روی همان سرور مستقیم */
    fun resolveSmsWebhookUrl(@Suppress("UNUSED_PARAMETER") configured: String = ""): String =
        "$ROOT/webhook/sms"

    private val json = "application/json; charset=utf-8".toMediaType()

    /**
     * روی خیلی از اینترنت‌های ایران، IPv6 دامنهٔ Cloudflare گیر می‌کند
     * در حالی که مرورگر با IPv4 سایت را باز می‌کند. اول فقط IPv4 را امتحان می‌کنیم.
     */
    private val ipv4FirstDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val all = Dns.SYSTEM.lookup(hostname)
            val v4 = all.filterIsInstance<Inet4Address>()
            return if (v4.isNotEmpty()) v4 else all
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .dns(ipv4FirstDns)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "TCartSeller/Android")
                .header("Accept", "application/json")
                .header("Accept-Language", "fa-IR,fa;q=0.9")
                .build()
            chain.proceed(req)
        }
        .build()
    private val uploadClient = client.newBuilder()
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .callTimeout(0)
        .build()

    // ───────────────────────── احراز هویت ─────────────────────────

    fun login(identifier: String, password: String): Account {
        val id = identifier.trim()
        val body = JSONObject().put("password", password)
        if (id.contains("@")) body.put("email", id) else body.put("phone", Fmt.en(id))
        return finishAuth(call("POST", "$P/auth/login", null, body))
    }

    fun register(name: String, phone: String, email: String, password: String): Account {
        val body = JSONObject()
            .put("phone", Fmt.en(phone).trim())
            .put("password", password)
        if (name.isNotBlank()) body.put("full_name", name.trim())
        if (email.isNotBlank()) body.put("email", email.trim())
        return finishAuth(call("POST", "$P/auth/register", null, body))
    }

    fun forgotPassword(phone: String): String {
        val payload = call("POST", "$P/auth/forgot-password", null, JSONObject().put("phone", Fmt.en(phone).trim()))
        return payload.str("message").ifBlank { "اگر این شماره ثبت شده باشد، کد بازیابی در تلگرام برایت ارسال می‌شود." }
    }

    fun resetPassword(phone: String, code: String, newPassword: String): Account {
        val body = JSONObject()
            .put("phone", Fmt.en(phone).trim())
            .put("code", Fmt.en(code).trim())
            .put("new_password", newPassword)
        return finishAuth(call("POST", "$P/auth/reset-password", null, body))
    }

    private fun finishAuth(payload: JSONObject): Account {
        if (payload.has("account_type") && payload.str("account_type") != "seller") {
            throw ApiException("این اپ فقط برای حساب فروشنده است")
        }
        val token = payload.str("token")
        if (token.isBlank()) throw ApiException("توکن ورود دریافت نشد")
        // فقط با پاسخ لاگین وارد شو — درخواست دوم (/auth/me) ورود را کند/خراب می‌کرد
        return parseAccount(token, payload)
    }

    fun account(token: String): Account {
        val raw = call("GET", "$P/auth/me", token)
        return parseAccount(token, raw)
    }

    fun accountFromCache(token: String, raw: String): Account? =
        runCatching { parseAccount(token, JSONObject(raw)) }.getOrNull()

    fun pullAccount(token: String): Pair<Account, String> {
        val raw = call("GET", "$P/auth/me", token)
        return parseAccount(token, raw) to raw.toString()
    }

    fun linkPhone(token: String, phone: String) {
        call("POST", "$P/auth/link-phone", token, JSONObject().put("phone", Fmt.en(phone).trim()))
    }

    fun config(): AppConfig {
        val p = call("GET", "$P/config")
        return AppConfig(
            botUsername = p.str("bot_username").removePrefix("@"),
            orderExpiryMinutes = p.int("order_expiry_minutes").takeIf { it > 0 } ?: 30,
        )
    }

    // ───────────────────────── سفارش‌ها ─────────────────────────

    fun orders(token: String): OrdersData = parseOrders(call("GET", "$P/seller/orders", token))

    fun pullOrders(token: String): Pair<OrdersData, String> {
        val raw = call("GET", "$P/seller/orders", token)
        return parseOrders(raw) to raw.toString()
    }

    fun ordersFromCache(raw: String): OrdersData? =
        runCatching { parseOrders(JSONObject(raw)) }.getOrNull()

    private fun parseOrders(p: JSONObject): OrdersData {
        val s = p.optJSONObject("stats") ?: JSONObject()
        return OrdersData(
            orders = p.optJSONArray("orders").mapObjects { it.toOrder() },
            stats = SellerStats(
                total = s.int("total_orders"),
                paid = s.int("paid_count"),
                paidThisMonth = s.int("paid_this_month"),
                pending = s.int("pending_count"),
                expired = s.int("expired_count"),
                cancelled = s.int("cancelled_count"),
                revenueAll = s.lng("revenue_all_time_toman"),
                revenueMonth = s.lng("revenue_this_month_toman"),
                planTier = s.str("plan_tier"),
                planTitle = s.str("plan_title"),
                planLimit = s.int("plan_limit"),
                subscriptionActive = s.bool("subscription_active"),
                testPaid = s.int("test_paid_count"),
            ),
        )
    }

    fun lookupOrder(token: String, query: String): Order {
        val p = call("GET", "$P/seller/orders/lookup?q=${URLEncoder.encode(Fmt.en(query).trim(), "UTF-8")}", token)
        val o = p.optJSONObject("order") ?: throw ApiException("سفارش پیدا نشد")
        return o.toOrder()
    }

    fun confirmOrder(token: String, id: Long) {
        call("POST", "$P/seller/orders/$id/confirm", token, JSONObject())
    }

    fun rejectOrder(token: String, id: Long) {
        call("POST", "$P/seller/orders/$id/reject", token, JSONObject())
    }

    // ───────────────────────── محصولات ─────────────────────────

    fun products(token: String): List<Product> = parseProducts(call("GET", "$P/seller/products", token))

    fun pullProducts(token: String): Pair<List<Product>, String> {
        val raw = call("GET", "$P/seller/products", token)
        return parseProducts(raw) to raw.toString()
    }

    fun productsFromCache(raw: String): List<Product>? =
        runCatching { parseProducts(JSONObject(raw)) }.getOrNull()

    private fun parseProducts(p: JSONObject): List<Product> =
        p.optJSONArray("products").mapObjects { it.toProduct() }

    fun saveProduct(token: String, id: Long?, input: ProductInput): Product {
        val body = JSONObject()
            .put("name", input.name)
            .put("price_toman", input.priceToman)
            .put("description", input.description)
            .put("delivery_type", input.deliveryType)
            .put("delivery_content", input.deliveryContent)
            .put("delivery_instructions", input.deliveryInstructions)
            .put("max_purchases", input.maxPurchases ?: JSONObject.NULL)
            .put("sales_enabled", input.salesEnabled)
            .put("payment_expiry_minutes", input.paymentExpiryMinutes ?: JSONObject.NULL)
        val p = if (id == null) call("POST", "$P/seller/products", token, body)
        else call("PATCH", "$P/seller/products/$id", token, body)
        return (p.optJSONObject("product") ?: JSONObject()).toProduct()
    }

    /** پیام سرور: حذف کامل یا بایگانی (اگر سفارش داشته). */
    fun deleteProduct(token: String, id: Long): String {
        val p = call("DELETE", "$P/seller/products/$id", token)
        return p.str("message").ifBlank { "محصول حذف شد" }
    }

    fun uploadDeliveryFile(token: String, id: Long, file: PickedFile) {
        val media = file.mime.toMediaTypeOrNull()
        val part: RequestBody = file.bytes.toRequestBody(media)
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, part)
            .build()
        val request = Request.Builder()
            .url("$ROOT$P/seller/products/$id/delivery-file")
            .header("Authorization", "Bearer $token")
            .post(multipart)
            .build()
        uploadClient.newCall(request).execute().use { response ->
            val payload = parse(response.body?.string().orEmpty())
            if (response.code == 401) throw ApiException("نشست تمام شده. دوباره وارد شو", 401)
            if (!response.isSuccessful || (payload.has("ok") && !payload.optBoolean("ok"))) {
                throw ApiException(payload.str("error").ifBlank { "آپلود فایل ناموفق بود" }, response.code)
            }
        }
    }

    // ───────────────────────── کارت‌ها ─────────────────────────

    fun cards(token: String): CardsData = parseCards(call("GET", "$P/seller/cards", token))

    fun addCard(
        token: String,
        number: String,
        holder: String,
        bank: String,
        sheba: String,
        nickname: String,
        maxTx: Int?,
    ): CardsData {
        val body = JSONObject()
            .put("card_number", Fmt.digitsOnly(number))
            .put("card_holder", holder.trim())
            .put("bank_name", bank.trim())
        if (sheba.isNotBlank()) body.put("sheba", normalizeSheba(sheba))
        if (nickname.isNotBlank()) body.put("nickname", nickname.trim())
        if (maxTx != null) body.put("max_transactions", maxTx)
        call("POST", "$P/seller/cards", token, body)
        return cards(token)
    }

    fun patchCard(token: String, id: Long, body: JSONObject): CardsData {
        call("PATCH", "$P/seller/cards/$id", token, body)
        return cards(token)
    }

    fun deleteCard(token: String, id: Long): CardsData {
        call("DELETE", "$P/seller/cards/$id", token)
        return cards(token)
    }

    fun primaryCard(token: String, id: Long): CardsData {
        call("POST", "$P/seller/cards/$id/primary", token, JSONObject())
        return cards(token)
    }

    fun rotateCards(token: String, enabled: Boolean): CardsData {
        call("PUT", "$P/seller/card-rotation", token, JSONObject().put("rotate_enabled", enabled))
        return cards(token)
    }

    fun savePaySettings(token: String, showSheba: Boolean, minutes: Int?): CardsData {
        call(
            "PUT",
            "$P/seller/payment-settings",
            token,
            JSONObject()
                .put("show_sheba_on_invoice", showSheba)
                .put("default_payment_expiry_minutes", minutes ?: JSONObject.NULL),
        )
        return cards(token)
    }

    fun normalizeSheba(raw: String): String {
        val cleaned = Fmt.en(raw).replace(" ", "").uppercase()
        return if (cleaned.startsWith("IR") || cleaned.isBlank()) cleaned else "IR$cleaned"
    }

    // ───────────────────────── اشتراک ─────────────────────────

    fun plans(): PlansData {
        val p = call("GET", "$P/plans")
        return PlansData(
            tiers = p.optJSONArray("tiers").mapObjects {
                PlanTier(
                    key = it.str("key"),
                    title = it.str("title").ifBlank { it.str("key") },
                    monthly = it.lng("monthly_price_toman"),
                    limit = it.int("max_paid_orders_per_month"),
                )
            },
            offers = p.optJSONArray("offers").mapObjects {
                PlanOffer(
                    tier = it.str("tier_key"),
                    title = it.str("tier_title").ifBlank { it.str("tier_key") },
                    months = it.int("duration_months"),
                    total = it.lng("total_price_toman"),
                    discount = it.int("discount_percent"),
                )
            },
        )
    }

    fun checkout(token: String, tier: String, months: Int): Invoice {
        val p = call(
            "POST",
            "$P/seller/subscription/checkout",
            token,
            JSONObject().put("tier_key", tier).put("duration_months", months),
        )
        return parseInvoice(p, tier, months)
    }

    fun cancelCheckout(token: String) {
        call("POST", "$P/seller/subscription/cancel", token, JSONObject())
    }

    /** وضعیت پرداخت اشتراک: pending / paid / expired / cancelled */
    fun paymentStatus(token: String, paymentId: Long): String {
        val p = call("GET", "$P/seller/subscription/payments/$paymentId", token)
        return (p.optJSONObject("payment") ?: JSONObject()).str("status")
    }

    private fun parseInvoice(p: JSONObject, tier: String, months: Int): Invoice {
        val pay = p.optJSONObject("payment") ?: JSONObject()
        val card = p.optJSONObject("platform_card") ?: JSONObject()
        return Invoice(
            payment = SubPayment(
                id = pay.lng("id"),
                tracking = pay.str("tracking_code"),
                tier = pay.str("plan_tier").ifBlank { tier },
                months = pay.int("duration_months").takeIf { it > 0 } ?: months,
                amount = pay.lng("unique_amount_toman"),
                expiresAtMs = Fmt.isoMillis(pay.str("expires_at")),
            ),
            card = PlatformCard(
                number = card.str("number"),
                numberFormatted = card.str("number_formatted").ifBlank { card.str("number") },
                holder = card.str("holder"),
                bank = card.str("bank_name"),
                shebaFormatted = card.str("sheba_formatted").ifBlank { card.str("sheba") },
            ),
        )
    }

    // ───────────────────────── کلید API و افزونه ─────────────────────────

    fun regenerateKey(token: String): Account {
        call("POST", "$P/seller/regenerate-api-key", token, JSONObject())
        return account(token)
    }

    fun downloadPlugin(token: String, target: File) {
        val request = Request.Builder()
            .url("$ROOT$P/seller/woocommerce-plugin")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        uploadClient.newCall(request).execute().use { response ->
            if (response.code == 401) throw ApiException("نشست تمام شده. دوباره وارد شو", 401)
            if (!response.isSuccessful) {
                val payload = parse(response.body?.string().orEmpty())
                throw ApiException(payload.str("error").ifBlank { "دانلود افزونه ناموفق بود" }, response.code)
            }
            val body = response.body ?: throw ApiException("پاسخ خالی از سرور")
            target.parentFile?.mkdirs()
            target.outputStream().use { out -> body.byteStream().use { it.copyTo(out) } }
        }
    }

    // ───────────────────────── فروشنده‌های پلتفرم ─────────────────────────

    fun vendors(token: String): VendorsData {
        val p = call("GET", "$P/seller/vendors", token)
        return VendorsData(
            vendors = p.optJSONArray("vendors").mapObjects { it.toVendor() },
            count = p.int("vendor_count"),
            max = p.int("max_vendors"),
        )
    }

    fun createVendor(token: String, externalId: String, name: String) {
        call(
            "POST",
            "$P/seller/vendors",
            token,
            JSONObject().put("external_id", externalId.trim()).put("name", name.trim()),
        )
    }

    fun deactivateVendor(token: String, id: Long) {
        call("PATCH", "$P/seller/vendors/$id", token, JSONObject().put("deactivate", true))
    }

    fun savePlatformWebhook(token: String, url: String) {
        call(
            "PUT",
            "$P/seller/platform-vendor-webhook",
            token,
            JSONObject().put("platform_vendor_webhook_url", url.trim()),
        )
    }

    // ───────────────────────── وب‌هوک پیامک ─────────────────────────

    fun postSms(webhookUrl: String, secret: String, sms: String): SmsPostResult {
        val url = resolveSmsWebhookUrl(webhookUrl)
        val body = JSONObject().put("sms", sms).toString().toRequestBody(json)
        val request = Request.Builder()
            .url(url)
            .header("X-Webhook-Secret", secret)
            .header("User-Agent", "TCartSeller-SMS/Android")
            .header("Accept", "application/json")
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
                error = payload.str("error"),
            )
        }
    }

    // ───────────────────────── پایه ─────────────────────────

    private fun call(method: String, path: String, token: String? = null, body: JSONObject? = null): JSONObject {
        try {
            return executeCall(method, path, token, body)
        } catch (e: IOException) {
            // فقط GET را یک‌بار سریع retry کن (POST لاگین را دوبار نفرست)
            if (method != "GET") throw e
            try {
                Thread.sleep(250)
            } catch (_: InterruptedException) {
            }
            return executeCall(method, path, token, body)
        }
    }

    private fun executeCall(method: String, path: String, token: String?, body: JSONObject?): JSONObject {
        val builder = Request.Builder().url("$ROOT$path")
        if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
        when (method) {
            "POST" -> builder.post((body ?: JSONObject()).toString().toRequestBody(json))
            "PUT" -> builder.put((body ?: JSONObject()).toString().toRequestBody(json))
            "PATCH" -> builder.patch((body ?: JSONObject()).toString().toRequestBody(json))
            "DELETE" -> builder.delete()
            else -> builder.get()
        }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val payload = parse(text)
            if (response.code == 401 && !token.isNullOrBlank()) {
                throw ApiException("نشست تمام شده. دوباره وارد شو", 401)
            }
            if (!response.isSuccessful || (payload.has("ok") && !payload.optBoolean("ok", true))) {
                val message = payload.str("error").ifBlank {
                    if (text.isNotBlank() && payload.length() == 0) "پاسخ نامعتبر از سرور (کد ${response.code})"
                    else "خطا در ارتباط با سرور"
                }
                throw ApiException(message, response.code)
            }
            return payload
        }
    }

    private fun parse(text: String): JSONObject =
        if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrDefault(JSONObject())

    private fun parseAccount(token: String, payload: JSONObject): Account {
        val user = payload.optJSONObject("user") ?: JSONObject()
        val pr = payload.optJSONObject("profile") ?: JSONObject()
        return Account(
            token = token,
            name = user.str("full_name"),
            phone = user.str("phone"),
            email = user.str("email"),
            hasTelegram = user.bool("has_telegram"),
            hasCard = pr.bool("has_card"),
            cardFormatted = pr.str("card_number_formatted"),
            cardHolder = pr.str("card_holder"),
            bankName = pr.str("bank_name"),
            shebaFormatted = pr.str("sheba_formatted"),
            apiKey = pr.str("api_key"),
            smsSecret = pr.str("sms_webhook_secret"),
            callbackSecret = pr.str("callback_secret"),
            callbackUrl = pr.str("default_callback_url"),
            webhookUrl = pr.str("webhook_sms_url"),
            subscriptionEnabled = pr.bool("subscription_enabled"),
            subscriptionActive = pr.bool("subscription_active"),
            planTier = pr.str("plan_tier"),
            expiresAt = pr.str("subscription_expires_at"),
            daysLeft = pr.intOrNull("subscription_days_left"),
            paidThisMonth = pr.int("paid_orders_this_month"),
            planLimit = pr.int("plan_limit"),
            rotateEnabled = pr.bool("card_rotate_enabled"),
            isPlatform = pr.bool("is_platform_tier"),
            maxVendors = pr.int("max_vendors"),
            platformWebhook = pr.str("platform_vendor_webhook_url"),
            notices = pr.optJSONArray("notifications").mapObjects {
                Notice(
                    id = it.lng("id"),
                    title = it.str("title"),
                    message = it.str("message"),
                    createdAt = it.str("created_at"),
                )
            },
        )
    }

    private fun parseCards(p: JSONObject): CardsData {
        val s = p.optJSONObject("payment_settings") ?: JSONObject()
        return CardsData(
            cards = p.optJSONArray("cards").mapObjects { it.toCard() },
            maxCards = p.int("max_cards"),
            canAdd = p.bool("can_add", true),
            rotate = p.bool("rotate_enabled"),
            settings = PaymentSettings(
                defaultExpiry = s.intOrNull("default_payment_expiry_minutes"),
                showSheba = s.bool("show_sheba_on_invoice"),
                effectiveExpiry = s.int("effective_default_payment_expiry_minutes").takeIf { it > 0 } ?: 30,
                systemExpiry = s.int("system_default_expiry_minutes").takeIf { it > 0 } ?: 30,
                shebaExpiry = s.int("sheba_default_expiry_minutes").takeIf { it > 0 } ?: 60,
            ),
        )
    }

    private fun JSONObject.toOrder(): Order {
        val paid = str("paid_at")
        val created = str("created_at")
        return Order(
            id = lng("id"),
            tracking = str("tracking_code"),
            title = str("title").ifBlank { "سفارش" },
            status = str("status"),
            source = str("source"),
            buyerName = str("buyer_name"),
            baseToman = lng("base_price_toman"),
            uniqueToman = lng("unique_amount_toman"),
            expiresAt = str("expires_at"),
            paidAt = paid,
            createdAt = created,
            buyerUrl = str("buyer_url"),
            timeMs = Fmt.isoMillis(paid.ifBlank { created }),
        )
    }

    private fun JSONObject.toProduct(): Product = Product(
        id = lng("id"),
        name = str("name"),
        description = str("description"),
        price = lng("price_toman"),
        buyPath = str("buy_url"),
        deliveryType = str("delivery_type").ifBlank { "manual" },
        deliveryContent = str("delivery_content"),
        deliveryInstructions = str("delivery_instructions"),
        deliveryFileName = str("delivery_file_name"),
        hasFile = bool("has_delivery_file"),
        maxPurchases = intOrNull("max_purchases"),
        paidCount = int("paid_count"),
        reserved = int("reserved_count"),
        slotsRemaining = intOrNull("slots_remaining"),
        salesEnabled = bool("sales_enabled", true),
        paymentExpiry = intOrNull("payment_expiry_minutes"),
        effectiveExpiry = intOrNull("effective_payment_expiry_minutes"),
        soldOut = bool("sold_out"),
        linksTotal = int("delivery_links_total"),
        linksAvailable = int("delivery_links_available"),
        linksAssigned = int("delivery_links_assigned"),
        linksShared = bool("delivery_links_shared"),
    )

    private fun JSONObject.toCard(): BankCard = BankCard(
        id = lng("id"),
        number = str("card_number"),
        numberFormatted = str("card_number_formatted").ifBlank { str("card_number") },
        holder = str("card_holder"),
        bank = str("bank_name"),
        sheba = str("sheba"),
        shebaFormatted = str("sheba_formatted").ifBlank { str("sheba") },
        nickname = str("nickname"),
        paused = bool("paused"),
        maxTx = intOrNull("max_transactions"),
        usedTx = int("used_transactions"),
        primary = bool("is_primary"),
    )

    private fun JSONObject.toVendor(): Vendor = Vendor(
        id = lng("vendor_id"),
        externalId = str("external_id"),
        name = str("name"),
        onboarded = bool("is_onboarded"),
        hasAccount = bool("has_account"),
        contactName = str("contact_full_name"),
        contactPhone = str("contact_phone"),
        contactEmail = str("contact_email"),
        onboardingUrl = str("onboarding_url"),
        panelUrl = str("panel_url"),
        active = bool("is_active", true),
    )
}
