package ir.tcart.seller.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import ir.tcart.seller.MainActivity
import ir.tcart.seller.R
import ir.tcart.seller.sms.SmsText

object PaidNotifier {
    const val CHANNEL_PAID = "tcart_paid_v5"
    const val CHANNEL_WAIT = "tcart_wait_v3"
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
            vibrationPattern = longArrayOf(0, 30, 40, 30, 50, 90, 35, 160)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        val wait = NotificationChannel(
            CHANNEL_WAIT,
            "در انتظار تأیید",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "واریز یا سفارشی که هنوز تأیید نشده"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 25, 45, 25)
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
        manager.createNotificationChannel(wait)
        manager.createNotificationChannel(presence)
        manager.createNotificationChannel(alert)
    }

    fun showPaid(context: Context, amount: Long?, status: String, detail: String) {
        val money = if (amount != null && amount > 0) "+${SmsText.faAmount(amount)} تومان" else ""
        val title = if (money.isNotBlank()) "💰 $money به حسابت نشست!" else "💰 ${status.ifBlank { "پرداخت تأیید شد" }}"
        val cleanDetail = if (detail.isBlank() || detail == "نمونه") "تأیید خودکار پیامک بانک" else detail
        val line = "پرداخت با موفقیت تأیید شد · $cleanDetail"
        send(context, CHANNEL_PAID, title, line, 0xFF10B981.toInt())
    }

    fun showPending(context: Context, amount: Long?, detail: String) {
        val money = if (amount != null && amount > 0) "${SmsText.faAmount(amount)} تومان" else ""
        val title = if (money.isNotBlank()) "⏳ سفارش $money در انتظار تأیید" else "⏳ سفارش جدید در انتظار تأیید"
        val line = detail.ifBlank { "منتظر دریافت پیامک واریز بانک" }
        send(context, CHANNEL_WAIT, title, line, 0xFF0EA5E9.toInt())
    }

    private fun send(context: Context, channel: String, title: String, text: String, color: Int) {
        ensureChannels(context)
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.tcart_logo)
        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat)
            .setLargeIcon(logo)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText("تیکارت")
            .setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(text))
            .setContentIntent(openApp(context))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setColor(color)
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
            .setContentTitle("موتور پیامک تیکارت فعال است")
            .setContentText(if (line.isBlank()) "آمادهٔ دریافت پیامک واریز بانک" else line)
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
