package io.shahinhasanov.broker.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeclarationDto(
    val id: String,
    val reference: String,
    @Json(name = "declarant_name") val declarantName: String,
    val operator: String,
    val status: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    val lines: List<DeclarationLineDto>,
    val attachments: List<String>,
    val history: List<StatusEventDto>
)

@JsonClass(generateAdapter = true)
data class DeclarationLineDto(
    @Json(name = "line_number") val lineNumber: Int,
    @Json(name = "hs_code") val hsCode: String,
    val description: String,
    @Json(name = "origin_country") val originCountry: String,
    val quantity: String,
    val unit: String,
    @Json(name = "customs_value") val customsValue: String,
    val currency: String
)

@JsonClass(generateAdapter = true)
data class StatusEventDto(
    val status: String,
    @Json(name = "occurred_at") val occurredAt: String,
    val actor: String,
    val note: String? = null
)

@JsonClass(generateAdapter = true)
data class ApprovalRequestDto(
    @Json(name = "idempotency_key") val idempotencyKey: String,
    val note: String? = null
)

@JsonClass(generateAdapter = true)
data class RejectionRequestDto(
    @Json(name = "idempotency_key") val idempotencyKey: String,
    val reason: String
)
