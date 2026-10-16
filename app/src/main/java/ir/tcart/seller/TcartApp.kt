package ir.tcart.seller

import android.app.Application
import ir.tcart.seller.notify.PaidNotifier

class TcartApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PaidNotifier.ensureChannels(this)
    }
}
