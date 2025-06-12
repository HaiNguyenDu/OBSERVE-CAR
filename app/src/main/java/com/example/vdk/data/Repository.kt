package com.example.vdk.data

import android.content.Context
import android.util.Log
import com.example.vdk.model.Tracking
import com.example.vdk.utils.FIREBASE_URL
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Repository(private val context: Context) {
    private val database = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("Stack")
    private var childEventListener: ChildEventListener? = null

    fun getLatestTrackingData(callback: (Tracking) -> Unit) {
        database.orderByChild("timestamp").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val latestSnapshot = snapshot.children.firstOrNull()
                    if (latestSnapshot != null) {
                        val trackingData = latestSnapshot.getValue(Tracking::class.java)
                        trackingData?.id = latestSnapshot.key
                        callback(trackingData ?: Tracking())
                    } else {
                        callback(Tracking())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Repository", "getLatestTrackingData onCancelled", error.toException())
                    callback(Tracking())
                }
            })
    }

    fun getAllTrackingData(callback: (List<Tracking>) -> Unit) {
        database.orderByChild("timestamp")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val listData = snapshot.children.mapNotNull { child ->
                        val trackingData = child.getValue(Tracking::class.java)
                        trackingData?.apply { id = child.key }
                    }
                    callback(listData)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Repository", "getAllTrackingData onCancelled", error.toException())
                    callback(emptyList())
                }
            })
    }


    fun registerObserveData(callback: (Tracking) -> Unit) {
        childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val trackingData = snapshot.getValue(Tracking::class.java)
                trackingData?.id = snapshot.key
                if (trackingData != null) {
                    callback(trackingData)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("Repository", "registerObserveData onCancelled", error.toException())
            }
        }
        database.addChildEventListener(childEventListener!!)
    }

    fun unregisterObserveData() {
        childEventListener?.let {
            database.removeEventListener(it)
        }
        childEventListener = null
    }
}
