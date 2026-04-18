package io.shahinhasanov.broker.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BrokerApi {

    @GET("declarations")
    suspend fun listDeclarations(
        @Query("status") status: String? = null
    ): List<DeclarationDto>

    @GET("declarations/{id}")
    suspend fun declaration(@Path("id") id: String): DeclarationDto

    @POST("declarations/{id}/approve")
    suspend fun approve(
        @Path("id") id: String,
        @Body body: ApprovalRequestDto
    ): DeclarationDto

    @POST("declarations/{id}/reject")
    suspend fun reject(
        @Path("id") id: String,
        @Body body: RejectionRequestDto
    ): DeclarationDto
}
