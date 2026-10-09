package com.greninjaop.mailorganizer.core

/**
 * Account identity — the first-class data boundary of the whole app.
 *
 * Every account-owned entity (emails, sync state, rules, intelligence, prefs)
 * must be keyed by [AccountId]. Code that handles account-owned data must take
 * an [AccountId] explicitly so accounts can never be mixed accidentally.
 */
@JvmInline
value class AccountId(val value: String) {
    init {
        require(value.isNotBlank()) { "AccountId must not be blank" }
    }
}

/**
 * Namespaces a preference/setting key to one account. Account-scoped storage
 * keys MUST go through here (or an equivalent) — never string-concatenate
 * account ids ad hoc.
 */
fun scopedKey(accountId: AccountId, name: String): String {
    require(name.isNotBlank()) { "key name must not be blank" }
    return "account_${accountId.value}_$name"
}
