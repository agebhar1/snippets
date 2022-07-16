package io.github.agebhar1.snippets.domain

import org.w3c.dom.Document

import java.time.Instant
import java.util.UUID

data class InboxXmlMessage(
    val id: UUID,
    val occurredAtUtc: Instant,
    val processedAtUtc: Instant? = null,
    val type: String,
    val data: Document
)

interface InboxXmlMessageRepository {
    fun save(entity: InboxXmlMessage)
}
