package com.greninjaop.mailorganizer.core.ai

import java.util.Collections
import java.util.LinkedHashMap

/**
 * Thread-safe, account-isolated bounded cache for AI results (Phase 26 §43, §44, §92, §94).
 *
 * Cache entries are keyed by `(accountId, sourceId, capability, version)`.
 * Automatically purges on account removal or message modification.
 */
class AiCache(
    private val maxEntries: Int = 500,
) {
    data class Key(
        val accountId: String,
        val sourceId: String,
        val capability: AiCapability,
        val version: Int,
    )

    private val map: MutableMap<Key, AiResult<*>> = Collections.synchronizedMap(
        object : LinkedHashMap<Key, AiResult<*>>(maxEntries, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<Key, AiResult<*>>?): Boolean {
                return size > maxEntries
            }
        }
    )

    /**
     * Retrieves a cached result, or null if missing or expired.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : AiStructuredOutput> get(
        accountId: String,
        sourceId: String,
        capability: AiCapability,
        version: Int = 1,
    ): AiResult<T>? {
        val key = Key(accountId, sourceId, capability, version)
        return map[key] as? AiResult<T>
    }

    /**
     * Stores a validated result into the cache.
     */
    fun <T : AiStructuredOutput> put(result: AiResult<T>) {
        val key = Key(
            accountId = result.accountId,
            sourceId = result.sourceId,
            capability = result.capability,
            version = result.version,
        )
        map[key] = result
    }

    /**
     * Invalidates all cached AI items for a specific account (Phase 26 §92).
     * Invoked when an account is disconnected or deleted.
     */
    fun invalidateAccount(accountId: String) {
        synchronized(map) {
            val iterator = map.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.key.accountId == accountId) {
                    iterator.remove()
                }
            }
        }
    }

    /**
     * Invalidates all cached items for a specific message/thread (Phase 26 §94, §101).
     */
    fun invalidateSource(accountId: String, sourceId: String) {
        synchronized(map) {
            val iterator = map.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.key.accountId == accountId && entry.key.sourceId == sourceId) {
                    iterator.remove()
                }
            }
        }
    }

    /**
     * Clears entire cache.
     */
    fun clear() {
        map.clear()
    }

    val size: Int
        get() = map.size
}
