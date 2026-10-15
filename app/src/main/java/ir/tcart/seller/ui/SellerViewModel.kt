package ir.tcart.seller.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.tcart.seller.data.Account
import ir.tcart.seller.data.Dash
import ir.tcart.seller.data.Invoice
import ir.tcart.seller.data.OrderRow
import ir.tcart.seller.data.OrderStats
import ir.tcart.seller.data.PlanOffer
import ir.tcart.seller.data.Prefs
import ir.tcart.seller.data.ProductRow
import org.json.JSONObject
import ir.tcart.seller.data.TcartApi
import ir.tcart.seller.notify.PaidNotifier
import ir.tcart.seller.sms.BankSender
import ir.tcart.seller.sms.Celebration
import ir.tcart.seller.sms.ListenService
import ir.tcart.seller.sms.PaymentBus
import ir.tcart.seller.sms.SmsInbox
import ir.tcart.seller.sms.SmsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Calendar

data class SimLine(val id: Int, val title: String, val detail: String)
data class DayPoint(val label: String, val revenue: Long)

enum class Stage { Splash, Welcome, Permissions, Login, Panel }

class SellerViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    var stage by mutableStateOf(Stage.Splash)
    var permStep by mutableIntStateOf(0)
    var tab by mutableIntStateOf(0)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var name by mutableStateOf(prefs.sellerName)
    var line by mutableStateOf(prefs.lineLabel)
    var listening by mutableStateOf(prefs.listening)
    var sims by mutableStateOf<List<SimLine>>(emptyList())
    var picked by mutableStateOf(prefs.subscriptionId)
    var history by mutableStateOf(prefs.history())
    var celebrate by mutableStateOf<Celebration?>(null)
    var account by mutableStateOf<Account?>(null)
    var orders by mutableStateOf<List<OrderRow>>(emptyList())
    var products by mutableStateOf<List<ProductRow>>(emptyList())
    var stats by mutableStateOf<OrderStats?>(null)
    var wallet by mutableStateOf(0L)
    var series by mutableStateOf<List<DayPoint>>(emptyList())
    var copied by mutableStateOf(false)
    var light by mutableStateOf(prefs.lightTheme)
    var senders by mutableStateOf<List<BankSender>>(emptyList())
    var selectedBanks by mutableStateOf(prefs.bankSenders())
    var offers by mutableStateOf<List<PlanOffer>>(emptyList())
    var invoice by mutableStateOf<Invoice?>(null)
    var manualSender by mutableStateOf("")
    var screen by mutableStateOf("main")
    var cardsList by mutableStateOf<List<ir.tcart.seller.data.CardRow>>(emptyList())
    var rotateOn by mutableStateOf(false)
    var payShowSheba by mutableStateOf(false)
    var payMinutes by mutableStateOf(30)
    var walletTx by mutableStateOf<List<ir.tcart.seller.data.WalletTx>>(emptyList())
    var search by mutableStateOf("")

    init {
        viewModelScope.launch {
            PaymentBus.celebrations.collect { event ->
                history = prefs.history()
                celebrate = event
                delay(4200)
                if (celebrate == event) celebrate = null
            }
        }
        viewModelScope.launch {
            delay(900)
            if (prefs.token.isBlank()) {
                stage = if (prefs.seenIntro) Stage.Login else Stage.Welcome
            } else {
                restore()
            }
        }
    }

    fun startIntro() {
        prefs.seenIntro = true
        permStep = 0
        error = null
        stage = Stage.Permissions
    }

    fun nextPerm() {
        if (permStep >= 3) {
            stage = Stage.Login
        } else {
            permStep += 1
        }
    }

    fun login(phone: String, password: String) {
        if (phone.isBlank() || password.isBlank()) {
            error = "شماره و رمز را بنویس"
            return
        }
        busy = true
        error = null
        viewModelScope.launch {
            try {
                val acc = withContext(Dispatchers.IO) { TcartApi.login(phone, password) }
                store(acc)
                stage = Stage.Panel
                loadDash()
            } catch (_: IOException) {
                error = "به سرور تیکارت وصل نشد"
            } catch (e: Exception) {
                error = e.message ?: "ورود انجام نشد"
            } finally {
                busy = false
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            try {
                val acc = withContext(Dispatchers.IO) { TcartApi.account(prefs.token) }
                store(acc)
                stage = Stage.Panel
                loadDash()
            } catch (_: IOException) {
                name = prefs.sellerName
                stage = Stage.Panel
                error = "آفلاین هستی. اعداد با وصل شدن اینترنت تازه می‌شوند"
            } catch (e: Exception) {
                if ((e.message ?: "").contains("نشست")) logout() else {
                    stage = Stage.Panel
                    error = e.message
                }
            }
        }
    }

    fun loadDash() {
        if (prefs.token.isBlank()) return
        viewModelScope.launch {
            busy = true
            try {
                val dash = withContext(Dispatchers.IO) { TcartApi.dashboard(prefs.token) }
                applyDash(dash)
                error = null
            } catch (_: IOException) {
                error = "به سرور وصل نشد"
            } catch (e: Exception) {
                if ((e.message ?: "").contains("نشست")) logout() else error = e.message
            } finally {
                busy = false
            }
        }
    }

    fun reloadSims() {
        sims = readSims()
        if (sims.none { it.id == picked } && sims.isNotEmpty()) picked = sims.first().id
    }

    fun pick(id: Int) {
        picked = id
    }

    fun toggleTheme() {
        light = !light
        prefs.lightTheme = light
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
    }

    fun addManualSender() {
        val key = SmsInbox.normalizeSender(manualSender)
        if (key.isBlank()) return
        toggleBank(key)
        manualSender = ""
    }

    fun loadPlans() {
        viewModelScope.launch {
            try {
                offers = withContext(Dispatchers.IO) { TcartApi.plans() }
            } catch (e: Exception) {
                error = e.message ?: "پلن‌ها خوانده نشد"
            }
        }
    }

    fun buy(tier: String, months: Int) {
        if (prefs.token.isBlank()) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                invoice = withContext(Dispatchers.IO) { TcartApi.checkout(prefs.token, tier, months) }
            } catch (e: Exception) {
                error = e.message ?: "خرید انجام نشد"
            } finally {
                busy = false
            }
        }
    }

    fun openScreen(name: String) {
        screen = name
        error = null
        when (name) {
            "cards" -> loadCards()
            "wallet" -> loadWallet()
        }
    }

    fun loadCards() = work {
        val pack = TcartApi.cards(prefs.token)
        withMain {
            cardsList = pack.first
            rotateOn = pack.second
            payShowSheba = pack.third.showSheba
            payMinutes = pack.third.expiryMinutes
        }
    }

    fun addCard(number: String, holder: String, bank: String, sheba: String, nickname: String) = work {
        TcartApi.addCard(prefs.token, number, holder, bank, sheba, nickname)
        reloadAfterWrite()
    }

    fun togglePause(id: Long, paused: Boolean) = work {
        TcartApi.patchCard(prefs.token, id, JSONObject().put("paused", paused))
        reloadAfterWrite()
    }

    fun removeCard(id: Long) = work {
        TcartApi.deleteCard(prefs.token, id)
        reloadAfterWrite()
    }

    fun makePrimary(id: Long) = work {
        TcartApi.primaryCard(prefs.token, id)
        reloadAfterWrite()
    }

    fun setRotate(enabled: Boolean) = work {
        TcartApi.rotateCards(prefs.token, enabled)
        reloadAfterWrite()
    }

    fun savePay(showSheba: Boolean, minutes: Int) = work {
        TcartApi.savePaySettings(prefs.token, showSheba, minutes)
        reloadAfterWrite()
    }

    fun saveProduct(id: Long?, name: String, price: Long, description: String, delivery: String, content: String, enabled: Boolean) = work {
        TcartApi.saveProduct(prefs.token, id, name, price, description, delivery, content, enabled)
        reloadAfterWrite()
    }

    fun removeProduct(id: Long) = work {
        TcartApi.deleteProduct(prefs.token, id)
        reloadAfterWrite()
    }

    fun confirm(id: Long) = work {
        TcartApi.confirmOrder(prefs.token, id)
        reloadAfterWrite()
    }

    fun reject(id: Long) = work {
        TcartApi.rejectOrder(prefs.token, id)
        reloadAfterWrite()
    }

    fun findOrder() = work {
        val row = TcartApi.lookupOrder(prefs.token, search.trim())
        withMain {
            orders = listOf(row) + orders.filter { it.id != row.id }
        }
    }

    fun loadWallet() = work {
        val rows = TcartApi.walletActivity(prefs.token)
        withMain { walletTx = rows }
    }

    fun regenKey() = work {
        TcartApi.regenerateKey(prefs.token)
        val acc = TcartApi.account(prefs.token)
        withMain { store(acc) }
    }

    private fun reloadAfterWrite() {
        val dash = TcartApi.dashboard(prefs.token)
        val pack = runCatching { TcartApi.cards(prefs.token) }.getOrNull()
        val walletRows = runCatching { TcartApi.walletActivity(prefs.token) }.getOrDefault(emptyList())
        kotlinx.coroutines.runBlocking(Dispatchers.Main) {
            applyDash(dash)
            walletTx = walletRows
            if (pack != null) {
                cardsList = pack.first
                rotateOn = pack.second
                payShowSheba = pack.third.showSheba
                payMinutes = pack.third.expiryMinutes
            }
        }
    }

    private fun work(block: suspend () -> Unit) {
        if (prefs.token.isBlank()) return
        viewModelScope.launch {
            busy = true
            error = null
            try {
                withContext(Dispatchers.IO) { block() }
            } catch (e: Exception) {
                error = e.message ?: "خطا"
            } finally {
                busy = false
            }
        }
    }

    private suspend fun withMain(block: () -> Unit) {
        withContext(Dispatchers.Main) { block() }
    }

    fun startListening() {
        if (prefs.bankSenders().isEmpty()) {
            error = "اول سرشمارهٔ بانک را انتخاب کن"
            refreshSenders()
            return
        }
        if (prefs.secret.isBlank()) {
            error = "اول در سایت کارت بانکی را ثبت کن تا کلید پیامک ساخته شود"
            return
        }
        prefs.subscriptionId = Prefs.UNSET
        prefs.lineLabel = "${prefs.bankSenders().size} سرشمارهٔ بانک"
        prefs.listening = true
        line = prefs.lineLabel
        listening = true
        ListenService.start(getApplication())
        error = null
    }

    fun pause() {
        prefs.listening = false
        listening = false
        ListenService.stop(getApplication())
        reloadSims()
    }

    fun logout() {
        ListenService.stop(getApplication())
        prefs.clearSession()
        account = null
        orders = emptyList()
        products = emptyList()
        stats = null
        name = ""
        line = ""
        listening = false
        history = emptyList()
        celebrate = null
        error = null
        tab = 0
        stage = Stage.Login
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

    fun permissions(): Array<String> = buildList {
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.READ_PHONE_NUMBERS)
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

    private fun store(acc: Account) {
        prefs.token = acc.token
        prefs.sellerName = acc.name
        prefs.webhookUrl = acc.webhookUrl
        prefs.secret = acc.secret
        name = acc.name
        account = acc
        if (prefs.listening && acc.secret.isNotBlank()) ListenService.start(getApplication())
    }

    private fun applyDash(dash: Dash) {
        store(dash.account)
        orders = dash.orders
        products = dash.products
        stats = dash.stats
        wallet = dash.wallet
        series = buildSeries(dash.orders)
        history = prefs.history()
        line = prefs.lineLabel
        listening = prefs.listening
    }

    private fun buildSeries(rows: List<OrderRow>): List<DayPoint> {
        val days = 14
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val buckets = LongArray(days)
        val labels = Array(days) { i ->
            val cal = Calendar.getInstance().apply { timeInMillis = today - (days - 1 - i) * 86_400_000L }
            SmsText.faDigits(cal.get(Calendar.DAY_OF_MONTH).toLong())
        }
        rows.filter { it.status == "paid" && it.at > 0 }.forEach { row ->
            val index = ((row.at - today) / 86_400_000L).toInt() + (days - 1)
            if (index in 0 until days) buckets[index] += row.amount
        }
        return List(days) { DayPoint(labels[it], buckets[it]) }
    }

    private fun readSims(): List<SimLine> {
        val ctx = getApplication<Application>()
        if (!granted(ctx, Manifest.permission.READ_PHONE_STATE)) return fallbackSim()
        val manager = ctx.getSystemService(SubscriptionManager::class.java) ?: return fallbackSim()
        val infos = try {
            manager.activeSubscriptionInfoList
        } catch (_: SecurityException) {
            null
        } ?: return fallbackSim()
        if (infos.isEmpty()) return fallbackSim()
        return infos.map { info ->
            val slot = (info.simSlotIndex + 1).coerceAtLeast(1)
            val carrier = info.carrierName?.toString().orEmpty().ifBlank { "اپراتور" }
            val number = info.number.orEmpty().ifBlank { "شماره خوانده نشد" }
            SimLine(info.subscriptionId, "خط $slot", "$carrier · $number")
        }
    }

    private fun fallbackSim() = listOf(SimLine(Prefs.UNSET, "همین گوشی", "پیامک واریز این گوشی"))

    fun getSavedToken(): String = prefs.token

    private fun granted(ctx: Context, permission: String) =
        ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED
}
