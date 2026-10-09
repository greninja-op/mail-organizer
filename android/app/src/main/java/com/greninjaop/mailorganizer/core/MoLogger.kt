package com.greninjaop.mailorganizer.core

import android.util.Log

/**
 * Safe logging facade (Phase 0 logging rules).
 *
 * MUST NEVER log: email bodies/subjects, OAuth tokens, refresh tokens,
 * authorization headers, passwords, or full message content.
 * MAY log: lifecycle events, sync state transitions, non-sensitive
 * diagnostics, error categories, performance measurements.
 *
 * Debug/verbose output is enabled only on debuggable builds; release builds
 * emit warnings and errors only. Sensitive logs must be removable from
 * release builds — this facade is the single choke point for that.
 */
object MoLogger {

    @Volatile
    private var debugEnabled: Boolean = false

    fun init(isDebug: Boolean) {
        debugEnabled = isDebug
    }

    fun d(tag: String, message: String) {
        if (debugEnabled) Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        if (debugEnabled) Log.i(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (debugEnabled) Log.w(tag, message, throwable) else Log.w(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
}
