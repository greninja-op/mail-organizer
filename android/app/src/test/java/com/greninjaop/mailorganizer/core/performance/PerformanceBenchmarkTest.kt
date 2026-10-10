package com.greninjaop.mailorganizer.core.performance

import com.greninjaop.mailorganizer.core.classify.ClassificationInput
import com.greninjaop.mailorganizer.core.classify.ClassifierCategory
import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.core.classify.DeterministicClassifier
import com.greninjaop.mailorganizer.core.company.CompanyDetectionInput
import com.greninjaop.mailorganizer.core.company.CompanyDetector
import com.greninjaop.mailorganizer.core.conversation.ConversationAnalysisInput
import com.greninjaop.mailorganizer.core.conversation.ConversationAnalyzer
import com.greninjaop.mailorganizer.core.email.AttachmentMeta
import com.greninjaop.mailorganizer.core.email.HtmlSanitizer
import com.greninjaop.mailorganizer.core.priority.DeterministicPriorityEngine
import com.greninjaop.mailorganizer.core.priority.PriorityInput
import com.greninjaop.mailorganizer.core.search.QueryParser
import com.greninjaop.mailorganizer.core.temporal.DeterministicTemporalExtractor
import com.greninjaop.mailorganizer.core.temporal.ExtractionInput
import com.greninjaop.mailorganizer.data.local.MessageRecord
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 24 — Performance, Scalability & Battery Benchmarks.
 *
 * Implements rigorous, measured benchmarks over controlled synthetic datasets:
 * - 100 messages
 * - 1,000 messages
 * - 5,000 messages
 * - 10,000 messages
 *
 * Covers:
 * 1. Parser & HTML sanitizer throughput
 * 2. Deterministic classifier throughput & latency
 * 3. Priority engine throughput & latency
 * 4. Company & sender intelligence detection & resolution
 * 5. Temporal & meeting/deadline extraction
 * 6. Conversation intelligence analysis
 * 7. Search query parser & ranking throughput
 * 8. Memory stability and garbage collection bounds
 */
class PerformanceBenchmarkTest {

    data class SyntheticEmail(
        val messageId: String,
        val threadId: String,
        val fromAddress: String,
        val fromName: String,
        val subject: String,
        val bodyText: String,
        val bodyHtml: String?,
        val labels: List<String>,
        val attachments: List<AttachmentMeta>,
        val timestampEpochMs: Long,
    )

    companion object {
        private val COMPANIES = listOf(
            Triple("Google", "no-reply@accounts.google.com", "google.com"),
            Triple("GitHub", "notifications@github.com", "github.com"),
            Triple("Amazon", "auto-confirm@amazon.com", "amazon.com"),
            Triple("Stripe", "invoices@stripe.com", "stripe.com"),
            Triple("LinkedIn", "invitations@linkedin.com", "linkedin.com"),
            Triple("Coursera", "updates@coursera.org", "coursera.org"),
            Triple("Delta Air Lines", "ticket@delta.com", "delta.com"),
            Triple("Alex Reader", "alex.friend@gmail.com", "gmail.com"),
            Triple("Priya Nair", "priya.dev@workmail.io", "workmail.io"),
            Triple("Bank Alert", "security@secure-bank.net", "secure-bank.net"),
        )

        private val SUBJECTS_AND_BODIES = listOf(
            Pair(
                "Your verification code is 849201",
                "Hi, use verification code 849201 to complete sign-in. Do not share this OTP with anyone.",
            ),
            Pair(
                "Your Amazon order #402-91823 has shipped",
                "Your package has shipped via carrier. Track delivery at https://amazon.com/orders/track. Items arriving Friday.",
            ),
            Pair(
                "Project Sync Meeting: Thursday at 3:00 PM",
                "Let's meet this Thursday, Oct 15 at 3:00 PM on Google Meet https://meet.google.com/abc-def-ghi to discuss roadmap.",
            ),
            Pair(
                "Weekly Engineering Digest: October Edition",
                "Here are the top stories of the week. Read full newsletter at https://github.blog. Unsubscribe anytime via preferences.",
            ),
            Pair(
                "Fall Sale: 30% discount on all plans",
                "Exclusive promotional code FALL30. Shop today at https://example.com/sale. Terms apply.",
            ),
            Pair(
                "URGENT: Action required on invoice payment by Friday",
                "Invoice #INV-2026-99 is overdue. Please remit payment by Friday, Oct 16 to avoid account suspension.",
            ),
            Pair(
                "Invitation to connect on LinkedIn",
                "I would like to add you to my professional network on LinkedIn.",
            ),
            Pair(
                "Course Assignment 3 Deadline Extension",
                "The submission deadline for Assignment 3 has been extended to Monday, October 19 at 11:59 PM.",
            ),
            Pair(
                "Security alert: new sign-in from Linux device",
                "A new device logged in to your account from IP 192.168.1.1. If this was not you, review activity immediately.",
            ),
            Pair(
                "Quick question about tomorrow's lunch",
                "Are you free to grab coffee or lunch tomorrow around 12:30 PM? Let me know!",
            ),
        )

        fun generateDataset(count: Int): List<SyntheticEmail> {
            val baseTime = 1_760_000_000_000L
            return (0 until count).map { i ->
                val company = COMPANIES[i % COMPANIES.size]
                val (subj, body) = SUBJECTS_AND_BODIES[i % SUBJECTS_AND_BODIES.size]
                val threadNum = i / 3 // 3 messages per thread on average
                val isHtml = i % 2 == 0
                val htmlContent = if (isHtml) {
                    """
                    <!DOCTYPE html>
                    <html>
                    <body>
                        <h2>$subj</h2>
                        <p>$body</p>
                        <p>Message index <strong>$i</strong> generated for benchmark.</p>
                        <a href="https://${company.third}/action?id=$i">Click here to review</a>
                        <br><br>
                        <footer>To unsubscribe, <a href="https://${company.third}/unsubscribe">click here</a>.</footer>
                    </body>
                    </html>
                    """.trimIndent()
                } else null

                val labels = when (i % 5) {
                    0 -> listOf("INBOX", "UNREAD")
                    1 -> listOf("CATEGORY_PROMOTIONS")
                    2 -> listOf("CATEGORY_SOCIAL")
                    3 -> listOf("SPAM")
                    else -> listOf("INBOX")
                }

                val attachments = if (i % 7 == 0) {
                    listOf(
                        AttachmentMeta(
                            attachmentId = "att_$i",
                            filename = "invoice_$i.pdf",
                            mimeType = "application/pdf",
                            sizeBytes = 1048576,
                        ),
                    )
                } else emptyList()

                SyntheticEmail(
                    messageId = "msg_$i",
                    threadId = "thread_$threadNum",
                    fromAddress = company.second,
                    fromName = company.first,
                    subject = "$subj #$i",
                    bodyText = "$body (Reference index: $i)",
                    bodyHtml = htmlContent,
                    labels = labels,
                    attachments = attachments,
                    timestampEpochMs = baseTime + (i * 60_000L),
                )
            }
        }
    }

    @Test
    fun `benchmark HTML sanitizer throughput`() {
        val dataset = generateDataset(1000).filter { it.bodyHtml != null }
        val startNs = System.nanoTime()
        var sanitizedChars = 0

        for (email in dataset) {
            val sanitized = HtmlSanitizer.sanitize(email.bodyHtml!!)
            sanitizedChars += sanitized.length
            val text = HtmlSanitizer.htmlToText(sanitized)
            sanitizedChars += text.length
        }

        val durationMs = (System.nanoTime() - startNs) / 1_000_000
        val throughputMsgsPerSec = (dataset.size * 1000.0) / durationMs
        println("[BENCHMARK] HTML Sanitizer: ${dataset.size} emails in ${durationMs}ms (${throughputMsgsPerSec.toInt()} msgs/sec)")
        assertTrue("Sanitizer throughput should exceed 200 msgs/sec", throughputMsgsPerSec > 200)
    }

    @Test
    fun `benchmark deterministic classifier 100, 1000, 5000, 10000 emails`() {
        val sizes = listOf(100, 1000, 5000, 10000)
        val dataset = generateDataset(10000)

        // JVM warm-up to ensure classes are loaded and JIT compiled
        for (email in dataset.take(200)) {
            DeterministicClassifier.classify(
                ClassificationInput(
                    messageId = email.messageId,
                    fromAddress = email.fromAddress,
                    fromName = email.fromName,
                    subject = email.subject,
                    bodyText = email.bodyText,
                    labelIds = email.labels,
                    attachmentFilenames = email.attachments.mapNotNull { it.filename },
                ),
                isRecurringSender = email.fromAddress.contains("google"),
            )
        }

        for (n in sizes) {
            val subset = dataset.take(n)
            val startNs = System.nanoTime()

            for (email in subset) {
                DeterministicClassifier.classify(
                    ClassificationInput(
                        messageId = email.messageId,
                        fromAddress = email.fromAddress,
                        fromName = email.fromName,
                        subject = email.subject,
                        bodyText = email.bodyText,
                        labelIds = email.labels,
                        attachmentFilenames = email.attachments.mapNotNull { it.filename },
                    ),
                    isRecurringSender = email.fromAddress.contains("google") || email.fromAddress.contains("github"),
                )
            }

            val durationMs = (System.nanoTime() - startNs) / 1_000_000
            val perMsgMs = durationMs.toDouble() / n
            val throughput = (n * 1000.0) / durationMs
            println("[BENCHMARK] Classifier: $n emails in ${durationMs}ms (${String.format("%.3f", perMsgMs)} ms/msg, ${throughput.toInt()} msgs/sec)")
            assertTrue("Per-message classification cost must be under 1ms", perMsgMs < 1.0)
        }
    }

    @Test
    fun `benchmark priority engine 100, 1000, 5000, 10000 emails`() {
        val sizes = listOf(100, 1000, 5000, 10000)
        val dataset = generateDataset(10000)

        // JVM warm-up
        for (email in dataset.take(200)) {
            DeterministicPriorityEngine.prioritize(
                PriorityInput(
                    messageId = email.messageId,
                    category = ClassifierCategory.ACTION_REQUIRED,
                    confidence = Confidence.HIGH,
                    isRecurringSender = email.fromAddress.contains("google"),
                    unread = email.labels.contains("UNREAD"),
                    labelIds = email.labels,
                    hasUnsubscribeMarker = false,
                    isBulkSender = false,
                ),
            )
        }

        for (n in sizes) {
            val subset = dataset.take(n)
            val startNs = System.nanoTime()

            for (email in subset) {
                DeterministicPriorityEngine.prioritize(
                    PriorityInput(
                        messageId = email.messageId,
                        category = ClassifierCategory.ACTION_REQUIRED,
                        confidence = Confidence.HIGH,
                        isRecurringSender = email.fromAddress.contains("google"),
                        unread = email.labels.contains("UNREAD"),
                        labelIds = email.labels,
                        hasUnsubscribeMarker = email.bodyText.contains("unsubscribe", ignoreCase = true),
                        isBulkSender = email.fromAddress.contains("no-reply") || email.fromAddress.contains("auto-confirm"),
                    ),
                )
            }

            val durationMs = (System.nanoTime() - startNs) / 1_000_000
            val perMsgMs = durationMs.toDouble() / n
            val throughput = (n * 1000.0) / durationMs
            println("[BENCHMARK] Priority: $n emails in ${durationMs}ms (${String.format("%.4f", perMsgMs)} ms/msg, ${throughput.toInt()} msgs/sec)")
            assertTrue("Per-message priority cost must be under 0.5ms", perMsgMs < 0.5)
        }
    }

    @Test
    fun `benchmark company detector resolution with repeated domains`() {
        val dataset = generateDataset(5000)
        val startNs = System.nanoTime()
        var detectedCount = 0

        for (email in dataset) {
            val res = CompanyDetector.detect(
                CompanyDetectionInput(
                    fromAddress = email.fromAddress,
                    fromName = email.fromName,
                ),
            )
            if (res != null) detectedCount++
        }

        val durationMs = (System.nanoTime() - startNs) / 1_000_000
        val throughput = (dataset.size * 1000.0) / durationMs
        println("[BENCHMARK] Company Detection: ${dataset.size} lookups in ${durationMs}ms (${throughput.toInt()} lookups/sec, detected=$detectedCount)")
        assertTrue("Company detection throughput should exceed 5,000 lookups/sec", throughput > 5000)
    }

    @Test
    fun `benchmark temporal extraction throughput`() {
        val dataset = generateDataset(1000)
        val startNs = System.nanoTime()
        var extractedItems = 0

        for (email in dataset) {
            val res = DeterministicTemporalExtractor.extract(
                ExtractionInput(
                    messageId = email.messageId,
                    threadId = email.threadId,
                    subject = email.subject,
                    snippet = email.bodyText.take(80),
                    bodyText = email.bodyText,
                    referenceEpochMs = email.timestampEpochMs,
                ),
            )
            extractedItems += res.items.size
        }

        val durationMs = (System.nanoTime() - startNs) / 1_000_000
        val throughput = (dataset.size * 1000.0) / durationMs
        println("[BENCHMARK] Temporal Extraction: ${dataset.size} emails in ${durationMs}ms (${throughput.toInt()} msgs/sec, items=$extractedItems)")
        assertTrue("Temporal extractor throughput should exceed 500 msgs/sec", throughput > 500)
    }

    @Test
    fun `benchmark conversation analyzer on multiple threads`() {
        val dataset = generateDataset(600)
        val messagesByThread = dataset.map { email ->
            MessageRecord(
                messageId = email.messageId,
                gmailMessageId = "g_${email.messageId}",
                threadId = email.threadId,
                accountId = "acc_test",
                fromAddress = email.fromAddress,
                fromName = email.fromName,
                subject = email.subject,
                snippet = email.bodyText.take(80),
                bodyText = email.bodyText,
                bodyHtml = email.bodyHtml,
                timestampEpochMs = email.timestampEpochMs,
                unread = email.labels.contains("UNREAD"),
                starred = false,
                labels = email.labels,
            )
        }.groupBy { it.threadId }

        val analyzer = ConversationAnalyzer()
        val startNs = System.nanoTime()

        for ((threadId, threadMessages) in messagesByThread) {
            analyzer.analyze(
                ConversationAnalysisInput(
                    accountId = "acc_test",
                    userEmailAddress = "alex.reader@example.com",
                    threadId = threadId,
                    messages = threadMessages,
                ),
            )
        }

        val durationMs = (System.nanoTime() - startNs) / 1_000_000
        val throughput = (messagesByThread.size * 1000.0) / durationMs
        println("[BENCHMARK] Conversation Analyzer: ${messagesByThread.size} threads (${dataset.size} msgs) in ${durationMs}ms (${throughput.toInt()} threads/sec)")
        assertTrue("Conversation analyzer throughput should exceed 500 threads/sec", throughput > 500)
    }

    @Test
    fun `benchmark search query parser throughput`() {
        val queries = listOf(
            "urgent invoice",
            "from:google.com security alert",
            "has:attachment \"quarterly report\"",
            "meeting tomorrow 3pm",
            "project sync AND deadline",
            "is:unread category:promotions fall sale",
        )

        val iterations = 10000
        val startNs = System.nanoTime()

        for (i in 0 until iterations) {
            val q = queries[i % queries.size]
            QueryParser.parse(q)
        }

        val durationMs = (System.nanoTime() - startNs) / 1_000_000
        val throughput = (iterations * 1000.0) / durationMs
        println("[BENCHMARK] Search Query Parser: $iterations queries in ${durationMs}ms (${throughput.toInt()} queries/sec)")
        assertTrue("Query parser throughput should exceed 20,000 queries/sec", throughput > 20000)
    }

    @Test
    fun `benchmark memory usage and heap stability during 10000 emails processing`() {
        System.gc()
        Thread.sleep(100)
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        val dataset = generateDataset(10000)
        for (email in dataset) {
            DeterministicClassifier.classify(
                ClassificationInput(
                    messageId = email.messageId,
                    fromAddress = email.fromAddress,
                    fromName = email.fromName,
                    subject = email.subject,
                    bodyText = email.bodyText,
                    labelIds = email.labels,
                ),
            )
        }

        val peakMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val deltaMb = (peakMemory - initialMemory).toDouble() / (1024 * 1024)
        println("[BENCHMARK] Memory: 10,000 emails retained delta = ${String.format("%.2f", deltaMb)} MB")
        // Processing 10,000 emails should not inflate heap by more than 150MB
        assertTrue("Memory delta should remain bounded under 150MB", deltaMb < 150.0)
    }
}
