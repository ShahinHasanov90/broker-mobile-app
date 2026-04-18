package io.shahinhasanov.broker.data.model

import java.math.BigDecimal

data class DeclarationLine(
    val lineNumber: Int,
    val hsCode: String,
    val description: String,
    val originCountry: String,
    val quantity: BigDecimal,
    val unit: String,
    val customsValue: BigDecimal,
    val currency: String
)
