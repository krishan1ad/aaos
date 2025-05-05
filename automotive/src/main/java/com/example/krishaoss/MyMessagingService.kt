package com.example.krishaoss

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.Build
import android.annotation.SuppressLint
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CarExtender
import androidx.core.app.NotificationCompat.CarExtender.UnreadConversation
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput

const val CHANNEL_ID = "com.example.krishaoss.CHANNEL_ID"
const val READ_ACTION = "com.example.krishaoss.ACTION_MESSAGE_READ"
const val REPLY_ACTION = "com.example.krishaoss.ACTION_MESSAGE_REPLY"
const val CONVERSATION_ID = "conversation_id"
const val EXTRA_VOICE_REPLY = "extra_voice_reply"

class MyMessagingService : Service() {

    private val mMessenger = Messenger(IncomingHandler())
    private lateinit var mNotificationManager: NotificationManagerCompat

    override fun onCreate() {
        mNotificationManager = NotificationManagerCompat.from(applicationContext)
    }

    override fun onBind(intent: Intent): IBinder? {
        return mMessenger.binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }

    private fun createIntent(conversationId: Int, action: String): Intent {
        return Intent().apply {
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            setAction(action)
            putExtra(CONVERSATION_ID, conversationId)
        }
    }

    private fun sendNotification(
        conversationId: Int,
        message: String,
        participant: String,
        timestamp: Long
    ) {
        val readPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            conversationId,
            createIntent(conversationId, READ_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteInput = RemoteInput.Builder(EXTRA_VOICE_REPLY)
            .setLabel("Reply by voice")
            .build()

        val replyIntent = PendingIntent.getBroadcast(
            applicationContext,
            conversationId,
            createIntent(conversationId, REPLY_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val unreadConversationBuilder = UnreadConversation.Builder(participant)
            .setLatestTimestamp(timestamp)
            .setReadPendingIntent(readPendingIntent)
            .setReplyAction(replyIntent, remoteInput)

        val channel = NotificationChannelCompat
            .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(resources.getText(R.string.app_name))
            .build()
        mNotificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            //.setSmallIcon(R.drawable.notification_icon)
            //.setLargeIcon(personBitmap)
            .setContentText(message)
            .setWhen(timestamp)
            .setContentTitle(participant)
            .setContentIntent(readPendingIntent)
            .extend(
                CarExtender()
                    .setUnreadConversation(unreadConversationBuilder.build())
            )

        showNotification(conversationId, builder)
    }

    @SuppressLint("MissingPermission") // Safe because we check permission status first
    private fun showNotification(conversationId: Int, builder: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            mNotificationManager.areNotificationsEnabled()
        ) {
            mNotificationManager.notify(conversationId, builder.build())
        } else {
            Log.w("MyMessagingService", "Notification skipped: POST_NOTIFICATIONS permission not granted.")
        }
    }

    internal inner class IncomingHandler : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            sendNotification(1, "This is a sample message", "John Doe", System.currentTimeMillis())
        }
    }
}
