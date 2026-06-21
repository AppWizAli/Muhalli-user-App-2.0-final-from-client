package com.hiskytechs.muhallinewuserapp.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.R

class AppMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AppData.registerNotificationToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.notifications)
        val body = message.notification?.body
            ?: message.data["message"]
            ?: getString(R.string.new_notification_received)
        val navigateTo = message.data["navigate_to"] ?: "home"

        AppNotificationHelper.showBuyerNotification(this, title, body, navigateTo)
    }
}
