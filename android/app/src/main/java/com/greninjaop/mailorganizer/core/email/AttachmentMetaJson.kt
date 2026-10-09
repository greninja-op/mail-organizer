package com.greninjaop.mailorganizer.core.email

/**
 * Minimal JSON codec for `List<AttachmentMeta>` (Phase 5).
 *
 * The attachment list is persisted as a JSON string column (see
 * `MoConverters`), because it is write-once/read-with-message metadata —
 * never a query predicate (per the Phase 2 converter rationale, typed
 * columns are reserved for queried attributes).
 *
 * Deliberately dependency-free (no kotlinx.serialization in the catalog):
 * the shape is fixed and flat, so a small hand-rolled codec is auditable
 * and sufficient. Both directions are total — malformed input decodes to an
 * empty list rather than throwing.
 *
 * Wire shape:
 * `[{"filename":"a.pdf","mimeType":"application/pdf","sizeBytes":123,"attachmentId":"att1"}]`
 * Nullable fields encode as JSON `null`.
 */
object AttachmentMetaJson {

    fun encode(list: List<AttachmentMeta>): String {
        if (list.isEmpty()) return ""
        val sb = StringBuilder("[")
        list.forEachIndexed { index, a ->
            if (index > 0) sb.append(',')
            sb.append('{')
            sb.append("\"filename\":").append(jsonString(a.filename)).append(',')
            sb.append("\"mimeType\":").append(jsonString(a.mimeType)).append(',')
            sb.append("\"sizeBytes\":").append(a.sizeBytes.coerceAtLeast(0)).append(',')
            sb.append("\"attachmentId\":").append(jsonString(a.attachmentId))
            sb.append('}')
        }
        sb.append(']')
        return sb.toString()
    }

    fun decode(json: String): List<AttachmentMeta> {
        if (json.isBlank()) return emptyList()
        return try {
            Parser(json).parseArray()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun jsonString(value: String?): String {
        if (value == null) return "null"
        val sb = StringBuilder("\"")
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') {
                    sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(c)
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    /** Tiny recursive-descent parser limited to this codec's flat shape. */
    private class Parser(private val text: String) {
        private var pos = 0

        fun parseArray(): List<AttachmentMeta> {
            skipWs()
            expect('[')
            val out = mutableListOf<AttachmentMeta>()
            skipWs()
            if (peek() == ']') {
                pos++
                return out
            }
            while (true) {
                out.add(parseObject())
                skipWs()
                when (peek()) {
                    ',' -> {
                        pos++
                        skipWs()
                    }
                    ']' -> {
                        pos++
                        return out
                    }
                    else -> throw IllegalArgumentException("expected , or ]")
                }
            }
        }

        private fun parseObject(): AttachmentMeta {
            skipWs()
            expect('{')
            var filename: String? = null
            var mimeType = ""
            var sizeBytes = 0L
            var attachmentId: String? = null
            skipWs()
            if (peek() == '}') {
                pos++
                return AttachmentMeta(filename, mimeType, sizeBytes, attachmentId)
            }
            while (true) {
                skipWs()
                val key = parseString()
                skipWs()
                expect(':')
                skipWs()
                when (key) {
                    "filename" -> filename = parseNullableString()
                    "mimeType" -> mimeType = parseNullableString().orEmpty()
                    "sizeBytes" -> sizeBytes = parseNumber()
                    "attachmentId" -> attachmentId = parseNullableString()
                    else -> skipValue()
                }
                skipWs()
                when (peek()) {
                    ',' -> {
                        pos++
                    }
                    '}' -> {
                        pos++
                        return AttachmentMeta(filename, mimeType, sizeBytes, attachmentId)
                    }
                    else -> throw IllegalArgumentException("expected , or }")
                }
            }
        }

        private fun parseNullableString(): String? {
            skipWs()
            if (text.startsWith("null", pos)) {
                pos += 4
                return null
            }
            return parseString()
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < text.length) {
                val c = text[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= text.length) throw IllegalArgumentException("bad escape")
                        when (val e = text[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > text.length) {
                                    throw IllegalArgumentException("bad unicode escape")
                                }
                                val hex = text.substring(pos, pos + 4)
                                sb.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw IllegalArgumentException("bad escape \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
            throw IllegalArgumentException("unterminated string")
        }

        private fun parseNumber(): Long {
            val start = pos
            while (pos < text.length && (text[pos].isDigit() || text[pos] == '-')) pos++
            if (start == pos) throw IllegalArgumentException("expected number")
            return text.substring(start, pos).toLong()
        }

        private fun skipValue() {
            skipWs()
            when (peek()) {
                '"' -> parseString()
                '{' -> {
                    var depth = 0
                    while (pos < text.length) {
                        when (text[pos]) {
                            '"' -> parseString()
                            '{' -> {
                                depth++
                                pos++
                            }
                            '}' -> {
                                pos++
                                depth--
                                if (depth == 0) return
                            }
                            else -> pos++
                        }
                    }
                }
                '[' -> {
                    var depth = 0
                    while (pos < text.length) {
                        when (text[pos]) {
                            '"' -> parseString()
                            '[' -> {
                                depth++
                                pos++
                            }
                            ']' -> {
                                pos++
                                depth--
                                if (depth == 0) return
                            }
                            else -> pos++
                        }
                    }
                }
                else -> {
                    while (pos < text.length && !",}]".contains(text[pos])) pos++
                }
            }
        }

        private fun skipWs() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        private fun peek(): Char {
            if (pos >= text.length) throw IllegalArgumentException("unexpected end")
            return text[pos]
        }

        private fun expect(c: Char) {
            skipWs()
            if (pos >= text.length || text[pos] != c) {
                throw IllegalArgumentException("expected '$c'")
            }
            pos++
        }
    }
}
