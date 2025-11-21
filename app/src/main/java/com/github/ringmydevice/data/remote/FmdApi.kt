package com.github.ringmydevice.data.remote

import retrofit2.Response
import retrofit2.http.GET

interface FmdApi {
    // A simple endpoint to check if server is alive
    @GET("health")
    suspend fun checkHealth(): Response<Void>
}