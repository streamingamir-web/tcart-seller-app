package ir.tcart.seller.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import ir.tcart.seller.MainActivity
import ir.tcart.seller.R
import ir.tcart.seller.sms.SmsText

object PaidNotifier {
    const val CHANNEL_PAID = "tcart_paid"
    const val CHANNEL_PRESENCE = "tcart_presence"
    const val CHANNEL_ALERT = "tcart_alert"
    private const val PRESENCE_ID = 41

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val paid = NotificationChannel(
            CHANNEL_PAID,
            context.getString(R.string.channel_paid),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_paid_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 28, 50, 28, 40, 18)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        val presence = NotificationChannel(
            CHANNEL_PRESENCE,
            context.getString(R.string.channel_presence),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = context.getString(R.string.channel_presence_desc)
            setSound(null, null)
            enableVibration(false)
        }
        val alert = NotificationChannel(
            CHANNEL_ALERT,
            context.getString(R.string.channel_alert),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(paid)
        manager.createNotificationChannel(presence)
        manager.createNotificationChannel(alert)
    }

    fun showPaid(context: Context, amount: Long?, status: String, detail: String) {
        ensureChannels(context)
        val amountLine = if (amount != null && amount > 0) {
            "${SmsText.faAmount(amount)} تومان"
        } else {
            "پرداخت جدید"
        }
        val heads = RemoteViews(context.packageName, R.layout.notification_paid_heads).apply {
            setTextViewText(R.id.amount, amountLine)
            setTextViewText(R.id.status, status)
        }
        val big = RemoteViews(context.packageName, R.layout.notification_paid).apply {
            setTextViewText(R.id.amount, amountLine)
            setTextViewText(R.id.status, status)
            setTextViewText(R.id.detail, detail)
        }
        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val notification = NotificationCompat.Builder(context, CHANNEL_PAID)
            .setSmallIcon(R.drawable.ic_stat)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(heads)
            .setCustomHeadsUpContentView(heads)
            .setCustomBigContentView(big)
            .setContentIntent(openApp(context))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setColor(0xFF1D4ED8.toInt())
            .build()
        manager(context).notify(id, notification)
    }

    fun showQuietProblem(context: Context, title: String, text: String) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .build()
        manager(context).notify(90, notification)
    }

    fun presenceNotification(context: Context, line: String) =
        NotificationCompat.Builder(context, CHANNEL_PRESENCE)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("تیکارت وصل است")
            .setContentText(if (line.isBlank()) "منتظر پیامک واریز" else line)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openApp(context))
            .build()

    fun cancelPresence(context: Context) {
        manager(context).cancel(PRESENCE_ID)
    }

    fun presenceId(): Int = PRESENCE_ID

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            7,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun manager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
