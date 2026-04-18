package io.shahinhasanov.broker.data.model

import java.time.Instant

data class StatusEvent(
    val status: DeclarationStatus,
    val occurredAt: Instant,
    val actor: String,
    val note: String? = null
)
