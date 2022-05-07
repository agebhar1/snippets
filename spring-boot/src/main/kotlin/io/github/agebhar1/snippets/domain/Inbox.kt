package io.github.agebhar1.snippets.domain

import java.time.Instant
import java.util.UUID
import org.w3c.dom.Document

data class InboxXmlMessage(
  val id: UUID,
  val occurredAtUTC: Instant,
  val processedAtUTC: Instant? = null,
  val type: String,
  val data: Document
)

interface InboxXmlMessageRepository {
  fun save(entity: InboxXmlMessage)
}
