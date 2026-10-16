package ir.tcart.seller.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
 * گیرندهٔ پیامک: فقط پیامک را در صف می‌گذارد و فوراً آزاد می‌شود.
 * ارسال به سرور در پس‌زمینه انجام می‌شود و اگر اینترنت/سرور کند بود،
 * سرویس شنونده بعداً دوباره می‌فرستد؛ پس هیچ‌وقت «not responding» نمی‌دهد.
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

        // اول در صف امن ذخیره می‌شود تا با هیچ مشکلی گم نشود
        prefs.outboxAdd(System.currentTimeMillis(), body)

        val pending = goAsync()
        val finished = AtomicBoolean(false)
        fun finish() {
            if (finished.compareAndSet(false, true)) {
                try {
                    pending.finish()
                } catch (_: Exception) {
                }
            }
        }
        // حتی اگر شبکه گیر کرد، حداکثر ۷ ثانیه بعد گیرنده آزاد می‌شود
        watchdog.schedule(Runnable { finish() }, 7, TimeUnit.SECONDS)
        worker.execute {
            try {
                SmsOutbox.flush(app)
            } catch (_: Throwable) {
            } finally {
                finish()
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
            Thread(r).apply { isDaemon = true }
        }
    }
}

/** صف ارسال پیامک‌ها به وب‌هوک تیکارت؛ همیشه خارج از ترد اصلی اجرا شود. */
object SmsOutbox {
    private val lock = Any()
    private const val MAX_AGE_MS = 3L * 60 * 60 * 1000
    private const val FIRST_TRY_MS = 30_000L

    fun flush(context: Context) {
        synchronized(lock) {
            val prefs = Prefs(context)
            if (prefs.secret.isBlank() || prefs.webhookUrl.isBlank()) return
            for ((at, body) in prefs.outbox()) {
                if (System.currentTimeMillis() - at > MAX_AGE_MS) {
                    prefs.outboxRemove(at)
                    continue
                }
                val reached = deliver(context, prefs, at, body)
                if (!reached) break // اینترنت یا سرور در دسترس نیست؛ بقیه هم در صف می‌مانند
            }
        }
    }

    /** true یعنی سرور جواب داد (پیامک از صف خارج می‌شود). */
    private fun deliver(context: Context, prefs: Prefs, at: Long, body: String): Boolean {
        val amount = SmsText.amountToman(body)
        val firstTry = System.currentTimeMillis() - at < FIRST_TRY_MS
        val result = try {
            TcartApi.postSms(prefs.webhookUrl, prefs.secret, body)
        } catch (_: Exception) {
            if (firstTry) {
                prefs.pushHistory(
                    PayEvent(System.currentTimeMillis(), "در صف ارسال", "اینترنت وصل شود خودکار می‌فرستم", "error", amount ?: 0),
                )
                PaidNotifier.showQuietProblem(
                    context,
                    "پیام بانک در صف ارسال است",
                    "اتصال به تیکارت برقرار نشد. به‌محض وصل شدن اینترنت خودکار فرستاده می‌شود.",
                )
            }
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
            // خطا در نمایش اعلان/تاریخچه نباید صف را خراب کند
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
