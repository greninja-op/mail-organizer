package com.greninjaop.mailorganizer.data.local

/**
 * Enumerations for the local data layer (Phase 2).
 *
 * All enums are persisted by [MoConverters] as their [Enum.name] strings —
 * never ordinals — so reordering enum entries can never corrupt stored data.
 * Intelligence *values* (which category/priority was assigned) are stored;
 * intelligence *algorithms* are NOT implemented in Phase 2 (see phase-04+).
 */

/** Lifecycle of a connected Gmail account. OAuth itself lands in Phase 3. */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    TOKEN_EXPIRED,
    DISABLED,
}

/**
 * Mail Organizer categories (requirements.md §Categories).
 * Deterministic classification arrives in Phase 7; Phase 2 only stores.
 */
enum class MailCategory {
    ACTION_REQUIRED,
    IMPORTANT,
    CAREER,
    EDUCATION,
    RECEIPTS_ORDERS,
    SECURITY,
    NOTIFICATIONS,
    NEWSLETTERS,
    PROMOTIONS,
    LOW_VALUE,
    UNCLASSIFIED,
}

/** Where a classification/priority/action decision came from. */
enum class ClassificationSource {
    USER_CORRECTION,
    USER_RULE,
    DETERMINISTIC,
    OPTIONAL_AI,
    UNKNOWN,
}

/** Priority is independent from category (requirements.md). */
enum class Priority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL,
}

/** Types of action-required items (Phase 14 builds the engine). */
enum class ActionType {
    REPLY_REQUIRED,
    MEETING,
    DEADLINE,
    PAYMENT,
    TRAVEL,
    APPLICATION,
    REMINDER,
    OTHER,
}

/** Sync engine state per account (engine itself is Phase 4). */
enum class SyncStatus {
    IDLE,
    RUNNING,
    PAUSED,
    FAILED,
    NEVER_SYNCED,
}

/** User rule kinds (rule engine is Phase 12). */
enum class RuleType {
    SENDER_TO_CATEGORY,
    DOMAIN_TO_CATEGORY,
    COMPANY_TO_CATEGORY,
    KEYWORD_TO_CATEGORY,
    SENDER_TO_PRIORITY,
    COMPANY_TO_PRIORITY,
    SENDER_TO_ACTION_REQUIRED,
}

/** Where a user-defined rule came from (Phase 12). */
enum class RuleSource {
    /** Created manually in the Rules UI. */
    MANUAL,

    /** Created from a correction via "also create a rule". */
    FROM_CORRECTION,
}

/** What a user correction applies to. */
enum class CorrectionScope {
    SENDER,
    DOMAIN,
    COMPANY,
    MESSAGE,
}

/**
 * Which field a user correction overrides.
 *
 * ACTION_REQUIRED is reserved for the Phase 14 action-item engine; in
 * Phase 12 the user expresses action-required intent through CATEGORY
 * (correcting to/from [MailCategory.ACTION_REQUIRED]).
 */
enum class CorrectionField {
    CATEGORY,
    PRIORITY,
    ACTION_REQUIRED,
    /** Company display-name override (Phase 12; maps to CompanyRecord.userOverrideName). */
    COMPANY_NAME,
}

/**
 * Structured-extraction item kinds (extraction engine is Phase 13).
 *
 * Enums are stored by [Enum.name] (never ordinal), so adding values is
 * migration-free. The Phase 13 temporal types were appended after the
 * Phase 2 foundation values; legacy generic values (PAYMENT, APPLICATION,
 * REMINDER) remain readable and map to their temporal equivalents in
 * domain/temporal.
 */
enum class ExtractedItemType {
    MEETING,
    DEADLINE,
    PAYMENT,
    APPOINTMENT,
    TRAVEL,
    APPLICATION,
    REMINDER,
    REPLY_REQUIRED,
    // ---- Phase 13 temporal types ----
    EVENT,
    INTERVIEW,
    SUBMISSION_DEADLINE,
    APPLICATION_DEADLINE,
    PAYMENT_DEADLINE,
    REGISTRATION_DEADLINE,
    REMINDER_DATE,
    DATE_ONLY,
    TIME_ONLY,
    DATE_TIME,
    DATE_RANGE,
}
