package io.shahinhasanov.broker.data.local

import io.shahinhasanov.broker.data.model.DeclarationLine
import io.shahinhasanov.broker.data.model.StatusEvent

interface JsonCodec {
    fun encodeLines(value: List<DeclarationLine>): String
    fun decodeLines(raw: String): List<DeclarationLine>
    fun encodeAttachments(value: List<String>): String
    fun decodeAttachments(raw: String): List<String>
    fun encodeHistory(value: List<StatusEvent>): String
    fun decodeHistory(raw: String): List<StatusEvent>
}
