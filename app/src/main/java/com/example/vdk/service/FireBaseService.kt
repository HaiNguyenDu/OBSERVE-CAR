package com.example.vdk.service


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.example.vdk.R
import com.example.vdk.ui.home.MainActivity
import com.example.vdk.utils.FIREBASE_URL
import com.example.vdk.utils.SOUND.CHANNEL_ID
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class FireBaseService : Service() {
    private val database = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("Notification")
    private var valueEventListener: ChildEventListener? = null
    private val player: MediaPlayer = MediaPlayer()
    var count = 0
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        listenToFirebaseChanges()
        player.setDataSource(this, "android.resource://${packageName}/${R.raw.sound}".toUri())
        player.prepareAsync()
        player.setOnCompletionListener { player.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "off") {
            val keyToDelete = "on"
            database.child(keyToDelete)
                .removeValue()
            player.pause()
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning_yellow)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText("dang lang nghe")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        if (count == 0) {
            startForeground(12, notification)
            count += 1
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        valueEventListener?.let { database.removeEventListener(it) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Channel với heads-up",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel demo"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun sendNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val bitmap = BitmapFactory.decodeResource(
            resources,
            R.drawable.bg
        )
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        player.start()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning_yellow)
            .setLargeIcon(bitmap)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText("Nguy Hiểm Có Trẻ Nhỏ Trong Xe ❗ ❗ ❗ ❗ ❗")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(1001, notification)
    }

    private fun off() {
        val keyToDelete = "on"
        database.child(keyToDelete)
            .removeValue()
    }

    private fun listenToFirebaseChanges() {
        var count = 1
        valueEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (count != 1)
                    sendNotification()
                count = count + 1
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                player.pause()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                player.pause()
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                player.pause()
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        database.addChildEventListener(valueEventListener!!)
    }
}