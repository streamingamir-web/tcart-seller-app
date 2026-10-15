package ir.tcart.seller.sms

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import ir.tcart.seller.data.Prefs
import ir.tcart.seller.notify.PaidNotifier

class ListenService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = Prefs(this)
        PaidNotifier.ensureChannels(this)
        val notification = PaidNotifier.presenceNotification(this, prefs.lineLabel)
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
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        PaidNotifier.cancelPresence(this)
        super.onDestroy()
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
