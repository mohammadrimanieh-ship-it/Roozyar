package ir.roozyaar.planner

import android.app.Application

class RoozYaarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
