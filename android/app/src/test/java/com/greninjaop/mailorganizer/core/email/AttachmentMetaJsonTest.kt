package com.greninjaop.mailorganizer.core.email

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Codec tests (Phase 5): attachment metadata survives the DB round-trip. */
class AttachmentMetaJsonTest {

    @Test
    fun `empty list encodes to empty string and back`() {
        assertEquals("", AttachmentMetaJson.encode(emptyList()))
        assertEquals(emptyList<AttachmentMeta>(), AttachmentMetaJson.decode(""))
        assertEquals(emptyList<AttachmentMeta>(), AttachmentMetaJson.decode("   "))
    }

    @Test
    fun `round trip preserves all fields`() {
        val list = listOf(
            AttachmentMeta("doc.pdf", "application/pdf", 1234L, "att-1"),
            AttachmentMeta(null, "image/png", 0L, null),
        )
        val decoded = AttachmentMetaJson.decode(AttachmentMetaJson.encode(list))
        assertEquals(list, decoded)
    }

    @Test
    fun `special characters are escaped and restored`() {
        val list = listOf(
            AttachmentMeta("quote \"name\".pdf\nline2", "application/pdf", 10L, null),
        )
        val decoded = AttachmentMetaJson.decode(AttachmentMetaJson.encode(list))
        assertEquals(list, decoded)
    }

    @Test
    fun `malformed json decodes to empty list without throwing`() {
        assertEquals(emptyList<AttachmentMeta>(), AttachmentMetaJson.decode("not json"))
        assertEquals(emptyList<AttachmentMeta>(), AttachmentMetaJson.decode("[{"))
        assertEquals(emptyList<AttachmentMeta>(), AttachmentMetaJson.decode("[1,2]"))
        assertTrue(AttachmentMetaJson.decode("null").isEmpty())
    }

    @Test
    fun `unknown fields are ignored`() {
        val decoded = AttachmentMetaJson.decode(
            """[{"filename":"a","mimeType":"t","sizeBytes":1,"attachmentId":null,"extra":"x"}]""",
        )
        assertEquals(1, decoded.size)
        assertEquals("a", decoded[0].filename)
    }
}
