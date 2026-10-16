package ir.tcart.seller.sms

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import ir.tcart.seller.data.PayEvent
import ir.tcart.seller.data.Prefs
import ir.tcart.seller.data.TcartApi
import ir.tcart.seller.notify.PaidNotifier
import java.util.concurrent.Executors

class ListenService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val poll = object : Runnable {
        override fun run() {
            worker.execute {
                try {
                    checkOrders()
                } catch (_: Exception) {
                }
            }
            handler.postDelayed(this, 45_000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = Prefs(this)
        PaidNotifier.ensureChannels(this)
        val notification = PaidNotifier.presenceNotification(this, prefs.lineLabel.ifBlank { "منتظر پیامک بانک" })
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    PaidNotifier.presenceId(),
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(PaidNotifier.presenceId(), notification)
            }
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!prefs.listening) {
            handler.removeCallbacks(poll)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        handler.removeCallbacks(poll)
        handler.post(poll)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        PaidNotifier.cancelPresence(this)
        super.onDestroy()
    }

    /** هم سفارش‌های جدیدِ در انتظار، هم پرداخت‌های تازه تأییدشده را اعلان می‌کند. */
    private fun checkOrders() {
        val prefs = Prefs(this)
        if (!prefs.listening || prefs.token.isBlank()) return
        val data = try {
            TcartApi.orders(prefs.token)
        } catch (_: Exception) {
            return
        }

        val pending = data.orders.filter { it.status == "pending" }
        val paid = data.orders.filter { it.status == "paid" }
        val pendingIds = pending.map { it.id.toString() }.toSet()
        val paidIds = paid.map { it.id.toString() }.toSet()

        if (!prefs.pendingPrimed) {
            prefs.setKnownPending(pendingIds)
            prefs.pendingPrimed = true
        } else {
            val knownPending = prefs.knownPending()
            pending.filter { it.id.toString() !in knownPending }.forEach { order ->
                PaidNotifier.showPending(this, order.uniqueToman, order.title.ifBlank { "سفارش جدید" })
            }
            prefs.setKnownPending(pendingIds)
        }

        if (!prefs.paidPrimed) {
            prefs.setKnownPaid(paidIds)
            prefs.paidPrimed = true
        } else {
            val knownPaid = prefs.knownPaid()
            paid.filter { it.id.toString() !in knownPaid }.forEach { order ->
                val detail = order.title.ifBlank { "سفارش ${order.id}" }
                prefs.pushHistory(
                    PayEvent(
                        at = System.currentTimeMillis(),
                        title = "پرداخت تأیید شد",
                        detail = detail,
                        kind = "paid",
                        amount = order.uniqueToman,
                    ),
                )
                PaidNotifier.showPaid(this, order.uniqueToman, "پرداخت تأیید شد", detail)
                PaymentBus.celebrations.tryEmit(
                    Celebration(order.uniqueToman, "پرداخت تأیید شد", detail),
                )
            }
            prefs.setKnownPaid(paidIds)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ListenService::class.java)
            try {
                context.startForegroundService(intent)
            } catch (_: Exception) {
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ListenService::class.java))
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = Prefs(context)
        if (prefs.listening && prefs.secret.isNotBlank()) {
            ListenService.start(context)
        }
    }
}
