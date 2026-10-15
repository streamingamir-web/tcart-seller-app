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

data class Celebration(
    val amount: Long?,
    val status: String,
    val detail: String,
)

object PaymentBus {
    val celebrations = MutableSharedFlow<Celebration>(extraBufferCapacity = 4)
}

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val prefs = Prefs(context)
        if (!prefs.listening || prefs.secret.isBlank() || prefs.webhookUrl.isBlank()) return

        val subId = readSubscription(intent)
        val selected = prefs.subscriptionId
        if (selected != Prefs.UNSET && subId != Prefs.UNSET && subId != selected) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val body = messages.joinToString("") { it?.messageBody.orEmpty() }.trim()
        val sender = messages.firstOrNull()?.originatingAddress.orEmpty()
        if (body.isEmpty() || SmsText.isSecret(body)) return
        if (!SmsInbox.matches(sender, prefs.bankSenders())) return

        val pending = goAsync()
        worker.execute {
            try {
                deliver(context.applicationContext, prefs, body)
            } finally {
                pending.finish()
            }
        }
    }

    private fun deliver(context: Context, prefs: Prefs, body: String) {
        val amount = SmsText.amountToman(body)
        try {
            val result = TcartApi.postSms(prefs.webhookUrl, prefs.secret, body)
            when {
                result.orderId != null -> celebrate(
                    context,
                    prefs,
                    amount,
                    "پرداخت تأیید شد",
                    "سفارش ${SmsText.faDigits(result.orderId)}",
                    "paid",
                )
                result.subscriptionPaymentId != null -> celebrate(
                    context,
                    prefs,
                    amount,
                    "اشتراک فعال شد",
                    "پرداخت اشتراک ثبت شد",
                    "paid",
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
            prefs.pushHistory(
                PayEvent(System.currentTimeMillis(), "به سرور نرسید", "اینترنت گوشی را چک کن", "error", amount ?: 0),
            )
            PaidNotifier.showQuietProblem(
                context,
                "پیام بانک ماند روی گوشی",
                "تیکارت نتوانست این پیامک را بفرستد. اینترنت را وصل کن.",
            )
        }
    }

    private fun celebrate(
        context: Context,
        prefs: Prefs,
        amount: Long?,
        status: String,
        detail: String,
        kind: String,
    ) {
        prefs.pushHistory(
            PayEvent(
                at = System.currentTimeMillis(),
                title = status,
                detail = detail,
                kind = kind,
                amount = amount ?: 0,
            ),
        )
        PaidNotifier.showPaid(context, amount, status, detail)
        PaymentBus.celebrations.tryEmit(Celebration(amount, status, detail))
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
    }
}
