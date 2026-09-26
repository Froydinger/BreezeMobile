package com.froydinger.breeze.browser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Removes the private launch-trust record when Android fully removes an installed Web App. */
class PwaInstallRecordReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_FULLY_REMOVED) return
        val packageName = intent.data?.schemeSpecificPart ?: return
        PwaTwaBridge.removeInstalledWebApp(context, packageName)
    }
}
