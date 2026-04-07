package com.yrd.emergencylanemobile.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun createDemoApiService(baseUrl: String): DemoApiService =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DemoApiService::class.java)
