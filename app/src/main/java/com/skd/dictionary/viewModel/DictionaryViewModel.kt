package com.skd.dictionary.viewModel

import androidx.lifecycle.*
import com.skd.dictionary.repository.DictionaryRepository
import kotlinx.coroutines.launch

class DictionaryViewModel : ViewModel() {

    private val repository = DictionaryRepository()

    private val _wordDetails =
        MutableLiveData<List<DictionaryResponse>>()

    val wordDetails: LiveData<List<DictionaryResponse>> =
        _wordDetails

    private val _error =
        MutableLiveData<String>()

    val error: LiveData<String> = _error

    fun getWordDetails(word: String) {
        viewModelScope.launch {
            val result = repository.fetchWordDetails(word)

            result.onSuccess {
                _wordDetails.value = it
            }.onFailure {
                _error.value = it.message
            }
        }
    }
}
