package io.shahinhasanov.broker.data.model

import java.time.Instant

data class Declaration(
    val id: String,
    val reference: String,
    val declarantName: String,
    val operator: String,
    val status: DeclarationStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lines: List<DeclarationLine>,
    val attachments: List<String>,
    val history: List<StatusEvent>
) {
    val lineCount: Int get() = lines.size
    val hsChapterSummary: String
        get() = lines
            .map { it.hsCode.take(2) }
            .distinct()
            .sorted()
            .joinToString(prefix = "HS ", separator = ", ")
            .ifBlank { "-" }
}
