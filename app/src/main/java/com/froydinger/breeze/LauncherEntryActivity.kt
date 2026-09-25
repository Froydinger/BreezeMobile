package com.froydinger.breeze

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Starts the stable browser activity so theme-specific launcher aliases can change safely. */
class LauncherEntryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
