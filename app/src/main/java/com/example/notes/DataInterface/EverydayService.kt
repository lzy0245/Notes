package com.example.notes.DataInterface

import com.example.notes.DataClass.EverydayData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface EverydayService {
    @GET("en")
    fun getEverydayData(@Query("type") type: String): Call<EverydayData>
}