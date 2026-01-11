package com.skd.dictionary.service

import com.skd.dictionary.viewModel.DictionaryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface DictionaryApi {

    @GET("entries/en/{word}")
    suspend fun getWordDetails(
        @Path("word") word: String
    ): Response<List<DictionaryResponse>>
}