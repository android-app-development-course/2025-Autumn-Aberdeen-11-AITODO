package io.project.finalwork.aitodo.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface OpenAIService {
    @GET
    suspend fun getModels(
        @Url url: String,
        @Header("Authorization") auth: String?
    ): ModelListResponse

    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") auth: String?,
        @Body request: ChatRequest
    ): ChatResponse
}
