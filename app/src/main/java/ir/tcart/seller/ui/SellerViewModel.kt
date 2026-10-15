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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.tcart.seller.data.Prefs
import ir.tcart.seller.data.SellerSession
import ir.tcart.seller.data.TcartApi
import ir.tcart.seller.notify.PaidNotifier
import ir.tcart.seller.sms.Celebration
import ir.tcart.seller.sms.ListenService
import ir.tcart.seller.sms.PaymentBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

data class SimLine(val id: Int, val title: String, val detail: String)

enum class Stage { Boot, Login, NeedCard, Setup, Home }

class SellerViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    var stage by mutableStateOf(Stage.Boot)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var name by mutableStateOf(prefs.sellerName)
    var line by mutableStateOf(prefs.lineLabel)
    var listening by mutableStateOf(prefs.listening)
    var sims by mutableStateOf<List<SimLine>>(emptyList())
    var picked by mutableStateOf(prefs.subscriptionId)
    var history by mutableStateOf(prefs.history())
    var celebrate by mutableStateOf<Celebration?>(null)

    init {
        viewModelScope.launch {
            PaymentBus.celebrations.collect { event ->
                history = prefs.history()
                celebrate = event
                delay(4800)
                if (celebrate == event) celebrate = null
            }
        }
        if (prefs.token.isBlank()) stage = Stage.Login else restore()
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
                val session = withContext(Dispatchers.IO) { TcartApi.login(phone, password) }
                save(session)
            } catch (_: IOException) {
                error = "به سرور تیکارت وصل نشد. اینترنت گوشی را چک کن"
            } catch (e: Exception) {
                error = e.message ?: "ورود انجام نشد"
            } finally {
                busy = false
            }
        }
    }

    fun restore() {
        busy = true
        error = null
        viewModelScope.launch {
            try {
                val session = withContext(Dispatchers.IO) { TcartApi.loadSession(prefs.token, prefs.sellerName) }
                save(session)
            } catch (_: IOException) {
                openCached("الان آفلاینی. اگر قبلاً وصل شده باشد، گوش دادن ادامه دارد")
            } catch (e: Exception) {
                if ((e.message ?: "").contains("نشست")) logout()
                else openCached(e.message)
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

    fun startListening() {
        val sim = sims.firstOrNull { it.id == picked } ?: sims.firstOrNull()
        if (sim == null) {
            error = "اول خط را انتخاب کن"
            return
        }
        prefs.subscriptionId = sim.id
        prefs.lineLabel = listOf(sim.title, sim.detail).filter { it.isNotBlank() }.joinToString(" · ")
        prefs.listening = true
        line = prefs.lineLabel
        listening = true
        ListenService.start(getApplication())
        history = prefs.history()
        stage = Stage.Home
        error = null
    }

    fun pause() {
        prefs.listening = false
        listening = false
        ListenService.stop(getApplication())
        reloadSims()
        stage = Stage.Setup
    }

    fun logout() {
        ListenService.stop(getApplication())
        prefs.clearSession()
        name = ""
        line = ""
        listening = false
        history = emptyList()
        celebrate = null
        error = null
        stage = Stage.Login
    }

    fun previewNotification() {
        val event = Celebration(250_000, "پرداخت تأیید شد", "این یک نمونه است")
        celebrate = event
        PaidNotifier.showPaid(getApplication(), event.amount, event.status, event.detail)
        viewModelScope.launch {
            delay(4800)
            if (celebrate == event) celebrate = null
        }
    }

    fun permissions(): Array<String> {
        val items = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
        )
        if (Build.VERSION.SDK_INT >= 33) items += Manifest.permission.POST_NOTIFICATIONS
        return items.toTypedArray()
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

    fun refreshHistory() {
        history = prefs.history()
    }

    private fun save(session: SellerSession) {
        prefs.token = session.token
        prefs.sellerName = session.name
        prefs.webhookUrl = session.webhookUrl
        prefs.secret = session.secret
        name = session.name
        if (session.secret.isBlank() || session.webhookUrl.isBlank()) {
            prefs.listening = false
            listening = false
            ListenService.stop(getApplication())
            stage = Stage.NeedCard
            return
        }
        if (prefs.listening) {
            line = prefs.lineLabel
            listening = true
            ListenService.start(getApplication())
            history = prefs.history()
            stage = Stage.Home
        } else {
            reloadSims()
            stage = Stage.Setup
        }
    }

    private fun openCached(message: String?) {
        if (prefs.secret.isBlank() || prefs.token.isBlank()) {
            stage = Stage.Login
            error = message
            return
        }
        name = prefs.sellerName
        error = message
        if (prefs.listening) {
            line = prefs.lineLabel
            ListenService.start(getApplication())
            history = prefs.history()
            stage = Stage.Home
        } else {
            reloadSims()
            stage = Stage.Setup
        }
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
            val number = info.number.orEmpty().ifBlank { "شماره روی سیم‌کارت خوانده نشد" }
            SimLine(info.subscriptionId, "خط $slot", "$carrier · $number")
        }
    }

    private fun fallbackSim() = listOf(
        SimLine(Prefs.UNSET, "همین گوشی", "پیامک واریز هر خطی که به این گوشی بیاید فرستاده می‌شود"),
    )

    private fun granted(ctx: Context, permission: String) =
        ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED
}
