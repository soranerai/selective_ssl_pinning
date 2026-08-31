package dev.soranerai.netprivacy

import android.app.Application
import dev.soranerai.netprivacy.config.RemoteServiceBridge
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class NetPrivacyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NetPrivacyLog.info("application onCreate; registering LibXposed service listener")
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                NetPrivacyLog.info("LibXposed service bound api=${service.apiVersion}")
                RemoteServiceBridge.bind(service)
                RemoteServiceBridge.publish(dev.soranerai.netprivacy.data.TrustConfigStore(this@NetPrivacyApplication).read())
            }
            override fun onServiceDied(service: XposedService) { NetPrivacyLog.warn("LibXposed service died"); RemoteServiceBridge.unbind(service) }
        })
    }
}
