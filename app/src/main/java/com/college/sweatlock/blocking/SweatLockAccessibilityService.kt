package com.college.sweatlock.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.college.sweatlock.data.local.SweatLockDatabase
import kotlinx.coroutines.*

class SweatLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: SweatLockDatabase
    private var currentForegroundPackage: String? = null
    private var relockJob: Job? = null

    private val ignoredPackages = setOf(
        "com.college.sweatlock",
        "com.android.systemui",
        "com.android.settings"
    )

    override fun onCreate() {
        super.onCreate()
        db = SweatLockDatabase.getInstance(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ignoredPackages) return

        currentForegroundPackage = pkg

        serviceScope.launch {
            checkAndEnforceLock(pkg)
        }
    }

    private suspend fun checkAndEnforceLock(packageName: String) {
        val blockedApp = db.dao().getBlockedApp(packageName) ?: return
        if (!blockedApp.isBlocked) return

        val now = System.currentTimeMillis()
        if (now >= blockedApp.unlockUntilMs) {
            launchLockScreen(packageName)
        } else {
            // Schedule automatic re-lock the exact second the unlock timer expires
            val remainingMs = blockedApp.unlockUntilMs - now
            scheduleRelockTimer(packageName, remainingMs)
        }
    }

    private fun scheduleRelockTimer(packageName: String, delayMs: Long) {
        relockJob?.cancel()
        relockJob = serviceScope.launch {
            delay(delayMs + 150L)
            // If user is still inside the app when the timer expires, re-lock immediately
            val activePkg = rootInActiveWindow?.packageName?.toString() ?: currentForegroundPackage
            if (activePkg == packageName) {
                launchLockScreen(packageName)
            }
        }
    }

    private fun launchLockScreen(targetPackage: String) {
        val intent = Intent(this, LockActivity::class.java).apply {
            putExtra(LockActivity.EXTRA_BLOCKED_PACKAGE, targetPackage)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        relockJob?.cancel()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
