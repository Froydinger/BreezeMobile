package com.froydinger.breeze.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.froydinger.breeze.LauncherEntryActivity

/** Keep the single Breeze launcher mark in sync with the app's light or dark theme. */
object AppIconManager {
    private const val DARK_ALIAS = "IconClassicDarkActivity"
    private const val LIGHT_ALIAS = "IconClassicLightActivity"

    fun applyTheme(context: Context, dark: Boolean): Boolean {
        val manager = context.packageManager
        val target = component(context, if (dark) DARK_ALIAS else LIGHT_ALIAS)
        val other = component(context, if (dark) LIGHT_ALIAS else DARK_ALIAS)
        return try {
            if (isEnabled(manager, target) && !isEnabled(manager, other)) return true
            manager.setComponentEnabledSetting(target, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            manager.setComponentEnabledSetting(other, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun component(context: Context, alias: String) =
        // The manifest component class stays in the Gradle namespace even when the
        // dev build adds `.dev` to its installed application ID.
        ComponentName(context.packageName, "${LauncherEntryActivity::class.java.packageName}.$alias")

    @Suppress("DEPRECATION")
    private fun isEnabled(manager: PackageManager, component: ComponentName): Boolean = when (
        manager.getComponentEnabledSetting(component)
    ) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
        else -> manager.getActivityInfo(component, PackageManager.GET_META_DATA).enabled
    }
}
