package ir.tcart.seller.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import ir.tcart.seller.data.PayEvent
import ir.tcart.seller.data.Prefs
import ir.tcart.seller.data.TcartApi
import ir.tcart.seller.notify.PaidNotifier
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class Celebration(
    val amount: Long?,
    val status: String,
    val detail: String,
)

object PaymentBus {
    val celebrations = MutableSharedFlow<Celebration>(extraBufferCapacity = 4)
}

/**
 * مثل قبل: به‌محض آمدن پیامک، در پس‌زمینه فوراً به سرور می‌فرستد.
 * فقط برای جلوگیری از ANR روی ترد اصلی نمی‌ماند؛ کار شبکه بعد از آزاد شدن گیرنده ادامه پیدا می‌کند.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val app = context.applicationContext
        val prefs = Prefs(app)
        if (!prefs.listening || prefs.secret.isBlank() || prefs.webhookUrl.isBlank()) return

        val subId = readSubscription(intent)
        val selected = prefs.subscriptionId
        if (selected != Prefs.UNSET && subId != Prefs.UNSET && subId != selected) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val body = messages.joinToString("") { it?.messageBody.orEmpty() }.trim()
        val sender = messages.firstOrNull()?.originatingAddress.orEmpty()
        if (body.isEmpty() || SmsText.isSecret(body)) return
        if (!SmsInbox.matches(sender, prefs.bankSenders())) return

        // برای گم‌نشدن؛ ارسال همان لحظه شروع می‌شود
        prefs.outboxAdd(System.currentTimeMillis(), body)

        val pending = goAsync()
        val finished = AtomicBoolean(false)
        fun releaseReceiver() {
            if (finished.compareAndSet(false, true)) {
                try {
                    pending.finish()
                } catch (_: Exception) {
                }
            }
        }
        // بودجهٔ گیرنده را آزاد می‌کنیم تا ANR ندهد؛ ارسال شبکه قطع نمی‌شود
        watchdog.schedule(Runnable { releaseReceiver() }, 8, TimeUnit.SECONDS)

        worker.execute {
            try {
                SmsOutbox.flushNow(app)
            } catch (_: Throwable) {
            } finally {
                releaseReceiver()
            }
        }
    }

    private fun readSubscription(intent: Intent): Int {
        val keys = arrayOf(
            "subscription",
            "android.telephony.extra.SUBSCRIPTION_INDEX",
        )
        for (key in keys) {
            val value = intent.getIntExtra(key, Prefs.UNSET)
            if (value >= 0) return value
        }
        return Prefs.UNSET
    }

    companion object {
        private val worker = Executors.newSingleThreadExecutor()
        private val watchdog = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "tcart-sms-watch").apply { isDaemon = true }
        }
    }
}

/** صف پشتیبان + ارسال فوری. همیشه خارج از ترد اصلی. */
object SmsOutbox {
    private val lock = Any()
    private val retryHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private const val MAX_AGE_MS = 3L * 60 * 60 * 1000
    private val retryDelaysMs = longArrayOf(2_000L, 6_000L, 15_000L)
    private var retryRound = 0

    /** ارسال فوری صف (جدیدترین اول). اگر نشد، چند ثانیه بعد دوباره تلاش می‌کند. */
    fun flushNow(context: Context) {
        val app = context.applicationContext
        val ok = flush(app)
        if (!ok) scheduleRetry(app) else retryRound = 0
    }

    fun flush(context: Context): Boolean {
        synchronized(lock) {
            val prefs = Prefs(context)
            if (prefs.secret.isBlank() || prefs.webhookUrl.isBlank()) return true
            // جدیدترین اول تا تأیید همین پیامک تازه‌رسیده سریع باشد
            val items = prefs.outbox().sortedByDescending { it.first }
            if (items.isEmpty()) return true
            var allOk = true
            for ((at, body) in items) {
                if (System.currentTimeMillis() - at > MAX_AGE_MS) {
                    prefs.outboxRemove(at)
                    continue
                }
                if (!deliver(context, prefs, at, body)) {
                    allOk = false
                    break
                }
            }
            return allOk
        }
    }

    private fun scheduleRetry(context: Context) {
        val delay = retryDelaysMs.getOrElse(retryRound) { retryDelaysMs.last() }
        if (retryRound < retryDelaysMs.size) retryRound++
        retryHandler.removeCallbacksAndMessages(null)
        retryHandler.postDelayed({
            worker.execute {
                try {
                    flushNow(context)
                } catch (_: Throwable) {
                }
            }
        }, delay)
    }

    /** true = سرور جواب داد و از صف خارج شد. */
    private fun deliver(context: Context, prefs: Prefs, at: Long, body: String): Boolean {
        val amount = SmsText.amountToman(body)
        val result = try {
            TcartApi.postSms(prefs.webhookUrl, prefs.secret, body)
        } catch (_: Exception) {
            return false
        }
        prefs.outboxRemove(at)
        try {
            when {
                result.orderId != null -> {
                    val known = prefs.knownPaid().toMutableSet()
                    known.add(result.orderId.toString())
                    prefs.setKnownPaid(known)
                    celebrate(
                        context,
                        prefs,
                        amount,
                        "پرداخت تأیید شد",
                        "سفارش ${SmsText.faDigits(result.orderId)}",
                    )
                }
                result.subscriptionPaymentId != null -> celebrate(
                    context,
                    prefs,
                    amount,
                    "اشتراک فعال شد",
                    "پرداخت اشتراک تیکارت ثبت شد",
                )
                result.ignored -> prefs.pushHistory(
                    PayEvent(System.currentTimeMillis(), "پیام نادیده گرفته شد", "واریز نبود", "ignored", amount ?: 0),
                )
                result.ok -> {
                    prefs.pushHistory(
                        PayEvent(System.currentTimeMillis(), "در انتظار تأیید", "پیام بانک رسید", "pending", amount ?: 0),
                    )
                    if (amount != null && amount > 0) {
                        PaidNotifier.showPending(context, amount, "هنوز با سفارشی جور نشد")
                    }
                }
                else -> {
                    val detail = result.error.ifBlank { "سفارش بازی با این مبلغ پیدا نشد" }
                    prefs.pushHistory(PayEvent(System.currentTimeMillis(), "در انتظار تأیید", detail, "pending", amount ?: 0))
                    if (amount != null && amount > 0) PaidNotifier.showPending(context, amount, detail)
                }
            }
        } catch (_: Exception) {
        }
        return true
    }

    private fun celebrate(context: Context, prefs: Prefs, amount: Long?, status: String, detail: String) {
        prefs.pushHistory(
            PayEvent(
                at = System.currentTimeMillis(),
                title = status,
                detail = detail,
                kind = "paid",
                amount = amount ?: 0,
            ),
        )
        PaidNotifier.showPaid(context, amount, status, detail)
        PaymentBus.celebrations.tryEmit(Celebration(amount, status, detail))
    }
}
