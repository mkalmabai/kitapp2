package com.technifysoft.olxkotlin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.technifysoft.olxkotlin.activities.ChatActivity
import java.util.Random

class MyFcmService : FirebaseMessagingService() {

    private companion object {
        //Tag to show logs  in logcat
        private const val TAG  = "MY_FCM_TAG"

        //Notification Channel ID
        private const val NOTIFICATION_CHANNEL_ID = "OLX_CHANNEL_ID"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        //Get data to show in notification
        val title = "${remoteMessage.notification?.title}"
        val body = "${remoteMessage.notification?.body}"

        val senderUid = "${remoteMessage.data["senderUid"]}"
        val notificationType = "${remoteMessage.data["notificationType"]}"

        Log.d(TAG, "onMessageReceived: title: $title")
        Log.d(TAG, "onMessageReceived: body: $body")
        Log.d(TAG, "onMessageReceived: senderUid: $senderUid")
        Log.d(TAG, "onMessageReceived: notificationType: $notificationType")

        //function call to show notification
        showChatNotification(title, body, senderUid)
    }

    private fun showChatNotification(notificationTitle: String, notificationDescription: String, senderUid: String){
        //Generate random integer between 3000 to use as notificationn id
        val notificationId = Random().nextInt(3000)
        //init NotificationManager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        //function call to setup notification  channel in case of Android O and above
        setupNotificationChannel(notificationManager)
        //Intent to launch ChatActivity when notification is clicked
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("receiptUid", senderUid)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        //PendingIntent to add in notification
        val pendingIntent = PendingIntent.getActivity(this,  0, intent, PendingIntent.FLAG_IMMUTABLE)

        //Setup Notification
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.kitaap)
            .setContentTitle(notificationTitle)
            .setContentText(notificationDescription)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        //Show Notification
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun setupNotificationChannel(notificationManager: NotificationManager){
        //Starting in Android 8.0 (API level 26), all notifications must be assigned to a channel https://developer.android.com/develop/ui/views/notifications/channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Chat Chanel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = "Show Chat Notifications"
            notificationChannel.enableVibration(true)

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}