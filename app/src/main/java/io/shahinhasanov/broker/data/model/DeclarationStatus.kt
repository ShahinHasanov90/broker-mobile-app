package io.shahinhasanov.broker.data.model

enum class DeclarationStatus {
    DRAFT,
    SUBMITTED,
    ACCEPTED,
    REJECTED,
    RELEASED;

    val isTerminal: Boolean
        get() = this == RELEASED || this == REJECTED

    val isAwaitingBroker: Boolean
        get() = this == DRAFT
}
