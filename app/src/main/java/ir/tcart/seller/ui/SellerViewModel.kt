package ir.tcart.seller.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.tcart.seller.data.Account
import ir.tcart.seller.data.ApiException
import ir.tcart.seller.data.AppConfig
import ir.tcart.seller.data.CardsData
import ir.tcart.seller.data.DayPoint
import ir.tcart.seller.data.Fmt
import ir.tcart.seller.data.Invoice
import ir.tcart.seller.data.Order
import ir.tcart.seller.data.OrdersData
import ir.tcart.seller.data.PickedFile
import ir.tcart.seller.data.PlansData
import ir.tcart.seller.data.Prefs
import ir.tcart.seller.data.Product
import ir.tcart.seller.data.ProductInput
import ir.tcart.seller.data.SellerStats
import ir.tcart.seller.data.TcartApi
import ir.tcart.seller.data.VendorsData
import ir.tcart.seller.notify.PaidNotifier
import ir.tcart.seller.sms.BankSender
import ir.tcart.seller.sms.Celebration
import ir.tcart.seller.sms.ListenService
import ir.tcart.seller.sms.PaymentBus
import ir.tcart.seller.sms.SmsInbox
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Calendar

enum class Stage { Splash, Welcome, Permissions, Auth, Panel }

enum class AuthMode { Login, Register, Forgot, Reset }

enum class Dest { Home, Products, Orders, Cards, More, Sms, Subscription, Api, Vendors }

data class Flash(val text: String, val error: Boolean, val id: Long)

data class Period(
    val revenue30: Long = 0,
    val paid30: Int = 0,
    val previousRevenue30: Long = 0,
    val todayRevenue: Long = 0,
    val todayPaid: Int = 0,
)

class SellerViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    // ───────── ناوبری و وضعیت عمومی ─────────
    var stage by mutableStateOf(Stage.Splash)
    var permStep by mutableIntStateOf(0)
    var authMode by mutableStateOf(AuthMode.Login)
    var dest by mutableStateOf(Dest.Home)
    private var backTo = Dest.More
    var busySet by mutableStateOf<Set<String>>(emptySet())
    var authError by mutableStateOf<String?>(null)
    var authInfo by mutableStateOf<String?>(null)
    var flash by mutableStateOf<Flash?>(null)
    var light by mutableStateOf(prefs.lightTheme)
    var celebrate by mutableStateOf<Celebration?>(null)
    var booted by mutableStateOf(false)

    /** از داشبورد می‌گوییم فرم «محصول جدید» / «کارت جدید» خودش باز شود. */
    var newProductRequest by mutableStateOf(false)
    var newCardRequest by mutableStateOf(false)

    fun isBusy(key: String): Boolean = key in busySet

    val anyBusy: Boolean get() = busySet.isNotEmpty()

    // ───────── داده‌های حساب ─────────
    var account by mutableStateOf<Account?>(null)
    var config by mutableStateOf<AppConfig?>(null)
    var orders by mutableStateOf<List<Order>>(emptyList())
    var stats by mutableStateOf<SellerStats?>(null)
    var products by mutableStateOf<List<Product>>(emptyList())
    var cards by mutableStateOf<CardsData?>(null)
    var series by mutableStateOf<List<DayPoint>>(emptyList())
    var period by mutableStateOf(Period())
    var lookupResult by mutableStateOf<Order?>(null)
    var plans by mutableStateOf<PlansData?>(null)
    var invoice by mutableStateOf<Invoice?>(null)
    var invoiceActivated by mutableStateOf(false)
    var vendors by mutableStateOf<VendorsData?>(null)
    var pluginFile by mutableStateOf<File?>(null)
    var pickedFile by mutableStateOf<PickedFile?>(null)

    // ───────── پیامک بانکی ─────────
    var listening by mutableStateOf(prefs.listening)
    var history by mutableStateOf(prefs.history())
    var senders by mutableStateOf<List<BankSender>>(emptyList())
    var selectedBanks by mutableStateOf(prefs.bankSenders())
    var manualSender by mutableStateOf("")

    private var flashJob: Job? = null
    private var pollJob: Job? = null
    private var autoJob: Job? = null

    private val token: String get() = prefs.token

    init {
        viewModelScope.launch {
            PaymentBus.celebrations.collect { event ->
                history = prefs.history()
                celebrate = event
                quietRefreshOrders()
                delay(4200)
                if (celebrate == event) celebrate = null
            }
        }
        viewModelScope.launch {
            delay(900)
            if (token.isBlank()) {
                stage = if (prefs.seenIntro) Stage.Auth else Stage.Welcome
            } else {
                stage = Stage.Panel
                enterPanel()
            }
        }
    }

    // ───────── کمکی‌های عمومی ─────────

    fun say(text: String, error: Boolean = false) {
        val item = Flash(text, error, System.nanoTime())
        flash = item
        flashJob?.cancel()
        flashJob = viewModelScope.launch {
            delay(if (error) 4800 else 2800)
            if (flash?.id == item.id) flash = null
        }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    /** مثل io ولی خطاهای غیرِ نشست را بی‌صدا نادیده می‌گیرد. */
    private suspend fun <T> soft(block: () -> T): T? = try {
        withContext(Dispatchers.IO) { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        if (e.code == 401) throw e
        null
    } catch (e: IOException) {
        null
    }

    private fun work(key: String, block: suspend () -> Unit) {
        if (key in busySet) return
        viewModelScope.launch {
            busySet = busySet + key
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.code == 401) logout(expired = true) else report(e.message ?: "خطا")
            } catch (e: IOException) {
                report("به سرور تیکارت وصل نشد. اینترنت را بررسی کن")
            } catch (e: Exception) {
                report(e.message ?: "خطای نامشخص")
            } finally {
                busySet = busySet - key
            }
        }
    }

    private fun report(message: String) {
        if (stage == Stage.Auth) authError = message else say(message, true)
    }

    // ───────── ناوبری ─────────

    fun isRoot(d: Dest) = d == Dest.Home || d == Dest.Products || d == Dest.Orders || d == Dest.Cards || d == Dest.More

    fun go(target: Dest) {
        if (target == dest) return
        if (isRoot(target)) {
            dest = target
        } else {
            if (isRoot(dest)) backTo = dest
            dest = target
        }
        when (target) {
            Dest.Subscription -> loadPlans()
            Dest.Vendors -> loadVendors()
            Dest.Sms -> refreshHistory()
            else -> {}
        }
    }

    /** true یعنی بک مصرف شد. */
    fun back(): Boolean {
        if (!isRoot(dest)) {
            if (dest == Dest.Subscription) closeInvoiceLocal()
            dest = backTo
            return true
        }
        if (dest != Dest.Home) {
            dest = Dest.Home
            return true
        }
        return false
    }

    fun startIntro() {
        prefs.seenIntro = true
        permStep = 0
        stage = Stage.Permissions
    }

    fun nextPerm() {
        if (permStep >= 3) {
            stage = Stage.Auth
        } else {
            permStep += 1
        }
    }

    fun toggleTheme() {
        light = !light
        prefs.lightTheme = light
    }

    // ───────── احراز هویت ─────────

    fun switchAuth(mode: AuthMode) {
        authMode = mode
        authError = null
        authInfo = null
    }

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            authError = "شماره موبایل (یا ایمیل) و رمز را بنویس"
            return
        }
        authError = null
        work("auth") {
            val acc = io { TcartApi.login(identifier, password) }
            afterAuth(acc)
        }
    }

    fun register(name: String, phone: String, email: String, password: String, confirm: String) {
        val phoneDigits = Fmt.digitsOnly(phone)
        when {
            phoneDigits.length < 10 -> authError = "شماره موبایل را درست بنویس (مثلاً ۰۹۱۲…)"
            password.length < 8 -> authError = "رمز عبور حداقل ۸ کاراکتر باشد"
            password != confirm -> authError = "رمز و تکرار آن یکی نیست"
            else -> {
                authError = null
                work("auth") {
                    val acc = io { TcartApi.register(name, phone, email, password) }
                    afterAuth(acc)
                }
            }
        }
    }

    fun forgot(phone: String) {
        if (Fmt.digitsOnly(phone).length < 10) {
            authError = "شماره موبایل ثبت‌شده را بنویس"
            return
        }
        authError = null
        work("auth") {
            val message = io { TcartApi.forgotPassword(phone) }
            authInfo = message
            authMode = AuthMode.Reset
        }
    }

    fun reset(phone: String, code: String, password: String, confirm: String) {
        when {
            Fmt.digitsOnly(code).isBlank() -> authError = "کد بازیابی را بنویس"
            password.length < 8 -> authError = "رمز جدید حداقل ۸ کاراکتر باشد"
            password != confirm -> authError = "رمز و تکرار آن یکی نیست"
            else -> {
                authError = null
                work("auth") {
                    val acc = io { TcartApi.resetPassword(phone, code, password) }
                    afterAuth(acc)
                }
            }
        }
    }

    private fun afterAuth(acc: Account) {
        store(acc)
        authError = null
        authInfo = null
        stage = Stage.Panel
        dest = Dest.Home
        enterPanel()
    }

    fun logout(expired: Boolean = false) {
        ListenService.stop(getApplication())
        pollJob?.cancel()
        autoJob?.cancel()
        prefs.clearSession()
        account = null
        config = null
        orders = emptyList()
        stats = null
        products = emptyList()
        cards = null
        series = emptyList()
        period = Period()
        lookupResult = null
        plans = null
        invoice = null
        vendors = null
        listening = false
        history = emptyList()
        celebrate = null
        booted = false
        dest = Dest.Home
        authMode = AuthMode.Login
        authInfo = null
        authError = if (expired) "نشست تو تمام شده؛ دوباره وارد شو" else null
        stage = Stage.Auth
    }

    // ───────── بارگذاری داده‌ها ─────────

    private fun enterPanel() {
        refreshAll()
        autoJob?.cancel()
        autoJob = viewModelScope.launch {
            while (true) {
                delay(45_000)
                if (stage == Stage.Panel && (dest == Dest.Home || dest == Dest.Orders)) quietRefreshOrders()
            }
        }
    }

    fun refreshAll() {
        if (token.isBlank()) return
        work("refresh") {
            val acc = try {
                io { TcartApi.account(token) }
            } catch (e: IOException) {
                booted = true
                say("اتصال به سرور برقرار نشد. اگر سایت بدون VPN باز می‌شود، یک‌بار اپ را ببند و دوباره باز کن", true)
                return@work
            }
            store(acc)
            if (config == null) config = soft { TcartApi.config() }
            if (!acc.locked) {
                soft { TcartApi.orders(token) }?.let { applyOrders(it) }
                soft { TcartApi.products(token) }?.let { products = it }
                soft { TcartApi.cards(token) }?.let { cards = it }
            }
            history = prefs.history()
            booted = true
        }
    }

    private fun quietRefreshOrders() {
        if (token.isBlank() || account?.locked == true) return
        viewModelScope.launch {
            try {
                soft { TcartApi.orders(token) }?.let { applyOrders(it) }
            } catch (e: ApiException) {
                if (e.code == 401) logout(expired = true)
            }
        }
    }

    fun loadOrders() {
        if (token.isBlank() || account?.locked == true) return
        work("orders") { soft { TcartApi.orders(token) }?.let { applyOrders(it) } }
    }

    fun loadProducts() {
        if (token.isBlank() || account?.locked == true) return
        work("products") { products = io { TcartApi.products(token) } }
    }

    fun loadCards() {
        if (token.isBlank() || account?.locked == true) return
        work("cards") { cards = io { TcartApi.cards(token) } }
    }

    private fun applyOrders(data: OrdersData) {
        orders = data.orders
        stats = data.stats
        val day = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val starts = List(60) { n ->
            val c = day.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -n)
            c.timeInMillis
        }
        val revenue = LongArray(30)
        val counts = IntArray(30)
        var prev = 0L
        var rev30 = 0L
        var paid30 = 0
        var today = 0L
        var todayPaid = 0
        data.orders.filter { it.status == "paid" && it.timeMs > 0 }.forEach { o ->
            val ago = starts.indexOfFirst { o.timeMs >= it }
            when {
                ago < 0 -> {}
                ago < 30 -> {
                    revenue[ago] += o.uniqueToman
                    counts[ago] += 1
                    rev30 += o.uniqueToman
                    paid30 += 1
                    if (ago == 0) {
                        today += o.uniqueToman
                        todayPaid += 1
                    }
                }
                else -> prev += o.uniqueToman
            }
        }
        series = List(30) { i ->
            val n = 29 - i
            DayPoint(Fmt.dayLabel(starts[n]), revenue[n], counts[n])
        }
        period = Period(rev30, paid30, prev, today, todayPaid)
    }

    private fun store(acc: Account) {
        prefs.token = acc.token
        prefs.sellerName = acc.name
        prefs.webhookUrl = acc.webhookUrl
        prefs.secret = acc.smsSecret
        account = acc
        if (prefs.listening && acc.smsSecret.isNotBlank()) ListenService.start(getApplication())
    }

    private fun refreshAccountQuiet() {
        viewModelScope.launch {
            try {
                soft { TcartApi.account(token) }?.let { store(it) }
            } catch (e: ApiException) {
                if (e.code == 401) logout(expired = true)
            }
        }
    }

    // ───────── سفارش‌ها ─────────

    fun confirm(order: Order) = work("order:${order.id}") {
        io { TcartApi.confirmOrder(token, order.id) }
        say("سفارش تأیید شد و تحویل انجام می‌شود ✓")
        lookupResult = null
        soft { TcartApi.orders(token) }?.let { applyOrders(it) }
    }

    fun reject(order: Order) = work("order:${order.id}") {
        io { TcartApi.rejectOrder(token, order.id) }
        say("سفارش رد شد")
        lookupResult = null
        soft { TcartApi.orders(token) }?.let { applyOrders(it) }
    }

    fun lookup(query: String) {
        if (query.isBlank()) {
            lookupResult = null
            return
        }
        work("lookup") {
            lookupResult = io { TcartApi.lookupOrder(token, query) }
        }
    }

    fun clearLookup() {
        lookupResult = null
    }

    // ───────── محصولات ─────────

    fun buyUrl(p: Product): String = "${TcartApi.ROOT}/buy/${p.id}"

    fun telegramUrl(p: Product): String {
        val bot = config?.botUsername.orEmpty()
        return if (bot.isBlank()) "" else "https://t.me/$bot?start=buy_${p.id}"
    }

    fun pickFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val picked = withContext(Dispatchers.IO) { readPicked(uri) }
                if (picked == null) say("فایل خوانده نشد", true) else pickedFile = picked
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                say(e.message ?: "فایل خوانده نشد", true)
            }
        }
    }

    fun clearPicked() {
        pickedFile = null
    }

    private fun readPicked(uri: Uri): PickedFile? {
        val resolver = getApplication<Application>().contentResolver
        var name = "file"
        var size = -1L
        resolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = c.getString(nameIdx) ?: name
                if (sizeIdx >= 0 && !c.isNull(sizeIdx)) size = c.getLong(sizeIdx)
            }
        }
        if (size > MAX_FILE) throw IllegalStateException("حداکثر حجم فایل ۲۵ مگابایت است")
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        if (bytes.size > MAX_FILE) throw IllegalStateException("حداکثر حجم فایل ۲۵ مگابایت است")
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        return PickedFile(name, mime, bytes)
    }

    fun saveProduct(editing: Product?, input: ProductInput, file: PickedFile?, onDone: () -> Unit) {
        val hadFile = editing?.hasFile == true
        when {
            input.name.isBlank() -> {
                say("نام محصول را بنویس", true)
                return
            }
            input.priceToman < 1000 -> {
                say("حداقل قیمت محصول ۱٬۰۰۰ تومان است", true)
                return
            }
            input.deliveryType == "link" && input.deliveryContent.isBlank() -> {
                say("حداقل یک لینک تحویل بنویس", true)
                return
            }
            input.deliveryType == "text" && input.deliveryContent.isBlank() -> {
                say("متن یا کد تحویل را بنویس", true)
                return
            }
            input.deliveryType == "file" && file == null && !hadFile -> {
                say("فایل محصول را انتخاب کن", true)
                return
            }
        }
        work("product") {
            val sendType = if (input.deliveryType == "file" && !hadFile) "manual" else input.deliveryType
            val payload = input.copy(
                deliveryType = sendType,
                deliveryContent = if (input.deliveryType == "file") "" else input.deliveryContent,
            )
            val saved = io { TcartApi.saveProduct(token, editing?.id, payload) }
            var uploadError: String? = null
            if (input.deliveryType == "file" && file != null) {
                try {
                    io { TcartApi.uploadDeliveryFile(token, saved.id, file) }
                } catch (e: ApiException) {
                    if (e.code == 401) throw e
                    uploadError = e.message
                } catch (e: IOException) {
                    uploadError = "اتصال هنگام آپلود قطع شد"
                }
            }
            soft { TcartApi.products(token) }?.let { products = it }
            pickedFile = null
            if (uploadError != null) {
                say("محصول ذخیره شد ولی فایل آپلود نشد: $uploadError. از ویرایش محصول دوباره امتحان کن.", true)
            } else {
                say(if (editing == null) "محصول ساخته شد؛ لینک خرید آماده است ✓" else "تغییرات محصول ذخیره شد ✓")
            }
            onDone()
        }
    }

    fun deleteProduct(p: Product) = work("product:${p.id}") {
        val message = io { TcartApi.deleteProduct(token, p.id) }
        soft { TcartApi.products(token) }?.let { products = it }
        say(message)
    }

    // ───────── کارت‌ها ─────────

    fun addCard(
        number: String,
        holder: String,
        bank: String,
        sheba: String,
        nickname: String,
        maxTx: Int?,
        onDone: () -> Unit,
    ) {
        if (Fmt.digitsOnly(number).length != 16) {
            say("شمارهٔ کارت باید ۱۶ رقم باشد", true)
            return
        }
        if (holder.isBlank()) {
            say("نام صاحب کارت را بنویس", true)
            return
        }
        if (bank.isBlank()) {
            say("نام بانک را انتخاب یا بنویس", true)
            return
        }
        work("card") {
            cards = io { TcartApi.addCard(token, number, holder, bank, sheba, nickname, maxTx) }
            refreshAccountQuiet()
            say("کارت ثبت شد ✓")
            onDone()
        }
    }

    fun setPaused(id: Long, paused: Boolean) = work("card:$id") {
        cards = io { TcartApi.patchCard(token, id, JSONObject().put("paused", paused)) }
        say(if (paused) "کارت متوقف شد" else "کارت دوباره فعال شد")
    }

    fun saveShebaAndCap(id: Long, sheba: String, capRaw: String, rotation: Boolean) = work("card:$id") {
        val body = JSONObject().put("sheba", TcartApi.normalizeSheba(sheba))
        if (rotation) {
            val cap = Fmt.digitsOnly(capRaw)
            body.put("max_transactions", if (cap.isBlank()) JSONObject.NULL else cap.toInt())
        }
        cards = io { TcartApi.patchCard(token, id, body) }
        say("تغییرات کارت ذخیره شد ✓")
    }

    fun deleteCard(id: Long) = work("card:$id") {
        cards = io { TcartApi.deleteCard(token, id) }
        refreshAccountQuiet()
        say("کارت حذف شد")
    }

    fun makePrimary(id: Long) = work("card:$id") {
        cards = io { TcartApi.primaryCard(token, id) }
        say("این کارت اصلی شد ✓")
    }

    fun setRotation(enabled: Boolean) = work("rotation") {
        cards = io { TcartApi.rotateCards(token, enabled) }
        say(if (enabled) "چرخش بین کارت‌ها فعال شد" else "چرخش کارت‌ها خاموش شد")
    }

    fun savePaySettings(showSheba: Boolean, minutesRaw: String) {
        val raw = Fmt.digitsOnly(minutesRaw)
        val minutes = if (raw.isBlank()) null else raw.toIntOrNull()
        if (minutes != null && (minutes < 5 || minutes > 1440)) {
            say("مهلت واریز باید بین ۵ تا ۱۴۴۰ دقیقه باشد", true)
            return
        }
        work("pay") {
            cards = io { TcartApi.savePaySettings(token, showSheba, minutes) }
            say("تنظیمات پرداخت ذخیره شد ✓")
        }
    }

    // ───────── اشتراک ─────────

    fun loadPlans() {
        if (plans != null || token.isBlank()) return
        viewModelScope.launch {
            try {
                plans = soft { TcartApi.plans() }
            } catch (e: ApiException) {
                if (e.code == 401) logout(expired = true)
            }
        }
    }

    fun buy(tier: String, months: Int) = work("buy:$tier") {
        val inv = io { TcartApi.checkout(token, tier, months) }
        invoice = inv
        invoiceActivated = false
        startPolling(inv.payment.id)
    }

    fun cancelCheckout() = work("cancel") {
        io { TcartApi.cancelCheckout(token) }
        closeInvoiceLocal()
        say("فاکتور لغو شد")
    }

    fun closeInvoice() {
        closeInvoiceLocal()
        refreshAccountQuiet()
    }

    private fun closeInvoiceLocal() {
        pollJob?.cancel()
        invoice = null
        invoiceActivated = false
    }

    private fun startPolling(paymentId: Long) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                val status = try {
                    withContext(Dispatchers.IO) { TcartApi.paymentStatus(token, paymentId) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    continue
                }
                if (status == "paid") {
                    invoiceActivated = true
                    soft { TcartApi.account(token) }?.let { store(it) }
                    refreshAll()
                    break
                }
                if (status == "expired" || status == "cancelled") {
                    say("فاکتور اشتراک منقضی یا لغو شد", true)
                    invoice = null
                    break
                }
            }
        }
    }

    // ───────── کلید API، افزونه، وندورها ─────────

    fun regenerateKey() = work("regen") {
        val acc = io { TcartApi.regenerateKey(token) }
        store(acc)
        say("کلید API جدید ساخته شد؛ کلید قبلی دیگر کار نمی‌کند")
    }

    fun downloadPlugin() = work("plugin") {
        val target = File(getApplication<Application>().cacheDir, "shared/tcart-for-woocommerce.zip")
        io { TcartApi.downloadPlugin(token, target) }
        pluginFile = target
    }

    fun consumePlugin() {
        pluginFile = null
    }

    fun loadVendors() {
        if (token.isBlank()) return
        viewModelScope.launch {
            try {
                soft { TcartApi.vendors(token) }?.let { vendors = it }
            } catch (e: ApiException) {
                if (e.code == 401) logout(expired = true)
            }
        }
    }

    fun createVendor(externalId: String, name: String, onDone: () -> Unit) {
        if (externalId.isBlank() || name.isBlank()) {
            say("شناسه و نام فروشنده را بنویس", true)
            return
        }
        work("vendor") {
            io { TcartApi.createVendor(token, externalId, name) }
            vendors = io { TcartApi.vendors(token) }
            say("فروشنده ساخته شد ✓")
            onDone()
        }
    }

    fun deactivateVendor(id: Long) = work("vendor:$id") {
        io { TcartApi.deactivateVendor(token, id) }
        vendors = io { TcartApi.vendors(token) }
        say("فروشنده غیرفعال شد")
    }

    fun savePlatformWebhook(url: String) = work("webhook") {
        io { TcartApi.savePlatformWebhook(token, url) }
        refreshAccountQuiet()
        say("آدرس وب‌هوک ذخیره شد ✓")
    }

    fun linkPhone(phone: String) {
        if (Fmt.digitsOnly(phone).length < 10) {
            say("شمارهٔ موبایل را درست بنویس", true)
            return
        }
        work("phone") {
            io { TcartApi.linkPhone(token, phone) }
            soft { TcartApi.account(token) }?.let { store(it) }
            say("شماره موبایل ثبت شد ✓")
        }
    }

    // ───────── پیامک بانکی ─────────

    fun refreshHistory() {
        history = prefs.history()
        listening = prefs.listening
    }

    fun refreshSenders() {
        senders = runCatching { SmsInbox.recentSenders(getApplication()) }.getOrDefault(emptyList())
        selectedBanks = prefs.bankSenders()
    }

    fun toggleBank(key: String) {
        val next = selectedBanks.toMutableSet()
        if (!next.add(key)) next.remove(key)
        selectedBanks = next
        prefs.setBankSenders(next)
        if (prefs.listening) prefs.lineLabel = "${next.size} سرشمارهٔ بانک"
    }

    fun addManualSender() {
        val key = SmsInbox.normalizeSender(manualSender)
        if (key.isBlank()) return
        if (key !in selectedBanks) toggleBank(key)
        manualSender = ""
    }

    fun startListening() {
        if (prefs.bankSenders().isEmpty()) {
            say("اول سرشمارهٔ بانک را انتخاب کن", true)
            refreshSenders()
            return
        }
        if (prefs.secret.isBlank() || prefs.webhookUrl.isBlank()) {
            say("اول یک کارت بانکی ثبت کن تا کلید پیامک ساخته شود", true)
            return
        }
        prefs.subscriptionId = Prefs.UNSET
        prefs.lineLabel = "${prefs.bankSenders().size} سرشمارهٔ بانک"
        prefs.listening = true
        listening = true
        ListenService.start(getApplication())
        say("ارسال خودکار پیامک روشن شد ✓")
    }

    fun pauseListening() {
        prefs.listening = false
        listening = false
        ListenService.stop(getApplication())
        say("ارسال خودکار پیامک متوقف شد")
    }

    fun previewNotification() {
        val event = Celebration(250_000, "پرداخت تأیید شد", "نمونه")
        celebrate = event
        PaidNotifier.showPaid(getApplication(), 250_000, "پرداخت تأیید شد", "نمونه")
        viewModelScope.launch {
            delay(4200)
            if (celebrate == event) celebrate = null
        }
    }

    // ───────── دسترسی‌ها ─────────

    fun permissions(): Array<String> = buildList {
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    fun notifGranted(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return granted(getApplication(), Manifest.permission.POST_NOTIFICATIONS)
    }

    fun smsGranted(): Boolean {
        val ctx = getApplication<Application>()
        return granted(ctx, Manifest.permission.RECEIVE_SMS) && granted(ctx, Manifest.permission.READ_SMS)
    }

    fun askBattery(context: Context) {
        val pm = context.getSystemService(PowerManager::class.java) ?: return
        if (pm.isIgnoringBatteryOptimizations(context.packageName)) return
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun granted(ctx: Context, permission: String) =
        ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

    /** تب پایینی که باید روشن باشد (برای صفحه‌های داخلی، تبِ مبدأ). */
    fun activeRoot(): Dest = if (isRoot(dest)) dest else backTo

    companion object {
        private const val MAX_FILE = 25L * 1024 * 1024
    }
}

