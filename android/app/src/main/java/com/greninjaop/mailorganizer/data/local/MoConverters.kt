package com.greninjaop.mailorganizer.data.local

import androidx.room.TypeConverter
import com.greninjaop.mailorganizer.core.email.AttachmentMeta
import com.greninjaop.mailorganizer.core.email.AttachmentMetaJson

/**
 * Room type converters (Phase 2).
 *
 * - Enums are stored by [Enum.name] (never ordinal) so enum reordering is safe.
 * - String lists use the ASCII unit separator (U+001F) as delimiter. It cannot
 *   legally appear in email addresses, domains, or label names, unlike commas
 *   or semicolons. Typed columns are preferred over serialized blobs wherever
 *   the data is queried (§25 of the phase contract); lists are only used for
 *   genuinely multi-valued, non-queried attributes (recipients, labels, …).
 */
class MoConverters {

    // ---- String lists ----

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        value.joinToString(SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    // ---- Attachment metadata (Phase 5) ----
    // JSON blob: attachments are write-once/read-with-message metadata, never
    // a query predicate, so a serialized column is appropriate here.

    @TypeConverter
    fun fromAttachmentList(value: List<AttachmentMeta>): String =
        AttachmentMetaJson.encode(value)

    @TypeConverter
    fun toAttachmentList(value: String): List<AttachmentMeta> =
        AttachmentMetaJson.decode(value)

    // ---- Enums (one pair per enum; Room needs concrete signatures) ----

    @TypeConverter fun fromConnectionState(v: ConnectionState): String = v.name
    @TypeConverter fun toConnectionState(v: String): ConnectionState = ConnectionState.valueOf(v)

    @TypeConverter fun fromMailCategory(v: MailCategory): String = v.name
    @TypeConverter fun toMailCategory(v: String): MailCategory = MailCategory.valueOf(v)

    @TypeConverter fun fromClassificationSource(v: ClassificationSource): String = v.name
    @TypeConverter fun toClassificationSource(v: String): ClassificationSource =
        ClassificationSource.valueOf(v)

    @TypeConverter fun fromPriority(v: Priority): String = v.name
    @TypeConverter fun toPriority(v: String): Priority = Priority.valueOf(v)

    @TypeConverter fun fromActionType(v: ActionType): String = v.name
    @TypeConverter fun toActionType(v: String): ActionType = ActionType.valueOf(v)

    @TypeConverter fun fromSyncStatus(v: SyncStatus): String = v.name
    @TypeConverter fun toSyncStatus(v: String): SyncStatus = SyncStatus.valueOf(v)

    @TypeConverter fun fromRuleType(v: RuleType): String = v.name
    @TypeConverter fun toRuleType(v: String): RuleType = RuleType.valueOf(v)

    @TypeConverter fun fromCorrectionScope(v: CorrectionScope): String = v.name
    @TypeConverter fun toCorrectionScope(v: String): CorrectionScope = CorrectionScope.valueOf(v)

    @TypeConverter fun fromCorrectionField(v: CorrectionField): String = v.name
    @TypeConverter fun toCorrectionField(v: String): CorrectionField = CorrectionField.valueOf(v)

    @TypeConverter fun fromExtractedItemType(v: ExtractedItemType): String = v.name
    @TypeConverter fun toExtractedItemType(v: String): ExtractedItemType =
        ExtractedItemType.valueOf(v)

    private companion object {
        const val SEPARATOR = ""
    }
}
