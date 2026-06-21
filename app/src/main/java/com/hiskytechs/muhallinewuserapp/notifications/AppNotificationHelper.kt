package com.hiskytechs.muhallinewuserapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hiskytechs.muhallinewuserapp.MainActivity
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierMainActivity
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierMessagesActivity

object AppNotificationHelper {
    private const val CHANNEL_ID = "muhalli_buyer_updates"

    fun showBuyerNotification(
        context: Context,
        title: String,
        message: String,
        navigateTo: String = "home",
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        ensureChannel(context)
        val intent = notificationIntent(context, navigateTo)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_muhalli_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private fun notificationIntent(context: Context, navigateTo: String): Intent {
        return when (navigateTo) {
            "supplier_orders" -> Intent(context, SupplierMainActivity::class.java)
                .putExtra(SupplierMainActivity.EXTRA_TAB_ID, R.id.nav_supplier_orders)
            "supplier_products" -> Intent(context, SupplierMainActivity::class.java)
                .putExtra(SupplierMainActivity.EXTRA_TAB_ID, R.id.nav_supplier_products)
            "supplier_messages" -> Intent(context, SupplierMessagesActivity::class.java)
            "supplier_home" -> Intent(context, SupplierMainActivity::class.java)
                .putExtra(SupplierMainActivity.EXTRA_TAB_ID, R.id.nav_supplier_home)
            else -> Intent(context, MainActivity::class.java)
                .putExtra("navigate_to", navigateTo)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notifications),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notifications_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun clearAll(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancelAll()
        }
    }
}
