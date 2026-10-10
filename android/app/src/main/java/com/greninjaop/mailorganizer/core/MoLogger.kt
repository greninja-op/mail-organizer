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
        if (debugEnabled) Log.d(tag, com.greninjaop.mailorganizer.core.privacy.SecuritySanitizer.sanitizeForLog(message))
    }

    fun i(tag: String, message: String) {
        if (debugEnabled) Log.i(tag, com.greninjaop.mailorganizer.core.privacy.SecuritySanitizer.sanitizeForLog(message))
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val sanitized = com.greninjaop.mailorganizer.core.privacy.SecuritySanitizer.sanitizeForLog(message)
        if (debugEnabled) Log.w(tag, sanitized, throwable) else Log.w(tag, sanitized)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, com.greninjaop.mailorganizer.core.privacy.SecuritySanitizer.sanitizeForLog(message), throwable)
    }
}
