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
import ir.tcart.seller.data.Prefs
import ir.tcart.seller.data.TcartApi
import ir.tcart.seller.notify.PaidNotifier
import java.util.concurrent.Executors

class ListenService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val poll = object : Runnable {
        override fun run() {
            worker.execute { checkPending() }
            handler.postDelayed(this, 90_000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = Prefs(this)
        PaidNotifier.ensureChannels(this)
        val notification = PaidNotifier.presenceNotification(this, prefs.lineLabel.ifBlank { "منتظر پیامک بانک" })
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                PaidNotifier.presenceId(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(PaidNotifier.presenceId(), notification)
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

    private fun checkPending() {
        val prefs = Prefs(this)
        if (!prefs.listening || prefs.token.isBlank()) return
        try {
            val orders = TcartApi.orders(prefs.token).orders.filter { it.status == "pending" }
            val ids = orders.map { it.id.toString() }.toSet()
            if (!prefs.pendingPrimed) {
                prefs.setKnownPending(ids)
                prefs.pendingPrimed = true
                return
            }
            val known = prefs.knownPending()
            orders.filter { it.id.toString() !in known }.forEach { order ->
                PaidNotifier.showPending(this, order.uniqueToman, order.title.ifBlank { "سفارش جدید" })
            }
            prefs.setKnownPending(ids)
        } catch (_: Exception) {
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ListenService::class.java)
            context.startForegroundService(intent)
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
