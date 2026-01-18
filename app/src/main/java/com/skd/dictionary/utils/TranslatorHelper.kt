package com.skd.dictionary.utils

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslatorHelper {

    private val translators = mutableMapOf<String, Translator>()

    fun translate(
        text: String,
        targetLanguage: String,
        onDownloading: () -> Unit,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val translator = translators.getOrPut(targetLanguage) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(targetLanguage)
                .build()
            Translation.getClient(options)
        }

        onDownloading()

        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }

    fun closeAll() {
        translators.values.forEach { it.close() }
        translators.clear()
    }
}

