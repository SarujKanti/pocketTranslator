package com.skd.dictionary.repository

import com.skd.dictionary.service.RetrofitClient
import com.skd.dictionary.viewModel.DictionaryResponse

class DictionaryRepository {

    suspend fun fetchWordDetails(word: String): Result<List<DictionaryResponse>> {
        return try {
            val response =
                RetrofitClient.api.getWordDetails(word)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Word not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
